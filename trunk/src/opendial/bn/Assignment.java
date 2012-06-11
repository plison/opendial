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

package opendial.bn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import opendial.arch.Logger;

/**
 * Representation of an assignment of variables (expressed via their unique identifiers) 
 * to specific values.  The assignment is logically represented as a conjunction of 
 * (variable,value) pairs.
 * 
 * <p>Technically, the assignment is encoded as a hashmap between the variable identifiers
 * and their associated value. This class offers various methods are provided for creating, 
 * comparing and manipulating such assignments.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Assignment extends HashMap<String,Object> implements Comparable<Assignment> {

	private static final long serialVersionUID = 1;


	// logger
	public static Logger log = new Logger("Assignment", Logger.Level.NORMAL);
	

	// ===================================
	//  CONSTRUCTORS
	// ===================================


	public Assignment() {
		super();
	}
	
	/**
	 * Creates a copy of the assignment
	 * 
	 * @param a the assignment to copy
	 */
	public Assignment(Assignment a) {
		super();
		addPairs(a.getPairs());
	}
	
	
	/**
	 * Creates an assignment with a single <var, Object pair
	 * 
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(String var, Object val) {
		super();
		addPair(var,val);
	}
	
	
	
	/**
	 * Creates an assignment with a single pair, given a boolean assignment 
	 * such as "Variable" or "!Variable".  If booleanAssign start with an
	 * exclamation mark, the value is set to false, else the value is set
	 * to true. 
	 * 
	 * @param booleanAssign the boolean assignment
	 */
	public Assignment(String booleanAssign) {
		super();
		addPair(booleanAssign);
	}
	
	/**
	 * Creates an assignment with a list of boolean assignments (cf. method
	 * above).  
	 * 
	 * @param booleanAssigns the list of boolean assignments
	 */
	public Assignment(List<String> booleanAssigns) {
		super();
		for (String ba: booleanAssigns) {
			addPair(ba);
		}
	}
	
	
	/**
	 * Creates an assignment with a map of <var,Object pairs
	 * 
	 * @param pairs the pairs
	 */
	public Assignment(Map<String,? extends Object> pairs) {
		putAll(pairs);
	}
	
	/**
	 * Creates an assignment from an existing one (which is copied),
	 * plus a single <var, Object pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, Object val) {
		super();
		addPairs(ass.getPairs());
		addPair(var, val);
	}
	
	
	/**
	 * Creates an assignment by concatenating two existing assignments
	 * (which are copied)
	 * 
	 * @param ass1 the first assignment
	 * @param ass2 the second assignment
	 */
	public Assignment(Assignment ass1, Assignment ass2) {
		super();
		addPairs(ass1.getPairs()); 
		addPairs(ass2.getPairs());
	}


	/**
	 * Creates an assignment by adding a set of map entries
	 * 
	 * @param entries the entries to add
	 */
	public Assignment(Set<Entry<String, Object>> entries) {
		super();
		for (Entry<String,Object> entry: entries) {
			addPair(entry.getKey(), entry.getValue());
		}
	}

	// ===================================
	//  SETTERS
	// ===================================


	
	/**
	 * Adds a new <var,value pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value
	 */
	public void addPair(String var, Object val) {
		put(var, val);
	}
	
	
	/**
	 * Adds a new <var,Object pair as determined by the form of the argument.  
	 * If the argument starts with an exclamation mark, the value is set to
	 * False, else the value is set to True.
	 * 
	 * @param booleanAssign the pair to add
	 */
	public void addPair(String booleanAssign) {
		if (!booleanAssign.startsWith("!")) {
			addPair(booleanAssign, Boolean.TRUE);
		}
		else {
			addPair(booleanAssign.substring(1,booleanAssign.length()), Boolean.FALSE);
		}
	}
	
	
	/**
	 * Adds a set of <var,Object pairs to the assignment
	 * 
	 * @param pairs the pairs to add
	 */
	public void addPairs (Map<String,Object> pairs) {
		putAll(pairs);
	}
	
	

	/**
	 * Add a new set of pairs defined in the assignment given as argument
	 * (i.e. merge the given assignment into the present one).
	 * 
	 * @param assignment the assignment to merge
	 */
	public void addAssignment(Assignment assignment) {
		addPairs(assignment.getPairs());
	}


	/**
	 * Removes the pair associated with the var label
	 * 
	 * @param var the variable to remove
	 */
	public void removePair(String var) {
		remove(var);
	}
	
	
	/**
	 * Remove the pairs associated with the labels
	 * 
	 * @param vars the variable labels to remove
	 */
	public void removePairs(Set<String> vars) {
		for (String var: vars) {
			remove(var);
		}
	}
	

	// ===================================
	//  GETTERS
	// ===================================


	
	/**
	 * Returns the pairs of the assignment
	 * 
	 * @return all pairs
	 */
	public Map<String,Object> getPairs() {
		return this;
	}


	/**
	 * Returns the size (number of pairs) of the assignment
	 * 
	 * @return the number of pairs
	 */
	public int getSize() {
		return size();
	}
	
	
	
	/**
	 * Returns a trimmed version of the assignment, where only the
	 * variables given as parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmed(Collection<String> variables) {
		SortedMap<String,Object> newPair = new TreeMap<String,Object>();
		for (String key : keySet()) {
			if (variables.contains(key)) {
				newPair.put(key, get(key));	
			}
		}
		return new Assignment(newPair);
	}
	
	
	/**
	 * Returns a copy of the assignment
	 * 
	 * @return the copy
	 */
	public Assignment copy() {
		Assignment ass = new Assignment();
		ass.addPairs(this);
		return ass;
	}
	
	
	/**
	 * Returns the list of variables used 
	 * 
	 * @return variables list
	 */
	public Set<String> getVariables() {
		return keySet();
	} 
	
	
	/**
	 * Returns the value associated with the variable in the assignment,
	 * if one is specified.  Else, returns null.
	 * 
	 * @param var the variable
	 * @return the associated value
	 */
	public Object getValue(String var) {
		return get(var);
	}
	
	
	/**
	 * Returns true if the assignment contains all pairs specified
	 * in the assignment given as parameter (both the label and its
	 * value must match for all pairs).
	 * 
	 * @param a the assignment
	 * @return true if a is contained in assignment, false otherwise
	 */
	public boolean contains(Assignment a) {	
		for (String key : a.getVariables()) {
			if (containsKey(key)) {
				Object val = a.getValue(key);
				if (get(key) == null) {
					if (val != null) {
						return false;
					}
				}
				else if (!get(key).equals(val)) {
					return false;
				}
			}
			else {
				return false;
			}	
		}
		return true;
	}
	
	
	/**
	 * Returns true if the two assignments are mutually consistent,
	 * i.e. if there is a label l which appears in both assignment, 
	 * then their value must be equal.
	 * 
	 * @param a the second assignment
	 * @return true if assignments are consistent, false otherwise
	 */
	public boolean consistentWith(Assignment a) {
		for (String evidenceVar : a.getVariables()) {
			if (containsKey(evidenceVar)) {
				if (get(evidenceVar) == null) {
					if (a.getValue(evidenceVar) != null) {
					return false;
					}
				}
				else if (!get(evidenceVar).equals(a.getValue(evidenceVar))) {
					return false;
				}
			}
		}
		return true;
	}

	

	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================

	
	
	/**
	 * Returns the hashcode associated with the assignment.  The hashcode is
	 * calculated from the hashmap corresponding to the assignment.
	 * 
	 * @return the corresponding hashcode
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	} 
	
	
	/**
	 * Returns true if the object given as argument is an assignment identical
	 * to the present one
	 *
	 * @param o the object to compare
	 * @return true if the assignments are equal, false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof Assignment) {
			return super.equals((Assignment)o);
		}
		return false;
	}
	
	

	/**
	 * Returns a string representation of the assignment
	 *
	 * @return the string representation
	 */
	public String toString() {
		String str = "";
		if (keySet().isEmpty()) {
			return "[]";
		}
		List<String> keyList = new ArrayList<String>(keySet());
		for (String key: keyList) {
			if (get(key) == null) {
				str += key + "=None";
			}
			else if (get(key).equals(Boolean.TRUE)) {
				str += key;
			}
			else if (get(key).equals(Boolean.FALSE)) {
				str += "!" + key;
			}
			else {
				str += key + "=" + get(key) ;
			}
			if (!key.equals(keyList.get(keySet().size() - 1))) {
				str += " ^ ";
			}
		}
		return str;
		
	}
	

	/**
	 * Compares the size (in terms of number of assignment pairs) of the
	 * two assignments
	 * 
	 * @param arg0 the assignment to compare to the current one
	 * @return the difference between the size of the present assignment and
	 *         the one given as argument
	 */
	@Override
	public int compareTo(Assignment arg0) {
		return getSize()-arg0.getSize();
	}

	
}
