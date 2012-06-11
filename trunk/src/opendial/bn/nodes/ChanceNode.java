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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.ProbabilityTable;
import opendial.utils.InferenceUtils;

/**
 * Representation of a chance node (sometimes also called belief node), which
 * is a random variable associated with a specific probability distribution.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ChanceNode extends BNode {
	
	// logger
	public static Logger log = new Logger("ChanceNode", Logger.Level.NORMAL);
		
	// the probability distribution for the node
	protected ProbDistribution distrib;
	
	// the set of cached values for the node
	// NB: if the node has a continuous range, these values are based on 
	// a discretisation procedure defined by the distribution
	Set<Object> cachedValues;
	
	
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
	public ChanceNode(String nodeId) throws DialException {
		this(nodeId, new ProbabilityTable());
	}
	
	/**
	 * Creates a new chance node, with the given identifier and 
	 * probability distribution
	 * 
	 * @param nodeId the unique node identifier
	 * @param distrib the probability distribution for the node
	 * @throws DialException if the distribution is not well-formed
	 */
	public ChanceNode(String nodeId, ProbDistribution distrib) throws DialException {
		super(nodeId);
		this.distrib = distrib;		
		if (!distrib.isWellFormed()) {
			throw new DialException("Distribution for node " + nodeId + " is not well-formed");
		}
	}
	
	
	/**
	 * Sets the probability distribution of the node, and erases the existing one.
	 * 
	 * @param distrib the distribution for the node
	 * @throws DialException if the distribution is not well-formed
	 */
	public void setDistribution(ProbDistribution distrib) throws DialException {
		this.distrib = distrib;
		if (!distrib.isWellFormed()) {
			throw new DialException("Distribution for node " + nodeId + " is not well-formed");
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
	public void addRelation(BNode inputNode) throws DialException {
		super.addRelation(inputNode);
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
	public void addProb (Object nodeValue, float prob) {
		if (!inputNodes.isEmpty()) {
			log.warning("adding a independent probability for a node with conditional dependencies");
		}
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
	public void addProb(Assignment condition, Object nodeValue, float prob) {
		if (distrib instanceof ProbabilityTable) {
			((ProbabilityTable)distrib).addRow(condition, new Assignment(nodeId, nodeValue), prob);
		}
		else {
			log.warning("distribution is not defined as a table, impossible to add probability");
		}
		cachedValues = null;
	}
	

	
	public void removeProb(Object nodeValue) {
		removeProb(new Assignment(), nodeValue);
	}
	
	public void removeProb(Assignment condition, Object nodeValue) {
		if (distrib instanceof ProbabilityTable) {
			((ProbabilityTable)distrib).removeRow(condition, new Assignment(nodeId, nodeValue));
		}
		else {
			log.warning("distribution is not defined as a table, impossible to remove probability");
		}
		cachedValues = null;
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
	 * @param nodeValue the value for the node
	 * @return its probability
	 */
	public float getProb(Object nodeValue) {
		return getProb(new Assignment(), nodeValue);
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
	public float getProb(Assignment condition, Object nodeValue) {
		return distrib.getProb(condition, new Assignment(nodeId, nodeValue));
	}
	
	/**
	 * Returns a sample value for the node, according to the probability distribution
	 * currently defined.
	 * 
	 * <p>The method assumes that the node is conditionally independent of every 
	 * other node. If it isn't, one should use the sample(condition) method instead.
	 * 
	 * @return the sample value
	 */
	public Object sample() {
		return sample(new Assignment());
	}

	/**
	 * Returns a sample value for the node, given a condition.  The sample is selected
	 * according to the probability distribution for the node.
	 * 
	 * @param condition the value assignment on conditional nodes
	 * @return the sample value
	 */
	public Object sample(Assignment condition) {
		Assignment result = distrib.sample(condition);
		if (!result.containsKey(nodeId)) {
			log.warning("result of sampling does not contain nodeId" + nodeId +": " + result);
		}
		return result.getValue(nodeId);
	}
	
	
	/**
	 * Returns a discrete set of values for the node.  If the variable for the node
	 * has a continuous range, this set if based on a discretisation procedure defined
	 * by the distribution.
	 *
	 * @return the discrete set of values
	 */
	@Override
	public Set<Object> getValues() {
		
		// for efficiency, use a cache to avoid recomputing the set
		if (cachedValues != null) {
			return cachedValues;
		}
		
		Map<String,Set<Object>> possibleInputValues = new HashMap<String,Set<Object>>();
		for (BNode inputNode : inputNodes.values()) {
				possibleInputValues.put(inputNode.getId(), inputNode.getValues());
		}
		List<Assignment> possibleConditions = 
			InferenceUtils.getAllCombinations(possibleInputValues);

		cachedValues = new HashSet<Object>();
		for (Assignment condition : possibleConditions) {
			Map<Assignment,Float> table = distrib.getProbTable(condition);
			for (Assignment head : table.keySet()) {
				Object value = head.getValue(nodeId);
				cachedValues.add(value);
			}
		}
		return cachedValues;
	}
	

	/**
	 * Returns the probability distribution attached to th enode
	 * 
	 * @return the distribution
	 */
	public ProbDistribution getDistribution() {
		return distrib;
	}
	
	// ===================================
	//  UTILITIES
	// ===================================

	
	/**
	 * Returns a copy of the node
	 *
	 * @return the copy
	 * @throws DialException 
	 */
	@Override
	public ChanceNode copy() throws DialException {
		ChanceNode copy = new ChanceNode(nodeId, distrib.copy());
		for (BNode inputNode : inputNodes.values()) {
			copy.addRelation(inputNode);
		}
		return copy;
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
	private Assignment sampleConditionalAssignment(Map<String,List<Object>> possiblePairs) {
		Random generator = new Random();
		Assignment a = new Assignment();
		for (String variable : possiblePairs.keySet()) {
			List<Object> possibleValues = possiblePairs.get(variable);
			int index = generator.nextInt(possibleValues.size());
			a.addPair(variable, possibleValues.get(index));
		}
		return a;
	}

	
}
