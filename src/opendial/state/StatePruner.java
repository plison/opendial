// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.queries.ReductionQuery;
import opendial.state.distribs.EquivalenceDistribution;
import opendial.state.nodes.ProbabilityRuleNode;


/**
 * Prunes the dialogue state by removing all intermediary nodes (that is, rule nodes, 
 * utility and action nodes, equivalence nodes, and outdated versions of updated variables).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class StatePruner {

	// logger
	public static Logger log = new Logger("StatePruner", Logger.Level.DEBUG);

	public static double VALUE_PRUNING_THRESHOLD = 0.03;
			
	public static boolean ENABLE_PRUNING = true;
	
	/**
	 * Prunes the state of all the non-necessary nodes.  the operation selects a subset 
	 * of relevant nodes to keep, prunes the irrelevant ones,
	 * remove the primes from the variable labels, and delete all empty nodes.
	 * 
	 * 
	 * @param state the state to prune
	 */
	public static void prune(DialogueState state) {
	
		if (!ENABLE_PRUNING) {
			 pruneSimplified(state);
			 return;
		}
		
		try {
			// step 1 : selection of nodes to keep
			Set<String> nodesToKeep = getNodesToKeep(state);			
			// step 2: reduction
			ReductionQuery reductionQuery = new ReductionQuery(state, nodesToKeep);
			BNetwork reduced = new SwitchingAlgorithm().reduce(reductionQuery);
			
			// step 3: reinsert action and utility nodes (if necessary)
			reinsertActionAndUtilityNodes(reduced, state);
			
			// step 4: remove the primes from the identifiers
			removePrimes(reduced);
			
			// step 5: filter the distribution and remove and empty nodes
			removeSpuriousNodes(reduced);
			// step 6: and final reset the state to the reduced form		
			
			state.reset(reduced);
		
		}
		catch (DialException e) {
			log.warning("cannot prune state: " + e);
		}

	}


	/**
	 * Perform a simplified pruning operation that only modifies the primes in 
	 * the node identifiers, and keep the rest of the state.
	 * 
	 * @param state the state to prune
	 */
	private static void pruneSimplified(DialogueState state) {

		Set<String> toKeep = getNodesToKeep(state);
		Set<String> nodeIds = new HashSet<String>(state.getChanceNodeIds());
		for (String id : nodeIds) {
			if (!toKeep.contains(id) && !id.contains("^t")) {
				state.getNode(id).setId(state.getUniqueId(id)+"^t");
			}
		}
		for (String id : nodeIds) {
			if (id.contains("'")) {
				state.getNode(id).setId(id.replace("'", ""));
			}
		}

	}


	
	/**
	 * Selects the set of variables to retain in the dialogue state.
	 * 
	 * @param state the dialogue state
	 * @return the set of variable labels to keep
	 */
	public static Set<String> getNodesToKeep(DialogueState state) {

		Set<String> nodesToKeep = new HashSet<String>();
		Set<String> nodesToRemove = new HashSet<String>();

		for (BNode node : state.getNodes()) {

			if (node instanceof ActionNode || node instanceof UtilityNode  || (node instanceof ChanceNode 
					&& ((ChanceNode)node).getDistrib() instanceof EquivalenceDistribution)) {
				nodesToRemove.add(node.getId());
			}

			// removing the prediction nodes once they have been used
			else if (node.getId().contains("^p") && 
					node.hasDescendant(state.getEvidence().getVariables())) {
				nodesToRemove.add(node.getId());
			} 
			else if (node.getId().endsWith("^t") || node.getId().endsWith("^o")) {
				nodesToRemove.add(node.getId());
			}

			// keeping the newest nodes
			else if (!(state.hasChanceNode(node.getId()+"'")) && !(node instanceof ProbabilityRuleNode)) {
				nodesToKeep.add(node.getId());
			}
			else {
				nodesToRemove.add(node.getId());
			}
		}

		//	log.debug("keeping : " + nodesToKeep);

		return nodesToKeep;
	}



	/**
	 * Removes the prime characters from the variable labels in the dialogue state.
	 * 
	 * @param reduced the reduced state
	 */
	private static void removePrimes(BNetwork reduced) {

		for (String nodeId: new HashSet<String>(reduced.getChanceNodeIds())) {
			if (nodeId.contains("'")) {
				String newId = nodeId.replace("'", "");
				if (!reduced.hasChanceNode(newId)) {
					reduced.getChanceNode(nodeId).setId(newId);
				}
				else {
					log.warning("reduced state still contains duplicates: " + reduced.getNodeIds());
				}
			}
		}
	}



	
	/**
	 * Removes all non-necessary nodes from the dialogue state.
	 * 
	 * @param reduced the reduced dialogue state
	 * @throws DialException if the removal fails
	 */
	private static void removeSpuriousNodes(BNetwork reduced) throws DialException {

		// looping on every chance node
		for (ChanceNode node: new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			
			// if the node only contain a None value, prunes it
			if (node.getInputNodes().isEmpty() && node.getOutputNodes().isEmpty() 
					&& node.getDistrib() instanceof CategoricalTable 
					&& node.getProb(ValueFactory.none())> 0.99) {
					reduced.removeNode(node.getId());
					continue;
			}
			// prune values with a probability below the threshold
			node.getDistrib().pruneValues(VALUE_PRUNING_THRESHOLD);
		
			// if the node only contains a single (non-none) value, remove outgoing dependency
			// edges (as the dependency relation is in this case superfluous)
			if (node.getInputNodeIds().isEmpty() && node.getNbValues() == 1
					&& !node.getOutputNodes().isEmpty() && reduced.getUtilityNodeIds().isEmpty()) {
				Assignment onlyAssign = new Assignment(node.getId(), node.sample());
				node.setDistrib(new CategoricalTable(onlyAssign));
				for (BNode outputNode : node.getOutputNodes()) {
					outputNode.removeInputNode(node.getId());
					((ChanceNode)outputNode).setDistrib(((ChanceNode)
							outputNode).getDistrib().getPartialPosterior(onlyAssign));
				}
			} 
		}
	}
	
	
	/**
	 * Reinserts the action and utility nodes in the reduced dialogue state.
	 * 
	 * @param reduced the reduced state
	 * @param original the original state
	 * @throws DialException
	 */
	private static void reinsertActionAndUtilityNodes(BNetwork reduced, BNetwork original) 
			throws DialException {
		
		// action nodes
		for (ActionNode n : original.getActionNodes()) {
			reduced.addNode(n.copy());
		}
		
		// utility nodes
		for (UtilityNode n : original.getUtilityNodes()) {
			reduced.addNode(n.copy());
			for (String input : n.getInputNodeIds()) {
				if (reduced.hasNode(input)) {
					reduced.getUtilityNode(n.getId()).addInputNode(reduced.getNode(input));
				}
			}
		}
	}

}

