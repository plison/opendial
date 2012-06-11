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

package opendial.bn.distribs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.utils.InferenceUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ValueDistribution {

	// logger
	public static Logger log = new Logger("ValueDistribution", Logger.Level.NORMAL);
	
	// table mapping value assignments for the input nodes (usually including at least 
	// one action node) to a specific value
	Map<Assignment, Float> table;
	
	
	// ===================================
	//  CONSTRUCTION
	// ===================================

	

	/**
	 * Constructs a new value distribution, with no values
	 */
	public ValueDistribution() {
		table = new HashMap<Assignment,Float>();
	}
	
	/**
	 * Constructs a new value distribution, given the values provided as argument
	 * 
	 * @param values the values
	 */
	public ValueDistribution(Map<Assignment,Float> values) {
		this.table = new HashMap<Assignment,Float>(values);
	}
	
	
	/**
	 * Adds a new value to the distribution, associated with a value assignment
	 * 
	 * @param input the value assignment for the input nodes
	 * @param value the resulting value
	 */
	public void addValue(Assignment input, float value) {
		table.put(input,value);
	}
	
	/**
	 * Adds a set of new values to the distribution
	 * 
	 * @param values the values to add
	 */
	public void addValues(Map<Assignment,Float> values) {
		table.putAll(values);
	}
	
	
	/**
	 * Removes a value from the value distribution
	 * 
	 * @param input the assignment associated with the value to be removed
	 */
	public void removeValue(Assignment input) {
		table.remove(input);
	}

	// ===================================
	//  GETTERS
	// ===================================
	
	
	
	/**
	 * Returns the value associated with the specific assignment of values for
	 * the input nodes.  If none exists, returns 0.0f.
	 * 
	 * @param input the value assignment for the input nodes
	 * @return the associated value
	 */
	public float getValue(Assignment input) {
		if (table.containsKey(input)) {
			return table.get(input);
		}
		else {
			log.warning("assignment " + input + " not defined in value distribution");
			return 0.0f;
		}
	}
	
	
	/**
	 * Returns the set of all possible values defined in the distribution
	 * 
	 * @return the set of possible values
	 */
	public Set<Float> getPossibleValues() {
		Set<Float> possibleValues = new HashSet<Float>();
		for (Assignment assignment : table.keySet()) {
			possibleValues.add(table.get(assignment));
		}
		return possibleValues;
	}
	
	/**
	 * Returns true if the value distribution if well-formed -- i.e. if it defines
	 * a specific value for each possible assignment of input values
	 * 
	 * @return true if well-formed, false otherwise
	 */
	public boolean isWellFormed() {
		Map<String,Set<Object>> possiblePairs = 
			InferenceUtils.extractPossiblePairs(table.keySet());
		List<Assignment> possibleAssignments = 
			InferenceUtils.getAllCombinations(possiblePairs);
		
		for (Assignment assignment : possibleAssignments) {
			if (!table.containsKey(assignment)) {
				log.warning("assignment " + assignment + " not defined in value distribution");
				return false;
			}
		}
		return true;
	}
	

	// ===================================
	//  UTILITIES
	// ===================================

	
	/**
	 * Copies the distribution
	 * 
	 * @return the copy
	 */
	public ValueDistribution copy() {
		return new ValueDistribution(table);
	}
	
	
	/**
	 * Returns a string representation of the distribution
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment input: table.keySet()) {
			str += "Q(" + input + "):=" + table.get(input) + "\n";
		}
		return str;
	}
	
	/**
	 * Returns a pretty print representation of the distribution
	 * 
	 * @return the pretty print representation
	 */
	public String prettyPrint() {
		return toString();
	}
	
	
	/**
	 * Returns the hashcode for the distribution (computed from the 
	 * hashtable of value)
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	
	
}
