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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.modules.StatePruner;

/**
 * Representation of a rule case, containing a condition and a list of alternative
 * effects if the condition holds. Each alternative effect has a distinct probability
 * or utility parameter.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleCase {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// pointer to the rule containing the case
	Rule rule;

	// the condition for the case
	final Condition condition;

	// the list of alternative effects, together with their probability/utility
	protected Map<Effect, Parameter> effects;

	// ===================================
	// CASE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new case, with a void condition and an empty list of effects
	 */
	public RuleCase(Rule rule) {
		this.rule = rule;
		this.condition = new VoidCondition();
		effects = new HashMap<Effect, Parameter>();
	}

	/**
	 * Creates a new case, with a given condition and an empty list of effects
	 */
	public RuleCase(Rule rule, Condition condition) {
		this.rule = rule;
		this.condition = condition;
		effects = new HashMap<Effect, Parameter>();
	}

	/**
	 * Adds an new effect and its associated probability/utility to the case
	 * 
	 * @param effect the effect
	 * @param param the effect's probability or utility
	 */
	public void addEffect(Effect effect, double param) {
		addEffect(effect, new FixedParameter(param));
	}

	/**
	 * Adds a new effect and its associated parameter to the case
	 * 
	 * @param effect the effect
	 * @param param the parameter for the effect's probability or utility
	 */
	public void addEffect(Effect effect, Parameter param) {
		effects.put(effect, param);
	}

	/**
	 * Removes an effect from the rule case
	 * 
	 * @param e the effect to remove
	 */
	public void removeEffect(Effect e) {
		effects.remove(e);
	}

	/**
	 * Returns a grounded version of the rule case, based on the grounding
	 * assignment.
	 * 
	 * @param grounding the grounding associated with the filled values
	 * @return the grounded copy of the case.
	 */
	public RuleCase ground(Assignment grounding) {
		RuleCase groundCase = new RuleCase(this.rule, this.condition);
		for (Effect e : effects.keySet()) {
			Effect groundedEffect = e.ground(grounding);
			if (!groundedEffect.getSubEffects().isEmpty()
					|| e.getSubEffects().isEmpty()) {
				Parameter param = effects.get(e);
				if (param instanceof ComplexParameter) {
					param = ((ComplexParameter) param).ground(grounding);
				}
				groundCase.addEffect(groundedEffect, param);
			}
		}
		if (rule.getRuleType() == RuleType.PROB) {
			groundCase.pruneEffects();
			groundCase.addVoidEffect();
		}

		return groundCase;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the type of the case
	 * 
	 * @return the type
	 */
	public RuleType getRuleType() {
		return rule.getRuleType();
	}

	/**
	 * Returns all the effects specified in the case.
	 * 
	 * @return the set of effects
	 */
	public Set<Effect> getEffects() {
		return effects.keySet();
	}

	/**
	 * Returns the parameter associated with the effect. If the effect is not part of
	 * the case, returns null.
	 * 
	 * @param e the effect
	 * @return the parameter associated with the effect.
	 */
	public Parameter getParameter(Effect e) {
		return effects.get(e);
	}

	/**
	 * Returns the condition for the case
	 * 
	 * @return the condition
	 */
	public Condition getCondition() {
		return condition;
	}

	/**
	 * Returns true if the case is void (empty or with a single void effect)
	 * 
	 * @return true if void, false otherwise
	 */
	public boolean isVoid() {
		return effects.isEmpty()
				|| (rule.getRuleType() == RuleType.PROB && effects.size() == 1 && effects
						.containsKey(new Effect()));
	}

	/**
	 * Returns the input variables for the case, composed of the input variables for
	 * the condition, plus the additional input variables for the effects.
	 * 
	 * @return the set of input variables for the case
	 */
	public Set<Template> getInputVariables() {
		Set<Template> inputVariables = new HashSet<Template>();
		inputVariables.addAll(condition.getInputVariables());
		for (Effect effect : effects.keySet()) {
			for (String inputVariable : effect.getValueSlots()) {
				inputVariables.add(new Template(inputVariable));
			}
		}
		return inputVariables;
	}

	/**
	 * Returns the set of output variables for the case, as defined in the effects
	 * associated with the case condition.
	 * 
	 * @return the set of output variables defined in the case's effects
	 */
	public Set<String> getOutputVariables() {
		return effects.keySet().stream()
				.flatMap(e -> e.getOutputVariables().stream())
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the possible groundings for the case, based on the provided input
	 * assignment.
	 * 
	 * @param input the input assignment
	 * @return the set of possible groundings
	 */
	public RuleGrounding getGroundings(Assignment input) {
		RuleGrounding grounding = condition.getGroundings(input);

		if (rule.getRuleType() == RuleType.UTIL) {
			boolean hasActionVars =
					getOutputVariables().stream().allMatch(
							v -> input.containsVar(v + "'"));
			for (Effect e : effects.keySet()) {
				if (hasActionVars) {
					Condition co = e.convertToCondition();
					RuleGrounding effectGrounding = co.getGroundings(input);
					grounding.add(effectGrounding);
				}
				else {
					Set<String> slots = e.getValueSlots();
					slots.removeAll(input.getVariables());
					grounding.add(Assignment.createOneValue(slots,
							ValueFactory.create("")));
				}
			}
		}
		return grounding;
	}

	/**
	 * Returns the mapping between effects and parameters for the case.
	 * 
	 * @return the mapping effects: parameters
	 */
	public Map<Effect, Parameter> getEffectMap() {
		return effects;
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

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
		for (Effect e : effects.keySet()) {
			str += e.toString();
			str += " [" + effects.get(e) + "]";
			str += ",";
		}
		if (!effects.isEmpty()) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * Returns the hashcode for the case
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return condition.hashCode() - 2 * effects.hashCode();
	}

	/**
	 * Returns true if the object is a identical rule case, and false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof RuleCase && condition.equals(((RuleCase) o).condition) && effects
				.equals(((RuleCase) o).effects));
	}

	// ===================================
	// PROTECTED METHODS
	// ===================================

	/**
	 * Prunes all effects whose parameter is lower than the provided threshold. This
	 * only works for fixed parameters.
	 * 
	 */
	protected void pruneEffects() {
		for (Effect e : new HashSet<Effect>(effects.keySet())) {
			Parameter p = effects.get(e);
			if (p instanceof FixedParameter
					&& ((FixedParameter) p).getValue() < StatePruner.VALUE_PRUNING_THRESHOLD) {
				effects.remove(e);
			}
		}
	}

	/**
	 * Adds a void effect to the rule if the fixed mass is lower than 0.99. Does not
	 * do anything if the rule contains unknown parameters or already contains an
	 * empty effect.
	 */
	private void addVoidEffect() {
		double fixedMass = 0;
		for (Entry<Effect, Parameter> e : effects.entrySet()) {
			Effect eff = e.getKey();
			Parameter param = e.getValue();
			if (eff.length() == 0 || (!(param instanceof FixedParameter))) {
				return;
			}
			else {
				fixedMass += ((FixedParameter) param).getValue();
			}
		}

		if (fixedMass < 0.99) {
			FixedParameter param = new FixedParameter(1 - fixedMass);
			addEffect(new Effect(), param);
		}
	}

}
