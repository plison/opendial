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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbabilityTable;
import opendial.bn.distribs.ValueDistribution;

/**
 * Representation of a value node (sometimes also called utility node)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ValueNode extends BNode {

	// logger
	public static Logger log = new Logger("ValueNode", Logger.Level.NORMAL);

	// the value distribution
	ValueDistribution distrib;
	
	
	// ===================================
	//  NODE CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new value node, with an empty value distribution
	 * 
	 * @param nodeId the node identifier
	 */
	public ValueNode(String nodeId) {
		super(nodeId);
		distrib = new ValueDistribution();
	}
	
	/**
	 * Creates a new value node, with the given value distribution
	 * 
	 * @param nodeId the node identifier
	 * @param distrib the value distribution
	 */
	public ValueNode(String nodeId, ValueDistribution distrib) {
		super(nodeId);
		this.distrib = distrib;
	}

	/**
	 * Adds a new value to the node, valid for the given assignment on 
	 * the input nodes
	 * 
	 * @param input a value assignment on the input nodes
	 * @param value the assigned value
	 */
	public void addValue(Assignment input, float value) {
		distrib.addValue(input, value);
	}
	
	/**
	 * Removes the value associated with the input assignment from the node
	 * 
	 * @param input the input associated with the value to be removed
	 */
	public void removeValue(Assignment input) {
		distrib.removeValue(input);
	}

	
	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns the value associated with the specific assignment on the
	 * input variables of the node
	 * 
	 * @param input the input assignment
	 * @return the associated value
	 */
	public float getValue(Assignment input) {
		return distrib.getValue(input);
	}

	/**
	 * Returns the set of possible values for the node
	 * 
	 * @return the possible values
	 */
	@Override
	public Set<Object> getValues() {
		Set<Object> possibleValues = new HashSet<Object>();
		possibleValues.addAll(distrib.getPossibleValues());
		return possibleValues;
	}
	
	/**
	 * Returns the value distribution
	 * 
	 * @return the value distribution
	 */
	public ValueDistribution getDistribution() {
		return distrib;
	}
	
	
	// ===================================
	//  UTILITIES
	// ===================================
	
	
	/**
	 * Returns a copy of the value node
	 * 
	 * @return the copy
	 * @throws DialException 
	 */
	@Override
	public ValueNode copy() throws DialException {
		ValueNode copy = new ValueNode(nodeId, distrib.copy());
		for (BNode inputNode : inputNodes.values()) {
			copy.addRelation(inputNode);
		}
		return copy;
	}
	

	/**
	 * Returns a string representation of the node, consisting of 
	 * the node identifier and its distribution
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return nodeId + ": " + distrib.toString();
	}
	
	
	/**
	 * Returns the hashcode for the value, computed from the node
	 * identifier and the distribution
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode() - distrib.hashCode();
	}
	
	
	/**
	 * Returns a pretty print representation of the node, which
	 * displays its value distribution
	 * 
	 * @return the pretty print representation
	 */
	@Override
	public String prettyPrint() {
		return distrib.prettyPrint();
	}



}
