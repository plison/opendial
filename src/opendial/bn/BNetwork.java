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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.DerivedActionNode;
import opendial.bn.nodes.IdChangeListener;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.bn.nodes.UtilityNode;
import opendial.inference.queries.Query;


/**
 * Representation of a Bayesian Network augmented with value and action nodes.
 * The network is simply defined as a set of nodes connected with each other.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class BNetwork implements IdChangeListener {

	// logger
	public static Logger log = new Logger("BNetwork", Logger.Level.DEBUG);

	// the set of nodes for the network
	Map<String,BNode> nodes;

	// the chance nodes 
	Map<String,ChanceNode> chanceNodes;
	// the utility nodes
	Map<String, UtilityNode> utilityNodes;	
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
		utilityNodes = new HashMap<String,UtilityNode>();
		actionNodes = new HashMap<String,ActionNode>();
	}

	/**
	 * Adds a new node to the network.  Note: if the node already exists, it is better
	 * to use the "replaceNode" method, to avoid warning messages.
	 * 
	 * @param node the node to add
	 */
	public synchronized void addNode(BNode node) {
		if (nodes.containsKey(node.getId())) {
			log.warning("network already contains a node with identifier " + node.getId());
		}
		nodes.put(node.getId(), node);
		node.addIdChangeListener(this);

		// adding the node in the type-specific collections
		if (node instanceof ChanceNode) {
			chanceNodes.put(node.getId(), (ChanceNode)node);
		}
		else if (node instanceof UtilityNode) {
			utilityNodes.put(node.getId(), (UtilityNode)node);
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
			log.debug("network does not contain a node with identifier " + node.getId());
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
	public synchronized BNode removeNode(String nodeId) {
		if (!nodes.containsKey(nodeId)) {
	//		log.warning("network does not contain a node with identifier " + nodeId);
		}
		else {
			BNode node = nodes.get(nodeId);
			node.removeIdChangeListener(this);

			for (BNode inputNode : node.getInputNodes()) {
				node.removeInputNode(inputNode.getId());
			}
			for (BNode outputNode: node.getOutputNodes()) {
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
	 */
	public void removeNodes(Collection<String> valueNodeIds) {
		for (String id: new ArrayList<String>(valueNodeIds)) {
			removeNode(id);
		}
	}



	/**
	 * Modifies the node identifier in the Bayesian Network
	 * 
	 * @param oldNodeId the old node identifier
	 * @param newNodeId the new node identifier
	 */
	@Override
	public synchronized void modifyNodeId(String oldNodeId, String newNodeId) {
		BNode node = nodes.remove(oldNodeId);
		chanceNodes.remove(oldNodeId);
		utilityNodes.remove(oldNodeId);
		actionNodes.remove(oldNodeId);
		if (node != null) {
			addNode(node);
		}
		else {
			log.warning("node " + oldNodeId + " did not exist, cannot change its identifier");
		}
	}
	
	
	/**
	 * Merges all the utility nodes into a single one,combining all their dependency 
	 * relations.  This method is used when reducing the network.
	 * 
	 * @throws DialException if the merge operation failed
	 */
	/**
	private void mergeUtilityNodes() throws DialException {

		// we need to merge the utility nodes together
		if (!getUtilityNodeIds().isEmpty()) {
			UtilityNode totalNode = new UtilityNode("");		
			for (UtilityNode utilNode : new HashSet<UtilityNode>(getUtilityNodes())){
				totalNode.setId(totalNode.getId()+utilNode.getId()+"+");
				for (BNode input : utilNode.getInputNodes()) {
					if (!totalNode.hasInputNode(input.getId())) {
						totalNode.addInputNode(input);
					}
				}
				removeNode(utilNode.getId());
			}
			totalNode.setId(totalNode.getId().substring(0, totalNode.getId().length()-1));
			addNode(totalNode);
		}
	} */



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
	 * Returns true if the network contains a chance node with the given
	 * identifier, and false otherwise
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
		for (String nodeId: nodeIds) {
			if (!chanceNodes.containsKey(nodeId)) {
				return false;
			}
		}
		return true;
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
	 * Returns the collection of chance node identifiers currently in 
	 * the network
	 * 
	 * @return the collection of identifiers of chance nodes
	 */
	public Set<String> getChanceNodeIds() {
		return chanceNodes.keySet();
	}

	/**
	 * Returns true if the network contains an action node with the given
	 * identifier, and false otherwise
	 * 
	 * @param nodeId the node identifier to check
	 * @return true if a action node is found, false otherwise
	 */
	public boolean hasActionNode(String nodeId) {
		return actionNodes.containsKey(nodeId);
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
	 * Returns the collection of action node identifiers currently in 
	 * the network
	 * 
	 * @return the collection of identifiers of action nodes
	 */
	public Set<String> getActionNodeIds() {
		return actionNodes.keySet();
	}

	/**
	 * Returns true if the network contains a utility node with the given
	 * identifier, and false otherwise
	 * 
	 * @param nodeId the node identifier to check
	 * @return true if a utility node is found, false otherwise
	 */
	public boolean hasUtilityNode(String nodeId) {
		return utilityNodes.containsKey(nodeId);
	}


	/**
	 * Returns the utility node associated with the identifier, if one
	 * exists.  Else, returns null
	 * 
	 * @param nodeId the node identifier
	 * @return the utility node
	 */
	public UtilityNode getUtilityNode(String nodeId) {
		if (!utilityNodes.containsKey(nodeId)) {
			log.severe("network does not contain a utility node with identifier " + nodeId);
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
	 * Returns the collection of utility node identifiers currently in 
	 * the network
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
	 * Returns a unique node identifier, with the given base identifier
	 * complemented with a counter
	 * 
	 * @param baseId the base identifier
	 * @return the unique node identifier, with baseVar+counter
	 */
	public synchronized String getUniqueNodeId(String baseId) {
		if (!nodes.containsKey(baseId)) {
			return baseId;
		}
		else {
			int counter = 2;
			while (nodes.containsKey(baseId +"^" + counter)) {
				counter++;
			}
			return baseId +"^" +counter;
		}

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
	

	/**
	 * Returns the identifiers for the node that remain identical in this network and the one
	 * given as argument, taking into account the evidence.
	 * 
	 * @param reducedNetwork the other network
	 * @param evidence the evidence to consider as well
	 * @return the set of node identifiers that remain identical
	 */
	public Set<String> getIdenticalNodes(BNetwork reducedNetwork, Assignment evidence) {
		Set<String> identicalNodes = new HashSet<String>();
		for (ChanceNode node : reducedNetwork.getChanceNodes()) {
			ChanceNode initNode = getChanceNode(node.getId());
				if (node.getInputNodeIds().equals(initNode.getInputNodeIds()) 
			//			&& node.getChanceOutputNodesIds().equals(initNode.getChanceOutputNodesIds()) 
						&& !initNode.hasDescendant(evidence.getVariables())
				//		&& !initNode.hasDescendant(Pattern.compile(".*(\\^p)"))
						) {
					identicalNodes.add(node.getId());
				//	log.debug("identical node: " + node.getId() + " since descendants " 
				//	+ initNode.getDescendantIds() + " and evidence " + evidence);
			}	
		}	
		return identicalNodes;
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
		BNetwork copyNetwork = new BNetwork();
		List<BNode> sortedNodes = getSortedNodes();
		Collections.reverse(sortedNodes);

		for (BNode node : sortedNodes) {
			BNode nodeCopy = node.copy();
			for (BNode inputNode : node.getInputNodes()) {
				if (!copyNetwork.hasNode(inputNode.getId())) {
					throw new DialException("cannot copy the network: structure " +
							"is corrupt (" + inputNode.getId() + " is not present)");
				}
				nodeCopy.addInputNode(copyNetwork.getNode(inputNode.getId()));
			}
			copyNetwork.addNode(nodeCopy);
		}
		return copyNetwork;
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
			s += "\t" + node.getInputNodeIds() + "-->" + node.getId();
		}
		return s;
	}


	/**
	 * Returns the set of node IDs that are the leaves of the provided node (as
	 * identified by its ID)
	 * 
	 * @return the set of leave node IDs
	 */
	public Set<String> getLeafNodesIds(String topNodeId) {
		Set<String> leaves = new HashSet<String>();
		if (hasNode(topNodeId)) {
			BNode topNode = nodes.get(topNodeId);
			for (String ancestorId : topNode.getAncestorIds()) {
				if (nodes.get(ancestorId).getAncestorIds().isEmpty()) {
					leaves.add(ancestorId);
				}
			}
		}
		return leaves;
	}

	public void addNetwork(BNetwork network) throws DialException {
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

}
