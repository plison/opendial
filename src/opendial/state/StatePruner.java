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
import java.util.regex.Pattern;

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

	//	Set<String> nodesToIsolate = getNodesToIsolate(nodesToKeep);
		try {

			BNetwork reduced = reduceNetwork(nodesToKeep);
			removePrimes(reduced);
			reinsertActionAndUtilityNodes(reduced);
			removeEmptyNodes(reduced);
			state.reset(reduced, new Assignment());
			
		}
		catch (Exception e) {
			log.debug("nodes to keep: " + nodesToKeep); // + " nodes to isolate " + nodesToIsolate);
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



	private Set<String> getNodesToIsolate(Set<String> nodesToKeep) {
		Set<String> nodesToIsolate = new HashSet<String>();
		for (BNode node : state.getNetwork().getNodes()) {

			if (nodesToKeep.contains(node.getId()) && node.getInputNodeIds().isEmpty()) {
				boolean canBeIsolated = true;
				for (BNode oNode: node.getOutputNodes()) {
					if (nodesToKeep.contains(oNode.getId())) {
						canBeIsolated = false;
					}
				}
				if (canBeIsolated) {
					//				log.debug("isolating " + node.getId() + " oNodes were " + node.getOutputNodes());
					nodesToIsolate.add(node.getId());
				}

			}
		}
		return nodesToIsolate;
	}


	private Set<String> getNodesToKeep() {

		Set<String> nodesToKeep = new HashSet<String>();
		Set<String> nodesToRemove = new HashSet<String>();

		for (BNode node : state.getNetwork().getNodes()) {

			if (node instanceof ActionNode || node instanceof UtilityNode  || 
					state.getEvidence().containsVar(node.getId())) {
				nodesToRemove.add(node.getId());
			}
			else if (node instanceof ProbabilityRuleNode && !state.isFictive 
					&& ((ProbabilityRuleNode)node).hasDescendant(Pattern.compile("(?:.*)\\^p'")) 
					&& !node.getOutputNodes().isEmpty() && node.getOutputNodesIds().iterator().next().contains("'")
					&& !((ProbabilityRuleNode)node).getRule().getParameterNodes().isEmpty() 
					&& !node.hasDescendant(state.getEvidence().getVariables())) {
				nodesToKeep.add(node.getId());
			}

			// removing the prediction nodes once they have been used
			else if (node.getId().contains("^p") && 
					node.hasDescendant(state.getEvidence().getVariables())) {
				nodesToRemove.add(node.getId());
			} 
			else if (node.getId().contains("^temp")) {
				nodesToRemove.add(node.getId());
			}

			// keeping the newest nodes
			else if (!(state.getNetwork().hasChanceNode(node.getId()+"'")) && !(node instanceof ProbabilityRuleNode)) {
				nodesToKeep.add(node.getId());
			}
			else {
				//		log.debug("removing node " + node.getId() + " instance? " + node.getClass().getCanonicalName() + " updated? " + state.getNetwork().hasChanceNode(node.getId()+"'"));
				nodesToRemove.add(node.getId());
			}

		}


		//	log.debug("keeping : " + nodesToKeep);
		//	log.debug("removing : " + nodesToRemove);

		return nodesToKeep;
	}


	private BNetwork reduceNetwork(Set<String> nodesToKeep) 
			throws DialException, InstantiationException, IllegalAccessException {

		ReductionQuery reductionQuery = new ReductionQuery(state, nodesToKeep);
		BNetwork reducedCopy = reductionQuery.getReducedCopy();
		for (BNode reducedNode : reducedCopy.getNodes()) {
			if (state.isParameter(reducedNode.getId())) {
				for (BNode outputNode : reducedNode.getOutputNodes()) {
					if (!(outputNode instanceof ProbabilityRuleNode)) {
						reductionQuery.removeRelation(reducedNode.getId(), outputNode.getId());
					}					
				}
			}
		}
		
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
		for (ChanceNode cn : state.getNetwork().getChanceNodes()) {
			if (!reduced.hasChanceNode(cn.getId()) && cn.getId().contains("^p") && 
				cn.hasDescendant(state.getEvidence().getVariables())) {
				reintegrateNode(cn, reduced);
			}
		}
		for (UtilityNode un : state.getNetwork().getUtilityNodes()) {
			reintegrateNode(un, reduced);
		}
	}
	
	private void reintegrateNode (BNode node, BNetwork reduced) throws DialException {
		Set<String> inputNodeIds = new HashSet<String>(node.getInputNodeIds());
		node.removeAllRelations();
		node.clearListeners();
		for (String inputNodeId: inputNodeIds) {
			String formattedId = StringUtils.removePrimes(inputNodeId);	
			if (reduced.hasActionNode(inputNodeId)) {
				node.addInputNode(reduced.getNode(inputNodeId));
			}
			else if (reduced.hasChanceNode(formattedId)) {
				node.addInputNode(reduced.getNode(formattedId));
			}

			else {
				reintegrateNode(state.getNetwork().getNode(inputNodeId), reduced);
				node.addInputNode(reduced.getNode(formattedId));
	//			log.debug("node " + inputNodeId + " is not in the reduced network: " + reduced.getNodeIds()
	//					+ " (when trying to add node " + node.getId()+")");
			}
		}
		reduced.addNode(node);
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

