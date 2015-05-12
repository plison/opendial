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

import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.modules.StatePruner;

/**
 * Representation of a rule output, consisting of a set of alternative (mutually
 * exclusive) effects, each being associated with a particular probability or utility
 * parameter.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleOutput {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// rule type
	RuleType type;

	// the list of alternative effects, together with their probability/utility
	protected Map<Effect, Parameter> effects;

	// ===================================
	// CASE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new case, with a void condition and an empty list of effects
	 */
	public RuleOutput(RuleType type) {
		this.type = type;
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
	public RuleOutput ground(Assignment grounding) {
		RuleOutput groundCase = new RuleOutput(type);
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
		if (type == RuleType.PROB) {
			groundCase.pruneEffects();
			groundCase.addVoidEffect();
		}

		return groundCase;
	}

	/**
	 * Adds a rule case to the current one. The result is a joint probability
	 * distribution in the case of a probability rule, and an add table in the case
	 * of a utility rule.
	 * 
	 * @param newCase the new rule case to add
	 */
	public void addCase(RuleOutput newCase) {

		if (isVoid()) {
			effects = newCase.effects;
		}
		else if (newCase.isVoid()
				|| effects.hashCode() == newCase.effects.hashCode()) {
			return;
		}

		else if (type == RuleType.PROB) {

			Map<Effect, Parameter> newOutput = new HashMap<Effect, Parameter>();

			for (Effect o : effects.keySet()) {
				Parameter param1 = effects.get(o);
				for (Effect o2 : newCase.getEffects()) {
					Parameter param2 = newCase.getParameter(o2);
					Effect newEffect = new Effect(o, o2);
					Parameter newParam = param1.multiply(param2);
					if (newOutput.containsKey(newEffect)) {
						newParam = newOutput.get(newEffect).sum(newParam);
					}
					newOutput.put(newEffect, newParam);
				}
			}
			effects = newOutput;
			newCase.pruneEffects();
		}
		else if (type == RuleType.UTIL) {
			for (Effect o2 : newCase.getEffects()) {
				Parameter param2 = newCase.getParameter(o2);
				if (effects.containsKey(o2)) {
					param2 = param2.sum(effects.get(o2));
				}
				effects.put(o2, newCase.getParameter(o2));
			}
		}
	}

	// ===================================
	// GETTERS
	// ===================================

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
	 * Returns true if the case is void (empty or with a single void effect)
	 * 
	 * @return true if void, false otherwise
	 */
	public boolean isVoid() {
		return effects.isEmpty()
				|| (type == RuleType.PROB && effects.size() == 1 && effects
						.containsKey(new Effect()));
	}

	/**
	 * Returns the set of output variables for the case, as defined in the effects
	 * associated with the case condition. The output variables are appended with a '
	 * suffix.
	 * 
	 * @return the set of output variables defined in the case's effects
	 */
	public Set<String> getOutputVariables() {
		return effects.keySet().stream()
				.flatMap(e -> e.getOutputVariables().stream()).map(o -> o + "'")
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the set of (effect,parameters) pairs in the rule case
	 * 
	 * @return the set of associated pairs
	 */
	public Set<Map.Entry<Effect, Parameter>> getPairs() {
		return effects.entrySet();
	}

	/**
	 * Returns the total probability mass specified by the output (possibly given an
	 * assignment of parameter values).
	 * 
	 * @param input input assignment (with parameters values)
	 * @return the corresponding mass
	 */
	public double getTotalMass(Assignment input) {
		return effects.values().stream().mapToDouble(p -> p.getValue(input)).sum();
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
		return -2 * effects.hashCode();
	}

	/**
	 * Returns true if the object is a identical rule case, and false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof RuleOutput && effects.equals(((RuleOutput) o).effects));
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
