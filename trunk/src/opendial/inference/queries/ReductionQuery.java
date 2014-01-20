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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;

/**
 * Representation of a reduction query corresponding to the estimation of the 
 * probability distributions for a reduced version of the Bayesian network.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ReductionQuery extends ProbQuery {

	/**
	 * Creates a new reduction query with the given network and the variables to retain
	 * in the reduced network.
	 * 
	 * @param network the full Bayesian network
	 * @param varsToRetain the subset of variables to retain in the network
	 */
	public ReductionQuery (BNetwork network, String... varsToRetain) {
		this(network, Arrays.asList(varsToRetain), new Assignment());
	}
	
	/**
	 * Creates a new reduction query with the given network and the variables to retain
	 * in the reduced network.
	 * 
	 * @param network the full Bayesian network
	 * @param varsToRetain the subset of variables to retain in the network
	 */
	public ReductionQuery (BNetwork network, Collection<String> varsToRetain) {
		this(network, varsToRetain, new Assignment());
	}


	/**
	 * Creates a new reduction query with the given network and the variables to retain
	 * in the reduced network.
	 * 
	 * @param network the full Bayesian network
	 * @param varsToRetain the subset of variables to retain in the network
	 * @param the additional evidence
	 */
	public ReductionQuery (BNetwork network, Collection<String> varsToRetain, Assignment evidence) {
		super(network, varsToRetain, evidence);
	}

	/**
	 * Creates a new reduction query with the given network and the variables to retain
	 * in the reduced network.
	 * 
	 * @param network the structured network
	 * @param varsToRetain the subset of variables to retain in the network
	 */
	public ReductionQuery (DialogueState sn, Collection<String> varsToRetain) {
		this(sn, varsToRetain, sn.getEvidence());
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
	 * Returns the input nodes of the variable in the reduced network
	 * 
	 * @param var the variable
	 * @return the input nodes of the variable in the reduced network
	 */
	public Set<String> getInputNodes(String var) {
		Set<String> ancestors = network.getNode(var).getAncestorsIds(queryVars);
		return ancestors;
	}
	
	
	@Override
	public String toString() {
		String prob = super.toString();
		return "Reduce"+prob.substring(1, prob.length());
	}
		
	
}
