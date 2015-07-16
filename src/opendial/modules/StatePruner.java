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

package opendial.modules;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.DialogueState;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.rules.distribs.AnchoredRule;
import opendial.domains.rules.distribs.EquivalenceDistribution;
import opendial.inference.SwitchingAlgorithm;

/**
 * Prunes the dialogue state by removing all intermediary nodes (that is, rule nodes,
 * utility and action nodes, equivalence nodes, and outdated versions of updated
 * variables).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class StatePruner {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static double VALUE_PRUNING_THRESHOLD = 0.01;

	public static boolean ENABLE_REDUCTION = true;

	/**
	 * Prunes the state of all the non-necessary nodes. the operation selects a
	 * subset of relevant nodes to keep, prunes the irrelevant ones, remove the
	 * primes from the variable labels, and delete all empty nodes.
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
		catch (RuntimeException e) {
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
			else if (ENABLE_REDUCTION & node.getDistrib() instanceof AnchoredRule) {
				continue;
			}
			else if (node.getInputNodeIds().size() < 3 && node.getNbValues() == 1
					&& node.getValues().iterator().next()
							.equals(ValueFactory.none())) {
				continue;
			}
			else if (node.getId().endsWith("^p") && node.getOutputNodesIds().stream()
					.anyMatch(i -> i.startsWith("=_"))) {
				continue;
			}
			// keeping the newest nodes
			else if (!(state.hasChanceNode(node.getId() + "'"))) {
				nodesToKeep.add(node.getId());
			}

			if (state.isIncremental(node.getId())) {
				node.getDescendantIds().stream().filter(i -> state.hasChanceNode(i))
						.filter(i -> !state.hasChanceNode(i + "'"))
						.forEach(i -> nodesToKeep.add(i));
			}

			if (state.getParameterIds().contains(node.getId())
					&& !node.hasDescendant(state.getEvidence().getVariables())) {
				node.getOutputNodes(ChanceNode.class).stream()
						.filter(n -> n.getDistrib() instanceof AnchoredRule)
						.forEach(n -> nodesToKeep.add(n.getId()));
			}
		}
		return nodesToKeep;
	}

	/**
	 * Reduces a Bayesian network to a subset of variables. The method is divided in
	 * three steps:
	 * <ul>
	 * <li>The method first checks whether inference is necessary at all or whether
	 * the current network can be returned as it is.
	 * <li>If inference is necessary, the algorithm divides the network into cliques
	 * and performs inference on each clique separately.
	 * <li>Finally, if only one clique is present, the reduction selects the best
	 * algorithm and return the result of the reduction process.
	 * </ul>
	 * 
	 * @param state the dialogue state to reduce
	 * @param nodesToKeep the nodes to preserve in the reduction
	 * 
	 * @return the reduced dialogue state @
	 */
	private static DialogueState reduce(DialogueState state,
			Set<String> nodesToKeep) {
		Assignment evidence = state.getEvidence();
		// if all nodes to keep are included in the evidence, no inference is
		// needed
		if (evidence.containsVars(nodesToKeep)) {
			DialogueState newState = new DialogueState();
			for (String toKeep : nodesToKeep) {
				ChanceNode newNode =
						new ChanceNode(toKeep, evidence.getValue(toKeep));
				newState.addNode(newNode);
			}
			return newState;
		}

		// if the current network can be returned as such, do it
		else if (nodesToKeep.containsAll(state.getNodeIds())) {
			return state;
		}

		// if all nodes belong to a single clique and the evidence does not
		// pertain to them, return the subset of nodes
		else if (state.isClique(nodesToKeep)
				&& !evidence.containsOneVar(nodesToKeep)) {
			DialogueState newState =
					new DialogueState(state.getNodes(nodesToKeep), evidence);
			return newState;

		}
		// if some rule nodes are included
		else if (state.containsDistrib(nodesToKeep, AnchoredRule.class)) {
			return reduce_light(state, nodesToKeep);
		}

		// if the network can be divided into cliques, extract the cliques
		// and do a separate reduction for each
		List<Set<String>> cliques = state.getCliques(nodesToKeep);
		if (cliques.size() > 1) {
			DialogueState fullState = new DialogueState();
			for (Set<String> clique : cliques) {
				clique.retainAll(nodesToKeep);
				DialogueState cliqueState = reduce(state, clique);
				fullState.addNetwork(cliqueState);
				fullState.addEvidence(cliqueState.getEvidence());
			}
			return fullState;
		}

		// else, select the best reduction algorithm and performs the reduction
		BNetwork result =
				new SwitchingAlgorithm().reduce(state, nodesToKeep, evidence);
		return new DialogueState(result);
	}

	/**
	 * "lightweight" reduction of the dialogue state (without actual inference).
	 * 
	 * @param state the dialogue state
	 * @param nodesToKeep the nodes to keep
	 * @return the reduced dialogue state @
	 */
	private static DialogueState reduce_light(DialogueState state,
			Collection<String> nodesToKeep) {

		DialogueState newState = new DialogueState(state, state.getEvidence());
		for (ChanceNode node : new ArrayList<ChanceNode>(
				newState.getChanceNodes())) {

			if (!nodesToKeep.contains(node.getId())) {
				CategoricalTable initDistrib =
						state.queryProb(node.getId(), false).toDiscrete();
				for (ChanceNode outputNode : node.getOutputNodes(ChanceNode.class)) {
					MarginalDistribution newDistrib = new MarginalDistribution(
							outputNode.getDistrib(), initDistrib);
					outputNode.setDistrib(newDistrib);
				}
				newState.removeNode(node.getId());
			}
		}
		return newState;
	}

	/**
	 * Removes the prime characters from the variable labels in the dialogue state.
	 * 
	 * @param reduced the reduced state @
	 */
	private static void removePrimes(DialogueState reduced) {

		for (ChanceNode cn : new HashSet<ChanceNode>(reduced.getChanceNodes())) {
			if (reduced.hasChanceNode(cn.getId() + "'")) {
				log.warning("Reduction problem: two variables for " + cn.getId());
				reduced.removeNode(cn.getId());
			}
		}

		for (String nodeId : new HashSet<String>(reduced.getChanceNodeIds())) {
			if (nodeId.contains("'")) {
				String newId = nodeId.replace("'", "");
				if (!reduced.hasChanceNode(newId)) {
					reduced.getChanceNode(nodeId).setId(newId);
				}
				else {
					log.warning("reduced state still contains duplicates: "
							+ reduced.getNodeIds());
				}
			}
		}
	}

	/**
	 * Removes all non-necessary nodes from the dialogue state.
	 * 
	 * @param reduced the reduced dialogue state
	 */
	private static void removeSpuriousNodes(DialogueState reduced) {

		// looping on every chance node
		for (ChanceNode node : new HashSet<ChanceNode>(reduced.getChanceNodes())) {

			// if the node only contain a None value, prunes it
			if (node.getInputNodes().isEmpty() && node.getOutputNodes().isEmpty()
					&& node.getDistrib() instanceof CategoricalTable
					&& node.getProb(ValueFactory.none()) > 0.99) {
				reduced.removeNode(node.getId());
				continue;
			}
			else if (node.getDistrib() instanceof EquivalenceDistribution
					&& node.getInputNodeIds().isEmpty()) {
				reduced.removeNode(node.getId());
			}
			// prune values with a probability below the threshold
			node.pruneValues(VALUE_PRUNING_THRESHOLD);

			// if the node only contains a single (non-none) value, remove
			// outgoing edges (as the dependency relation is superfluous)
			if (node.getNbValues() == 1 && !node.getOutputNodes().isEmpty()
			// && reduced.getUtilityNodeIds().isEmpty()
					&& reduced.getIncrementalVars().isEmpty()) {
				Assignment onlyAssign = new Assignment(node.getId(), node.sample());
				for (ChanceNode outputNode : node.getOutputNodes(ChanceNode.class)) {
					if (!(outputNode.getDistrib() instanceof AnchoredRule)) {
						ProbDistribution curDistrib = outputNode.getDistrib();
						outputNode.removeInputNode(node.getId());
						if (outputNode.getInputNodeIds().isEmpty()) {
							outputNode.setDistrib(
									curDistrib.getProbDistrib(onlyAssign));
						}
						else {
							outputNode
									.setDistrib(curDistrib.getPosterior(onlyAssign));
						}
					}
				}
			}
		}
	}

	/**
	 * Reinserts the action and utility nodes in the reduced dialogue state.
	 * 
	 * @param reduced the reduced state
	 * @param original the original state @
	 */
	private static void reinsertActionAndUtilityNodes(BNetwork reduced,
			BNetwork original) {

		// action nodes
		for (ActionNode n : original.getActionNodes()) {
			if (!reduced.hasActionNode(n.getId())) {
				reduced.addNode(n.copy());
			}
		}

		// utility nodes
		for (UtilityNode n : original.getUtilityNodes()) {
			if (!reduced.hasUtilityNode(n.getId())) {
				reduced.addNode(n.copy());
				for (String input : n.getInputNodeIds()) {
					if (reduced.hasNode(input)) {
						reduced.getUtilityNode(n.getId())
								.addInputNode(reduced.getNode(input));
					}
				}
			}
		}
	}

}
