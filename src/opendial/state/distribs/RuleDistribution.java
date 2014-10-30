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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.RuleOutput;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.Effect;
import opendial.state.AnchoredRule;


/**
 * Discrete probability distribution based on a rule specification (which can be for
 * an update rule or a prediction rule).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleDistribution implements ProbDistribution {

	// logger
	public static Logger log = new Logger("RuleDistribution", Logger.Level.DEBUG);

	String id;

	AnchoredRule arule;

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================


	/**
	 * Creates a new rule-base distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not an update or a prediction rule
	 */
	public RuleDistribution(AnchoredRule rule) throws DialException {
		if (rule.getRule().getRuleType() == RuleType.PROB) {
			this.arule = rule;
		}
		else {
			throw new DialException("only probabilistic rules can define a " +
					"rule-based probability distribution");
		}
		id = rule.getRule().getRuleId();

	}


	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (id.equals(oldId)) {
			id = newId;
		}
	}



	/**
	 * Does nothing
	 */
	@Override
	public void pruneValues(double threshold) {
		return;
	}


	// ===================================
	//  GETTERS
	// ===================================



	/**
	 * Returns the probability for P(head|condition), where head is 
	 * an assignment of an output value for the rule node.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the probability
	 * @throws DialException if the probability could not be calculated.
	 */
	@Override
	public double getProb(Assignment condition, Value head) throws DialException {

		CategoricalTable outputTable = getProbDistrib(condition);
		double prob = outputTable.getProb(head);

		return prob;
	}




	/**
	 * Returns the probability table associated with the given input assignment
	 * 
	 * @param condition the conditional assignment
	 * @return the associated probability table (as a CategoricalTable)
	 * @throws DialException if the distribution could not be calculated.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition) throws DialException {
		return new MarginalDistribution(this, condition);
	}


	/**
	 * Returns the possible values for the rule.
	 * 
	 * @param range the range of input values (is ignored).
	 */
	@Override
	public Set<Value> getValues(ValueRange range) throws DialException {
		return new HashSet<Value>(arule.getEffects());
	}



	/**
	 * Samples one possible output value given the input assignment
	 * 
	 * @param condition the input assignment
	 * @return the sampled value
	 * @throws DialException if sampling returned an error
	 */
	@Override
	public Value sample(Assignment condition) throws DialException {

		CategoricalTable outputTable = getProbDistrib(condition);	
		return outputTable.sample();
	}


	/**
	 * Returns the label of the anchored rule
	 * 
	 * @return the label of the anchored rule
	 */
	@Override
	public String getVariable() {
		return id;
	}


	@Override
	public CategoricalTable getProbDistrib(Assignment input) throws DialException {

		// search for the matching case

		Assignment ruleInput = input.getTrimmed(arule.getInputs().getVariables());
		RuleOutput output = arule.getRule().getOutput(ruleInput);

		// creating the distribution
		double totalMass = output.getTotalMass(input);
		CategoricalTable probTable = new CategoricalTable(id, false);
		if (totalMass < 0.99) {
			probTable.addRow(new Effect(), 1.0 - totalMass);
			totalMass = 1.0;
		}	
		
		for (Effect e : output.getEffects()) {
			double param = output.getParameter(e).getParameterValue(input) / totalMass;
			if (param > 0) {
				probTable.addRow(e, param);
			}
		}

		if (probTable.isEmpty()) {
			log.warning("probability table is empty (no effects) for "
					+ "input " +	ruleInput + " and rule " + arule.toString());
		}

		return probTable;
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
	public RuleDistribution copy() {
		try { 
			RuleDistribution distrib = new RuleDistribution (arule);
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
		return arule.toString();
	}




}
