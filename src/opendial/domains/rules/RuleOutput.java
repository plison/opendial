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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;

/**
 * Representation of a particular output derived from the application of a
 * probabilistic rule. The output essentially contains a (parametrised)
 * distribution over possible effects. If the rule contains multiple groundings,
 * the output is a merge (joint probability distribution for a probability rule,
 * add table for a utility rule) of the rule case for every possible groundings.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class RuleOutput extends RuleCase {

	// the rule type
	RuleType type;

	/**
	 * Creates an empty output for a particular rule type.
	 * 
	 * @param type the rule type
	 */
	public RuleOutput(RuleType type) {
		super();
		this.type = type;
	}

	/**
	 * Adds a rule case to the output. The result is a joint probability
	 * distribution in the case of a probability rule, and an add table in the
	 * case of a utility rule.
	 * 
	 * @param newCase the new rule case to add
	 */
	public void addCase(RuleCase newCase) {

		if (type == RuleType.PROB) {
			newCase.pruneEffects();
		}

		if (newCase.getEffects().isEmpty()) {
			return;
		}

		if (effects.hashCode() == newCase.getEffectMap().hashCode()) {
			return;
		}

		if (effects.isEmpty()) {
			effects.putAll(newCase.getEffectMap());
		}

		else if (type == RuleType.PROB) {
			Map<Effect, Parameter> newOutput = new HashMap<Effect, Parameter>();

			addVoidEffect();
			newCase.addVoidEffect();

			for (Effect o : effects.keySet()) {
				Parameter param = effects.get(o);
				for (Effect o2 : newCase.getEffects()) {
					Parameter newParam = newCase.getParameter(o2);

					Collection<BasicEffect> effectsList = new ArrayList<BasicEffect>(
							o.getSubEffects());
					effectsList.addAll(o2.getSubEffects());
					Effect newEffect = new Effect(effectsList);
					Parameter mergeParam = multiplyParameter(param, newParam);
					if (!newOutput.containsKey(newEffect)) {
						newOutput.put(newEffect, mergeParam);
					} else {
						Parameter addParam = sumParameter(
								newOutput.get(newEffect), mergeParam);
						newOutput.put(newEffect, addParam);
					}
				}
			}
			effects = newOutput;
			newCase.pruneEffects();
		} else if (type == RuleType.UTIL) {
			for (Effect o2 : newCase.getEffects()) {
				if (effects.containsKey(o2)) {
					Parameter mergeParam = sumParameter(effects.get(o2),
							newCase.getParameter(o2));
					effects.put(o2, mergeParam);
				} else {
					effects.put(o2, newCase.getParameter(o2));
				}
			}
		}
	}

	/**
	 * Returns the total probability mass specified by the output (possibly
	 * given an assignment of parameter values).
	 * 
	 * @param input input assignment (with parameters values)
	 * @return the corresponding mass
	 * @throws DialException if some parameters could not be found.
	 */
	public double getTotalMass(Assignment input) {
		return effects.values().stream()
				.mapToDouble(p -> p.getParameterValue(input)).sum();
	}

	/**
	 * Creates a new parameter out of the summation of p1 and p2
	 * 
	 * @param p1 the first parameter
	 * @param p2 the second parameter
	 * @return the parameter that equals p1 + p2
	 */
	private Parameter sumParameter(Parameter p1, Parameter p2) {
		if (p1 instanceof FixedParameter && p2 instanceof FixedParameter) {
			double sum = ((FixedParameter) p1).getParameterValue()
					+ ((FixedParameter) p2).getParameterValue();
			return new FixedParameter(sum);
		} else {
			String p1str = (p1 instanceof SingleParameter) ? "{" + p1 + "}"
					: p1.toString();
			String p2str = (p2 instanceof SingleParameter) ? "{" + p2 + "}"
					: p2.toString();
			Set<String> unknowns = new HashSet<String>();
			unknowns.addAll(p1.getVariables());
			unknowns.addAll(p2.getVariables());
			return new ComplexParameter(p1str + "+" + p2str, unknowns);
		}
	}

	/**
	 * Creates a new parameter out of the multiplication of p1 and p2
	 * 
	 * @param p1 the first parameter
	 * @param p2 the second parameter
	 * @return the parameter that equals p1 * p2
	 */
	private Parameter multiplyParameter(Parameter p1, Parameter p2) {
		if (p1 instanceof FixedParameter && p2 instanceof FixedParameter) {
			double sum = ((FixedParameter) p1).getParameterValue()
					* ((FixedParameter) p2).getParameterValue();
			return new FixedParameter(sum);
		} else {
			String p1str = (p1 instanceof SingleParameter) ? "{" + p1 + "}"
					: p1.toString();
			String p2str = (p2 instanceof SingleParameter) ? "{" + p2 + "}"
					: p2.toString();
			Set<String> unknowns = new HashSet<String>();
			unknowns.addAll(p1.getVariables());
			unknowns.addAll(p2.getVariables());
			return new ComplexParameter(p1str + "*" + p2str, unknowns);
		}
	}

}