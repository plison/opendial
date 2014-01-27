// =================================================================                                                                   
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

package opendial.bn.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

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
	public static Logger log = new Logger("ActionNode", Logger.Level.DEBUG);

	// the list of values for the node
	private Set<Value> actionValues;
	
	Random sampler;
		
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
		actionValues = new HashSet<Value>();
		sampler = new Random();
		actionValues.add(ValueFactory.none());
	}
	
	/**
	 * Creates a new action node with a unique identifier and a set of
	 * values
	 * 
	 * @param nodeId the node identifier
	 * @param actionValues the values for the action
	 */
	public ActionNode(String nodeId, Set<Value> actionValues) {
		this(nodeId);
		this.actionValues = new HashSet<Value>(actionValues);
	}
	
	@Override
	public void addInputNode(BNode inputNode) {
		log.warning("Action node cannot have any input nodes, ignoring call");
	}

	
	/**
	 * Adds a new action values to the node
	 * 
	 * @param value the value to add
	 */
	public void addValue(Value value) {
		actionValues.add(value);
	} 
	
	
	/**
	 * Adds a set of action values to the node
	 * 
	 * @param values the values to add
	 */
	public void addValues(Set<Value> values) {
		for (Value v : values) {
			addValue(v);
		}
	} 
	
	/**
	 * Removes a value from the action values set
	 * 
	 * @param value the value to remove
	 */
	public void removeValue(Value value) {
		actionValues.remove(value);
	} 
	
	/**
	 * Removes a set of values from the action values
	 * 
	 * @param values the values to remove
	 */
	public void removeValues(Set<Object> values) {
		actionValues.removeAll(values);
	} 
	
	/**
	 * Returns the factor matrix for the action node.  The matrix lists the possible
	 * actions for the node, along with a uniform probability distribution over
	 * its values
	 *
	 * @return the factor matrix corresponding to the node
	 */
	@Override
	public Map<Assignment,Double> getFactor() {
		Map<Assignment,Double> factor = new HashMap<Assignment,Double>();
		for (Value actionValue : actionValues) {
			factor.put(new Assignment(nodeId, actionValue), 1.0/actionValues.size());
		}
		return factor;
	}
	
	/**
	 * Returns a probability uniformly distributed on the alternative values.
	 * @param actionValue
	 * @return 1/|values|
	 */
	public double getProb(Value actionValue) {
		return 1.0/actionValues.size();
	}


	// ===================================
	//  GETTERS
	// ===================================
	
	
	
	/**
	 * Returns the list of values currently listed in the node
	 * 
	 * @return the list of values
	 */
	@Override
	public Set<Value> getValues() {
		return new HashSet<Value>(actionValues);
	}
	
	
	/**
	 * Returns a sample point for the action, assuming a uniform distribution
	 * over the action values
	 * 
	 * @return the sample value
	 */
	public Value sample() { 
		int index = sampler.nextInt(actionValues.size());
		return new ArrayList<Value>(actionValues).get(index);
	}
	
	
	/**
	 * Returns a sample point for the action, assuming a uniform distribution
	 * over the action values
	 * 
	 * @param input the input assignment
	 * @return the sample value
	 */
	public Value sample(Assignment input) {
		int index = sampler.nextInt(actionValues.size());
		return new ArrayList<Value>(actionValues).get(index);
	}
	

	// ===================================
	//  UTILITIES
	// ===================================
	

	/**
	 * Copies the action node. Note that only the node content is copied,
	 * not its connection with other nodes.
	 * 
	 * @return the copy of the node
	 */
	@Override
	public ActionNode copy() {
		return new ActionNode(nodeId, actionValues);
	}

	
	/**
	 * Returns a string representation of the node, which states the node identifier
	 * followed by the action values
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

	
	public void setValues(Set<Value> newValues) {
		actionValues = newValues;
	}

	
	
	
}
