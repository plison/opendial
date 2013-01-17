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


import java.util.HashSet;
import java.util.Set;

import opendial.arch.ConfigurationSettings;
import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ReductionQuery;

public class StatePruner extends Thread {

	// logger
	public static Logger log = new Logger("StatePruner", Logger.Level.DEBUG);

	DialogueState state;

	public StatePruner(DialogueState state) {
		this.state = state;
	}

	@Override
	public void run() {

	//	log.debug("start pruning");
		Set<String> nodesToKeep = getNodesToKeep();

		try {
			BNetwork reduced = reduceNetwork(nodesToKeep);
			reinsertActionAndUtilityNodes(reduced);
			removePrimes(reduced);
			removeEmptyNodes(reduced);
			state.reset(reduced, new Assignment());
		}
		catch (Exception e) {
			log.warning("Reduction failed: " + e);
		}

	//	log.debug("finished pruning");
		state.getController().setAsCompleted(this);
	}


	private Set<String> getNodesToKeep() {

		Set<String> nodesToKeep = new HashSet<String>();
		Set<String> nodesToRemove = new HashSet<String>();
		for (BNode node : state.getNetwork().getNodes()) {
			if (node instanceof ActionNode || node instanceof UtilityNode) {
				nodesToRemove.add(node.getId());
			}
			else if (node.getId().contains("'")) {
				nodesToKeep.add(node.getId());
			}
			else if (!(node instanceof ProbabilityRuleNode) && 
					!(state.getEvidence().containsVar(node.getId()))) {

				// here, still have to check whether to keep parameter nodes or not
				if (!state.getNetwork().hasChanceNode(node.getId()+"'")){
					nodesToKeep.add(node.getId());
				}
				else {
					nodesToRemove.add(node.getId());
				}
			}
			else {
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
		InferenceAlgorithm inference = ConfigurationSettings.getInstance().
				getInferenceAlgorithm().newInstance();
		BNetwork reduced = inference.reduceNetwork(reductionQuery);
		return reduced;
		
	}


	private void reinsertActionAndUtilityNodes(BNetwork reduced) throws DialException {
		for (ActionNode an : state.getNetwork().getActionNodes()) {
			reduced.addNode(an.copy());
		}
		for (UtilityNode un : state.getNetwork().getUtilityNodes()) {
			UtilityNode copy = un.copy();
			for (String inputNodeId: un.getInputNodeIds()) {
				if (reduced.hasNode(inputNodeId)) {
					copy.addInputNode(reduced.getNode(inputNodeId));
				}
				else {
					log.warning("node " + inputNodeId + " is not in the reduced network");
				}
			}
			reduced.addNode(copy);
		}
	}


	private void removePrimes(BNetwork reduced) {
		for (String nodeId: new HashSet<String>(reduced.getNodeIds())) {
			if (nodeId.contains("'")) {
				if (!reduced.hasChanceNode(nodeId.replace("'", ""))) {
					reduced.getNode(nodeId).setId(nodeId.replace("'", ""));
				}
				else {
					log.warning("reduced network still contain duplicates");
				}
			}
		}
	}
	
	

	private void removeEmptyNodes(BNetwork reduced) {
		for (ChanceNode node: new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			if (node.getInputNodes().isEmpty()) {
				if (node.getDistrib() instanceof DiscreteProbDistribution && 
						node.getProb(ValueFactory.none())> 0.99) {
					reduced.removeNode(node.getId());
				}
			}
		}
	}
	
}

