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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import opendial.Settings;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.SingleValueDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a chance node (sometimes also called belief node), which is a
 * random variable associated with a specific probability distribution.
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ChanceNode extends BNode {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the probability distribution for the node
	protected ProbDistribution distrib;

	// the set of cached values for the node
	// NB: if the node has a continuous range, these values are based on
	// a discretisation procedure defined by the distribution
	protected Set<Value> cachedValues;

	// ===================================
	// NODE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new chance node, with the given identifier and probability
	 * distribution
	 * 
	 * @param nodeId the unique node identifier
	 * @param distrib the probability distribution for the node
	 */
	public ChanceNode(String nodeId, ProbDistribution distrib) {
		super(nodeId);
		if (!distrib.getVariable().equals(nodeId)) {
			log.warning(nodeId + "  != " + distrib.getVariable());
		}
		this.distrib = distrib;
	}

	/**
	 * Creates a change node with a unique value (associated with a probability 1.0)
	 * 
	 * @param nodeId the node identifier
	 * @param value the single value for the node
	 */
	public ChanceNode(String nodeId, Value value) {
		this(nodeId, new SingleValueDistribution(nodeId, value));
	}

	/**
	 * Sets the probability distribution of the node, and erases the existing one.
	 * 
	 * @param distrib the distribution for the node
	 */
	public void setDistrib(ProbDistribution distrib) {
		this.distrib = distrib;
		if (!distrib.getVariable().equals(nodeId)) {
			log.warning(nodeId + "  != " + distrib.getVariable());
		}
		cachedValues = null;
	}

	/**
	 * Adds a new (input) relation for the node
	 *
	 * @param inputNode the input node to connect
	 */
	@Override
	public void addInputNode(BNode inputNode) {
		super.addInputNode(inputNode);
	}

	/**
	 * Replaces the node identifier with a new one
	 *
	 * @param newId the new identifier
	 */
	@Override
	public void setId(String newId) {
		// log.fine("changing id from " + this.nodeId + " to " + nodeId);
		String oldId = nodeId;
		super.setId(newId);
		distrib.modifyVariableId(oldId, newId);
	}

	/**
	 * Prune the values with a probability below a given threshold
	 * 
	 * @param threshold the probability threshold
	 */
	public void pruneValues(double threshold) {
		if (distrib.pruneValues(threshold)) {
			cachedValues = null;
		}
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the probability associated with a specific value, according to the
	 * current distribution.
	 * 
	 * <p>
	 * The method assumes that the node is conditionally independent of every other
	 * node. If it isn't, one should use the getProb(condition, nodeValue) method
	 * instead.
	 * 
	 * <p>
	 * NB: the method should *not* be used to perform sophisticated inference, as it
	 * is not optimised and might lead to distorted results for very dependent
	 * networks
	 * 
	 * @param nodeValue the value for the node
	 * @return its probability
	 */
	public double getProb(Value nodeValue) {

		if (distrib instanceof IndependentDistribution) {
			return ((IndependentDistribution) distrib).getProb(nodeValue);
		}

		// log.fine("Must marginalise to compute P(" + nodeId + "="+ nodeValue
		// + ")");
		Set<Assignment> combinations = getPossibleConditions();
		double totalProb = 0.0;
		for (Assignment combi : combinations) {
			double prob = 1.0;
			for (BNode inputNode : inputNodes.values()) {
				if (inputNode instanceof ChanceNode) {
					Value value = combi.getValue(inputNode.getId());
					prob = prob * ((ChanceNode) inputNode).getProb(value);
				}
			}
			totalProb += prob * getProb(combi, nodeValue);
		}
		return totalProb;
	}

	/**
	 * Returns the probability associated with the conditional assignment and the
	 * node value, if one is defined.
	 * 
	 * 
	 * @param condition the condition
	 * @param nodeValue the value
	 * @return the associated probability
	 */
	public double getProb(Assignment condition, Value nodeValue) {
		try {
			return distrib.getProb(condition, nodeValue);
		}
		catch (RuntimeException e) {
			log.warning("exception: " + e);
			return 0.0;
		}
	}

	/**
	 * Returns a sample value for the node, according to the probability distribution
	 * currently defined.
	 * 
	 * <p>
	 * The method assumes that the node is conditionally independent of every other
	 * node. If it isn't, one should use the sample(condition) method instead.
	 * 
	 * @return the sample value
	 */
	public Value sample() {

		if (distrib instanceof IndependentDistribution) {
			return ((IndependentDistribution) distrib).sample();
		}
		Assignment inputSample = new Assignment();
		for (BNode inputNode : inputNodes.values()) {
			if (inputNode instanceof ChanceNode) {
				inputSample.addPair(inputNode.getId(),
						((ChanceNode) inputNode).sample());
			}
			else if (inputNode instanceof ActionNode) {
				inputSample.addPair(inputNode.getId(),
						((ActionNode) inputNode).sample());
			}
		}
		return sample(inputSample);
	}

	/**
	 * Returns a sample value for the node, given a condition. The sample is selected
	 * according to the probability distribution for the node.
	 * 
	 * @param condition the value assignment on conditional nodes
	 * @return the sample value
	 */
	public Value sample(Assignment condition) {
		if (distrib instanceof IndependentDistribution) {
			return ((IndependentDistribution) distrib).sample();
		}
		return distrib.sample(condition);
	}

	/**
	 * Returns a discrete set of values for the node. If the variable for the node
	 * has a continuous range, this set if based on a discretisation procedure
	 * defined by the distribution.
	 *
	 * @return the discrete set of values
	 */
	@Override
	public Set<Value> getValues() {
		if (cachedValues == null) {
			cachedValues = distrib.getValues();
		}
		return cachedValues;
	}

	/**
	 * Returns the number of values for the node
	 * 
	 * @return the number of values
	 */
	public int getNbValues() {
		if (distrib instanceof ContinuousDistribution) {
			return Settings.discretisationBuckets;
		}
		else {
			return getValues().size();
		}
	}

	/**
	 * Returns the probability distribution attached to the node
	 * 
	 * @return the distribution
	 */
	public ProbDistribution getDistrib() {
		return distrib;
	}

	/**
	 * Returns the "factor matrix" mapping assignments of conditional variables + the
	 * node variable to a probability value.
	 *
	 * @return the factor matrix.
	 */
	@Override
	public Map<Assignment, Double> getFactor() {

		Map<Assignment, Double> factor = new HashMap<Assignment, Double>();

		Set<Assignment> combinations = getPossibleConditions();
		for (Assignment combination : combinations) {

			IndependentDistribution posterior = distrib.getProbDistrib(combination);
			for (Value value : posterior.getValues()) {
				factor.put(new Assignment(combination, nodeId, value),
						posterior.getProb(value));
			}
		}
		return factor;
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns a copy of the node. Note that only the node content is copied, not its
	 * connection with other nodes.
	 *
	 * @return the copy
	 */
	@Override
	public ChanceNode copy() {
		ChanceNode cn = new ChanceNode(nodeId, distrib.copy());
		if (cachedValues != null) {
			cn.cachedValues = new HashSet<Value>(cachedValues);
		}
		return cn;
	}

	/**
	 * Returns the hashcode for the node (based on the hashcode of the identifier and
	 * the distribution).
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + distrib.hashCode();
	}

	/**
	 * Returns the string representation of the distribution.
	 * 
	 */
	@Override
	public String toString() {
		String str = distrib.toString();
		return str;
	}

	// ===================================
	// PRIVATE AND PROTECTED METHODS
	// ===================================

	@Override
	protected void modifyVariableId(String oldId, String newId) {
		super.modifyVariableId(oldId, newId);
		distrib.modifyVariableId(oldId, newId);
	}

}
