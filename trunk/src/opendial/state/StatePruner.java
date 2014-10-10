// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.state;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.queries.ReductionQuery;
import opendial.state.distribs.EquivalenceDistribution;
import opendial.state.distribs.OutputDistribution;
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
			if (!nodesToKeep.isEmpty()) {

				DialogueState reduced = reduce(state, nodesToKeep);

				// step 3: reinsert action and utility nodes (if necessary)
				reinsertActionAndUtilityNodes(reduced, state);

				// step 4: remove the primes from the identifiers
				removePrimes(reduced);

				// step 5: filter the distribution and remove and empty nodes
				removeSpuriousNodes(reduced);

				// step 6: and final reset the state to the reduced form		
				state.reset(reduced);
			}
			else {
				state.reset(new BNetwork());
			}

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

		for (ChanceNode cn : new HashSet<ChanceNode>(state.getChanceNodes())) {
			if (state.hasNode(cn.getId()+"'")) {
				cn.setId(state.getUniqueId(cn.getId()+"^old"));
			}
		}
		for (ChanceNode cn : new HashSet<ChanceNode>(state.getChanceNodes())) {
			if (cn.getId().contains("'")) {
				if (cn.getInputNodeIds().size() < 3 && cn.getNbValues() == 1 
						&& cn.getValues().iterator().next().equals(ValueFactory.none())) {
					state.removeNode(cn.getId());
				}
				else {
					cn.setId(cn.getId().replace("'", ""));
				}
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

		for (BNode node : state.getNodes()) {

			if (node instanceof ActionNode || node instanceof UtilityNode) {
				continue;
			}
			else if (node instanceof ChanceNode && ((ChanceNode)node).getDistrib()
					instanceof EquivalenceDistribution) {
				continue;
			}

			// removing the prediction nodes once they have been used
			/** else if (node.getId().contains("^p") && 
					node.hasDescendant(state.getEvidence().getVariables())) {
				continue;
			}   */
			else if (node.getId().endsWith("^t") || node.getId().endsWith("^o") 
					|| (node.getId().endsWith("^old") && node.getOutputNodes().isEmpty())) {
				continue;
			}
			else if (node instanceof ChanceNode && node.getInputNodeIds().size() < 3 
					&& ((ChanceNode)node).getNbValues() == 1 
					&& node.getValues().iterator().next().equals(ValueFactory.none())) {
				continue;
			}
			// keeping the newest nodes
			else if (!(state.hasChanceNode(node.getId()+"'")) 
					&& !(node instanceof ProbabilityRuleNode)) {
				nodesToKeep.add(node.getId());
			}

			if (state.isIncremental(node.getId())) {
				nodesToKeep.addAll(node.getClique());
			}
			
			else if (state.getParameterIds().contains(node.getId())
					&& !node.hasDescendant(state.getEvidence().getVariables())
					&& node.getClique().size() < 20) {
				nodesToKeep.addAll(node.getClique());				
			} 
		} 
		return nodesToKeep;
	}


	/**
	 * Reduces a Bayesian network to a subset of variables.  The method is divided in 
	 * three steps: <ul>
	 * <li> The method first checks whether inference is necessary at all or whether 
	 * the current network can be returned as it is.  
	 * <li> If inference is necessary, the algorithm divides the network into cliques
	 * and performs inference on each clique separately.
	 * <li> Finally, if only one clique is present, the reduction selects the best
	 * algorithm and return the result of the reduction process.
	 * </ul>
	 * 
	 * @param state the dialogue state to reduce
	 * @param nodesToKeep the nodes to preserve in the reduction
	 * 
	 * @return the reduced dialogue state
	 * @throws DialException 
	 */
	private static DialogueState reduce(DialogueState state, 
			Set<String> nodesToKeep) throws DialException {

		Assignment evidence = state.getEvidence();

		// if the current network can be returned as such, do it
		if (nodesToKeep.containsAll(state.getNodeIds())) {
			return state;
		}


		// if all nodes to keep are included in the evidence, no inference is needed
		else if (evidence.containsVars(nodesToKeep)) {
			DialogueState newState = new DialogueState();
			for (String toKeep : nodesToKeep) {
				ChanceNode newNode = new ChanceNode(toKeep, new CategoricalTable
						(new Assignment(toKeep, evidence.getValue(toKeep))));
				newState.addNode(newNode);
			}
			return newState;
		}

		// if the network can be divided into cliques, extract the cliques
		// and do a separate reduction for each
		else if (state.getCliques().size() > 1) {
			DialogueState fullState = new DialogueState();
			for (BNetwork clique : state.createCliques()) {
				Set<String> subNodesToKeep = new HashSet<String>(nodesToKeep);
				subNodesToKeep.retainAll(clique.getNodeIds());
				if (!subNodesToKeep.isEmpty()) {
					Assignment subEvidence = evidence.getTrimmed(clique.getNodeIds());
					DialogueState substate = new DialogueState(clique, subEvidence);
					substate = reduce(substate, subNodesToKeep);
					fullState.addNetwork(substate);			
					fullState.addEvidence(substate.evidence);			
				}
			}
			return fullState;
		}

		// else, select the best reduction algorithm and performs the reduction
		ReductionQuery reductionQuery = new ReductionQuery(state, nodesToKeep);
		BNetwork result =new SwitchingAlgorithm().reduce(reductionQuery);
		return new DialogueState(result);

	}



	/**
	 * Removes the prime characters from the variable labels in the dialogue state.
	 * 
	 * @param reduced the reduced state
	 */
	private static void removePrimes(BNetwork reduced) {

		for (ChanceNode cn : new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			if (reduced.hasChanceNode(cn.getId()+"'")) {
				cn.setId(reduced.getUniqueId(cn.getId()+"^old"));
			}
		}

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
					
					// UGLY!! Find a more suitable way
					// (for instance, an interface for distributions that are "fully specified"
					// and can therefore implement getPartialPosterior) 
					if (!(outputNode instanceof ProbabilityRuleNode) && 
							!(((ChanceNode)outputNode).getDistrib() instanceof OutputDistribution)) {
						outputNode.removeInputNode(node.getId());
						ProbDistribution curDistrib = ((ChanceNode)outputNode).getDistrib();
						ProbDistribution newDistrib = curDistrib.getPartialPosterior(onlyAssign);
						((ChanceNode)outputNode).setDistrib(newDistrib);
					}
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
			if (!reduced.hasActionNode(n.getId())){
				reduced.addNode(n.copy());
			}
		}

		// utility nodes
		for (UtilityNode n : original.getUtilityNodes()) {
			if (!reduced.hasUtilityNode(n.getId())){
				reduced.addNode(n.copy());
				for (String input : n.getInputNodeIds()) {
					if (reduced.hasNode(input)) {
						reduced.getUtilityNode(n.getId()).addInputNode(reduced.getNode(input));
					}
				}
			}
		}
	}


}

