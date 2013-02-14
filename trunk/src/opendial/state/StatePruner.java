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



import java.util.HashSet;
import java.util.Set;

import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.nodes.UtilityRuleNode;
import opendial.bn.values.ValueFactory;
import opendial.domains.rules.PredictionRule;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ReductionQuery;
import opendial.state.rules.AnchoredRule;
import opendial.utils.StringUtils;

// PRUNING might create problems if e.g. floor=start --> floor=active, but a decision rule
// depends on floor=start
// in addition to the already noticed problems with chance / action nodes
public class StatePruner implements Runnable {

	// logger
	public static Logger log = new Logger("StatePruner", Logger.Level.DEBUG);

	DialogueState state;

	public StatePruner(DialogueState state) {
		this.state = state;
	}

	@Override
	public void run() {

		//	log.debug("start pruning for state " + state.getNetwork().getNodeIds());
		Set<String> nodesToKeep = getNodesToKeep();
		Set<String> nodesToIsolate = (state.isFictive())? getNodesToIsolate() : new HashSet<String>();
		try {
			BNetwork reduced = reduceNetwork(nodesToKeep, nodesToIsolate);
			removePrimes(reduced);
			reinsertActionAndUtilityNodes(reduced);
			removeEmptyNodes(reduced);
			state.reset(reduced, new Assignment());

		}
		catch (Exception e) {
			log.debug("nodes to keep: " + nodesToKeep + " nodes to isolate " + nodesToIsolate);
			e.printStackTrace();
			log.warning("Reduction failed: " + e);
		}

		//	log.debug("finished pruning");
		//	state.getController().setAsCompleted(this);
	}


	public boolean isPruningNeeded() {
		boolean pruningNeeded = false;
		for (String nodeId: state.getNetwork().getNodeIds()) {
			if (nodeId.contains("'") && !state.getNetwork().hasActionNode(nodeId)) {
				pruningNeeded = true;
			}
		}
		return (Settings.getInstance().activatePruning && pruningNeeded);
	}


	
	private Set<String> getNodesToIsolate() {
		Set<String> nodesToIsolate = new HashSet<String>();
		if (state.getEvidence().isEmpty()) {
		for (BNode node : state.getNetwork().getNodes()) {
			if (node instanceof ProbabilityRuleNode || node instanceof UtilityRuleNode) {
				AnchoredRule rule = (node instanceof ProbabilityRuleNode)? 
						((ProbabilityRuleNode)node).getRule() : ((UtilityRuleNode)node).getRule();

						if (!rule.getRule().getClass().equals(PredictionRule.class)) {
							for (BNode paramNode: rule.getParameterNodes()) {
								nodesToIsolate.add(paramNode.getId());
							}
						}
			}		
		}
		}
		return nodesToIsolate;
	}


	private Set<String> getNodesToKeep() {

		Set<String> nodesToKeep = new HashSet<String>();
		Set<String> nodesToRemove = new HashSet<String>();
		for (BNode node : state.getNetwork().getNodes()) {

			if (node instanceof ActionNode || node instanceof UtilityNode 
					|| node instanceof ProbabilityRuleNode  
					|| state.getEvidence().containsVar(node.getId())) {
				nodesToRemove.add(node.getId());
			}

			// removing the prediction nodes once they have been used
			else if (node.getId().contains("^p") && 
					node.hasOutputNode(state.getEvidence().getVariables())) {
				nodesToRemove.add(node.getId());
			}
			
			// keeping the newest nodes
			else if (!state.getNetwork().hasChanceNode(node.getId()+"'")) {
				nodesToKeep.add(node.getId());
			}
			else {
				nodesToRemove.add(node.getId());
			}

		}

		//	log.debug("keeping : " + nodesToKeep);
		//	log.debug("removing : " + nodesToRemove);

		return nodesToKeep;
	}


	private BNetwork reduceNetwork(Set<String> nodesToKeep, Set<String> nodesToIsolate) 
			throws DialException, InstantiationException, IllegalAccessException {

		ReductionQuery reductionQuery = new ReductionQuery(state, nodesToKeep, nodesToIsolate);
		InferenceAlgorithm inference = Settings.getInstance().inferenceAlgorithm.newInstance();

		if (state.isFictive()) {
			reductionQuery.setAsLightweight(true);
		}

		BNetwork reduced = inference.reduceNetwork(reductionQuery);
		return reduced;

	}


	private void reinsertActionAndUtilityNodes(BNetwork reduced) throws DialException {
		for (ActionNode an : state.getNetwork().getActionNodes()) {
			an.clearListeners();
			reduced.addNode(an.copy());
		}
		for (UtilityNode un : state.getNetwork().getUtilityNodes()) {
			Set<String> inputNodeIds = new HashSet<String>(un.getInputNodeIds());
			un.removeAllRelations();
			un.clearListeners();
			for (String inputNodeId: inputNodeIds) {
				String formattedId = StringUtils.removePrimes(inputNodeId);	
				if (reduced.hasActionNode(inputNodeId)) {
					un.addInputNode(reduced.getNode(inputNodeId));
				}
				else if (reduced.hasChanceNode(formattedId)) {
					un.addInputNode(reduced.getNode(formattedId));
				}

				else {
					log.warning("node " + inputNodeId + " is not in the reduced network: " + reduced.getNodeIds());
				}
			}
			reduced.addNode(un);
		}
	}


	private void removePrimes(BNetwork reduced) {
		for (String nodeId: new HashSet<String>(reduced.getChanceNodeIds())) {
			if (nodeId.contains("'")) {
				if (!reduced.hasChanceNode(nodeId.replace("'", ""))) {
					reduced.getNode(nodeId).setId(nodeId.replace("'", ""));
				}
				else {
					log.warning("reduced network still contains duplicates: " + reduced.getNodeIds());
					log.debug("original network nodes: " + state.getNetwork().getNodeIds());
				}
			}
		}
	}



	private void removeEmptyNodes(BNetwork reduced) {
		for (ChanceNode node: new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			if (node.getInputNodes().isEmpty() && node.getOutputNodes().isEmpty()) {
				if (node.getProb(ValueFactory.none())> 0.99) {
					reduced.removeNode(node.getId());
				}
			}
		}
	}

}

