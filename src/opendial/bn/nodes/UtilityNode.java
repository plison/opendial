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

package opendial.bn.nodes;

import java.util.logging.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.bn.distribs.UtilityFunction;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a utility node (sometimes also called utility node)
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class UtilityNode extends BNode {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the utility distribution
	protected UtilityFunction distrib;

	// ===================================
	// NODE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new utility node, with an empty utility distribution
	 * 
	 * @param nodeId the node identifier
	 */
	public UtilityNode(String nodeId) {
		super(nodeId);
		distrib = new UtilityTable();
	}

	/**
	 * Creates a new utility node, with the given utility distribution
	 * 
	 * @param nodeId the node identifier
	 * @param distrib the utility distribution
	 */
	public UtilityNode(String nodeId, UtilityFunction distrib) {
		super(nodeId);
		this.distrib = distrib;
	}

	/**
	 * Adds a new utility to the node, valid for the given assignment on the input
	 * nodes
	 * 
	 * @param input a value assignment on the input nodes
	 * @param value the assigned utility
	 */
	public void addUtility(Assignment input, double value) {
		if (distrib instanceof UtilityTable) {
			((UtilityTable) distrib).setUtil(input, value);
		}
		else {
			log.warning("utility distribution is not a table, cannot add value");
		}
	}

	/**
	 * Removes the utility associated with the input assignment from the node
	 * 
	 * @param input the input associated with the utility to be removed
	 */
	public void removeUtility(Assignment input) {
		if (distrib instanceof UtilityTable) {
			((UtilityTable) distrib).removeUtil(input);
		}
		else {
			log.warning("utility distribution is not a table, cannot remove value");
		}
	}

	public void setDistrib(UtilityFunction distrib) {
		this.distrib = distrib;
	}

	@Override
	public void setId(String newId) {
		super.setId(newId);
		distrib.modifyVariableId(this.nodeId, newId);
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the utility associated with the specific assignment on the input
	 * variables of the node
	 * 
	 * @param input the input assignment
	 * @return the associated utility
	 */
	public double getUtility(Assignment input) {
		return distrib.getUtil(input);
	}

	/**
	 * Returns an empty set (a utility node has no "value", only utilities).
	 * 
	 * @return the empty set
	 */
	@Override
	public Set<Value> getValues() {
		return Collections.emptySet();
	}

	/**
	 * Returns the utility distribution
	 * 
	 * @return the utility distribution
	 */
	public UtilityFunction getFunction() {
		return distrib;
	}

	/**
	 * Returns the factor matrix associated with the utility node, which maps an
	 * assignment of input variable to a given utility.
	 *
	 * @return the factor matrix
	 */
	@Override
	public Map<Assignment, Double> getFactor() {

		Map<Assignment, Double> factor = new HashMap<Assignment, Double>();

		Set<Assignment> combinations = getPossibleConditions();
		for (Assignment combination : combinations) {
			factor.put(combination, distrib.getUtil(combination));
		}
		return factor;
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns a copy of the utility node. Note that only the node content is copied,
	 * not its connection with other nodes.
	 * 
	 * @return the copy
	 */
	@Override
	public UtilityNode copy() {
		UtilityNode copy = new UtilityNode(nodeId, distrib.copy());
		return copy;
	}

	/**
	 * Returns a string representation of the node, consisting of the node utility
	 * distribution
	 */
	@Override
	public String toString() {
		return distrib.toString();
	}

	/**
	 * Returns the hashcode for the value, computed from the node identifier and the
	 * distribution
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode() - distrib.hashCode();
	}

}
