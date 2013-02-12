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
import java.util.List;
import java.util.Set;

import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.UtilityNode;
import opendial.state.DialogueState;

public class ReductionQuery extends Query {

	List<String> nodesToIsolate = new ArrayList<String>();
	
	public ReductionQuery (BNetwork network, String... queryVars) {
		this(network, getCollection(queryVars));
	}
	
	public ReductionQuery (DialogueState state, String... queryVars) {
		this(state, getCollection(queryVars));
	}
	
	public ReductionQuery(DialogueState state, Collection<String> queryVars, Collection<String> nodesToIsolate) {
		this(state.getNetwork(), queryVars, state.getEvidence());
		setNodesToIsolate(nodesToIsolate);
	}
	
	public ReductionQuery (DialogueState state, Collection<String> queryVars) {
		this(state.getNetwork(), queryVars, state.getEvidence());
	}
	
	public ReductionQuery (BNetwork network, Collection<String> queryVars) {
		this(network, queryVars, new Assignment());
	}
	
	public ReductionQuery (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) {
		super(network, queryVars, evidence, new ArrayList<String>());
	}
	
	public void setNodesToIsolate (Collection<String> nodesToIsolate) {
		this.nodesToIsolate.addAll(nodesToIsolate);
	}
	
	public Collection<String> getNodesToIsolate() {
		return nodesToIsolate;
	}

	/**
	 * Returns the nodes that are irrelevant for answering the given query
	 *
	 * @return the identifiers for the irrelevant nodes
	 */
	public Set<String> getIrrelevantNodes() {

			Set<String> irrelevantNodesIds = new HashSet<String>();

			boolean includeActionAndUtils = false;
			for (String var : queryVars) {
				if (network.hasActionNode(var) || network.hasUtilityNode(var)) {
					includeActionAndUtils = true;
				}
			}
			
			boolean continueLoop = true;
			while (continueLoop) {
				continueLoop = false;
				for (String nodeId : new ArrayList<String>(network.getNodeIds())) {
					BNode node = network.getNode(nodeId);
					if (!irrelevantNodesIds.contains(nodeId) && 
							irrelevantNodesIds.containsAll(node.getOutputNodesIds()) && 
							!queryVars.contains(nodeId) && 
							!evidence.containsVar(nodeId)) {
						if (!includeActionAndUtils ||
							!(node instanceof UtilityNode)) {
							irrelevantNodesIds.add(nodeId);
							continueLoop = true;
						}
					}
				}
			}
		return irrelevantNodesIds;
	}

	
	public String toString() {
		return "Reduction("+super.toString() +")";
	}

	
}
