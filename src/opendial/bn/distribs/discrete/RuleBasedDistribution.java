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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.statechange.AnchoredRule;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.StochasticParameter;

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
public class RuleBasedDistribution implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("RuleBasedDistribution", Logger.Level.DEBUG);

	// An anchored rule
	AnchoredRule rule;
	
	String id;
	
	// a cache with the output values in a simple table
	Map<Assignment, SimpleTable> cache;
	
	

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new rule-base distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not an update or a prediction rule
	 */
	public RuleBasedDistribution(AnchoredRule rule) throws DialException {
		if (rule.getRule().getRuleType() == RuleType.PROB) {
			this.rule = rule;
		}
		else {
			throw new DialException("only probabilistic rules can define a " +
					"rule-based probability distribution");
		}
		id = rule.getId();
		cache = new HashMap<Assignment,SimpleTable>();
	}
	
	
	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		for (Assignment a : cache.keySet()) {
			if (a.containsVar(oldId)) {
				Value v = a.removePair(oldId);
				a.addPair(newId, v);
			}
		}
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
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}
		
		return cache.get(condition).getProb(head);
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
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}
		
		return cache.get(condition).hasProb(head);
	}
	
	
	/**
	 * Returns the probability table associated with the given input assignment
	 * 
	 * @param condition the conditional assignment
	 * @return the associated probability table (as a SimpleTable)
	 */
	@Override
	public SimpleTable getProbTable(Assignment condition) {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}
		
		return cache.get(condition);
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
		
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}
		return cache.get(condition).sample();	
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
	public RuleBasedDistribution copy() {
		try { return new RuleBasedDistribution (rule); } 
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

	
	
	// ===================================
	//  PRIVATE METHODS
	// ===================================
	
	int nbLeft = 0;
	int nbRight = 0;

	/**
	 * Fills the cache with the SimpleTable representing the rule output for the
	 * specific condition.
	 * 
	 * @param condition the conditional assignment
	 */
	private synchronized void fillCacheForCondition(Assignment condition) {
		
		Assignment condition2 = condition.removeSpecifiers();
		try {
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(condition2);
		SimpleTable probTable = new SimpleTable();
		for (Output effectOutput : effectOutputs.keySet()) {
			Parameter param = effectOutputs.get(effectOutput);
			double parameterValue = param.getParameterValue(condition2);
			probTable.addRow(new Assignment(id, effectOutput), parameterValue);
		}
		if (!cache.containsKey(condition)) {
			cache.put(new Assignment(condition), probTable);
		}
		}
		catch (DialException e) {
			log.warning("could not fill cache for condition " + condition + ": " + e.toString());
		}
	}
	

}
