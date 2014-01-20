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
package opendial.inference.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.datastructs.Assignment;

/**
 * Abstract class representing a generic inference query. Each query is composed of a
 * Bayesian network, a set of query variables, and some evidence.
 *  
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public abstract class Query {
	
	// logger
	public static Logger log = new Logger("Query", Logger.Level.DEBUG);
	
	// the Bayesian network for the query
	BNetwork network;
	
	// the query variables
	Collection<String> queryVars;
	
	// the evidence
	Assignment evidence;
	
	/**
	 * Creates a new query with the three elements.
	 * 
	 * @param network the Bayesian network
	 * @param queryVars the query variables
	 * @param evidence the query evidence
	 */
	protected Query (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) {
		this.network = network;
		this.queryVars = queryVars;
		this.evidence = evidence; 
		
		if (queryVars.isEmpty()) {
			log.warning("empty set of query variables: " + toString());
		}
		else if (!network.getNodeIds().containsAll(queryVars)) {
			log.warning("mismatch between query variables and network nodes: " 
					+ queryVars + " not included in " + network.getNodeIds());
		}
	/**	else if (!network.getNodeIds().containsAll(evidence.getVariables())) {
			log.warning("mismatch between evidence variables and network nodes: " 
					+ evidence.getVariables() + " not included in " + network.getNodeIds());
		} */
	}

	
	/**
	 * Returns the Bayesian network for the query
	 * 
	 * @return the network
	 */
	public BNetwork getNetwork() {
		return network;
	}
	
	/**
	 * Returns the query variables for the query
	 * @return
	 */
	public Set<String> getQueryVars() {
		return new HashSet<String>(queryVars);
	}
	
	/**
	 * Returns the evidence
	 * 
	 * @return the evidence
	 */
	public Assignment getEvidence() {
		return evidence;
	}
	
	
	/**
	 * Returns a list of nodes sorted according to the ordering in 
	 * BNetwork.getSortedNodes() and pruned from the irrelevant nodes
	 * 
	 * @return the ordered list of relevant nodes
	 */
	public List<BNode> getFilteredSortedNodes() {
		List<BNode> filteredNodes = new ArrayList<BNode>();
		Set<String> irrelevantNodes = getIrrelevantNodes();
		for (BNode node : network.getSortedNodes()) {
			if (!irrelevantNodes.contains(node.getId())) {
				filteredNodes.add(node);
			}
		}
		return filteredNodes;
	}
	
	
	/**
	 * Returns a string representation of the query
	 */
	@Override
	public String toString() {
		String str = "";
		for (String q : queryVars) {
			str+= q + ",";
		}
		if (!queryVars.isEmpty()) {
			str = str.substring(0, str.length()-1);
		}
		if (!evidence.isEmpty()) {
			str += "|";
			if (!evidence.isEmpty()) {
				str += evidence;
			}
		}
		str += "";
		return str;
	}

	
	/**
	 * Returns the hashcode for the query
	 * 
	 * @return hashcode
	 */
	@Override
	public int hashCode() {
		return queryVars.hashCode() +2*evidence.hashCode();
	}
	
	
	/**
	 * Returns the set of nodes in the network that are not relevant for 
	 * the given query and can therefore be pruned from the inference.
	 * 
	 * @return the list of irrelevant nodes for the query
	 */
	protected abstract Set<String> getIrrelevantNodes() ;
	
}
