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

package opendial.bn.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;

/**
 * Basic representation of a node integrated in a Bayesian Network.  The node is defined
 * via a unique identifier and a set of incoming and outgoing relations with other nodes.
 * 
 * The class is abstract -- each node needs to be instantiated in a concrete subclass 
 * such as DiscreteChanceNode, ContinuousChanceNode, DecisionNode or UtilityNode.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public abstract class BNode implements Comparable<BNode> {

	// logger
	public static Logger log = new Logger("BNode", Logger.Level.DEBUG);

	// unique identifier for the node
	String nodeId;

	// set of nodes with incoming relations to the node
	Map<String,BNode> inputNodes;

	// set of nodes with outgoing relations to the node
	Map<String,BNode> outputNodes;

	// objects listening to changes of identifiers for this node
	List<IdChangeListener> idChangeListeners;


	// ===================================
	//  NODE CONSTRUCTION
	// ===================================


	/**
	 * Creates a node with a unique identifier, and a empty set of
	 * incoming nodes
	 * 
	 * @param nodeId the identifier for the node
	 */
	public BNode (String nodeId) {
		this.nodeId = nodeId;
		inputNodes = new HashMap<String,BNode>();
		outputNodes = new HashMap<String,BNode>();
		idChangeListeners = new LinkedList<IdChangeListener>();
	}

	/**
	 * Adds a new relation from the node given as argument to the current node.
	 * 
	 * @param node the node to add
	 * @throws DialException if the network becomes corrupted
	 */
	public synchronized void addInputNode (BNode inputNode) throws DialException {

		if (inputNode == this) {
			throw new DialException("cannot add itself: " + nodeId);
		}
		if (containsCycles(inputNode)) {
			throw new DialException("there is a cycle between " + inputNode.getId() + " and " + nodeId);
		}

		if (this instanceof ActionNode) {
			throw new DialException ("an action node cannot be dependent on any other node");
		}

		if (inputNode instanceof UtilityNode) {
			throw new DialException ("an utility node cannot be the input of any other node");
		}

		addInputNode_internal(inputNode);
		inputNode.addOutputNode_internal(this);

	}



	/**
	 * Removes a relation between an input node and the current node.  
	 * 
	 * @param nodeId the identifier for the incoming node to remove
	 * @return true if a relation between the nodes existed, false otherwise
	 */
	public synchronized boolean removeInputNode (String inputNodeId) {
		if (!inputNodes.containsKey(inputNodeId)) {
			log.warning("node " + inputNodeId + " is not an input node for " + nodeId);
		}
		boolean removal1 = inputNodes.containsKey(inputNodeId) && inputNodes.get(inputNodeId).removeOutputNode_internal(nodeId);
		boolean removal2 = removeInputNode_internal(inputNodeId);
		if (removal1!=removal2) {
			log.warning("inconsistency between the input and output links for " + inputNodeId + " and " + nodeId);
		}

		return removal2;
	}


	/**
	 * Removes all input relations to the node
	 */
	public synchronized void removeAllRelations() {
		for (BNode inputNode: new LinkedList<BNode>(inputNodes.values())) {
			removeInputNode(inputNode.getId());
		}
	}

	/**
	 * Changes the identifier for the node
	 * 
	 * @param nodeId the new identifier
	 */
	public synchronized void setId(String newNodeId) {
		String oldNodeId = this.nodeId;
		this.nodeId = newNodeId;

		modifyNodeId(oldNodeId, newNodeId);

		for (BNode inputNode : inputNodes.values()) {
			inputNode.modifyNodeId(oldNodeId, newNodeId);
		}
		for (BNode outputNode : outputNodes.values()) {
			outputNode.modifyNodeId(oldNodeId, newNodeId);
		}
		for (IdChangeListener listener : 
			new HashSet<IdChangeListener>(idChangeListeners)) {
			listener.modifyNodeId(oldNodeId, newNodeId);
		}
	}


	/**
	 * Adds the given listener on node identifier changes
	 * 
	 * @param listener the listener to add
	 */
	public synchronized void addIdChangeListener(IdChangeListener listener) {
		idChangeListeners.add(listener);
	}
	
	
	public synchronized void clearListeners() {
		idChangeListeners.clear();
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the identifier for the node
	 */
	public String getId() {
		return nodeId;
	}

	/**
	 * Returns true if the node contains an input node identified
	 * by the given id, and false otherwise.
	 * 
	 * @param nodeId the identifier for the node
	 * @return true if there is such input node, false otherwise
	 */
	public boolean hasInputNode(String nodeId) {
		return inputNodes.containsKey(nodeId);
	}

	/**
	 * Returns true if the node contains an output node identified
	 * by the given id, and false otherwise.
	 * 
	 * @param nodeId the identifier for the node
	 * @return true if there is such output node, false otherwise
	 */
	public boolean hasOutputNode(String nodeId) {
		return outputNodes.containsKey(nodeId);
	}


	/**
	 * Returns the set of input nodes
	 * 
	 * @return the input nodes
	 */
	public Set<BNode> getInputNodes() {
		return new HashSet<BNode>(inputNodes.values());
	}

	/**
	 * Returns the identifiers for the set of input nodes
	 * 
	 * @return the ids for the input nodes
	 */
	public Set<String> getInputNodeIds() {
		return inputNodes.keySet();
	}



	/**
	 * Returns the set of output nodes
	 * 
	 * @return the input nodes
	 */
	public Set<BNode> getOutputNodes() {
		Set<BNode> result = new HashSet<BNode>();
		result.addAll(outputNodes.values());
		return result;
	}

	/**
	 * Returns the identifiers for the set of output nodes
	 * 
	 * @return the ids for the input nodes
	 */
	public Set<String> getOutputNodesIds() {
		return outputNodes.keySet();
	}



	/**
	 * Returns an ordered list of nodes which are the ancestors
	 * (via the relations) of the current node.  The ordering
	 * puts the closest ancestors at the beginning of the list, 
	 * and the most remote ancestors at the end.
	 * 
	 * @return an ordered list of ancestors for the node
	 */
	public List<BNode> getAncestors() {
		List<BNode> ancestors = new ArrayList<BNode>();

		Queue<BNode> nodesToProcess = new LinkedList<BNode>();
		nodesToProcess.add(this);

		// NB: we try to avoid recursion for efficiency reasons, and
		// use a while loop instead
		while (!nodesToProcess.isEmpty()) {
			BNode currentNode = nodesToProcess.poll();
			for (BNode ancestorNode : currentNode.getInputNodes()) {
				if (!ancestors.contains(ancestorNode)) {
					ancestors.add(ancestorNode);
				}
				if (!nodesToProcess.contains(ancestorNode)) {
					nodesToProcess.add(ancestorNode);
				}
			}
		}
		return ancestors;
	}

	/**
	 * Returns an order list of node identifiers which are the ancestors
	 * (via the relations) of the current node.
	 * 
	 * @return the ordered list of ancestor identifiers
	 */
	public List<String> getAncestorIds() {
		List<BNode> ancestors = getAncestors();
		List<String> ancestorsIds = new ArrayList<String>(ancestors.size());
		for (BNode node : ancestors) {
			ancestorsIds.add(node.getId());
		}
		return ancestorsIds;
	}




	/**
	 * Returns the list of closest ancestors for the node among a set of possible 
	 * variables.  The variables not mentioned are ignored.
	 * 
	 * @param var the variable for which to find dependencies
	 * @param variablesToRetain the set of all variables from which to seek 
	 *        possible ancestors
	 * @return the set of dependencies for the given variable
	 */
	public Set<String> getAncestorsIds(Collection<String> variablesToRetain) {

		Set<String> ancestors = new HashSet<String>();

		Stack<BNode> nodesToProcess = new Stack<BNode>();
		nodesToProcess.addAll(getInputNodes());
		while (!nodesToProcess.isEmpty()) {
			BNode inputNode = nodesToProcess.pop();
			if (variablesToRetain.contains(inputNode.getId())) {
				ancestors.add(inputNode.getId());
			}
			else {
				nodesToProcess.addAll(inputNode.getInputNodes());
			}
		}

		return ancestors;
	}


	/**
	 * Returns an ordered list of nodes which are the descendants
	 * (via the relations) of the current node.  The ordering
	 * puts the closest descendants at the beginning of the list, 
	 * and the most remote descendants at the end.
	 * 
	 * @return an ordered list of descendants for the node
	 */
	public List<BNode> getDescendants() {
		List<BNode> descendants = new ArrayList<BNode>();

		Queue<BNode> nodesToProcess = new LinkedList<BNode>();
		nodesToProcess.add(this);

		// NB: we try to avoid recursion for efficiency reasons, and
		// use a while loop instead
		while (!nodesToProcess.isEmpty()) {
			BNode currentNode = nodesToProcess.poll();
			for (BNode descendantNode : currentNode.getOutputNodes()) {
				if (!descendants.contains(descendantNode)) {
					descendants.add(descendantNode);
				}
				if (!nodesToProcess.contains(descendantNode)) {
					nodesToProcess.add(descendantNode);
				}
			}
		}
		return descendants;
	}


	/**
	 * Returns an order list of node identifiers which are the descendants
	 * (via the relations) of the current node.
	 * 
	 * @return the ordered list of descendant identifiers
	 */
	public List<String> getDescendantIds() {
		List<BNode> descendants = getDescendants();
		List<String> descendantsIds = new ArrayList<String>(descendants.size());
		for (BNode node : descendants) {
			descendantsIds.add(node.getId());
		}
		return descendantsIds;
	}


	/**
	 * Returns true if at least one of the variables given as argument is a descendant
	 * of this node, and false otherwise
	 * 
	 * @param variables the node identifiers of potential descendants
	 * @return true if a descendant is found, false otherwise
	 */
	public boolean hasDescendant(Set<String> variables) {

		Queue<BNode> nodesToProcess = new LinkedList<BNode>();
		nodesToProcess.add(this);

		// NB: we try to avoid recursion for efficiency reasons, and
		// use a while loop instead
		while (!nodesToProcess.isEmpty()) {
			BNode currentNode = nodesToProcess.poll();
			for (BNode descendantNode : currentNode.getOutputNodes()) {
				if (variables.contains(descendantNode.getId())) {
					return true;
				}
				if (!nodesToProcess.contains(descendantNode)) {
					nodesToProcess.add(descendantNode);
				}
			}
		}
		
		return false;
	}
	
	
	
	/**
	 * Returns the set of distinct values that the node can take.  The
	 * nature of those values depends on the node type.  
	 * 
	 * @return the set of distinct values
	 */
	public abstract Set<Value> getValues();


	/**
	 * Return the factor matrix associated with the node.  The factor matrix is
	 * derived from the probability or utility distribution
	 * 
	 * @return
	 */
	public abstract Map<Assignment,Double> getFactor();


	// ===================================
	//  UTILITIES
	// ===================================


	/**
	 * Creates a copy of the current node.  Needs to be instantiated
	 * by the concrete subclasses.
	 * 
	 * @throws DialException if the copy operation failed
	 */
	public abstract BNode copy() throws DialException;


	/**
	 * Compares the node to other nodes, in order to derive the topological order
	 * of the network.  If the node given as argument is one ancestor of this node, 
	 * return -100.  If the opposite is true, returns +100.  Else, returns the
	 * difference between the size of the respective ancestors lists.  Finally, if both
	 * lists are empty, returns +1 or -1 depending on the lexicographic order of 
	 * the node identifiers.
	 * 
	 * <p>This ordering is used for various inference algorithms (e.g. variable elimination)
	 * relying on a topological ordering of the nodes in the network.
	 *
	 * @param node the node to compare
	 * @return the comparison result
	 *
	 */
	@Override
	public synchronized int compareTo(BNode otherNode) {

		// if one node has no incoming nodes, the answer is straightforward
		if (!otherNode.getInputNodeIds().isEmpty() && getInputNodeIds().isEmpty()) {
			return +100;
		}
		else if (otherNode.getInputNodeIds().isEmpty() && !getInputNodeIds().isEmpty()) {
			return -100;
		}

		// if both nodes have no ancestors, rely on lexicographic ordering
		// (and put action nodes first)
		else if (otherNode.getInputNodeIds().isEmpty() && getInputNodeIds().isEmpty()) {
			if (this instanceof ActionNode && !(otherNode instanceof ActionNode)) {
				return +10;
			}
			else if (otherNode instanceof ActionNode && !(this instanceof ActionNode)) {
				return -10;
			}
			return (nodeId.compareTo(otherNode.getId()) < 0) ? +1 : -1;
		} 

		// if both nodes have ancestors, we check whether one is contained in the other
		List<BNode> ownAncestors = getAncestors();
		if (ownAncestors.contains(otherNode)) {
			return -10;
		}	
		List<BNode> otherNodeAncestors = otherNode.getAncestors();
		if (otherNodeAncestors.contains(this)) {
			return 10;
		}

		// finally, if none of the above condition applies, simply return the difference
		// of size between the respective lists of ancestors
		int sizeDiff = (otherNodeAncestors.size() - ownAncestors.size());
		if (sizeDiff != 0) {
			return sizeDiff;
		}
		else {
			return (nodeId.compareTo(otherNode.getId()) < 0) ? +1 : -1;
		}
	}


	/**
	 * Returns the hashcode, simply defined as the hashcode of the identifier
	 * 
	 * @return the hashcode for the identifier
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode();
	}

	/**
	 * Returns true if the given argument is a node with identical identifier
	 *
	 * @param o the object to compare
	 * @return true if the argument is a node with an identical identifier
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof BNode) {
			return ((BNode)o).getId().equals(nodeId)
					&& ((BNode)o).getInputNodes().equals(getInputNodes());
		}
		return false;
	}


	/**
	 * Returns the string identifier for the node
	 *
	 * @return the node identifier
	 */
	@Override
	public String toString() {
		return nodeId;
	}


	/**
	 * Returns a pretty print representation of the node content
	 * 
	 * @return the node content (e.g. its distribution)
	 */
	public abstract String prettyPrint();


	// ===================================
	//  PROTECTED AND PRIVATE METHODS
	// ===================================


	/**
	 * Replaces the identifier for the input and output nodes 
	 * with the new identifier
	 * 
	 * @param oldNodeId the old label for the node
	 * @param newNodeId the new label for the node
	 */
	protected void modifyNodeId(String oldNodeId, String newNodeId) {
		if (inputNodes.containsKey(oldNodeId)) {
			BNode inputNode = inputNodes.get(oldNodeId);
			removeInputNode_internal(oldNodeId);
			addInputNode_internal(inputNode);
		}
		else if (outputNodes.containsKey(oldNodeId)) {
			BNode outputNode = outputNodes.get(oldNodeId);
			removeOutputNode_internal(oldNodeId);
			addOutputNode_internal(outputNode);
		}	
	}


	/**
	 * Returns the list of possible assignment of input values for the node.  If the
	 * node has no input, returns a list with a single, empty assignment.
	 * 
	 * <p>NB: this is a combinatorially expensive operation, use with caution.
	 * 
	 * @return the (unordered) list of possible conditions.  
	 */
	protected Set<Assignment> getPossibleConditions() {
		Map<String,Set<Value>> possibleInputValues = new HashMap<String,Set<Value>>();
		for (BNode inputNode : inputNodes.values()) {
			possibleInputValues.put(inputNode.getId(), inputNode.getValues());
		}
		Set<Assignment> possibleConditions = 
				CombinatoricsUtils.getAllCombinations(possibleInputValues);
		return possibleConditions;
	}


	/**
	 * Adds a new incoming relation to the node. This method should never be 
	 * called outside the addRelation method, to ensure consistency between 
	 * the input and output links.
	 * 
	 * @param node the input node to add
	 */
	protected void addInputNode_internal(BNode inputNode) {
		if (inputNodes.containsKey(inputNode.getId())) {
			log.warning("node " + inputNode.getId() + " already included in the input nodes of " + nodeId);
		}
		inputNodes.put(inputNode.getId(), inputNode);
	}


	/**
	 * Adds a new outgoing relation to the node. This method should never be 
	 * called outside the addRelation method, to ensure consistency between 
	 * the input and output links.
	 * 
	 * @param outputNode the output node to add
	 */
	protected void addOutputNode_internal(BNode outputNode) {
		if (outputNodes.containsKey(outputNode.getId())) {
			log.debug("node " + outputNode.getId() + " already included in the output nodes of " + nodeId);
		}
		else {
			outputNodes.put(outputNode.getId(), outputNode);
		}
	}


	protected boolean removeInputNode_internal(String inputNodeId) {
		BNode inputNode = inputNodes.remove(inputNodeId);
		return (inputNode!=null);
	}

	/**
	 * Removes an outgoing relation to the node.  This method should never be
	 * called outside the removeRelation method, to ensure consistency between
	 * the input and output links.
	 * 
	 * @param node the output node to remove
	 * @return true if a relation between the nodes existed, false otherwise
	 */
	private boolean removeOutputNode_internal(String outputNodeId) {
		if (!outputNodes.containsKey(outputNodeId)) {
			log.warning("node " + outputNodeId + " is not an output node for " + nodeId);
		}
		BNode outputNode = outputNodes.remove(outputNodeId);
		return (outputNode!=null);
	}


	/**
	 * Checks whether a cycle exists between the given node and the present one
	 * 
	 * @param inputNode the input node
	 * @return true if such a cycle exists, false otherwise
	 */
	private boolean containsCycles(BNode inputNode) {
		if (getDescendants().contains(inputNode)) {
			return true;
		}
		return false;
	}


}
