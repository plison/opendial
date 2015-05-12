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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;

/**
 * Representation of a particular output derived from the application of a
 * probabilistic rule. The output essentially contains a (parametrised) distribution
 * over possible effects. If the rule contains multiple groundings, the output is a
 * merge (joint probability distribution for a probability rule, add table for a
 * utility rule) of the rule case for every possible groundings.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class RuleOutput extends RuleCase {

	/**
	 * Creates an empty output for a particular rule type.
	 * 
	 * @param type the rule type
	 */
	public RuleOutput(Rule rule) {
		super(rule);
	}

	/**
	 * Returns the set of (effect,parameters) pairs in the rule output
	 * 
	 * @return the set of associated pairs
	 */
	public Set<Map.Entry<Effect, Parameter>> getPairs() {
		return effects.entrySet();
	}

	/**
	 * Adds a rule case to the output. The result is a joint probability distribution
	 * in the case of a probability rule, and an add table in the case of a utility
	 * rule.
	 * 
	 * @param newCase the new rule case to add
	 */
	public void addCase(RuleCase newCase) {

		if (isVoid()) {
			effects.clear();
			effects.putAll(newCase.getEffectMap());
		}

		else if (newCase.isVoid()) {
			return;
		}

		else if (effects.hashCode() == newCase.effects.hashCode()) {
			return;
		}

		else if (rule.getRuleType() == RuleType.PROB) {

			Map<Effect, Parameter> newOutput = new HashMap<Effect, Parameter>();
			if (rule.getRuleId().equals("rule5")) {
				log.info("cur " + effects);
				log.info("new " + newCase);
			}
			for (Effect o : effects.keySet()) {
				Parameter param = effects.get(o);
				for (Effect o2 : newCase.getEffects()) {
					Parameter newParam = newCase.getParameter(o2);
					Effect newEffect = new Effect(o, o2);
					Parameter mergeParam = multiplyParameter(param, newParam);
					if (!newOutput.containsKey(newEffect)) {
						newOutput.put(newEffect, mergeParam);
					}
					else {
						Parameter addParam =
								sumParameter(newOutput.get(newEffect), mergeParam);
						newOutput.put(newEffect, addParam);
					}
				}
			}
			effects = newOutput;
			newCase.pruneEffects();
		}
		else if (rule.getRuleType() == RuleType.UTIL) {
			for (Effect o2 : newCase.getEffects()) {
				if (effects.containsKey(o2)) {
					Parameter mergeParam =
							sumParameter(effects.get(o2), newCase.getParameter(o2));
					effects.put(o2, mergeParam);
				}
				else {
					effects.put(o2, newCase.getParameter(o2));
				}
			}
		}
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

	/**
	 * Creates a new parameter out of the summation of p1 and p2
	 * 
	 * @param p1 the first parameter
	 * @param p2 the second parameter
	 * @return the parameter that equals p1 + p2
	 */
	private Parameter sumParameter(Parameter p1, Parameter p2) {
		if (p1 instanceof FixedParameter && p2 instanceof FixedParameter) {
			double sum =
					((FixedParameter) p1).getValue()
							+ ((FixedParameter) p2).getValue();
			return new FixedParameter(sum);
		}
		else {
			String p1str =
					(p1 instanceof SingleParameter) ? "{" + p1 + "}" : p1.toString();
			String p2str =
					(p2 instanceof SingleParameter) ? "{" + p2 + "}" : p2.toString();
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
			double sum =
					((FixedParameter) p1).getValue()
							* ((FixedParameter) p2).getValue();
			return new FixedParameter(sum);
		}
		else {
			String p1str =
					(p1 instanceof SingleParameter) ? "{" + p1 + "}" : p1.toString();
			String p2str =
					(p2 instanceof SingleParameter) ? "{" + p2 + "}" : p2.toString();
			Set<String> unknowns = new HashSet<String>();
			unknowns.addAll(p1.getVariables());
			unknowns.addAll(p2.getVariables());
			return new ComplexParameter(p1str + "*" + p2str, unknowns);
		}
	}

}