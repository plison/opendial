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
	 * 
	 * @return the set of query variables
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
