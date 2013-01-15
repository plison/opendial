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

package opendial.bn.distribs.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.statechange.AnchoredRule;
import opendial.arch.statechange.Rule.RuleType;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.StochasticParameter;

/**
 * Utility distribution based on a rule specification.
 *  *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleBasedUtilDistribution implements UtilityDistribution {

	// logger
	public static Logger log = new Logger("RuleBasedUtilDistribution", Logger.Level.DEBUG);

	// An anchored rule
	AnchoredRule rule;
	
	// a cache with the utility assignments
	Map<Assignment,Double> cache;
	
	Map<Assignment, Set<Assignment>> relevantActionsCache;
		
	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new rule-based utility distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not a decision rule
	 */
	public RuleBasedUtilDistribution(AnchoredRule rule) throws DialException {
		if ((rule.getRule().getRuleType() == RuleType.UTIL)) {
			this.rule = rule;
		}
		else {
			throw new DialException("only utility rules can define a " +
					"rule-based utility distribution");
		}
		
		cache = new HashMap<Assignment,Double>();
		relevantActionsCache = new HashMap<Assignment,Set<Assignment>>();
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
	 * @param input the value assignment
	 * @return the corresponding utility
	 */
	@Override
	public double getUtility(Assignment input) {
		Assignment input2 = new Assignment(input);
		if (!cache.containsKey(input2)) {
			fillCacheForCondition(input2);
		}
		return cache.get(input2);
	}
	


	/**
	 * Returns the set of relevant actions that are made possible given the
	 * assignment of input values given as argument.
	 * 
	 * @param input the input values for the chance nodes attached to the utility
	 * @return the set of corresponding relevant actions
	 */
	@Override
	public Set<Assignment> getRelevantActions(Assignment input) {
		if (!relevantActionsCache.containsKey(input)) {
			fillRelevantActionsCache(input);
		}
		return relevantActionsCache.get(input);
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
	public RuleBasedUtilDistribution copy() {
		try { return new RuleBasedUtilDistribution (rule); } 
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
	

	/**
	 * Fills the cache with the utility value representing the rule output for the
	 * specific input.
	 * 
	 * @param fullInput the conditional assignment
	 */
	private void fillCacheForCondition(Assignment fullInput) {
		 
		Assignment input = fullInput.removeSpecifiers().getTrimmed(rule.getInputVariables());
		Assignment actions = new Assignment(fullInput).removeSpecifiers().getTrimmed(rule.getOutputVariables());
		actions.removePairs(input.getVariables());
		try {
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(input);
		for (Output effectOutput : effectOutputs.keySet()) {
			if (effectOutput.isCompatibleWith(actions)) {
				Parameter param = effectOutputs.get(effectOutput);
				double parameterValue = param.getParameterValue(input);
				cache.put(fullInput, parameterValue);
			}
		}
		if (!cache.containsKey(fullInput)) {
			cache.put(fullInput, 0.0);
		}
		}
		catch (DialException e) {
			log.warning("could not fill cache for condition " + fullInput + ": " + e.toString());
		}
	}
	
	
	private void fillRelevantActionsCache(Assignment input) {
		Assignment input2 = input.removeSpecifiers();

		Set<Assignment> relevantActions = new HashSet<Assignment>();
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(input2);
		for (Output effectOutput : effectOutputs.keySet()) {
			relevantActions.add(new Assignment(effectOutput.getAllSetValues()));
		}
		relevantActionsCache.put(input, relevantActions);
	}


}
