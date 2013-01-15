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
import opendial.arch.statechange.Rule.RuleType;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.discrete.EqualityDistribution;
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
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.parameters.StochasticParameter;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.StringUtils;

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

	// the Bayesian network for the state
	BNetwork network;

	// the rule to apply
	Rule rule;


	public RuleInstantiator (DialogueState state, Rule rule) {
		this.state = state;
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

		state.getController().setAsCompleted(this);
	}


	private List<AnchoredRule> anchorRule (Rule rule) {
		List<AnchoredRule> anchoredRules = new ArrayList<AnchoredRule>();

		AnchoredRule arule = new AnchoredRule(rule, state, new Assignment());

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

		synchronized (network) {
			for (String outputVariable : arule.getOutputVariables()) {
				boolean isAttached = false;
				for (int i = 1 ; i < 4 && !isAttached ; i++) {
					String specifiedVar = outputVariable + StringUtils.createNbPrimes(i);

					// if the node does not exist yet, it is created
					if (!network.hasNode(specifiedVar)) {
						BNode newNode = createNewNode(specifiedVar);
						outputNodes.add(newNode);
						isAttached = true;
					}

					else if (state.getController().hasNewVariable(specifiedVar)){
						outputNodes.add(network.getNode(specifiedVar));
						isAttached = true;
					}
				}
			}
		}

		return outputNodes;
	}


	public BNode createNewNode(String outputVar) throws DialException {
		
		if (rule.getRuleType() == RuleType.PROB) {
			
			ChanceNode outputNode = 
					new ChanceNode(outputVar, new OutputDistribution(outputVar));
			network.addNode(outputNode);
			state.getController().setVariableAsNew(outputVar);

			// adding the connection to the previous version of the variable (if any)
			if (network.hasChanceNode(outputVar.replaceFirst("'", "")) && !(rule instanceof PredictionRule)) {
				outputNode.addInputNode(network.
						getChanceNode(outputVar.replaceFirst("'", "")));
			}
			
			// adding the connection between the predicted and observed values
			String predictEquiv = StringUtils.removeSpecifiers(outputVar) + "^p";
			if (network.hasChanceNode(predictEquiv) && 
					!outputVar.equals(predictEquiv) && !outputVar.contains("^p")) {
				ChanceNode equalityNode = new ChanceNode(network.getUniqueNodeId("="));
				equalityNode.addInputNode(outputNode);
				equalityNode.addInputNode(network.getNode(predictEquiv));
				equalityNode.setDistrib(new EqualityDistribution(equalityNode.getId(), 
						StringUtils.removeSpecifiers(outputVar)));
				state.addEvidence(new Assignment(equalityNode.getId(), true));
				network.addNode(equalityNode);
			}
			return outputNode;
			
		}
		else {
			DerivedActionNode actionNode = new DerivedActionNode(outputVar);
			network.addNode(actionNode);
			state.getController().setVariableAsNew(outputVar);
			return actionNode;
		}
	}

}
