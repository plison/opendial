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

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a chance node (sometimes also called belief node), which is
 * a random variable associated with a specific probability distribution.
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ChanceNode extends BNode {

	// logger
	public static Logger log = new Logger("ChanceNode", Logger.Level.DEBUG);

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
	 * Creates a new chance node defined by the identifier. The default
	 * probability distribution is then a probability table.
	 * 
	 * @param nodeId the node identifier
	 */
	public ChanceNode(String nodeId) {
		this(nodeId, new ConditionalTable(nodeId));
	}

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
	 * Sets the probability distribution of the node, and erases the existing
	 * one.
	 * 
	 * @param distrib the distribution for the node
	 * @throws DialException if the distribution is not well-formed
	 */
	public void setDistrib(ProbDistribution distrib) throws DialException {
		this.distrib = distrib;
		if (!distrib.getVariable().equals(nodeId)) {
			log.warning(nodeId + "  != " + distrib.getVariable());
		}
		if (!distrib.isWellFormed()) {
			throw new DialException("Distribution for node " + nodeId
					+ " (type " + distrib.getClass().getSimpleName()
					+ ") is not well-formed");
		}
		cachedValues = null;
	}

	/**
	 * Adds a new (input) relation for the node
	 *
	 * @param inputNode the input node to connect
	 * @throws DialException if the network becomes corrupted
	 */
	@Override
	public void addInputNode(BNode inputNode) throws DialException {
		super.addInputNode(inputNode);
	}

	/**
	 * Adds a new value with associated probability to the node. The probability
	 * must be independent of other nodes, and the distribution must be a
	 * probability table.
	 * 
	 * @param nodeValue the node value
	 * @param prob the associated probability
	 */
	public void addProb(Value nodeValue, double prob) {
		addProb(new Assignment(), nodeValue, prob);
	}

	/**
	 * Adds a new value with associated probability, given a specific value
	 * assignment for the conditional nodes. The distribution must be a
	 * probability table.
	 * 
	 * @param condition the condition for which the probability is value
	 * @param nodeValue the new value
	 * @param prob the associated probability
	 */
	public void addProb(Assignment condition, Value nodeValue, double prob) {
		if (distrib instanceof ConditionalTable) {
			((ConditionalTable) distrib).addRow(condition, nodeValue, prob);
		} else {
			log.warning("distribution is not defined as a dependent table table, "
					+ "impossible to add probability");
		}
		cachedValues = null;
	}

	/**
	 * Removes the probability for the given value
	 * 
	 * @param nodeValue the node value to remove
	 */
	public void removeProb(Value nodeValue) {
		removeProb(new Assignment(), nodeValue);
	}

	/**
	 * Removes the probability for the value given the condition
	 * 
	 * @param condition the condition
	 * @param nodeValue the value for the node variable
	 */
	public void removeProb(Assignment condition, Value nodeValue) {
		if (distrib instanceof ConditionalTable) {
			((ConditionalTable) distrib).removeRow(condition, nodeValue);
		} else {
			log.warning("distribution is not defined as a table, impossible "
					+ "to remove probability");
		}
		cachedValues = null;
	}

	/**
	 * Replaces the node identifier with a new one
	 *
	 * @param newId the new identifier
	 */
	@Override
	public void setId(String newId) {
		// log.debug("changing id from " + this.nodeId + " to " + nodeId);
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
	 * Returns the probability associated with a specific value, according to
	 * the current distribution.
	 * 
	 * <p>
	 * The method assumes that the node is conditionally independent of every
	 * other node. If it isn't, one should use the getProb(condition, nodeValue)
	 * method instead.
	 * 
	 * <p>
	 * NB: the method should *not* be used to perform sophisticated inference,
	 * as it is not optimised and might lead to distorted results for very
	 * dependent networks
	 * 
	 * @param nodeValue the value for the node
	 * @return its probability
	 */
	public double getProb(Value nodeValue) {

		if (distrib instanceof IndependentProbDistribution) {
			return ((IndependentProbDistribution) distrib).getProb(nodeValue);
		}

		// log.debug("Must marginalise to compute P(" + nodeId + "="+ nodeValue
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
	 * Returns the probability associated with the conditional assignment and
	 * the node value, if one is defined.
	 * 
	 * 
	 * @param condition the condition
	 * @param nodeValue the value
	 * @return the associated probability
	 */
	public double getProb(Assignment condition, Value nodeValue) {
		try {
			return distrib.getProb(condition, nodeValue);
		} catch (DialException e) {
			log.warning("exception: " + e);
			return 0.0;
		}
	}

	/**
	 * Returns a sample value for the node, according to the probability
	 * distribution currently defined.
	 * 
	 * <p>
	 * The method assumes that the node is conditionally independent of every
	 * other node. If it isn't, one should use the sample(condition) method
	 * instead.
	 * 
	 * @return the sample value
	 * @throws DialException if no sample can be selected
	 */
	public Value sample() throws DialException {

		if (distrib instanceof IndependentProbDistribution) {
			return ((IndependentProbDistribution) distrib).sample();
		}
		Assignment inputSample = new Assignment();
		for (BNode inputNode : inputNodes.values()) {
			if (inputNode instanceof ChanceNode) {
				inputSample.addPair(inputNode.getId(),
						((ChanceNode) inputNode).sample());
			} else if (inputNode instanceof ActionNode) {
				inputSample.addPair(inputNode.getId(),
						((ActionNode) inputNode).sample());
			}
		}
		return sample(inputSample);
	}

	/**
	 * Returns a sample value for the node, given a condition. The sample is
	 * selected according to the probability distribution for the node.
	 * 
	 * @param condition the value assignment on conditional nodes
	 * @return the sample value
	 * @throws DialException if no sample can be selected
	 */
	public Value sample(Assignment condition) throws DialException {
		if (distrib instanceof IndependentProbDistribution) {
			return ((IndependentProbDistribution) distrib).sample();
		}
		return distrib.sample(condition.getTrimmed(inputNodes.keySet()));
	}

	/**
	 * Returns a discrete set of values for the node. If the variable for the
	 * node has a continuous range, this set if based on a discretisation
	 * procedure defined by the distribution.
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
		} else {
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
	 * Returns the "factor matrix" mapping assignments of conditional variables
	 * + the node variable to a probability value.
	 *
	 * @return the factor matrix.
	 */
	@Override
	public Map<Assignment, Double> getFactor() {

		Map<Assignment, Double> factor = new HashMap<Assignment, Double>();

		Set<Assignment> combinations = getPossibleConditions();
		for (Assignment combination : combinations) {

			IndependentProbDistribution posterior = distrib
					.getProbDistrib(combination);
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
	 * Returns a copy of the node. Note that only the node content is copied,
	 * not its connection with other nodes.
	 *
	 * @return the copy
	 * @throws DialException if the node could not be copied.
	 */
	@Override
	public ChanceNode copy() throws DialException {
		ChanceNode cn = new ChanceNode(nodeId, distrib.copy());
		if (cachedValues != null) {
			cn.cachedValues = new HashSet<Value>(cachedValues);
		}
		return cn;
	}

	/**
	 * Returns the hashcode for the node (based on the hashcode of the
	 * identifier and the distribution).
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
