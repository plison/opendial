// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.bn.distribs.discrete;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.state.rules.AnchoredRule;
import opendial.state.rules.Rule.RuleType;

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
public class RuleDistribution implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("RuleDistribution", Logger.Level.DEBUG);

	// An anchored rule
	AnchoredRule rule;

	String id;

	// a cache with the output values in a simple table
	DiscreteProbabilityTable cache;



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
			this.rule = rule;
		}
		else {
			throw new DialException("only probabilistic rules can define a " +
					"rule-based probability distribution");
		}
		id = rule.getId();
		cache = new DiscreteProbabilityTable();
	}


	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		cache.modifyVarId(oldId, newId);
		if (id.equals(oldId)) {
			id = newId;
		}
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

		if (rule.getParameterNodes().isEmpty()) {
			if (!cache.hasProbTable(condition)) {
				SimpleTable outputTable = getOutputTable(condition);
				cache.addRows(new Assignment(condition), outputTable);
			}
			return cache.getProb(condition, head);
		}

		else {
			return getOutputTable(condition).getProb(head);
		}
	}


	/**
	 * Returns true if a probability is explicitly defined for P(head|condition),
	 * where head is an assignment of an output value for the rule node.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return true if a probability is explicitly defined, false otherwise
	 */
	@Override
	public boolean hasProb(Assignment condition, Assignment head) {

		if (rule.getParameterNodes().isEmpty()) {
			if (!cache.hasProbTable(condition)) {
				SimpleTable outputTable = getOutputTable(condition);
				cache.addRows(new Assignment(condition), outputTable);
			}
			return cache.hasProb(condition, head);
		}

		else {
			return getOutputTable(condition).hasProb(head);
		}
	}






	/**
	 * Returns the probability table associated with the given input assignment
	 * 
	 * @param condition the conditional assignment
	 * @return the associated probability table (as a SimpleTable)
	 */
	@Override
	public SimpleTable getProbTable(Assignment condition) {
		if (rule.getParameterNodes().isEmpty()) {
			if (!cache.hasProbTable(condition)) {
				SimpleTable outputTable = getOutputTable(condition);
				cache.addRows(new Assignment(condition), outputTable);
			}
			return cache.getProbTable(condition);
		}

		else {
			return getOutputTable(condition);
		}
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

		if (rule.getParameterNodes().isEmpty()) {
			synchronized (this) {
			if (!cache.hasProbTable(condition)) {
				SimpleTable outputTable = getOutputTable(condition);
				cache.addRows(new Assignment(condition), outputTable);
			}
			return cache.sample(condition);
			}
		}

		else {
			return getOutputTable(condition).sample();
		}
		
	}


	/**
	 * Returns a singleton set with the label of the anchored rule
	 * 
	 * @return a singleton set with the label of the anchored rule
	 */
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
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}

	/**
	 * Throws an exception (the distribution cannot be converted)
	 * 
	 * @throws DialException always thrown
	 */
	@Override
	public ContinuousProbDistribution toContinuous()
			throws DialException {
		throw new DialException ("cannot convert to a continuous distribution");
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
		try { return new RuleDistribution (rule); } 
		catch (DialException e) { e.printStackTrace(); return null; }
	}



	/**
	 * Returns the pretty print for the rule
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		return rule.toString();
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




	private SimpleTable getOutputTable(Assignment condition) {
		try {
			OutputTable outputs = rule.getEffectOutputs(condition);

			Map<Output,Double> probs = outputs.getProbTable(condition);

			SimpleTable probTable = new SimpleTable();
			
			for (Output o : probs.keySet()) {
				probTable.addRow(new Assignment(id, o), probs.get(o));
			}
			if (probTable.isEmpty()) {
				log.warning("probability table is empty (no effects) for input " +
						condition + " and rule " + rule.toString());
			}
			return probTable;
		}
		catch (DialException e) {
			log.warning("could not extract output table for condition " + condition + ": " + e.toString());
			log.debug("rule is " + rule.toString());
			SimpleTable probTable = new SimpleTable();
			probTable.addRow(new Assignment(id, new Output()), 1.0);
			return probTable;
		}
	}


}
