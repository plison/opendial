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

import opendial.arch.Logger;

/**
 * Representation of an action node (sometimes also called decision node).
 * An action node is defined as a set of mutually exclusive action values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ActionNode extends BNode {

	// logger
	public static Logger log = new Logger("ActionNode", Logger.Level.NORMAL);

	// the list of values for the node
	Set<Object> actionValues;
	
	
	// ===================================
	//  NODE CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new action node with a unique identifier, and no values
	 * 
	 * @param nodeId the node identifier
	 */
	public ActionNode(String nodeId) {
		super(nodeId);
		actionValues = new HashSet<Object>();
	}
	
	/**
	 * Creates a new action node with a unique identifier and a set of
	 * values
	 * 
	 * @param nodeId the node identifier
	 * @param actionValues the values for the action
	 */
	public ActionNode(String nodeId, Set<Object> actionValues) {
		super(nodeId);
		this.actionValues = new HashSet<Object>(actionValues);
	}
	
	public void addRelation(BNode inputNode) {
		log.warning("Action node cannot have any input nodes, ignoring call");
	}

	
	/**
	 * Adds a new action values to the node
	 * 
	 * @param value the value to add
	 */
	public void addValue(Object value) {
		actionValues.add(value);
	}
	
	
	/**
	 * Adds a set of action values to the node
	 * 
	 * @param values the values to add
	 */
	public void addValues(Set<Object> values) {
		actionValues.addAll(values);
	}


	// ===================================
	//  GETTERS
	// ===================================
	
	
	
	/**
	 * Returns the list of values currently listed in the node
	 * 
	 * @return the list of values
	 */
	public Set<Object> getValues() {
		return actionValues;
	}
	

	// ===================================
	//  UTILITIES
	// ===================================
	

	/**
	 * Copies the action node
	 * 
	 * @return the copy of the node
	 */
	@Override
	public ActionNode copy() {
		ActionNode nodeCopy = new ActionNode(nodeId);
		nodeCopy.addValues(actionValues);
		return nodeCopy;
	}

	
	/**
	 * Returns a pretty print representation of the node, which enumerates
	 * the action values
	 * 
	 * @return the pretty print representation
	 */
	@Override
	public String prettyPrint() {
		return actionValues.toString() + "\n";
	}
	
	
	/**
	 * Returns a string representation of the node, which states the node identifier
	 * followed by the action values
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return nodeId + ": " + actionValues.toString();
	}
	
	
	/**
	 * Returns the hashcode corresponding to the action node
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode() + actionValues.hashCode();
	}
}
