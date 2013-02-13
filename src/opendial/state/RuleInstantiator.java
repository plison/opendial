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

package opendial.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;

import opendial.bn.distribs.discrete.EqualityDistribution;
import opendial.bn.distribs.discrete.OutputDistribution;
import opendial.bn.distribs.discrete.RuleDistribution;
import opendial.bn.distribs.utility.RuleUtilDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.DerivedActionNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.nodes.UtilityRuleNode;
import opendial.bn.values.NoneVal;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.domains.rules.quantification.LabelPredicate;
import opendial.domains.rules.quantification.UnboundPredicate;
import opendial.domains.rules.quantification.ValuePredicate;
import opendial.state.rules.AnchoredRule;
import opendial.state.rules.Rule;
import opendial.state.rules.Rule.RuleType;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.StringUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleInstantiator implements Runnable {

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
			List<AnchoredRule> anchoredRules = anchorRule(rule);

			for (AnchoredRule arule: anchoredRules) {
				if (arule.isRelevant()) {
					addRule(arule);
				}
				else {
				}
			}	

		}
		catch (DialException e) {
			log.warning("could not apply rule "+ rule.getRuleId() + ", aborting: " + e.toString());
			log.debug("rule: " + rule.toString());
		} 

	}


	private List<AnchoredRule> anchorRule (Rule rule) {

		if (rule.getUnboundPredicates().isEmpty()) {
			return Arrays.asList(new AnchoredRule(rule, state, new Assignment()));
		}
		
		Map<String, Set<Value>> possibleAnchors = new HashMap<String, Set<Value>>();

		for (UnboundPredicate predicate : rule.getUnboundPredicates()) {
			if (predicate instanceof LabelPredicate) {
				possibleAnchors.putAll(getAnchors((LabelPredicate)predicate));
			}
			else if (predicate instanceof ValuePredicate) {
				possibleAnchors.putAll(getAnchors((ValuePredicate)predicate));
			}
		}

		Set<Assignment> combinations = CombinatoricsUtils.getAllCombinations(possibleAnchors);

		List<AnchoredRule> anchoredRules = new ArrayList<AnchoredRule>();
		for (Assignment anchor : combinations) {
			AnchoredRule arule = new AnchoredRule(rule, state, anchor);			
			anchoredRules.add(arule);
		}

		return anchoredRules;
	}

	
	private Map<String,Set<Value>> getAnchors(LabelPredicate predicate) {
		
		Map<String,Set<Value>> possibleAnchors = new HashMap<String,Set<Value>>();

		for (String node : network.getChanceNodeIds()) {
			if (predicate.getPredicate().isMatching(node, false)) {
				Assignment extracted = predicate.getPredicate().extractParameters(node, false);
				for (String extractedVar : extracted.getVariables()) {
					if (!possibleAnchors.containsKey(extractedVar)) {
						possibleAnchors.put(extractedVar, new HashSet<Value>());
					}
					possibleAnchors.get(extractedVar).add(extracted.getValue(extractedVar));
				}
			}
		}
		
		return possibleAnchors;
	}


	private Map<String,Set<Value>> getAnchors(ValuePredicate predicate) {
		
		Map<String,Set<Value>> possibleAnchors = new HashMap<String,Set<Value>>();

		for (int i = 4 ; i >= 0; i--) {
			String variable2 = predicate.getVariable() + StringUtils.createNbPrimes(i);
			if (network.hasChanceNode(variable2)) {
				Set<Value> values = network.getChanceNode(variable2).getValues();
				Set<Value> values2 = new HashSet<Value>();
				for (Value val : values) {
					if (val instanceof SetVal) {
						values2.addAll(((SetVal)val).getSet());
					}
					else if (!(val instanceof NoneVal)) {
						values2.add(val);
					}
				}
				for (Value val : values2) {
					if (predicate.getPredicate().isMatching(val.toString(), false)) {
						Assignment extracted = predicate.getPredicate().extractParameters(val.toString(), false);
						for (String extractedVar : extracted.getVariables()) {
							if (!possibleAnchors.containsKey(extractedVar)) {
								possibleAnchors.put(extractedVar, new HashSet<Value>());
							}
							possibleAnchors.get(extractedVar).add(extracted.getValue(extractedVar));
						}
					}
				}
				break;
			}
		}
		
		return possibleAnchors;
	}

	
	private void addRule(AnchoredRule arule) throws DialException {

		BNode ruleNode = createRuleNode(arule);

		//	synchronized (network) {
		// if the node already exists, it is removed 
		if (network.hasNode(ruleNode.getId())) {
			network.removeNode(ruleNode.getId());
		}
		network.addNode(ruleNode);
		//	}

		Set<BNode> outputNodes = getOutputNodes(arule);
		for (BNode outputNode : outputNodes) {
			if (outputNode instanceof ChanceNode) {
				outputNode.addInputNode(ruleNode);
				if (ruleNode.getId().contains("^2") && outputNode.getInputNodeIds().contains(ruleNode.getId().replace("^2", ""))) {
					log.warning("output node " + outputNode.getId() + " contains duplicates: " + ruleNode.getId());
				}
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

		for (ChanceNode inputNode : arule.getInputNodes()) {
			ruleNode.addInputNode(inputNode);
		}
		for (ChanceNode paramNode : arule.getParameterNodes()) {
			ruleNode.addInputNode(paramNode);
		}
		return ruleNode;
	}



	private Set<BNode> getOutputNodes(AnchoredRule arule) throws DialException {

		Set<BNode> outputNodes = new HashSet<BNode>();

		//	synchronized (network) {
		for (String outputVariable : arule.getOutputVariables()) {
			String updatedVar = getOutputLabel(outputVariable, arule);

			// if the node does not exist yet, it is created
			if (!network.hasNode(updatedVar)) {
				BNode newNode = createNewNode(updatedVar);
				outputNodes.add(newNode);
			}

			else {
				outputNodes.add(network.getNode(updatedVar));
			}		
		}

		return outputNodes;
	}


	protected String getOutputLabel(String baseOutput, AnchoredRule arule) throws DialException {
		for (int i = 1 ; i < 4 ; i++) {
			String updatedLabel = baseOutput + StringUtils.createNbPrimes(i);

			// if the node does not exist yet, it is created
			if (!network.hasNode(updatedLabel)) {
				return updatedLabel;
			}

			else if (!arule.getInputVariables().contains(updatedLabel)){
				return updatedLabel;
			}
		}
		throw new DialException("cannot find output node to attach to the rule node");
	}

	/**
	private BNode getLatestNode(String baseLabel) throws DialException {
		for (int i = 4 ; i >= 0 ; i--) {
			String label = baseLabel + StringUtils.createNbPrimes(i);
			if (network.hasChanceNode(label)) {
				return network.getNode(label);
			}
		}
		throw new DialException("no node found with the base label " + baseLabel);
	} */


	public BNode createNewNode(String outputVar) throws DialException {

		if (rule.getRuleType() == RuleType.PROB) {
			ChanceNode outputNode = 
					new ChanceNode(outputVar, new OutputDistribution(outputVar));
			network.addNode(outputNode);
			state.setVariableToProcess(outputVar);

			// adding the connection to the previous version of the variable (if any)
			if (network.hasChanceNode(outputVar.replaceFirst("'", "")) && !(rule instanceof PredictionRule)) {
				outputNode.addInputNode(network.
						getChanceNode(outputVar.replaceFirst("'", "")));
			}

			// adding the connection between the predicted and observed values
			String predictEquiv = StringUtils.removePrimes(outputVar) + "^p";
			if (network.hasChanceNode(predictEquiv) && 
					!outputVar.equals(predictEquiv) && !outputVar.contains("^p")) {
				ChanceNode equalityNode = new ChanceNode(network.getUniqueNodeId("="));
				equalityNode.addInputNode(outputNode);
				equalityNode.addInputNode(network.getNode(predictEquiv));
				equalityNode.setDistrib(new EqualityDistribution(equalityNode.getId(), 
						StringUtils.removePrimes(outputVar)));
				state.addEvidence(new Assignment(equalityNode.getId(), true));
				network.addNode(equalityNode);
			}
			return outputNode;

		}
		else {
			DerivedActionNode actionNode = new DerivedActionNode(outputVar);
			network.addNode(actionNode);
			state.setVariableToProcess(outputVar);
			return actionNode;
		}
	}

}
