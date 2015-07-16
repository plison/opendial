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

package opendial.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.UtilityNode;
import opendial.datastructs.Assignment;

/**
 * Representation of an inference query, which can be either a probability query, a
 * utility query, or a reduction query.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public abstract class Query {

	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Representation of a probability query P(queryVars | evidence) on a specific
	 * Bayesian network.
	 */
	public static final class ProbQuery extends Query {

		public ProbQuery(BNetwork network, Collection<String> queryVars,
				Assignment evidence) {
			super(network, queryVars, evidence);
		}
	}

	/**
	 * Representation of an utility query U(queryVars | evidence) on a specific
	 * Bayesian network.
	 */
	public static final class UtilQuery extends Query {

		public UtilQuery(BNetwork network, Collection<String> queryVars,
				Assignment evidence) {
			super(network, queryVars, evidence);
		}
	}

	/**
	 * Representation of a reduction Query where the Bayesian network is reduced to a
	 * new network containing only the variables queryVars, and integrating the
	 * evidence.
	 */
	public static final class ReduceQuery extends Query {

		public ReduceQuery(BNetwork network, Collection<String> queryVars,
				Assignment evidence) {
			super(network, queryVars, evidence);
		}
	}

	BNetwork network;
	Collection<String> queryVars;
	Assignment evidence;

	public Query(BNetwork network, Collection<String> queryVars,
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
	}

	/**
	 * Returns the Bayesian network for the query.
	 * 
	 * @return the Bayesian network.
	 */
	public BNetwork getNetwork() {
		return network;
	}

	/**
	 * Returns the query variables.
	 * 
	 * @return the query variables.
	 */
	public Collection<String> getQueryVars() {
		return queryVars;
	}

	/**
	 * Returns the evidence for the query.
	 * 
	 * @return the evidence.
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
	 * Assuming a particular query P(queryVars|evidence) or U(queryVars|evidence) on
	 * the provided Bayesian network, determines which nodes is relevant for the
	 * inference and which one can be discarded without affecting the final result.
	 * 
	 */
	private Set<String> getIrrelevantNodes() {

		Set<String> irrelevantNodesIds = new HashSet<String>();

		whileLoop: while (true) {
			for (String nodeId : new ArrayList<String>(network.getNodeIds())) {
				BNode node = network.getNode(nodeId);
				if (!irrelevantNodesIds.contains(nodeId)
						&& irrelevantNodesIds.containsAll(node.getOutputNodesIds())
						&& !queryVars.contains(nodeId)
						&& !evidence.containsVar(nodeId)
						&& !(node instanceof UtilityNode)) {
					irrelevantNodesIds.add(nodeId);
					continue whileLoop;
				}
				else if (!(this instanceof UtilQuery)
						&& !irrelevantNodesIds.contains(nodeId)
						&& node instanceof UtilityNode) {
					irrelevantNodesIds.add(nodeId);
					continue whileLoop;
				}
			}
			break whileLoop;
		}

		return irrelevantNodesIds;
	}

	/**
	 * Returns a string representation of the query.
	 */
	@Override
	public String toString() {
		String str = "";
		for (String q : queryVars) {
			str += q + ",";
		}
		if (!queryVars.isEmpty()) {
			str = str.substring(0, str.length() - 1);
		}
		if (!evidence.isEmpty()) {
			str += "|";
			if (!evidence.isEmpty()) {
				str += evidence;
			}
		}
		str += "";

		if (this instanceof ProbQuery) {
			return "P(" + str + ")";
		}
		else if (this instanceof UtilQuery) {
			return "U(" + str + ")";
		}
		else {
			return "Reduce(" + str + ")";
		}
	}

	/**
	 * Returns the query variables in sorted order (from the base to the leaves)
	 * 
	 * @return the ordered query variables
	 */
	public List<String> getSortedQueryVars() {
		List<String> sorted = new ArrayList<String>();
		for (BNode n : network.getSortedNodes()) {
			if (queryVars.contains(n.getId())) {
				sorted.add(n.getId());
			}
		}
		Collections.reverse(sorted);
		return sorted;
	}

	/**
	 * Returns the hashcode for the query
	 * 
	 * @return hashcode
	 */
	@Override
	public int hashCode() {
		return queryVars.hashCode() + 2 * evidence.hashCode();
	}

}
