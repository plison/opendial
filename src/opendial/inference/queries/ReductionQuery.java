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
 * @version $Date:: 2014-03-20 21:16:08 #$
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
	 * @param evidence the additional evidence
	 */
	public ReductionQuery (BNetwork network, Collection<String> varsToRetain, Assignment evidence) {
		super(network, varsToRetain, evidence);
	}

	/**
	 * Creates a new reduction query with the given network and the variables to retain
	 * in the reduced network.
	 * 
	 * @param state the dialogue state
	 * @param varsToRetain the subset of variables to retain in the network
	 */
	public ReductionQuery (DialogueState state, Collection<String> varsToRetain) {
		this(state, varsToRetain, state.getEvidence());
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
