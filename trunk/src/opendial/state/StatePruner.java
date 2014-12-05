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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.inference.SwitchingAlgorithm;
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

	public static double VALUE_PRUNING_THRESHOLD = 0.01;

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

		try {

			// step 1 : selection of nodes to keep
			Set<String> nodesToKeep = getNodesToKeep(state);
			if (!nodesToKeep.isEmpty()) {

				// step 2: reduction
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
	 * Selects the set of variables to retain in the dialogue state.
	 * 
	 * @param state the dialogue state
	 * @return the set of variable labels to keep
	 */
	public static Set<String> getNodesToKeep(DialogueState state) {

		Set<String> nodesToKeep = new HashSet<String>();

		for (ChanceNode node : state.getChanceNodes()) {

			if (node.getId().startsWith("=_") || node.getId().endsWith("^t") 
					|| node.getId().endsWith("^o")) {
				continue;
			}
			else if (ENABLE_PRUNING & node instanceof ProbabilityRuleNode) {
				continue;
			}
			else if (node.getInputNodeIds().size() < 3 
					&& node.getNbValues() == 1 
					&& node.getValues().iterator().next().equals(ValueFactory.none())) {
				continue;
			}
			else if (node.getId().endsWith("^p") && 
					node.getOutputNodesIds().stream().anyMatch(i -> i.startsWith("=_"))) {
				continue;
			}
			// keeping the newest nodes
			else if (!(state.hasChanceNode(node.getId()+"'"))) {
				nodesToKeep.add(node.getId());
			}

			if (state.isIncremental(node.getId())) {
				Set<String> descendants = node.getDescendantIds().stream()
						.filter(i -> state.hasChanceNode(i)).collect(Collectors.toSet());
				nodesToKeep.addAll(descendants);
			}

			if (state.getParameterIds().contains(node.getId())
					&& !node.hasDescendant(state.getEvidence().getVariables())) {
				nodesToKeep.addAll(node.getOutputNodesIds(ProbabilityRuleNode.class));				
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
			newState.incrementalVars = state.incrementalVars;
			for (String toKeep : nodesToKeep) {
				ChanceNode newNode = new ChanceNode(toKeep, new CategoricalTable
						(toKeep, evidence.getValue(toKeep)));
				newState.addNode(newNode);
			}
			return newState;
		}

		// if the network can be divided into cliques, extract the cliques
		// and do a separate reduction for each
		else if (state.getCliques().size() > 1) {
			DialogueState fullState = new DialogueState();
			fullState.incrementalVars = state.incrementalVars;
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
		
		else if (!Collections.disjoint(nodesToKeep, 
				state.getNodesIds(ProbabilityRuleNode.class))) {
			return reduce_light(state, nodesToKeep);
		}


		// else, select the best reduction algorithm and performs the reduction
		BNetwork result =new SwitchingAlgorithm().reduce(state, nodesToKeep, state.getEvidence());
		return new DialogueState(result);
	}
	
	
	private static DialogueState reduce_light(DialogueState state, 
			Collection<String> nodesToKeep) throws DialException {

		for (ChanceNode node : new ArrayList<ChanceNode>(state.getChanceNodes())) {
			
			if (!nodesToKeep.contains(node.getId())) {
				CategoricalTable initDistrib = state.queryProb(node.getId(), false).toDiscrete();
				for (ChanceNode outputNode : node.getOutputNodes(ChanceNode.class)) {
					MarginalDistribution newDistrib = new MarginalDistribution(
							outputNode.getDistrib(), initDistrib);
					outputNode.setDistrib(newDistrib);
				}
				state.removeNode(node.getId());
			}
		}
		return state;
	}



	/**
	 * Removes the prime characters from the variable labels in the dialogue state.
	 * 
	 * @param reduced the reduced state
	 * @throws DialException 
	 */
	private static void removePrimes(DialogueState reduced) throws DialException {

		for (ChanceNode cn : new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			if (reduced.hasChanceNode(cn.getId()+"'")) {
				log.warning("Reduction problem: two variables for " + cn.getId());
				reduced.removeNode(cn.getId());
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
	private static void removeSpuriousNodes(DialogueState reduced) throws DialException {

		// looping on every chance node
		for (ChanceNode node: new HashSet<ChanceNode>(reduced.getChanceNodes())) {

			// if the node only contain a None value, prunes it
			if (node.getInputNodes().isEmpty() && node.getOutputNodes().isEmpty() 
					&& node.getDistrib() instanceof CategoricalTable 
					&& node.getProb(ValueFactory.none())> 0.99) {
				reduced.removeNode(node.getId());
				continue;
			}
			else if (node.getDistrib() instanceof EquivalenceDistribution 
					&& node.getInputNodeIds().isEmpty()) {
				reduced.removeNode(node.getId());
			}
			// prune values with a probability below the threshold
			node.pruneValues(VALUE_PRUNING_THRESHOLD);

			// if the node only contains a single (non-none) value, remove outgoing dependency
			// edges (as the dependency relation is in this case superfluous)
			if (node.getInputNodeIds().isEmpty() && node.getNbValues() == 1
					&& !node.getOutputNodes().isEmpty() 
					&& reduced.getUtilityNodeIds().isEmpty()
					&& !reduced.isIncremental(node.getId())) {
				Assignment onlyAssign = new Assignment(node.getId(), node.sample());
				for (ChanceNode outputNode : node.getOutputNodes(ChanceNode.class)) {
					ProbDistribution curDistrib = outputNode.getDistrib();
					outputNode.removeInputNode(node.getId());
					if (outputNode.getInputNodeIds().isEmpty()) {
						outputNode.setDistrib(curDistrib.getProbDistrib(onlyAssign));
					}
					else {
						outputNode.setDistrib(curDistrib.getPosterior(onlyAssign));
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

