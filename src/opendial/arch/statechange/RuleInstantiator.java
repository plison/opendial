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

package opendial.arch.statechange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.discrete.OutputDistribution;
import opendial.bn.distribs.discrete.RuleBasedDistribution;
import opendial.bn.distribs.utility.RuleBasedUtilDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.DerivedActionNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.nodes.UtilityRuleNode;
import opendial.bn.values.Value;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.parameters.StochasticParameter;
import opendial.utils.CombinatoricsUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleInstantiator extends Thread {

	// logger
	public static Logger log = new Logger("RuleInstantiator", Logger.Level.DEBUG);

	// the dialogue state
	DialogueState state;
	
	// the dialogue state controller
	StateController controller;

	// the Bayesian network for the state
	BNetwork network;

	// the rule to apply
	Rule rule;
	

	public RuleInstantiator (DialogueState state,
			StateController controller, Rule rule) {
		this.state = state;
		this.controller = controller;
		this.network = state.getNetwork();
		this.rule = rule;
	}
		
		
	@Override
	public void run() {
		try {
			// TODO step1: anchor rule in dialogue state
			List<AnchoredRule> anchoredRules = anchorRule(rule);

			for (AnchoredRule arule: anchoredRules) {
				if (arule.isRelevant()) {
					addRule(arule);
				}
			}	
		
		}
		catch (DialException e) {
			log.warning("could not apply rule "+ rule.getRuleId() + ", aborting: " + e.toString());
		} 
		
		controller.setAsCompleted(this);
	}
	
	
	private List<AnchoredRule> anchorRule (Rule rule) {
		List<AnchoredRule> anchoredRules = new ArrayList<AnchoredRule>();
		
		AnchoredRule arule = new AnchoredRule(rule, state, controller, new Assignment());
			
		anchoredRules.add(arule);
		
		return anchoredRules;
	}


	private void addRule(AnchoredRule arule) throws DialException {
		
		BNode ruleNode = createRuleNode(arule);

		// if the node already exists, it is removed 
		if (network.hasNode(ruleNode.getId())) {
			network.removeNode(ruleNode.getId());
		}
		network.addNode(ruleNode);

		Set<ChanceNode> paramNodes = getParameterNodes(arule);
		for (ChanceNode paramNode : paramNodes) {
			ruleNode.addInputNode(paramNode);
		}

		Set<BNode> outputNodes = getOutputNodes(arule);
		for (BNode outputNode : outputNodes) {
			if (outputNode instanceof ChanceNode) {
				outputNode.addInputNode(ruleNode);
			}
			else if (outputNode instanceof DerivedActionNode) {
				ruleNode.addInputNode(outputNode);
			}
		} 

	}

	private BNode createRuleNode(AnchoredRule arule) throws DialException {
		BNode ruleNode;
		switch (arule.getRule().getRuleType()) {
		case PROB: ruleNode = new ProbabilityRuleNode(arule); break;
		case UTIL: ruleNode = new UtilityRuleNode(arule); break;
		default: ruleNode = null; break;
		}

		return ruleNode;
	}


	private Set<ChanceNode> getParameterNodes(AnchoredRule arule) {
		Set<ChanceNode> paramNodes = new HashSet<ChanceNode>();

		Collection<ChanceNode> parameters = arule.getParameters();

		for (ChanceNode param : parameters) {
			if (!network.hasChanceNode(param.getId())) {
				network.addNode(param);
			}
			paramNodes.add(param);
		}
		return paramNodes;
	}


	private Set<BNode> getOutputNodes(AnchoredRule arule) throws DialException {
		Set<BNode> outputNodes = new HashSet<BNode>();

		Set<String> outputVariables =  arule.getOutputVariables();

		synchronized (network) {
		if (arule.getRule().getRuleType() == RuleType.PROB) {
			for (String outputVariable : outputVariables) {
				
				// if the node does not exist yet, it is created
				if (!network.hasChanceNode(outputVariable)) {
					ChanceNode outputNode = 
							new ChanceNode(outputVariable, new OutputDistribution(outputVariable));
					network.addNode(outputNode);
					controller.setVariableAsNew(outputVariable);

					// adding the connection to the previous version of the variable (if any)
					if (network.hasChanceNode(outputVariable.replaceFirst("'", ""))) {
						outputNode.addInputNode(network.
								getChanceNode(outputVariable.replaceFirst("'", "")));
					}
				}
				else {
					
				}
				outputNodes.add(network.getChanceNode(outputVariable));
			}
		}
		else {
			for (String outputVariable : outputVariables) {
				if (!network.hasActionNode(outputVariable)) {
					DerivedActionNode outputNode = new DerivedActionNode(outputVariable);
					network.addNode(outputNode);
				}
				outputNodes.add(network.getActionNode(outputVariable));
			}
		}
		}	
		return outputNodes;
	}

}
