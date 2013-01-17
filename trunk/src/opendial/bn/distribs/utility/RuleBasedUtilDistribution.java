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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	Map<Assignment,Map<Assignment, Double>> cache;
			
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
		
		cache = new HashMap<Assignment,Map<Assignment, Double>>();
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
	public double getUtility(Assignment fullInput) {
				
		Assignment input = extractChanceVariables(fullInput);
		Assignment actions = extractActionVariables(fullInput);
		
		if (!cache.containsKey(input) || !cache.get(input).containsKey(actions)) {
			fillCacheForCondition(input, actions);
		}
		Double result = cache.get(input).get(actions);
		if (result == null) {
			log.warning("something is wrong with the cache: " + input);
			Thread.dumpStack();
		}
		return result.doubleValue();
	}
	


	/**
	 * Returns the set of relevant actions that are made possible given the
	 * assignment of input values given as argument.
	 * 
	 * @param fullInput the input values for the chance nodes attached to the utility
	 * @return the set of corresponding relevant actions
	 */
	@Override
	public Set<Assignment> getRelevantActions(Assignment fullInput) {
		
		Assignment input = extractChanceVariables(fullInput);
		
		if (!cache.containsKey(input)) {
			fillCacheForCondition(input);
		}
		Set<Assignment> result = cache.get(input).keySet();
		if (result == null) {
			log.warning("something is wrong with the cache: " + input);
			Thread.dumpStack();
		}
		return result;
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
	private void fillCacheForCondition(Assignment input, Assignment actions) {
		
		if (!cache.containsKey(input)) {
			cache.put(input, new HashMap<Assignment,Double>());
		}
		try {
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(input);
		for (Output effectOutput : effectOutputs.keySet()) {
			if (effectOutput.isCompatibleWith(actions)) {
				Parameter param = effectOutputs.get(effectOutput);
				double parameterValue = param.getParameterValue(input);
				cache.get(input).put(actions, parameterValue);
			}
		}
		if (!cache.get(input).containsKey(actions)) {
			cache.get(input).put(actions, 0.0);
		}
		}
		catch (DialException e) {
			log.warning("could not fill cache for condition " + input + ": " + e.toString());
		}
	}
	
	

	/**
	 * Fills the cache with the utility value representing the rule output for the
	 * specific input.
	 * 
	 * @param fullInput the conditional assignment
	 */
	private void fillCacheForCondition(Assignment input) {
		
		if (!cache.containsKey(input)) {
			cache.put(input, new HashMap<Assignment,Double>());
		}
		try {
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(input);
		for (Output effectOutput : effectOutputs.keySet()) {
				Parameter param = effectOutputs.get(effectOutput);
				double parameterValue = param.getParameterValue(input);
				cache.get(input).put(new Assignment(effectOutput.getAllSetValues()), parameterValue);
		}
		}
		catch (DialException e) {
			log.warning("could not fill cache for condition " + input + ": " + e.toString());
		}
	}
	
	
	private Assignment extractChanceVariables(Assignment fullInput) {	
		Assignment corrected = fullInput.removeSpecifiers();
		return corrected.getTrimmed(rule.getInputVariables());	
	}
	
	

	private Assignment extractActionVariables(Assignment fullInput) {
		Assignment corrected = fullInput.removeSpecifiers();
		Assignment actions = corrected.getTrimmed(rule.getOutputVariables());
		return actions.getTrimmedInverse(rule.getInputVariables());
	}
}
