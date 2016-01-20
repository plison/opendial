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

import java.util.logging.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.templates.Template;

/**
 * Generic representation of a probabilistic rule, with an identifier and an ordered
 * list of cases. The rule can be either a probability or a utility rule.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Rule {

	final static Logger log = Logger.getLogger("OpenDial");

	// the rule identifier
	String id;

	// ordered list of cases
	List<RuleCase> cases;

	public enum RuleType {
		PROB, UTIL
	}

	RuleType ruleType;

	// ===================================
	// RULE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new rule, with the given identifier and type, and an empty list of
	 * cases
	 * 
	 * @param id the identifier
	 * @param ruleType the rule type
	 */
	public Rule(String id, RuleType ruleType) {
		this.id = id;
		this.ruleType = ruleType;
		cases = new ArrayList<RuleCase>();
	}

	/**
	 * Adds a new case to the abstract rule
	 * 
	 * @param condition the condition
	 * @param output the corresponding output
	 */
	public void addCase(Condition condition, RuleOutput output) {
		if (!cases.isEmpty()
				&& cases.get(cases.size() - 1).condition instanceof VoidCondition) {
			log.warning("unreachable case for rule " + id
					+ "(previous case trivially true)");
		}

		// ensuring that the probability values are between 0 and 1
		if (ruleType == RuleType.PROB) {
			double totalMass = output.getParameters().stream()
					.filter(p -> p instanceof FixedParameter)
					.mapToDouble(p -> ((FixedParameter) p).getValue()).peek(p -> {
						if (p < 0.0) throw new RuntimeException(p + " is < 0.0");
					}).sum();
			if (totalMass > 1.02) {
				throw new RuntimeException(totalMass + " is > 1.0");
			}
		}
		cases.add(new RuleCase(condition, output));
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
	 * Returns the input variables (possibly underspecified, with slots to fill) for
	 * the rule
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
	 * Returns the first rule output whose condition matches the input assignment
	 * provided as argument. The output contains the grounded list of effects
	 * associated with the satisfied condition.
	 * 
	 * @param input the input assignment
	 * @return the matched rule output.
	 */
	public RuleOutput getOutput(Assignment input) {
 
		RuleOutput output = new RuleOutput(ruleType);
		RuleGrounding groundings = getGroundings(input);
		for (Assignment g : groundings.getAlternatives()) {

			Assignment full = !(g.isEmpty()) ? new Assignment(input, g) : input;

			RuleOutput match = cases.stream()
					.filter(c -> c.condition.isSatisfiedBy(full)).map(c -> c.output)
					.findFirst().orElse(new RuleOutput(ruleType));

			match = match.ground(full);
			output.addOutput(match);

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
	 * Returns the set of all parameter identifiers employed in the rule
	 * 
	 * @return the set of parameter identifiers
	 */
	public Set<String> getParameterIds() {
		Set<String> params = new HashSet<String>();
		for (RuleCase c : cases) {
			for (Effect e : c.getEffects()) {
				params.addAll(c.output.getParameter(e).getVariables());
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
	 * Returns the set of groundings that can be derived from the rule and the
	 * specific input assignment.
	 * 
	 * @param input the input assignment
	 * @return the possible groundings for the rule
	 */
	private RuleGrounding getGroundings(Assignment input) {
		RuleGrounding groundings = new RuleGrounding();
		for (RuleCase c : cases) {
			groundings.add(c.getGroundings(input));
		}
		return groundings;
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
	 * Returns true if o is a rule that has the same identifier, rule type and list
	 * of cases than the current rule.
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

	/**
	 * Representation of a rule case, i.e. a condition associated with a rule output
	 *
	 */
	final class RuleCase {

		// the condition for the rule (can be a VoidCondition)
		final Condition condition;

		// the associated output for the case
		final RuleOutput output;

		/**
		 * Creates a new rule case with a condition and an output
		 * 
		 * @param condition the condition
		 * @param output the associated output
		 */
		public RuleCase(Condition condition, RuleOutput output) {
			this.condition = condition;
			this.output = output;
		}

		/**
		 * Returns the set of effects for the rule case
		 * 
		 * @return the set of effects
		 */
		public Set<Effect> getEffects() {
			return output.getEffects();
		}

		/**
		 * Returns the groundings associated with the rule case, given the input
		 * assignment
		 * 
		 * @param input the input assignment
		 * @return the resulting groundings
		 */
		public RuleGrounding getGroundings(Assignment input) {
			RuleGrounding groundings = new RuleGrounding();
			groundings.add(condition.getGroundings(input));

			if (ruleType == RuleType.UTIL) {
				boolean actionVars = input.containsVars(output.getOutputVariables());
				for (Effect e : getEffects()) {
					if (actionVars) {
						Condition co = e.convertToCondition();
						RuleGrounding effectGrounding = co.getGroundings(input);
						groundings.add(effectGrounding);
					}
					else {
						Set<String> slots = e.getValueSlots();
						slots.removeAll(input.getVariables());
						condition.getInputVariables().forEach(v -> slots.remove(v.toString()));
						groundings.add(Assignment.createOneValue(slots, ""));
					}
				}
			}

			for (Effect e : getEffects()) {
				for (String randomToGenerate : e.getRandomsToGenerate()) {
					groundings.extend(new Assignment(randomToGenerate,
							(new Random()).nextInt(99999)));
				}
			}
			return groundings;
		}

		/**
		 * Returns the input variables associated with the case
		 * 
		 * @return the set of input variables
		 */
		public Set<Template> getInputVariables() {
			Set<Template> inputVars = new HashSet<Template>();
			inputVars.addAll(condition.getInputVariables());
			for (Effect effect : getEffects()) {
				for (String inputVariable : effect.getValueSlots()) {
					inputVars.add(Template.create(inputVariable));
				}
			}
			return inputVars;
		}

		/**
		 * Returns a string representation of the rule case.
		 */
		@Override
		public String toString() {
			String str = "";
			if (!(condition instanceof VoidCondition)) {
				str += "if (" + condition.toString() + ") then ";
			}
			else {
				str += " ";
			}
			str += output.toString();
			return str;
		}

	}

}
