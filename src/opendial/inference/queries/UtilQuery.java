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
