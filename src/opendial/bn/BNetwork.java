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

package opendial.bn;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;

/**
 * Representation of a Bayesian Network augmented with value and action nodes. The
 * network is simply defined as a set of nodes connected with each other.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class BNetwork {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the set of nodes for the network
	Map<String, BNode> nodes;

	// the chance nodes
	Map<String, ChanceNode> chanceNodes;

	// the utility nodes
	Map<String, UtilityNode> utilityNodes;

	// the action nodes
	Map<String, ActionNode> actionNodes;

	// ===================================
	// NETWORK CONSTRUCTION
	// ===================================

	/**
	 * Constructs an empty network
	 */
	public BNetwork() {
		nodes = new HashMap<String, BNode>();
		chanceNodes = new HashMap<String, ChanceNode>();
		utilityNodes = new HashMap<String, UtilityNode>();
		actionNodes = new HashMap<String, ActionNode>();
	}

	/**
	 * Creates a network with the provided list of nodes
	 * 
	 * @param nodes the nodes to add
	 */
	public BNetwork(BNode... nodes) {
		this();
		addNodes(Arrays.asList(nodes));
	}

	/**
	 * Creates a network with the provided list of nodes
	 * 
	 * @param nodes the nodes to add
	 */
	public BNetwork(Collection<BNode> nodes) {
		this();
		addNodes(nodes);
	}

	/**
	 * Adds a new node to the network. Note: if the node already exists, it is better
	 * to use the "replaceNode" method, to avoid warning messages.
	 * 
	 * @param node the node to add
	 */
	public void addNode(BNode node) {
		if (nodes.containsKey(node.getId())) {
			log.warning("network already contains a node with identifier "
					+ node.getId());
		}
		nodes.put(node.getId(), node);
		node.setNetwork(this);

		// adding the node in the type-specific collections
		if (node instanceof ChanceNode) {
			chanceNodes.put(node.getId(), (ChanceNode) node);
		}
		else if (node instanceof UtilityNode) {
			utilityNodes.put(node.getId(), (UtilityNode) node);
		}
		else if (node instanceof ActionNode) {
			actionNodes.put(node.getId(), (ActionNode) node);
		}
	}

	/**
	 * Add a collection of new nodes to the network
	 * 
	 * @param newNodes the collection of nodes to add
	 */
	public void addNodes(Collection<BNode> newNodes) {
		for (BNode newNode : newNodes) {
			addNode(newNode);
		}
	}

	/**
	 * Adds all the nodes in the network provided as argument to the current network
	 * 
	 * @param network the network to include
	 * 
	 */
	public void addNetwork(BNetwork network) {
		for (BNode node : new ArrayList<BNode>(network.getNodes())) {
			if (hasNode(node.getId())) {
				removeNode(node.getId());
			}
		}
		for (BNode node : network.getNodes()) {
			addNode(node.copy());
		}
		for (BNode oldNode : network.getNodes()) {
			BNode newNode = getNode(oldNode.getId());
			for (String inputNodeId : oldNode.getInputNodeIds()) {
				BNode newInputNode = getNode(inputNodeId);
				newNode.addInputNode(newInputNode);
			}
		}
	}

	/**
	 * Replaces an existing node with a new one (with same identifier)
	 * 
	 * @param node the new value for the node
	 */
	public void replaceNode(BNode node) {
		if (!nodes.containsKey(node.getId())) {
			log.fine("network does not contain a node with identifier "
					+ node.getId());
		}
		else {
			removeNode(node.getId());
		}
		addNode(node);
	}

	/**
	 * Removes a node from the network, given its identifier
	 * 
	 * @param nodeId the node identifier
	 * @return the value for the node, if it exists
	 */
	public BNode removeNode(String nodeId) {
		if (!nodes.containsKey(nodeId)) {
			// log.warning("network does not contain a node with identifier " +
			// nodeId);
		}
		else {
			BNode node = nodes.get(nodeId);

			for (BNode inputNode : node.getInputNodes()) {
				node.removeInputNode(inputNode.getId());
			}
			for (BNode outputNode : node.getOutputNodes()) {
				outputNode.removeInputNode(nodeId);
			}

			// remove the node from the type-specific collections
			if (node instanceof ChanceNode) {
				chanceNodes.remove(nodeId);
			}
			else if (node instanceof UtilityNode) {
				utilityNodes.remove(nodeId);
			}
			else if (node instanceof ActionNode) {
				actionNodes.remove(nodeId);
			}
		}

		return nodes.remove(nodeId);
	}

	/**
	 * Remove all the specified nodes
	 * 
	 * @param valueNodeIds the nodes to remove
	 * @return the removed nodes
	 */
	public List<BNode> removeNodes(Collection<String> valueNodeIds) {
		List<BNode> removed = new ArrayList<BNode>();
		for (String id : new ArrayList<String>(valueNodeIds)) {
			BNode n = removeNode(id);
			removed.add(n);
		}
		return removed;
	}

	/**
	 * Modifies the node identifier in the Bayesian Network
	 * 
	 * @param oldNodeId the old node identifier
	 * @param newNodeId the new node identifier
	 */
	public void modifyVariableId(String oldNodeId, String newNodeId) {
		BNode node = nodes.remove(oldNodeId);
		chanceNodes.remove(oldNodeId);
		utilityNodes.remove(oldNodeId);
		actionNodes.remove(oldNodeId);
		if (node != null) {
			addNode(node);
		}
		else {
			log.warning("node " + oldNodeId
					+ " did not exist, cannot change its identifier");
		}
	}

	/**
	 * Resets the Bayesian network to only contain the nodes contained in the
	 * argument. Everything else is erased.
	 * 
	 * @param network the network that contains the nodes to include after the reset.
	 */
	public void reset(BNetwork network) {
		if (System.identityHashCode(this) != System.identityHashCode(network)) {
			nodes.clear();
			chanceNodes.clear();
			utilityNodes.clear();
			actionNodes.clear();
			for (BNode node : network.getNodes()) {
				addNode(node);
			}
		}

	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns true if the network contains a node with the given identifier
	 * 
	 * @param nodeId the node identifier
	 * @return true if the node exists in the network, false otherwise
	 */
	public boolean hasNode(String nodeId) {
		return nodes.containsKey(nodeId);
	}

	/**
	 * Returns the node associated with the given identifier in the network. If no
	 * such node is present, returns null.
	 * 
	 * @param nodeId the node identifier
	 * @return the node, if it exists, or null otherwise.
	 */
	public BNode getNode(String nodeId) {
		if (!nodes.containsKey(nodeId)) {
			log.severe("network does not contain a node with identifier " + nodeId);
		}
		return nodes.get(nodeId);
	}

	/**
	 * Returns the collection of nodes currently in the network
	 * 
	 * @return the collection of nodes
	 */
	public Collection<BNode> getNodes() {
		return nodes.values();
	}

	public Collection<BNode> getNodes(Collection<String> ids) {
		return nodes.keySet().stream().filter(k -> ids.contains(k))
				.map(k -> nodes.get(k)).collect(Collectors.toSet());
	}

	/**
	 * Returns the set of nodes belonging to a certain class (extending BNode)
	 * 
	 * @param cls the class
	 * @return the resulting set of nodes
	 */
	@SuppressWarnings("unchecked")
	public <T extends BNode> Collection<T> getNodes(Class<T> cls) {
		Set<T> nodesOfClass = new HashSet<T>();
		for (BNode n : nodes.values()) {
			if (cls.isInstance(n)) {
				nodesOfClass.add((T) n);
			}
		}
		return nodesOfClass;
	}

	/**
	 * Returns the set of nodes belonging to a certain class (extending BNode)
	 * 
	 * @param cls the class
	 * @return the resulting set of node identifiers
	 */
	public <T extends BNode> Collection<String> getNodesIds(Class<T> cls) {
		Set<String> nodesOfClass = new HashSet<String>();
		for (BNode n : nodes.values()) {
			if (cls.isInstance(n)) {
				nodesOfClass.add(n.getId());
			}
		}
		return nodesOfClass;
	}

	/**
	 * Returns true if the network contains a chance node with the given identifier,
	 * and false otherwise
	 * 
	 * @param nodeId the node identifier to check
	 * @return true if a chance node is found, false otherwise
	 */
	public boolean hasChanceNode(String nodeId) {
		return chanceNodes.containsKey(nodeId);
	}

	/**
	 * Returns true if the network contains chance nodes for all the given
	 * identifiers, and false otherwise
	 * 
	 * @param nodeIds the node identifiers to check
	 * @return true if all the chance nodes is found, false otherwise
	 */

	public boolean hasChanceNodes(Collection<String> nodeIds) {
		for (String nodeId : nodeIds) {
			if (!chanceNodes.containsKey(nodeId)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the chance node associated with the identifier, if one exists. Else,
	 * returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the chance node
	 */
	public ChanceNode getChanceNode(String nodeId) {
		if (!chanceNodes.containsKey(nodeId)) {
			log.severe("network does not contain a chance node with identifier "
					+ nodeId);
		}
		return chanceNodes.get(nodeId);
	}

	/**
	 * Returns the collection of chance nodes currently in the network
	 * 
	 * @return the collection of chance nodes
	 */
	public Collection<ChanceNode> getChanceNodes() {
		return chanceNodes.values();
	}

	/**
	 * Returns the collection of chance node identifiers currently in the network
	 * 
	 * @return the collection of identifiers of chance nodes
	 */
	public Set<String> getChanceNodeIds() {
		return chanceNodes.keySet();
	}

	/**
	 * Returns the collection of node identifiers currently in the network and that
	 * have a distribution or utility function of a certain class
	 * 
	 * @param cls the class of the distribution
	 * @return the collection of identifiers of chance nodes
	 */
	public <T extends ProbDistribution> Set<String> getNodeIds(Class<T> cls) {
		Set<String> ids = new HashSet<String>();
		for (ChanceNode cn : chanceNodes.values()) {
			if (cls.isInstance(cn.getDistrib())) {
				ids.add(cn.getId());
			}
		}
		for (UtilityNode un : utilityNodes.values()) {
			if (cls.isInstance(un.getFunction())) {
				ids.add(un.getId());
			}
		}
		return ids;
	}

	/**
	 * Returns true if at least one of the nodes in nodeIds has a distribution of
	 * type cls. Else, returns false.
	 * 
	 * @param nodeIds the node identifiers to check
	 * @param cls the distribution class
	 * @return true if at least one node in nodeIds has a distribution of type cls,
	 *         else false.›
	 */
	public <T extends ProbDistribution> boolean containsDistrib(Set<String> nodeIds,
			Class<T> cls) {
		return !Collections.disjoint(nodeIds, getNodeIds(cls));
	}

	/**
	 * Returns true if the network contains an action node with the given identifier,
	 * and false otherwise
	 * 
	 * @param nodeId the node identifier to check
	 * @return true if a action node is found, false otherwise
	 */
	public boolean hasActionNode(String nodeId) {
		return actionNodes.containsKey(nodeId);
	}

	/**
	 * Returns the action node associated with the identifier, if one exists. Else,
	 * returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the action node
	 */
	public ActionNode getActionNode(String nodeId) {
		if (!actionNodes.containsKey(nodeId)) {
			log.severe("network does not contain an action node with identifier "
					+ nodeId);
		}
		return actionNodes.get(nodeId);
	}

	/**
	 * Returns the collection of action nodes currently in the network
	 * 
	 * @return the collection of action nodes
	 */
	public Collection<ActionNode> getActionNodes() {
		return actionNodes.values();
	}

	/**
	 * Returns the collection of action node identifiers currently in the network
	 * 
	 * @return the collection of identifiers of action nodes
	 */
	public Set<String> getActionNodeIds() {
		return actionNodes.keySet();
	}

	/**
	 * Returns true if the network contains a utility node with the given identifier,
	 * and false otherwise
	 * 
	 * @param nodeId the node identifier to check
	 * @return true if a utility node is found, false otherwise
	 */
	public boolean hasUtilityNode(String nodeId) {
		return utilityNodes.containsKey(nodeId);
	}

	/**
	 * Returns the utility node associated with the identifier, if one exists. Else,
	 * returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the utility node
	 */
	public UtilityNode getUtilityNode(String nodeId) {
		if (!utilityNodes.containsKey(nodeId)) {
			log.severe("network does not contain a utility node with identifier "
					+ nodeId);
		}
		return utilityNodes.get(nodeId);
	}

	/**
	 * Returns the collection of utility nodes currently in the network
	 * 
	 * @return the collection of utility nodes
	 */
	public Collection<UtilityNode> getUtilityNodes() {
		return utilityNodes.values();
	}

	/**
	 * Returns the collection of utility node identifiers currently in the network
	 * 
	 * @return the collection of identifiers of utility nodes
	 */
	public Set<String> getUtilityNodeIds() {
		return utilityNodes.keySet();
	}

	/**
	 * Returns the set of node identifiers currently in the network
	 * 
	 * @return the set of identifiers
	 */
	public Set<String> getNodeIds() {
		return nodes.keySet();
	}

	/**
	 * Returns an ordered list of nodes, where the ordering is defined in the
	 * compareTo method implemented in BNode. The ordering will place end nodes (i.e.
	 * nodes with no outward edges) at the beginning of the list, and start nodes
	 * (nodes with no inward edges) at the end of the list.
	 * 
	 * <p>
	 * This ordering is used in particular for various inference algorithms relying
	 * on a topological ordering of the nodes (e.g. variable elimination).
	 * 
	 * @return the ordered list of nodes
	 */
	public List<BNode> getSortedNodes() {
		List<BNode> nodesList = new ArrayList<BNode>(nodes.values());
		Collections.sort(nodesList);
		return nodesList;
	}

	/**
	 * Returns the ordered list of node identifiers (see method above).
	 * 
	 * @return the ordered list of node identifiers.
	 */
	public List<String> getSortedNodesIds() {
		List<String> sorted = new ArrayList<String>();
		for (BNode n : getSortedNodes()) {
			sorted.add(n.getId());
		}
		return sorted;
	}

	/**
	 * Returns the subset of nodes that are referred to by the list of identifiers
	 * 
	 * @param ids the list of identifiers
	 * @return the corresponding nodes
	 */
	protected List<BNode> getNodes(Set<String> ids) {
		List<BNode> subset = new ArrayList<BNode>();
		for (String id : ids) {
			if (nodes.containsKey(id)) {
				subset.add(nodes.get(id));
			}
		}
		return subset;
	}

	/**
	 * Returns the set of maximal cliques that compose this network. The cliques are
	 * collections of nodes such that each node in the clique is connect to all the
	 * other nodes in the clique but to no nodes outside the clique.
	 * 
	 * @return the collection of cliques for the network.
	 */
	public List<Set<String>> getCliques() {

		List<Set<String>> cliques = new ArrayList<Set<String>>();

		Stack<String> nodesToProcess = new Stack<String>();
		nodesToProcess.addAll(nodes.keySet());
		while (!nodesToProcess.isEmpty()) {
			String node = nodesToProcess.pop();
			Set<String> newClique = nodes.get(node).getClique();
			cliques.add(newClique);
			nodesToProcess.removeAll(newClique);
		}

		Collections.sort(cliques, (s1, s2) -> s1.hashCode() - s2.hashCode());

		return cliques;
	}

	/**
	 * Returns the set of maximal cliques that compose this network, if one only
	 * looks at the clique containing the given subset of node identifiers. The
	 * cliques are collections of nodes such that each node in the clique is connect
	 * to all the other nodes in the clique but to no nodes outside the clique.
	 * 
	 * @param subsetIds the subset of node identifiers to use
	 * @return the collection of cliques for the network.
	 */
	public List<Set<String>> getCliques(Set<String> subsetIds) {

		List<Set<String>> cliques = new ArrayList<Set<String>>();

		Stack<String> nodesToProcess = new Stack<String>();
		nodesToProcess.addAll(nodes.keySet());
		nodesToProcess.retainAll(subsetIds);
		while (!nodesToProcess.isEmpty()) {
			String node = nodesToProcess.pop();
			Set<String> newClique = nodes.get(node).getClique();
			cliques.add(newClique);
			nodesToProcess.removeAll(newClique);
		}

		return cliques;
	}

	/**
	 * Returns true if the subset of node identifiers correspond to a maximal clique
	 * in the network, and false otherwise
	 * 
	 * @param subsetIds the subset of node identifiers
	 * @return true if subsetIds corresponds to a maximal clique, false otherwise
	 */
	public boolean isClique(Set<String> subsetIds) {

		if (!subsetIds.isEmpty()) {
			String first = subsetIds.iterator().next();
			return hasNode(first) && getNode(first).getClique().equals(subsetIds);
		}
		return false;
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns the hashcode for the network, defined as the hashcode for the node
	 * identifiers in the network.
	 * 
	 * @return the hashcode for the network
	 */
	@Override
	public int hashCode() {
		return nodes.keySet().hashCode();
	}

	/**
	 * Returns a copy of the Bayesian network
	 * 
	 * @return the copy
	 */
	public BNetwork copy() {
		BNetwork copyNetwork = new BNetwork();
		List<BNode> sortedNodes = getSortedNodes();
		Collections.reverse(sortedNodes);

		for (BNode node : sortedNodes) {
			BNode nodeCopy = node.copy();
			for (BNode inputNode : node.getInputNodes()) {
				if (!copyNetwork.hasNode(inputNode.getId())) {
					throw new RuntimeException("cannot copy the network: structure "
							+ "is corrupt (" + inputNode.getId()
							+ " is not present, but " + "should be input node to "
							+ node.getId() + ")");
				}
				nodeCopy.addInputNode(copyNetwork.getNode(inputNode.getId()));
			}
			copyNetwork.addNode(nodeCopy);
		}
		return copyNetwork;
	}

	/**
	 * Returns a basic string representation for the network, defined as the set of
	 * node identifiers in the network.
	 */
	@Override
	public String toString() {
		return nodes.keySet().toString();
	}

	/**
	 * Returns true if the object is also a Bayesian network with exactly the same
	 * node identifiers.
	 *
	 * @param o the object to compare
	 * @return true if o is network with identical identifiers, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof BNetwork) {
			return (nodes.keySet().equals(((BNetwork) o).getNodeIds()));
		}
		return false;
	}

	/**
	 * Returns a pretty print representation of the network, comprising both the node
	 * identifiers and the graph structure.
	 * 
	 * @return the pretty print representation
	 */
	public String prettyPrint() {
		String s = "Nodes: " + nodes.keySet() + "\n";
		s += "Edges: \n";
		for (BNode node : nodes.values()) {
			s += "\t" + node.getInputNodeIds() + "-->" + node.getId();
		}
		return s;
	}

}
