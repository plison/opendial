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

package opendial.inference.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.UtilityNode;
import opendial.state.DialogueState;

public class ProbQuery extends Query {

	public ProbQuery (BNetwork network, String... queryVars) {
		this(network, getCollection(queryVars));
	}
	
	public ProbQuery (DialogueState state, String... queryVars) {
		this(state, getCollection(queryVars));
	}
	public ProbQuery (DialogueState state, Collection<String> queryVars) {
		this(state.getNetwork(), queryVars, state.getEvidence());
	}
	
	public ProbQuery (BNetwork network, Collection<String> queryVars) {
		this(network, queryVars, new Assignment());
	}
	
	public ProbQuery (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) {
		this(network, queryVars, evidence, new ArrayList<String>());
	}
	
	public ProbQuery (BNetwork network, Collection<String> queryVars, 
			Assignment evidence, Collection<String> conditionalVars) {
		super(network, queryVars, evidence, conditionalVars);
	}
	
	

	/**
	 * Returns the nodes that are irrelevant for answering the given query
	 *
	 * @return the identifiers for the irrelevant nodes
	 */
	public Set<String> getIrrelevantNodes() {
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
				else if (!irrelevantNodesIds.contains(nodeId) &&
						node instanceof UtilityNode) {
					irrelevantNodesIds.add(nodeId);
					continueLoop = true;
				}
			}
		}

		return irrelevantNodesIds;
	}

	
	
	public String toString() {
		return "P("+super.toString() +")";
	}
	
	
}
