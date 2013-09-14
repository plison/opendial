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

package opendial.inference.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.state.DialogueState;

public class ReductionQuery extends Query {

	BNetwork reduced;
	
	Set<String> identicalNodes;

	
	public ReductionQuery (BNetwork network, String... queryVars) throws DialException {
		this(network, getCollection(queryVars));
	}
	
	public ReductionQuery (DialogueState state, String... queryVars) throws DialException {
		this(state, getCollection(queryVars));
	}
	
	
	public ReductionQuery (DialogueState state, Collection<String> queryVars) throws DialException {
		this(state.getNetwork(), queryVars, state.getEvidence());
	}
	
	public ReductionQuery (BNetwork network, Collection<String> queryVars) throws DialException {
		this(network, queryVars, new Assignment());
	}
	
	public ReductionQuery (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) throws DialException {
		super(network, queryVars, evidence, new ArrayList<String>());
		createReducedNetwork();
	}
	
	

	private void createReducedNetwork() throws DialException {

		reduced = new BNetwork();

		for (String var : queryVars) {
			if (!reduced.hasNode(var)) {
				if (network.getNode(var) instanceof ProbabilityRuleNode) {
					reduced.addNode(new ProbabilityRuleNode(((ProbabilityRuleNode)network.getNode(var)).getRule()));
				}
				else if (network.getNode(var) instanceof ChanceNode) {
					reduced.addNode(new ChanceNode(var));
				}
				else if (network.getNode(var) instanceof UtilityNode 
						|| network.getNode(var) instanceof ActionNode) {
					throw new DialException("retained variables can only be chance nodes");
				}
			}
		}
		for (String var : queryVars) {
			Set<String> ancestorIds = network.getNode(var).getAncestorsIds(queryVars);

			for (String inputDepId : ancestorIds) {
				if (reduced.hasNode(inputDepId)) {
					
					BNode inputDepNode = network.getNode(inputDepId);
				if (inputDepNode.getInputNodeIds().isEmpty() && inputDepNode instanceof ChanceNode
							&& ((ChanceNode)inputDepNode).getNbValues() == 1 && 
							!(reduced.getNode(var) instanceof ProbabilityRuleNode)) {
				//		log.debug("cutting the link between " + inputDepId + " and " + var);
						continue;
					} 
					reduced.getNode(var).addInputNode(reduced.getNode(inputDepId));
				}
			}
		}
		identicalNodes = network.getIdenticalNodes(reduced, evidence);
		for (String nodeId : identicalNodes) {
			ChanceNode originalNode = network.getChanceNode(nodeId);
			Collection<BNode> inputNodesInReduced = reduced.getNode(nodeId).getInputNodes();
			Collection<BNode> outputNodesInReduced = reduced.getNode(nodeId).getOutputNodes();
			reduced.replaceNode(originalNode.copy());
			reduced.getNode(nodeId).addInputNodes(inputNodesInReduced);
			reduced.getNode(nodeId).addOutputNodes(outputNodesInReduced);
		}  	
	}
	
	

	
	/**
	 * Returns the nodes that are irrelevant for answering the given query
	 *
	 * @return the identifiers for the irrelevant nodes
	 */
	public Set<String> getIrrelevantNodes() {

			Set<String> irrelevantNodesIds = new HashSet<String>();

			boolean includeActionAndUtils = false;
			for (String var : queryVars) {
				if (network.hasActionNode(var) || network.hasUtilityNode(var)) {
					includeActionAndUtils = true;
				}
			}
			
			boolean continueLoop = true;
			while (continueLoop) {
				continueLoop = false;
				for (String nodeId : new ArrayList<String>(network.getNodeIds())) {
					BNode node = network.getNode(nodeId);
					if (!irrelevantNodesIds.contains(nodeId) && 
							irrelevantNodesIds.containsAll(node.getOutputNodesIds()) && 
							!queryVars.contains(nodeId) && 
							!evidence.containsVar(nodeId)) {
						if (!includeActionAndUtils ||
							!(node instanceof UtilityNode)) {
							irrelevantNodesIds.add(nodeId);
							continueLoop = true;
						}
					}
				}
			}
		return irrelevantNodesIds;
	}

	
	public void removeRelation(String inputNodeId, String outputNodeId) {
		if (reduced.hasNode(inputNodeId) && reduced.hasNode(outputNodeId)) {
			reduced.getNode(inputNodeId).removeOutputNode(outputNodeId);
		}
		else {
			log.debug("cannot remove " + inputNodeId + " --> " + outputNodeId);
		}
	}
	
	public String toString() {
		return "Reduction("+super.toString() +")";
	}

	public BNetwork getReducedCopy() throws DialException {
		return reduced.copy();
	}

	public void filterIdenticalNodes() {
		for (String identicalNode: identicalNodes) {
			removeQueryVar(identicalNode);
		}
	}
	
}
