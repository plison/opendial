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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.ConditionalCategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.Effect;
import opendial.state.anchoring.AnchoredRule;
import opendial.state.anchoring.Output;


/**
 * Discrete probability distribution based on a rule specification (which can be for
 * an update rule or a prediction rule).
 * 
 * <p>The distribution exploits a cache to speed up the inference.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleDistribution implements DiscreteDistribution {

	// logger
	public static Logger log = new Logger("RuleDistribution", Logger.Level.DEBUG);

	String id;
	
	AnchoredRule arule;

	ConditionalCategoricalTable cache;

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
		
		if (rule.getParameters().isEmpty()) {
			cache = new ConditionalCategoricalTable();
		}
	}


	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		cache = new ConditionalCategoricalTable();
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
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {

		CategoricalTable outputTable = getOutputTable(condition);

		if (cache!= null && cache.hasProb(condition, head)) {
			return cache.getProb(condition, head);
		}
		
		double prob = outputTable.getProb(head);
		
		if (cache != null) {
			cache.addRow(condition, head, prob);
		}
		
		return prob;
	}




	/**
	 * Returns the probability table associated with the given input assignment
	 * 
	 * @param condition the conditional assignment
	 * @return the associated probability table (as a CategoricalTable)
	 * @throws DialException 
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition) throws DialException {

		if (cache != null && cache.hasProbTable(condition)) {
			return cache.getPosterior(condition);
		}
		
		CategoricalTable outputTable = getOutputTable(condition);
		
		if (cache != null) {
			cache.addRows(condition, outputTable);
		}	
		return outputTable;
	}
	
	
	@Override
	public CategoricalTable getPartialPosterior(Assignment condition) throws DialException {
		return getPosterior(condition);
	}


	@Override
	public Set<Assignment> getValues(ValueRange range) throws DialException {
		Set<Assignment> vals = new HashSet<Assignment>();
		for (Effect e : arule.getEffects()) {
			vals.add(new Assignment(arule.getId(), e));
		}
		return vals;
	}



	/**
	 * Samples one possible output value given the input assignment
	 * 
	 * @param condition the input assignment
	 * @return the sampled value
	 * @throws DialException if sampling returned an error
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {

		if (cache != null && cache.hasProbTable(condition)) {
			return cache.sample(condition);
		}
		
		CategoricalTable outputTable = getOutputTable(condition);
		
		if (cache != null) {
			cache.addRows(condition, outputTable);
		}
		
		Assignment sample = outputTable.sample();
		return sample;
	}


	/**
	 * Returns a singleton set with the label of the anchored rule
	 * 
	 * @return a singleton set with the label of the anchored rule
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>(Arrays.asList(id));
		return headVars;
	}


	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns itself
	 * 
	 * @return itself
	 */
	@Override
	public DiscreteDistribution toDiscrete() {
		return this;
	}



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


	
	/**
	 * Returns discrete if the rule does not contain parameters, and hybrid otherwise.
	 */
	@Override
	public DistribType getPreferredType() {
		if (arule.getParameters().isEmpty()) {
			return DistribType.DISCRETE;
		}
		else {
			return DistribType.HYBRID;
		}
	}

	// ===================================
	//  PRIVATE METHODS
	// ===================================




	private CategoricalTable getOutputTable(Assignment input) {
		try {
			// search for the matching case	
			Assignment ruleInput = input.getTrimmed(arule.getInputs().getVariables());
			Output output = arule.getRule().getOutput(ruleInput);
			
			// creating the distribution
			double totalMass = 	 output.getTotalMass(input);
			CategoricalTable probTable = new CategoricalTable();
			if (totalMass < 0.99) {
				probTable.addRow(new Assignment(id, new Effect()), 1.0 - totalMass);
				totalMass = 1.0;
			}	
			for (Effect e : output.getEffects()) {
				double param = output.getParameter(e).getParameterValue(input) / totalMass;
				if (param > 0) {
					probTable.addRow(new Assignment(id, e), param);
				}
			}

			if (probTable.isEmpty()) {
				log.warning("probability table is empty (no effects) for "
						+ "input " +	input + " and rule " + arule.toString());
			}
			return probTable;
		}
		
		catch (DialException e) {
			log.warning("could not extract output table for condition " + input + ": " + e.toString());
			log.debug("rule is " + arule);
			CategoricalTable probTable = new CategoricalTable();
			probTable.addRow(new Assignment(id, new Effect()), 1.0);
			return probTable;
		}
	}


}
