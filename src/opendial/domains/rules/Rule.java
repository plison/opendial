// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.Parameter;

/**
 * Generic representation of a probabilistic rule, with an identifier and an
 * ordered list of cases. The rule can be either a probability or a utility
 * rule.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Rule {

	static Logger log = new Logger("Rule", Logger.Level.DEBUG);

	// the rule identifier
	String id;

	// ordered list of cases
	List<RuleCase> cases;

	public enum RuleType {
		PROB, UTIL
	}

	RuleType ruleType;

	// cache with the outputs for a given assignment
	public static final int MAX_CACHE_SIZE = 500;
	Map<Assignment, RuleOutput> cache;

	// ===================================
	// RULE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new rule, with the given identifier and type, and an empty list
	 * of cases
	 * 
	 * @param id the identifier
	 * @param ruleType the rule type
	 */
	public Rule(String id, RuleType ruleType) {
		this.id = id;
		this.ruleType = ruleType;
		cases = new ArrayList<RuleCase>();
		cache = new HashMap<Assignment, RuleOutput>(10);
		cache = Collections.synchronizedMap(cache);
	}

	/**
	 * Adds a new case to the abstract rule
	 * 
	 * @param newCase the new case to add
	 */
	public void addCase(RuleCase newCase) {
		if (!cases.isEmpty()
				&& cases.get(cases.size() - 1).getCondition() instanceof VoidCondition) {
			log.info("new case for rule " + id
					+ " is unreachable (previous case is trivially true)");
		}
		cases.add(newCase);
	}

	/**
	 * Sets the priority level of the rule (1 being the highest)
	 * 
	 * @param priority the priority level
	 */
	public void setPriority(int priority) {
		List<RuleCase> newCases = new ArrayList<RuleCase>();
		for (RuleCase c : cases) {
			RuleCase newCase = new RuleCase(c.getCondition());
			for (Effect e : c.getEffects()) {
				Parameter param = c.getParameter(e);
				List<BasicEffect> newEffects = new ArrayList<BasicEffect>();
				for (BasicEffect e2 : e.getSubEffects()) {
					newEffects.add(e2.changePriority(priority));
				}
				newCase.addEffect(new Effect(newEffects), param);
			}
			newCases.add(newCase);
		}
		cases = newCases;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the rule identifier
	 * 
	 * @return the rule identifier
	 */
	public String getRuleId() {
		return id;
	}

	/**
	 * Returns the input variables (possibly underspecified, with slots to fill)
	 * for the rule
	 * 
	 * @return the set of labels for the input variables
	 */
	public Set<Template> getInputVariables() {
		Set<Template> inputVars = new HashSet<Template>();
		for (RuleCase c : cases) {
			inputVars.addAll(c.getInputVariables());
		}
		return inputVars;
	}

	/**
	 * Returns the first case whose condition matches the input assignment
	 * provided as argument. The case contains the grounded list of effects
	 * associated with the satisfied condition.
	 * 
	 * @param input the input assignment
	 * @return the matched rule case.
	 */
	public RuleOutput getOutput(Assignment input) {

		if (cache != null) {
			RuleOutput v = cache.get(input);
			if (v != null) {
				return v;
			}
		}
		RuleOutput output = new RuleOutput(ruleType);
		RuleGrounding groundings = getGroundings(input);

		for (Assignment g : groundings.getAlternatives()) {
			Assignment full = !(g.isEmpty()) ? new Assignment(input, g) : input;
			RuleCase match = cases.stream()
					.filter(c -> c.getCondition().isSatisfiedBy(full))
					.map(c -> c.ground(full)).findFirst()
					.orElse(new RuleCase());

			if (!match.getEffects().isEmpty()) {
				output.addCase(match);
			}
		}

		if (cache != null) {
			cache.put(input, output);
			if (cache != null && cache.size() > MAX_CACHE_SIZE) {
				cache = null;
			}
		}
		return output;
	}

	/**
	 * Returns the rule type
	 * 
	 * @return the rule type
	 */
	public RuleType getRuleType() {
		return ruleType;
	}

	/**
	 * Returns the set of groundings that can be derived from the rule and the
	 * specific input assignment.
	 * 
	 * @param input the input assignment
	 * @return the possible groundings for the rule
	 */
	public RuleGrounding getGroundings(Assignment input) {
		RuleGrounding groundings = new RuleGrounding();
		for (RuleCase c : cases) {
			RuleGrounding caseGrounding = c.getGroundings(input,
					ruleType == RuleType.UTIL);
			groundings.add(caseGrounding);
		}
		return groundings;
	}

	/**
	 * Returns the set of all (templated) output variables defined inside the
	 * rule
	 * 
	 * @return the set of all possible output variables
	 */
	public Set<String> getOutputVariables() {
		return cases.stream().flatMap(c -> c.getOutputVariables().stream())
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the set of all parameter identifiers employed in the rule
	 * 
	 * @return the set of parameter identifiers
	 */
	public Set<String> getParameterIds() {
		Set<String> params = new HashSet<String>();
		for (RuleCase c : cases) {
			for (Effect e : c.getEffects()) {
				params.addAll(c.getParameter(e).getVariables());
			}
		}
		return params;
	}

	/**
	 * Returns the set of all possible effects in the rule.
	 * 
	 * @return the set of all possible effects
	 */
	public Set<Effect> getEffects() {
		Set<Effect> effects = new HashSet<Effect>();
		for (RuleCase c : cases) {
			effects.addAll(c.getEffects());
		}
		return effects;
	}

	/**
	 * Returns the list of conditions in the rule
	 * 
	 * @return the conditions
	 */
	public List<Condition> getConditions() {
		return cases.stream().map(c -> c.getCondition())
				.collect(Collectors.toList());
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns a string representation for the rule
	 */
	@Override
	public String toString() {
		String str = id + ": ";
		for (RuleCase theCase : cases) {
			if (!theCase.equals(cases.get(0))) {
				str += "\telse ";
			}
			str += theCase.toString() + "\n";
		}
		if (!cases.isEmpty()) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * Returns the hashcode for the rule
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return this.getClass().hashCode() - id.hashCode() + cases.hashCode();
	}

	/**
	 * Returns true if o is a rule that has the same identifier, rule type and
	 * list of cases than the current rule.
	 * 
	 * @param o the object to compare
	 * @return true if the object is an identical rule, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Rule) {
			return id.equals(((Rule) o).getRuleId())
					&& ruleType.equals(((Rule) o).getRuleType())
					&& cases.equals(((Rule) o).cases);
		}
		return false;
	}

}
