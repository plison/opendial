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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;

/**
 * Basic representation of a node integrated in a Bayesian Network.  The node is defined
 * via a unique identifier and a set of incoming and outgoing relations with other nodes.
 * 
 * The class is abstract -- each node needs to be instantiated in a concrete subclass 
 * such as DiscreteChanceNode, ContinuousChanceNode, DecisionNode or UtilityNode.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class BNode implements Comparable<BNode> {

	// logger
	public static Logger log = new Logger("BNode", Logger.Level.NORMAL);
	
	// unique identifier for the node
	String nodeId;
	
	// set of nodes with incoming relations to the node
	Map<String,BNode> inputNodes;
	
	// set of nodes with outgoing relations to the node
	Map<String,BNode> outputNodes;
	
	
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
	}

	/**
	 * Adds a new relation from the node given as argument to the current node.
	 * 
	 * @param node the node to add
	 * @throws DialException if the network becomes corrupted
	 */
	public void addRelation (BNode inputNode) throws DialException {
		if (inputNodes.containsKey(inputNode.getId())) {
			log.warning("node " + inputNode.getId() + " already included in the input nodes of " + nodeId);
		}
		
		if (containsCycles(inputNode)) {
			throw new DialException("there is a cycle between " + inputNode.getId() + " and " + nodeId);
		}
		
		if (inputNode instanceof ValueNode) {
			throw new DialException ("a value node cannot be input to any other node");
		}
		if (this instanceof ActionNode) {
			throw new DialException ("an action node cannot be dependent on any other node");
		}
		
		inputNodes.put(inputNode.getId(), inputNode);
		inputNode.addOutputNode_internal(this);
	}
	
	
	
	/**
	 * Removes a relation between an input node and the current node.  
	 * 
	 * @param nodeId the identifier for the incoming node to remove
	 * @return true if a relation between the nodes existed, false otherwise
	 */
	public boolean removeRelation (BNode inputNode) {
		if (!inputNodes.containsKey(inputNode.getId())) {
			log.warning("node " + inputNode.getId() + " is not an input node for " + nodeId);
		}
		boolean removal1 = inputNode.removeOutputNode_internal(this);
		boolean removal2 = (inputNodes.remove(inputNode.getId())!=null);
		if (removal1!=removal2) {
			log.warning("inconsistency between the input and output links for " + inputNode.getId() + " and " + nodeId);
		}
		return removal2;
	}
	
	
	/**
	 * Removes all input relations to the node
	 */
	public void removeAllRelations() {
		inputNodes = new HashMap<String,BNode>();
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
	public Set<String> getInputNodesIds() {
		return inputNodes.keySet();
	}
	
	

	/**
	 * Returns the set of output nodes
	 * 
	 * @return the input nodes
	 */
	public Set<BNode> getOutputNodes() {
		return new HashSet<BNode>(outputNodes.values());
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
	 * Returns the set of distinct values that the node can take.  The
	 * nature of those values depends on the node type.  
	 * 
	 * @return the set of distinct values
	 */
	public abstract Set<Object> getValues();

	
	// ===================================
	//  UTILITIES
	// ===================================
	
	
	/**
	 * Creates a copy of the current node.  Needs to be instantiated
	 * by the concrete subclasses.
	 * 
	 * @throws DialException if the copy operation failed
	 */
	public abstract BNode copy() throws DialException ;


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
	public int compareTo(BNode otherNode) {
		
		// if one node has no incoming nodes, the answer is straightforward
		if (!otherNode.getInputNodesIds().isEmpty() && getInputNodesIds().isEmpty()) {
			return +100;
		}
		else if (otherNode.getInputNodesIds().isEmpty() && !getInputNodesIds().isEmpty()) {
			return -100;
		}
		
		// if both nodes have no ancestors, rely on lexicographic ordering
		else if (otherNode.getInputNodesIds().isEmpty() && getInputNodesIds().isEmpty()) {
			return (nodeId.compareTo(otherNode.getId()) < 0) ? -1 : +1;
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
		return (otherNodeAncestors.size() - ownAncestors.size());
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
			 return ((BNode)o).getId().equals(nodeId);
			// &&  ((BNode)o).getInputNodes().equals(getInputNodes());
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
	
	
	// ===================================
	//  PRIVATE METHODS
	// ===================================
	

	
	/**
	 * Adds a new outgoing relation to the node. This method should never be 
	 * called outside the addRelation method, to ensure consistency between 
	 * the input and output links.
	 * 
	 * @param node the output node to add
	 */
	private void addOutputNode_internal(BNode node) {
		if (outputNodes.containsKey(node.getId())) {
			log.debug("node " + node.getId() + " already included in the output nodes of " + nodeId);
		}
		else {
			outputNodes.put(node.getId(), node);
		}
	}
	

	/**
	 * Removes an outgoing relation to the node.  This method should never be
	 * called outside the removeRelation method, to ensure consistency between
	 * the input and output links.
	 * 
	 * @param node the output node to remove
	 * @return true if a relation between the nodes existed, false otherwise
	 */
	private boolean removeOutputNode_internal(BNode outputNode) {
		if (!outputNodes.containsKey(outputNode.getId())) {
			log.warning("node " + outputNode.getId() + " is not an output node for " + nodeId);
		}
		return (outputNodes.remove(outputNode.getId())!=null);
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

	/**
	 * 
	 * @return
	 */
	public abstract String prettyPrint();

	
}
