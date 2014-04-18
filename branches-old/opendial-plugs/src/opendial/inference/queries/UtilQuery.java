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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.UtilityNode;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;

/**
 * Representation of a utility query.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class UtilQuery extends Query {


	/**
	 * Creates a new utility query with the given network and query variables
	 * 
	 * @param network the Bayesian network for the query
	 * @param queryVars the query variables
	 */
	public UtilQuery (BNetwork network, String... queryVars) {
		this(network, Arrays.asList(queryVars));
	}

	/**
	 * Creates a new utility query with the given network and query variables
	 * 
	 * @param network the Bayesian network for the query
	 * @param queryVars the query variables
	 */
	public UtilQuery (BNetwork network, Collection<String> queryVars) {
		this(network, queryVars, new Assignment());
	}


	/**
	 * Creates a new utility query with the given network and query variables
	 * 
	 * @param network the Bayesian network for the query
	 * @param queryVars the query variables
	 * @param evidence the additional evidence
	 */
	public UtilQuery (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) {
		super(network, queryVars, evidence);
	}

	/**
	 * Creates a new utility query with the given network and query variables
	 * 
	 * @param state the dialogue state for the query
	 * @param queryVars the query variables
	 */
	public UtilQuery (DialogueState state, String... queryVars) {
		this(state, Arrays.asList(queryVars), state.getEvidence());
	}
	
	
	/**
	 * Creates a new utility query with the given network and query variables
	 * 
	 * @param state the dialogue state for the query
	 * @param queryVars the query variables
	 */
	public UtilQuery (DialogueState state, Collection<String> queryVars) {
		this(state, queryVars, state.getEvidence());
	}


	/**
	 * Returns a string representation for the utility query
	 */
	@Override
	public String toString() {
		return "U("+super.toString() +")";
	}


	/**
	 * Returns the nodes that are irrelevant to the inference process for the
	 * utility query
	 * 
	 * @return the set of variables that are irrelevant
	 */
	@Override
	protected Set<String> getIrrelevantNodes() {

		Set<String> irrelevantNodesIds = new HashSet<String>();

		boolean continueLoop = true;
		while (continueLoop) {
			continueLoop = false;
			for (String nodeId : new ArrayList<String>(network.getNodeIds())) {
				BNode node = network.getNode(nodeId);
				if (!irrelevantNodesIds.contains(nodeId) && 
						irrelevantNodesIds.containsAll(node.getOutputNodesIds()) && 
						!queryVars.contains(nodeId) && 
						!evidence.containsVar(nodeId) && 
						!(node instanceof UtilityNode)) {
					irrelevantNodesIds.add(nodeId);
					continueLoop = true;
				}
			}
		}

		return irrelevantNodesIds;
	}

}