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

package opendial.inference.bn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.inference.bn.distribs.Distribution;
import opendial.utils.Logger;

/**
 * Bayesian Network representation.  The Bayesian Network is constituted
 * of nodes connected with each other by directed edges.  The network must
 * not contain any cycles.
 * 
 * <p>The class provides basic methods for adding nodes (and verify their 
 * consistency), as well as retrieving and sorting them.  To perform 
 * probabilistic inference on such network, one must uses the algorithms 
 * in package <i>opendial.inference.algorithms</i>.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNetwork {

	// logger
	static Logger log = new Logger("BNetwork", Logger.Level.DEBUG);
	
	// the nodes, indexed by identifier
	Map<String,BNode> nodes;
		

	/**
	 * Creates a Bayesian Network, with no nodes
	 */
	public BNetwork () {
		nodes = new HashMap<String, BNode>();
	}
	

	/**
	 * Adds a new node to the network.  The node must contain a well-formed distribution,
	 * and its introduction must not create cycles in the network.
	 * 
	 * @param node the node to add
	 * @throws DialException if no valid distribution, or if cycles are present
	 */
	public void addNode(BNode node) throws DialException {
		Distribution distrib = node.getDistribution();
		if (distrib == null) {
			throw new DialException("Node " + node.getId() + " must specify a distribution");
		}
		if (node.getAncestors(DialConstants.MAX_PATH_LENGTH).contains(node)) {
			throw new DialException("Cyclic dependency for node " + node);
		}
		
		nodes.put(node.getId(), node);
	}
	
	
	/**
	 * Returns the node indexed by the identifier, if one exists.  Else, 
	 * returns null.
	 * 
	 * @param nodeId the node identifier
	 * @return the node, if it exists.
	 */
	public BNode getNode(String nodeId) {
		return nodes.get(nodeId);
	}
	
	/**
	 * Returns the (unordered) collection of nodes present in the network.
	 * 
	 * @return the collection of nodes
	 */
	public Collection<BNode> getNodes() {
		return nodes.values();
	}
	
	
	/**
	 * Returns the sorted list of nodes in the network, where the ordering is 
	 * defined in the compareTo method implemented in BNode.  The ordering will 
	 * place end nodes (i.e. nodes with no outward edges) at the beginning of 
	 * the list, and start nodes  (nodes with no inward edges) at the end of the 
	 * list. 
	 * 
	 * <p>This ordering is used in particular for the variable elimination algorithm.
	 * 
	 * @return the ordered list of nodes
	 */
	public List<BNode> getSortedNodes() {
		List<BNode> nodesList = new ArrayList<BNode>(nodes.values()); 
		Collections.sort(nodesList);
	//	log.debug("sorted nodes: " + nodesList);
		return nodesList;
	}
	
	
	/**
	 * Returns the nodes identifiers
	 * 
	 * @return the nodes identifiers
	 */
	public Set<String> getNodeIds() {
		return nodes.keySet();
	}


}
