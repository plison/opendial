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

package opendial.state.distribs;

import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.UtilityFunction;
import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.RuleOutput;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.effects.Effect;
import opendial.state.AnchoredRule;


/**
 * Utility distribution based on a rule specification.
 *  *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleUtilDistribution implements UtilityFunction {

	// logger
	public static Logger log = new Logger("RuleUtilDistribution", Logger.Level.DEBUG);

	// A rule
	AnchoredRule rule;

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================


	/**
	 * Creates a new rule-based utility distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not a decision rule
	 */
	public RuleUtilDistribution(AnchoredRule rule) throws DialException {

		if ((rule.getRule().getRuleType() == RuleType.UTIL)) {
			this.rule = rule;
		}
		else {
			throw new DialException("only utility rules can define a " +
					"rule-based utility distribution");
		}

	}




	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		return;
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the utility for Q(input), where input is the assignment
	 * of values for both the chance nodes and the action nodes
	 * 
	 * @param fullInput the value assignment
	 * @return the corresponding utility
	 */
	@Override
	public double getUtil(Assignment fullInput) {

		Set<String> outputVars = rule.getOutputVariables();
		Assignment input = fullInput.getTrimmedInverse(outputVars);
		Assignment actions = fullInput.getTrimmed(outputVars);
		
		return getUtil(input, actions);
	}

	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns true
	 * @return true
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	/**
	 * Returns a copy of the distribution
	 * 
	 * @return the copy
	 */
	@Override
	public RuleUtilDistribution copy() {
		try { 
			RuleUtilDistribution distrib = new RuleUtilDistribution (rule);
			return distrib;
		} 
		catch (DialException e) { e.printStackTrace(); return null; }
	}


	/**
	 * Returns the pretty print for the rule
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		return rule.toString();
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Returns the utility of the action assignment given the particular input.
	 * 
	 * @param input the input assignment
	 * @param actions the action assignment
	 * @return the resulting utility
	 */
	private double getUtil(Assignment input, Assignment actions) {

			Assignment ruleInput = input.getTrimmed(rule.getInputs().getVariables());
			Assignment ruleAction = actions.removePrimes();

			double totalUtil = 0;
			Assignment fullInput = new Assignment(ruleInput, actions);
			RuleOutput output = rule.getRule().getOutput(fullInput);	
			for (Effect effectOutput : output.getEffects()) {
				Condition effectCondition = effectOutput.convertToCondition();
				if (effectCondition.isSatisfiedBy(ruleAction)) {
					totalUtil += output.getParameter(effectOutput).
							getParameterValue(input);
				}
			}

			return totalUtil;
	}




}
