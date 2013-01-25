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
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.state.rules.AnchoredRule;
import opendial.state.rules.Rule.RuleType;
import opendial.utils.StringUtils;

/**
 * Utility distribution based on a rule specification.
 *  *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleUtilDistribution implements UtilityDistribution {

	// logger
	public static Logger log = new Logger("RuleUtilDistribution", Logger.Level.DEBUG);

	// An anchored rule
	AnchoredRule rule;

	// a cache with the utility assignments
	Map<Assignment,UtilityTable> cache;

	Set<String> actionVars;

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

		this.actionVars = new HashSet<String>();

		cache = new HashMap<Assignment,UtilityTable>();
	}


	/**
	 * Creates a new rule-based utility distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @param the set of action vars to attach to the distribution
	 * @throws DialException if the rule is not a decision rule
	 */
	public RuleUtilDistribution(AnchoredRule rule, Set<String> actionVars) throws DialException {
		this(rule);
		this.actionVars.addAll(actionVars);
	}



	public void addActionVariable(String actionVar) {
		actionVars.add(actionVar);
	}


	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		if (actionVars.contains(oldId)) {
			actionVars.remove(oldId);
			actionVars.add(newId);
		}
		for (UtilityTable table : cache.values()) {
			table.modifyVarId(oldId, newId);
		}
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
	public synchronized double getUtility(Assignment fullInput) {
		Assignment input = fullInput.getTrimmedInverse(actionVars);
		Assignment actions = fullInput.getTrimmed(actionVars);

	
		if (cache.get(input) == null || !cache.get(input).hasUtility(actions)) {
			fillCacheForCondition(input, actions);
		}
		
		return cache.get(input).getUtility(actions);
	}



	/**
	 * Returns the set of relevant actions that are made possible given the
	 * assignment of input values given as argument.
	 * 
	 * @param fullInput the input values for the chance nodes attached to the utility
	 * @return the set of corresponding relevant actions
	 */
	@Override
	public synchronized Set<Assignment> getRelevantActions(Assignment fullInput) {

		Assignment condition = fullInput.getTrimmedInverse(actionVars);
		
		Set<Assignment> relevantActions = new HashSet<Assignment>();
		
		Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(condition);
		for (Output effectOutput : effectOutputs.keySet()) {
			
			// only fill the cache for effects that are fully specified
			if (effectOutput.getAllDiscardValues().isEmpty() && 
					!effectOutput.getAllSetValues().isEmpty()) {

				Assignment concreteAction = new Assignment(effectOutput.getAllSetValues());
				relevantActions.add(formatAction(concreteAction));
			}
		}

		return relevantActions;
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
		try { return new RuleUtilDistribution (rule, actionVars); } 
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
	private synchronized void fillCacheForCondition(Assignment input, Assignment actions) {

		if (!cache.containsKey(input)) {
			cache.put(input, new UtilityTable());
		}
		try {
			Assignment formattedAction = actions.removeSpecifiers();
			Map<Output,Parameter> effectOutputs = rule.getEffectOutputs(input);
			
			for (Output effectOutput : effectOutputs.keySet()) {	
				if (effectOutput.isCompatibleWith(formattedAction)) {
					Parameter param = effectOutputs.get(effectOutput);
					double parameterValue = param.getParameterValue(input);
					cache.get(input).setUtility(actions, parameterValue);
					return;
				}
			}
			if (!cache.get(input).hasUtility(actions)) {
				cache.get(input).setUtility(actions, 0.0);
			}	
		}
		catch (DialException e) {
			log.warning("could not fill cache for condition/action " + input + ": " + e.toString());
		}
	}




	private Assignment formatAction (Assignment baseAction) {
		Assignment formattedAction = new Assignment();
		for (String actionVar : actionVars) {
			String baseVar = actionVar.replace("'", "");
			if (baseAction.containsVar(baseVar)) {
				formattedAction.addPair(actionVar, baseAction.getValue(baseVar));
			}
		}
		return formattedAction;
	}

}
