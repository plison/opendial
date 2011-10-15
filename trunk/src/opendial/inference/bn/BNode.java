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

package opendial.inference.bn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.inference.bn.distribs.Distribution;
import opendial.inference.bn.distribs.ProbabilityTable;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * Representation of a node in a Bayesian network.  The node is defined with four
 * components: (1) a unique identifier, (2) a set of values for the variable, 
 * (3) a set of input nodes describing the conditional dependencies, (4) the 
 * conditional probability distribution associated with the node.  
 * 
 * <p>Nodes can be efficiently ordered, in order to used the resulting list for
 * variable elimination.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNode implements Comparable<BNode> {

	// logger
	static Logger log = new Logger("BNode", Logger.Level.DEBUG);

	// node/variable identifier (must be unique)
	String id;

	// the set of values for the variable
	Set<Object> values;

	// the conditional nodes attached to the node
	Map<String, BNode> inputNodes ;

	// the conditional probability distribution
	Distribution distrib;
		
	// whether the probability distribution should be automatically
	// completed or not
	public static boolean autoCompletion = true;

	
	/**
	 * Creates a new node, with the given identifier
	 * 
	 * @param id the identifier
	 */
	public BNode(String id) {
		this.id = id;
		values = new HashSet<Object>();
		values.add((Object)Boolean.TRUE);
		values.add((Object)Boolean.FALSE);
				
		inputNodes = new HashMap<String, BNode>();
	}


	/**
	 * Creates a new node, with an identifier and a set of values
	 * 
	 * @param id the identifier
	 * @param values the values
	 */
	public BNode(String id, Collection<Object> values) {
		this.id = id;
		this.values = new HashSet<Object>();
		for (Object val: values) {
			this.values.add(val);
		}
		
		inputNodes = new HashMap<String, BNode>();
	}
	
	
	
	// ===================================
	//  SETTERS
	// ===================================



	/**
	 * Adds a new value to the node
	 * 
	 * @param val the value
	 */
	public void addValue(Object val) {
		values.add(val);
	}
	

	/**
	 * Attaches a new input node
	 * 
	 * @param inputNode the node to attach as input
	 */
	public void addInputNode(BNode inputNode) {
		inputNodes.put(inputNode.getId(), inputNode);
	}

	

	/**
	 * Sets the distribution associated with the node.  Before setting the
	 * distribution, the method (1) completes the table if necessary, (2)
	 * ensures that the resulting distribution if well-formed
	 * 
	 * @param distrib the distribution
	 * @throws DialException if the distribution is not well-formed
	 */
	public void setDistribution(Distribution distrib) throws DialException {

		 if (distrib instanceof ProbabilityTable && autoCompletion) {
			((ProbabilityTable)distrib).completeProbabilityTable();
		}
		
		if (!distrib.isWellFormed()) {
			throw new DialException("Probability table for node " + id + " is not well-formed");
		}
		
		
		for (Assignment a : getAllPossibleAssignments()) {
			if (!distrib.hasProb(a)) {
				throw new DialException("Probability distribution not defined for assignment: " + a);
			}
		}

		this.distrib = distrib;
	}
	
	
	// ===================================
	//  GETTERS
	// ===================================

	

	/**
	 * Returns true if the node contains the following value,
	 * and false otherwise
	 * 
	 * @param val the value
	 * @return true if value is contained, false otherwise
	 */
	public boolean hasValue(Object val) {
		return values.contains(val);
	}


	/**
	 * Returns the node identifier
	 * 
	 * @return the identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the node values
	 * 
	 * @return the node values
	 */
	public Set<Object> getValues() {
		return values;
	}

	
	/**
	 * Returns the list of all possible assignments for the node values
	 * and the values of its dependent nodes.
	 * 
	 * @return the list of all possible assignments
	 */
	public List<Assignment> getAllPossibleAssignments() {
		Map<String,Set<Object>> allValues = getDependentValues();
		allValues.put(id, values);
		return InferenceUtils.getAllCombinations(allValues);
	}
	
	
	
	/**
	 * Returns the input (i.e. conditional) nodes
	 * 
	 * @return the input nodes
	 */
	public List<BNode> getInputNodes() {
		return new ArrayList<BNode>(inputNodes.values());
	}

	
	
	/**
	 * Returns true if the distribution has a defined probability for the
	 * given assignment.  Else, returns false.
	 * 
	 * @param a the assignment
	 * @return true if a probability is defined, false otherwise
	 */
	public boolean hasProb(Assignment a) {
		if (distrib != null) {
			return distrib.hasProb(a);
		}
		else {
			return false;
		}
	}

	/**
	 * Returns the probability associated with the assignment, if one exists.
	 * Else, return 0.0f.
	 * 
	 * @param a the assignment
	 * @return the probability
	 */
	public float getProb(Assignment a) {
		if (distrib != null) {
			return distrib.getProb(a);
		}
		else {
			return 0.0f;
		}
	}
	


	/**
	 * Returns the distribution associated with the node
	 * @return the distribution
	 */
	public Distribution getDistribution() {
		return distrib;
	}



	/**
	 * Returns the list of variable identifiers which is the union of
	 * the identifier for this node + the identifiers of the input nodes
	 * 
	 * @return the list of variable identifiers
	 */
	public Set<String> getVariables() {
		Set<String> vars = new HashSet<String>();
		vars.add(id);
		vars.addAll(inputNodes.keySet());
		return vars;
	}
	

	/**
	 * Returns the variables + values which are associated with the dependent 
	 * nodes
	 * 
	 * @return the list of <var,values> pairs 
	 */
	public Map<String, Set<Object>> getDependentValues() {
		Map<String,Set<Object>> allValues = new HashMap<String,Set<Object>>();
		for (BNode n : inputNodes.values()) {
			allValues.put(n.getId(), n.getValues());
		}
		return allValues;
	}


	
	
	
	/**
	 * Returns the ordered list of ancestors for the node.  
	 * 
	 * @param max_length cut-off value for the maximum distance between the initial 
	 * node and an ancestor (used to avoid running into infinite loops)
	 * @return the list of ancestors
	 */
	public List<BNode> getAncestors(int max_length) {
		List<BNode> ancestors = new LinkedList<BNode>();
		
		if (max_length <= 0) {
			return ancestors;
		}
		for (BNode anc : inputNodes.values()) {
			ancestors.add(anc);
			for (BNode anc2 : anc.getAncestors(max_length - 1)) {
				if (!ancestors.contains(anc2)) {
					ancestors.add(anc2);
				}
			}
		}
		return ancestors;
	}
		
	
	// ===================================
	//  UTILITY METHODS
	// ===================================

	
	/**
	 * Returns a string representation of the node
	 *
	 * @return the string representation
	 */
	public String toString() {
		if (!inputNodes.isEmpty()) {
			return inputNodes.keySet() + " --> " + id;
		}
		else {
			return id;
		}
	}

	


	/**
	 * Compares the nodes to the one given as parameter.  If the given node
	 * is one ancestor of this node, return -1.  If the opposite is true, returns
	 * +1.  Else, returns 0.
	 * 
	 * <p>This ordering is used for the variable elimination algorithm.
	 *
	 * @param node the node to compare
	 * @return the comparison result
	 */
	@Override
	public int compareTo(BNode node) {
		if (getAncestors(DialConstants.MAX_PATH_LENGTH).contains(node)) {
			return -10;
		}
		else if (node.getAncestors(DialConstants.MAX_PATH_LENGTH).contains(this)) {
			return 10;
		}
		return id.compareTo(node.getId());
	}


}
