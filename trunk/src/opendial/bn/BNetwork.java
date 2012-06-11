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

package opendial.bn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.ValueNode;

/**
 * Representation of a Bayesian Network (often augmented with value and action nodes).
 * The network is simply defined as a set of nodes connected with each other.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNetwork {

	// logger
	public static Logger log = new Logger("BNetwork", Logger.Level.DEBUG);
	
	// the set of nodes for the network
	Map<String,BNode> nodes;
	
	// the chance nodes 
	Map<String,ChanceNode> chanceNodes;
	// the value nodes
	Map<String, ValueNode> valueNodes;	
	// the action nodes
	Map<String, ActionNode> actionNodes;
	
	
	// ===================================
	//  NETWORK CONSTRUCTION
	// ===================================
	

	/**
	 * Constructs an empty network
	 */
	public BNetwork() {
		nodes = new HashMap<String,BNode>();
		chanceNodes = new HashMap<String,ChanceNode>();
		valueNodes = new HashMap<String,ValueNode>();
		actionNodes = new HashMap<String,ActionNode>();
	}

	/**
	 * Adds a new node to the network
	 * 
	 * @param node the node to add
	 */
	public void addNode(BNode node) {
		if (nodes.containsKey(node.getId())) {
			log.warning("network already contains a node with identifier " + node.getId());
		}
		nodes.put(node.getId(), node);
		
		// adding the node in the type-specific collections
		if (node instanceof ChanceNode) {
			chanceNodes.put(node.getId(), (ChanceNode)node);
		}
		else if (node instanceof ValueNode) {
			valueNodes.put(node.getId(), (ValueNode)node);
		}
		else if (node instanceof ActionNode) {
			actionNodes.put(node.getId(), (ActionNode)node);
		}
	}
	
	/**
	 * Add a collection of new nodes to the network
	 * 
	 * @param newNodes the collection of nodes to add
	 */
	public void addNodes(Collection<BNode> newNodes) {
		for (BNode newNode: newNodes) {
			addNode(newNode);
		}
	}
	
	/**
	 * Replaces an existing node with a new one (with same identifier)
	 * 
	 * @param node the new value for the node
	 */
	public void replaceNode(BNode node) {
		if (!nodes.containsKey(node.getId())) {
			log.warning("network does not contain a node with identifier " + node.getId());
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
			log.warning("network does not contain a node with identifier " + nodeId);
		}
		else {
			BNode node = nodes.get(nodeId);
			for (BNode inputNode : node.getInputNodes()) {
				node.removeRelation(inputNode);
			}
			for (BNode outputNode: node.getOutputNodes()) {
				outputNode.removeRelation(node);
			}
			
			// remove the node from the type-specific collections
			if (node instanceof ChanceNode) {
				chanceNodes.remove(node.getId());
			}
			else if (node instanceof ValueNode) {
				valueNodes.remove(node.getId());
			}
			else if (node instanceof ActionNode) {
				actionNodes.remove(node.getId());
			}
		}
	
		return nodes.remove(nodeId);
	}
	
	// ===================================
	//  GETTERS
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
	 * Returns the node associated with the given identifier in the network.  If
	 * no such node is present, returns null.
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
	
	
	/**
	 * Returns the chance node associated with the identifier, if one
	 * exists.  Else, returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the chance node
	 */
	public ChanceNode getChanceNode(String nodeId) {
		if (!chanceNodes.containsKey(nodeId)) {
			log.severe("network does not contain a chance node with identifier " + nodeId);
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
	 * Returns the action node associated with the identifier, if one
	 * exists.  Else, returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the action node
	 */
	public ActionNode getActionNode(String nodeId) {
		if (!actionNodes.containsKey(nodeId)) {
			log.severe("network does not contain an action node with identifier " + nodeId);
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
	 * Returns the value node associated with the identifier, if one
	 * exists.  Else, returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the value node
	 */
	public ValueNode getValueNode(String nodeId) {
		if (!valueNodes.containsKey(nodeId)) {
			log.severe("network does not contain a value node with identifier " + nodeId);
		}
		return valueNodes.get(nodeId);
	}
	
	/**
	 * Returns the collection of chance nodes currently in the network
	 * 
	 * @return the collection of chance nodes
	 */
	public Collection<ValueNode> getValueNodes() {
		return valueNodes.values();
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
	 * compareTo method implemented in BNode.  The ordering will place end nodes 
	 * (i.e. nodes with no outward edges) at the beginning of the list, and start 
	 * nodes  (nodes with no inward edges) at the end of the list. 
	 * 
	 * <p>This ordering is used in particular for various inference algorithms relying
	 * on a topological ordering of the nodes (e.g. variable elimination).
	 * 
	 * @return
	 */
	public List<BNode> getSortedNodes() {
		List<BNode> nodesList = new ArrayList<BNode>(nodes.values());
		Collections.sort(nodesList);
		return nodesList;
	}
	
	
	// ===================================
	//  UTILITIES
	// ===================================
	
	
	/**
	 * Returns the hashcode for the network, defined as the hashcode for the
	 * node identifiers in the network.
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
	 * @throws DialException if the copy operation failed
	 */
	public BNetwork copy() throws DialException {
		BNetwork copy = new BNetwork();
		for (BNode node : nodes.values()) {
			copy.addNode(node.copy());
		}
		
		for (BNode node : copy.getNodes()) {
			Set<String> inputNodeIds = node.getInputNodesIds();
			node.removeAllRelations();
			for (String inputNodeId : inputNodeIds) {
				BNode copyInputNode = copy.getNode(inputNodeId);
				node.addRelation(copyInputNode);
			}
		}
		return copy;
	}
	
	/**
	 * Returns a basic string representation for the network, defined as the set
	 * of node identifiers in the network.
	 *
	 * @return the string representation
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
			return (nodes.keySet().equals(((BNetwork)o).getNodeIds()));
		}
		return false;
	}
	
	/**
	 * Returns a pretty print representation of the network, comprising both the 
	 * node identifiers and the graph structure.
	 * 
	 * @return the pretty print representation
	 */
	public String prettyPrint() {
		String s = "Nodes: " + nodes.keySet() + "\n";
		s += "Edges: \n";
		for (BNode node: nodes.values()) {
			s += "\t" + node.getInputNodesIds() + "-->" + node.getId();
		}
		return s;
	}

}
