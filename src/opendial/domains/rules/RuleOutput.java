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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.datastructs.Assignment;
import opendial.datastructs.MathExpression;
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
	// OUTPUT CONSTRUCTION
	// ===================================

	/**
	 * Creates a new output, with a void condition and an empty list of effects
	 * 
	 * @param type the type of the rule
	 */
	public RuleOutput(RuleType type) {
		this.type = type;
		effects = new HashMap<Effect, Parameter>();
	}

	/**
	 * Adds an new effect and its associated probability/utility to the output'
	 * 
	 * @param effect the effect
	 * @param param the effect's probability or utility
	 */
	public void addEffect(Effect effect, double param) {
		addEffect(effect, new FixedParameter(param));
	}

	/**
	 * Adds a new effect and its associated parameter to the output
	 * 
	 * @param effect the effect
	 * @param param the parameter for the effect's probability or utility
	 */
	public void addEffect(Effect effect, Parameter param) {
		effects.put(effect, param);
	}

	/**
	 * Removes an effect from the rule output
	 * 
	 * @param e the effect to remove
	 */
	public void removeEffect(Effect e) {
		effects.remove(e);
	}

	/**
	 * Returns a grounded version of the rule output, based on the grounding
	 * assignment.
	 * 
	 * @param grounding the grounding associated with the filled values
	 * @return the grounded copy of the output.
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
	 * Adds a rule output to the current one. The result is a joint probability
	 * distribution in the output of a probability rule, and an addition of utility
	 * tables in the case of a utility rule.
	 * 
	 * @param newCase the new rule case to add
	 */
	public void addOutput(RuleOutput newCase) {

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
					Parameter newParam = mergeParameters(param1, param2, '*');
					if (newOutput.containsKey(newEffect)) {
						newParam = mergeParameters(newOutput.get(newEffect),
								newParam, '+');
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
					param2 = mergeParameters(effects.get(o2), param2, '+');
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
		return effects.isEmpty() || (type == RuleType.PROB && effects.size() == 1
				&& effects.containsKey(new Effect()));
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
	 * Returns the collection of parameters used in the output
	 * 
	 * @return the parameters
	 */
	public Collection<Parameter> getParameters() {
		return effects.values();
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
	// PROTECTED AND PRIVATE METHODS
	// ===================================

	/**
	 * Prunes all effects whose parameter is lower than the provided threshold. This
	 * only works for fixed parameters.
	 * 
	 */
	protected void pruneEffects() {
		for (Effect e : new HashSet<Effect>(effects.keySet())) {
			Parameter p = effects.get(e);
			if (p instanceof FixedParameter && ((FixedParameter) p)
					.getValue() < StatePruner.VALUE_PRUNING_THRESHOLD) {
				effects.remove(e);
			}
		}
	}

	/**
	 * Adds a void effect to the rule if the fixed mass is lower than 1.0 and a void
	 * effect is not already defined.
	 */
	private void addVoidEffect() {

		// case 1: if there are no effects, insert a void one with prob.1
		if (effects.isEmpty()) {
			addEffect(new Effect(), new FixedParameter(1));
			return;
		}
		double fixedMass = 0;
		for (Effect e : effects.keySet()) {

			// case 2: if there is already a void effect, do nothing
			if (e.length() == 0) {
				return;
			}

			// sum up the fixed probability mass
			Parameter param = effects.get(e);
			if (param instanceof FixedParameter) {
				fixedMass += ((FixedParameter) param).getValue();
			}
		}

		// case 3: if the fixed probability mass is = 1, do nothing
		if (fixedMass > 0.99) {
			return;
		}

		// case 4: if the fixed probability mass is < 1, fill the remaining mass
		else if (fixedMass > 0.0) {
			addEffect(new Effect(), new FixedParameter(1 - fixedMass));
		}

		// case 5: in case the rule output is structured via single or complex
		// parameters p1, p2,... pn, create a new complex effect = 1 - (p1+p2+...pn)
		// that fill the remaining probability mass
		else {
			MathExpression[] params = effects.values().stream()
					.map(p -> p.getExpression()).toArray(s -> new MathExpression[s]);
			MathExpression one = new MathExpression("1");
			MathExpression negation = one.combine('-', params);
			addEffect(new Effect(), new ComplexParameter(negation));
		}
	}

	/**
	 * Merges the two parameters and returns the merged parameter
	 * 
	 * @param p1 the first parameter
	 * @param p2 the second parameter
	 * @param operator the operator, such as +, * or -
	 * @return the resulting parameter
	 */
	private static Parameter mergeParameters(Parameter p1, Parameter p2,
			char operator) {

		// if the two parameters are fixed, simply create a new fixed parameter
		if (p1 instanceof FixedParameter && p2 instanceof FixedParameter) {
			double v1 = ((FixedParameter) p1).getValue();
			double v2 = ((FixedParameter) p2).getValue();
			switch (operator) {
			case '+':
				return new FixedParameter(v1 + v2);
			case '*':
				return new FixedParameter(v1 * v2);
			case '-':
				return new FixedParameter(v1 - v2);
			default:
				throw new RuntimeException(operator + " is unsupported");
			}
		}

		// otherwise, create a complex parameter
		MathExpression exp1 = p1.getExpression();
		MathExpression exp2 = p2.getExpression();
		return new ComplexParameter(exp1.combine(operator, exp2));

	}

}
