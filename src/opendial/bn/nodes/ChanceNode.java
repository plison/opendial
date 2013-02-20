// =================================================================                                                                   
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

package opendial.bn.nodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.DepEmpiricalDistribution;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;


/**
 * Representation of a chance node (sometimes also called belief node), which
 * is a random variable associated with a specific probability distribution.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
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
	Set<Value> cachedValues;
	

	// ===================================
	//  NODE CONSTRUCTION
	// ===================================


	/**
	 * Creates a new chance node defined by the identifier.  The default probability
	 * distribution is then a probability table.
	 * 
	 * @param nodeId the node identifier
	 * @throws DialException if the distribution is not well-formed
	 */
	public ChanceNode(String nodeId)  {
		this(nodeId, new DiscreteProbabilityTable());
	}

	/**
	 * Creates a new chance node, with the given identifier and 
	 * probability distribution
	 * 
	 * @param nodeId the unique node identifier
	 * @param distrib the probability distribution for the node
	 * @throws DialException if the distribution is not well-formed
	 */
	public ChanceNode(String nodeId, ProbDistribution distrib) {
		super(nodeId);
		this.distrib = distrib;		
	}


	/**
	 * Sets the probability distribution of the node, and erases the existing one.
	 * 
	 * @param distrib the distribution for the node
	 * @throws DialException if the distribution is not well-formed
	 */
	public void setDistrib(ProbDistribution distrib) throws DialException {
		this.distrib = distrib;
		if (distrib.getHeadVariables().size() != 1) {
		
			log.debug("Distribution for " + nodeId + 
					"should have only one head variable, but is has: " + distrib.getHeadVariables() +
					" (distrib type=" + distrib.getClass().getCanonicalName()+")");
		}
		if (!distrib.isWellFormed()) {
			throw new DialException("Distribution for node " + nodeId + " (type " +
					distrib.getClass().getSimpleName() + ") is not well-formed");
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
		cachedValues = null;
	}


	/**
	 * Adds a new value with associated probability to the node.  The probability
	 * must be independent of other nodes, and the distribution must be a probability
	 * table.
	 * 
	 * @param nodeValue the node value
	 * @param prob the associated probability
	 */
	public void addProb (Value nodeValue, double prob) {
		addProb(new Assignment(), nodeValue, prob);
	}


	/**
	 * Adds a new value with associated probability, given a specific value assignment for the
	 * conditional nodes.  The distribution must be a probability table.
	 * 
	 * @param condition the condition for which the probability is value
	 * @param nodeValue the new value
	 * @param prob the associated probability
	 */
	public void addProb(Assignment condition, Value nodeValue, double prob) {
		if (distrib instanceof DiscreteProbabilityTable) {
			((DiscreteProbabilityTable)distrib).addRow(condition, new Assignment(nodeId, nodeValue), prob);
		}
		else {
			log.warning("distribution is not defined as a dependent table table, " +
					"impossible to add probability");
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
		if (distrib instanceof DiscreteProbabilityTable) {
			((DiscreteProbabilityTable)distrib).removeRow(condition, new Assignment(nodeId, nodeValue));
		}
		else {
			log.warning("distribution is not defined as a table, impossible " +
					"to remove probability");
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
	//	log.debug("changing id from " + this.nodeId + " to " + nodeId);
		String oldId = nodeId;
		super.setId(newId);
		distrib.modifyVarId(oldId, newId);
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the probability associated with a specific value, according to the
	 * current distribution.
	 * 
	 * <p>The method assumes that the node is conditionally independent of every other
	 * node.  If it isn't, one should use the getProb(condition, nodeValue) method 
	 * instead.
	 * 
	 * NB: the method should *not* be used to perform sophisticated inference, as it is
	 * not optimised and might lead to the distorted results for very dependent networks
	 * 
	 * @param nodeValue the value for the node
	 * @return its probability
	 */
	public double getProb(Value nodeValue) {
		Set<Assignment> combinations = getPossibleConditions();
		double totalProb = 0.0;
		if (combinations.size() > 1) {
			log.debug("marginalisation necessary to compute P("+nodeId+"=" + nodeValue+")");
		}
		for (Assignment combi : combinations) {
			double prob = 1.0;
			for (BNode inputNode : inputNodes.values()) {
				if (inputNode instanceof ChanceNode) {
					Value value = combi.getValue(inputNode.getId());
					prob = prob * ((ChanceNode)inputNode).getProb(value);
				}
			}
			totalProb += prob * getProb(combi, nodeValue);
		}
		return totalProb;
	}


	/**
	 * Returns the probability associated with the conditional assignment and the node
	 * value, if one is defined.  
	 * 
	 * 
	 * @param condition the condition
	 * @param nodeValue the value
	 * @return the associated probability
	 */
	public double getProb(Assignment condition, Value nodeValue) {
		return distrib.toDiscrete().getProb(condition, new Assignment(nodeId, nodeValue));			
	}


	/**
	 * Returns true if the node has a defined probability value associated with the given input
	 * condition and the node value.
	 * 
	 * @param condition the value assignment for the input nodes
	 * @param nodeValue the node value
	 * @return true if the node has assigned a probability to the value, false otherwise
	 */
	public boolean hasProb(Assignment condition, Value nodeValue) {
		return ((ProbDistribution)distrib).toDiscrete().hasProb
				(condition, new Assignment(nodeId, nodeValue));				
	}


	/**
	 * Returns a sample value for the node, according to the probability distribution
	 * currently defined.
	 * 
	 * <p>The method assumes that the node is conditionally independent of every 
	 * other node. If it isn't, one should use the sample(condition) method instead.
	 * 
	 * @return the sample value
	 * @throws DialException if no sample can be selected
	 */
	public Value sample() throws DialException {

		Assignment inputSample = new Assignment();
		for (BNode inputNode : inputNodes.values()) {
			if (inputNode instanceof ChanceNode) {
				inputSample.addPair(inputNode.getId(), ((ChanceNode)inputNode).sample());
			}
			else if (inputNode instanceof ActionNode) {
				inputSample.addPair(inputNode.getId(), ((ActionNode)inputNode).sample());
			}
		}
		return sample(inputSample);

	}

	/**
	 * Returns a sample value for the node, given a condition.  The sample is selected
	 * according to the probability distribution for the node.
	 * 
	 * @param condition the value assignment on conditional nodes
	 * @return the sample value
	 * @throws DialException if no sample can be selected
	 */
	public Value sample(Assignment condition) throws DialException {
		Assignment result = distrib.sample(condition.getTrimmed(inputNodes.keySet()));
		if (!result.containsVar(nodeId)) {
			log.warning("result of sampling does not contain " + nodeId +": " + 
					result + " distrib is " + distrib.getClass().getSimpleName());
			return ValueFactory.none();
		}
		return result.getValue(nodeId);
	}


	/**
	 * Returns a discrete set of values for the node.  If the variable for the 
	 * node has a continuous range, this set if based on a discretisation 
	 * procedure defined by the distribution.
	 *
	 * @return the discrete set of values
	 */
	@Override
	public Set<Value> getValues() {

		if (cachedValues == null) {
			fillCachedValues();
		}

		return new HashSet<Value>(cachedValues);
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
	public Map<Assignment,Double> getFactor() {

		Map<Assignment,Double> factor = new HashMap<Assignment,Double>();

		Set<Assignment> combinations = getPossibleConditions();
		for (Assignment combination : combinations) {
			try {
				SimpleTable condTable = distrib.toDiscrete().getProbTable(combination);
				for (Assignment head : condTable.getRows()) {
					factor.put(new Assignment(combination, head), condTable.getProb(head));
				}
			}
			catch (DialException e) {
				log.warning("excpeption thrown to compute factor: " + e.toString());
			}
		}

		return factor;
	}





	// ===================================
	//  UTILITIES
	// ===================================



	/**
	 * Returns a copy of the node.  Note that only the node content is copied,
	 * not its connection with other nodes.
	 *
	 * @return the copy
	 * @throws DialException 
	 */
	@Override
	public ChanceNode copy() throws DialException {
		return new ChanceNode(nodeId, distrib.copy());
	}

	public int hashCode() {
		return super.hashCode() + distrib.hashCode();
	}


	/**
	 * Returns a pretty print representation of the node, showing its
	 * distribution.
	 * 
	 * @return the pretty print representation
	 */
	@Override
	public String prettyPrint() {
		return distrib.prettyPrint();
	}



	/**
	 * Returns a sample assignment for the conditional values of the node, given
	 * the set of possible values for each node
	 * 
	 * @param possiblePairs the set of possible values for each input node
	 * @return a sampled assignment
	 */
	/**
	private Assignment sampleConditionalAssignment(Map<String,List<Value>> possiblePairs) {
		Random generator = new Random();
		Assignment a = new Assignment();
		for (String variable : possiblePairs.keySet()) {
			List<Value> possibleValues = possiblePairs.get(variable);
			int index = generator.nextInt(possibleValues.size());
			a.addPair(variable, possibleValues.get(index));
		}
		return a;
	}
	 */



	// ===================================
	//  PRIVATE AND PROTECTED METHODS
	// ===================================



	protected void modifyNodeId(String oldId, String newId) {
		super.modifyNodeId(oldId, newId);
		distrib.modifyVarId(oldId, newId);
	}



	/**
	 * Computes the cached list of values for the node
	 */
	protected synchronized void fillCachedValues() {

		Set<Value> cachedValuesTemp = new HashSet<Value>();

		if (! (distrib instanceof DepEmpiricalDistribution)) {
		Set<Assignment> possibleConditions = getPossibleConditions();

		for (Assignment condition : possibleConditions) {
			try {
				SimpleTable table = distrib.toDiscrete().getProbTable(condition);
				for (Assignment head : table.getRows()) {
					if (!head.containsVar(nodeId)) {
						log.warning("head assignment " + head + " should contain " + nodeId);
						log.debug("condition was " + condition);
						log.debug("distrib is " + distrib.getClass().getName() + " " + distrib);
					}
					else {
						Value value = head.getValue(nodeId);
						cachedValuesTemp.add(value);
					}
				}
			}
			catch (DialException e) {
				log.warning("exception thrown: "+ e.toString());
			}
		}
		}
		else {
			for (Assignment s : ((DepEmpiricalDistribution)distrib).getSamples()) {
				if (s.containsVar(nodeId)) {
					cachedValuesTemp.add(s.getValue(nodeId));
				}
			}
		}
		cachedValues = cachedValuesTemp;
	}


}
