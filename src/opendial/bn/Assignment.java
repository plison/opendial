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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;

import opendial.arch.Logger;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.bn.values.VectorVal;
import opendial.utils.StringUtils;

/**
 * Representation of an assignment of variables (expressed via their unique identifiers) 
 * to specific values.  The assignment is logically represented as a conjunction of 
 * (variable,value) pairs.
 * 
 * <p>Technically, the assignment is encoded as a map between the variable identifiers
 * and their associated value. This class offers various methods are provided for creating, 
 * comparing and manipulating such assignments.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class Assignment {

	// logger
	static Logger log = new Logger("Assignment", Logger.Level.DEBUG);
	
	// the hashmap encoding the assignment
	Map<String,Value> map;
	
	// the initial size of the hash
	public static final int MAP_SIZE = 3;
	
	int hashCodeCache = Integer.MAX_VALUE;
		
	// ===================================
	//  CONSTRUCTORS
	// ===================================


	/**
	 * Creates a new, empty assignment
	 */
	public Assignment() {
		map = new HashMap<String,Value>(MAP_SIZE);
	}
	
	
	/**
	 * Creates a new, empty assignment, with a given initial
	 * hash size
	 * 
	 * @param size the hash size
	 */
	public Assignment (int size) {
		map = new HashMap<String,Value>(size);
	}
	
	
	/**
	 * Creates a copy of the assignment
	 * 
	 * @param a the assignment to copy
	 */
	public Assignment(Assignment a) {
		this();
		addPairs(a.getPairs());
	}
	
	
	/**
	 * Creates an assignment with a single <var,value> pair
	 * 
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(String var, Value val) {
		this();
		addPair(var,val);
	}
	
	/**
	 * Creates a new assignment, with a single <var,value> pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a string)
	 */
	public Assignment(String var, String val) {
		this();
		addPair(var, val);
	}

	/**
	 * Creates a new assignment, with a single <var,value> pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a double)
	 */
	public Assignment(String var, double val) {
		this();
		addPair(var, val);
	}
	
	/**
	 * Creates a new assignment, with a single <var,value> pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a boolean)
	 */
	public Assignment(String var, boolean val) {
		this();
		addPair(var, val);
	}
	
	/**
	 * Creates a new assignment, with a single <var,value> pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a double array)
	 */
	public Assignment(String var, double[] val) {
		this();
		addPair(var, val);
	}
	
	
	/**
	 * Creates an assignment with a list of sub assignments
	 * 
	 * @param assignments the assignments to combine
	 */
	public Assignment(Assignment... assignments) {
		this();
		for (Assignment a : assignments) {
			addAssignment(a);
		}
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
		this();
		addPair(booleanAssign);
	}
	
	/**
	 * Creates an assignment with a list of boolean assignments (cf. method
	 * above).  
	 * 
	 * @param booleanAssigns the list of boolean assignments
	 */
	public Assignment(List<String> booleanAssigns) {
		this();
		for (String ba: booleanAssigns) {
			addPair(ba);
		}
	}
	
	
	/**
	 * Creates an assignment with a map of <var,Object pairs
	 * 
	 * @param pairs the pairs
	 */
	public Assignment(Map<String,? extends Value> pairs) {
		this();
		map.putAll(pairs);
	}
	
	/**
	 * Creates an assignment from an existing one (which is copied),
	 * plus a single <var, Object pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, Value val) {
		this();
		addPairs(ass.getPairs());
		addPair(var, val);
	}
	
	/**
	 * Creates an assignment from an existing one (which is copied),
	 * plus a single <var, Object pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, String val) {
		this();
		addPairs(ass.getPairs());
		addPair(var, val);
	}
	
	/**
	 * Creates an assignment from an existing one (which is copied),
	 * plus a single <var, Object pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, double val) {
		this();
		addPairs(ass.getPairs());
		addPair(var, val);
	}
	
	
	/**
	 * Creates an assignment from an existing one (which is copied),
	 * plus a single <var, Object pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, boolean val) {
		this();
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
		this();
		addPairs(ass1.getPairs()); 
		addPairs(ass2.getPairs());
	}


	/**
	 * Creates an assignment by adding a set of map entries
	 * 
	 * @param entries the entries to add
	 */
	public Assignment(Set<Entry<String, Value>> entries) {
		this();
		for (Entry<String,Value> entry: entries) {
			addPair(entry.getKey(), entry.getValue());
		}
	}
	
	
	/**
	 * Creates a new assignment with two pairs of <variable,value>
	 * 
	 * @param var1 label of first variable
	 * @param val1 value of first variable
	 * @param var2 label of second variable
	 * @param val2 value of second variable
	 */
	public Assignment(String var1, Value val1, String var2, Value val2) {
		this();
		addPair(var1, val1);
		addPair(var2, val2);
	}
	
	
	/**
	 * Creates an assignment with only none values for the variable labels
	 * given as argument.
	 * 
	 * @param variables the collection of variable labels
	 * @return the resulting default assignment
	 */
	public static Assignment createDefault (Collection<String> variables) {
		Assignment a = new Assignment();
		for (String var : variables) {
			a.addPair(var, ValueFactory.none());
		}
		return a;
	}

	
	// ===================================
	//  SETTERS
	// ===================================

	
	/**
	 * Adds a new <var,value> pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value
	 */
	public void addPair(String var, Value val) {
		map.put(var, val);
		resetHashCodeCache();
	}
	
	
	/**
	 * Adds a new <var, value> pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a string
	 */
	public void addPair(String var, String val) {
		map.put(var, ValueFactory.create(val));
		resetHashCodeCache();
	}
	
	/**
	 * Adds a new <var, value> pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a double
	 */
	public void addPair(String var, double val) {
		map.put(var, ValueFactory.create(val));
		resetHashCodeCache();
	}
	
	/**
	 * Adds a new <var, value> pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a boolean
	 */
	public void addPair(String var, boolean val) {
		map.put(var, ValueFactory.create(val));
		resetHashCodeCache();
	}
	
	
	/**
	 * Adds a new <var, value> pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a double array
	 */
	public void addPair(String var, double[] val) {
		map.put(var, ValueFactory.create(val));
		resetHashCodeCache();
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
			addPair(booleanAssign, ValueFactory.create(true));
		}
		else {
			addPair(booleanAssign.substring(1,booleanAssign.length()), ValueFactory.create(false));
		}
	}
	
	
	/**
	 * Adds a set of <var,Object pairs to the assignment
	 * 
	 * @param pairs the pairs to add
	 */
	public void addPairs (Map<String,Value> pairs) {
		for (String var : pairs.keySet()) {
			addPair(var, pairs.get(var));
		}
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
	public Value removePair(String var) {
		resetHashCodeCache();
		return map.remove(var);
	}
	
	
	/**
	 * Remove the pairs associated with the labels
	 * 
	 * @param vars the variable labels to remove
	 */
	public void removePairs(Collection<String> vars) {
		for (String var: vars) {
			removePair(var);
		}
	}
	

	// ===================================
	//  GETTERS
	// ===================================


	
	/**
	 * Returns a new assignment with all the accessory specifiers such as primes,
	 * ^p an ^o.
	 * 
	 * @return a new assignment, without the accessory specifiers
	 */
	public Assignment removeSpecifiers() {
		Assignment a = new Assignment();
		for (String var : map.keySet()) {
			String newVar = StringUtils.removePrimes(var);
			if (a.containsVar(newVar)) {
				if (var.contains("''")) {
					a.addPair(newVar, map.get(var).copy());
				}
			}
			else {
				a.addPair(newVar, map.get(var).copy());
			}
		}
		return a;
	}
	
	 
	/**
	 * Returns whether the assignment is empty
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	/**
	 * Returns the pairs of the assignment
	 * 
	 * @return all pairs
	 */
	public Map<String,Value> getPairs() {
		return map;
	}


	/**
	 * Returns the size (number of pairs) of the assignment
	 * 
	 * @return the number of pairs
	 */
	public int size() {
		return map.size();
	}
	
	
	/**
	 * Returns true if the assignment contains the given variable,
	 * and false otherwise
	 * 
	 * @param var the variable label
	 * @return true if the variable is included, false otherwise
	 */
	public boolean containsVar(String var) {
		return map.containsKey(var);
	}
	
	

	/**
	 * Returns true if the assignment contains all of the given variables,
	 * and false otherwise
	 * 
	 * @param vars the variable labels
	 * @return true if all variables are included, false otherwise
	 */
	public boolean containsVars(Collection<String> vars) {
		return map.keySet().containsAll(vars);
	}
	
	
	public boolean containsAVar(Collection<String> vars) {
		for (String var : vars) {
			if (map.containsKey(var)) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Returns a trimmed version of the assignment, where only the
	 * variables given as parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmed(Collection<String> variables) {
		Assignment a = new Assignment();
		for (String var : new ArrayList<String>(variables)) {
			if (map.containsKey(var)) {
				a.addPair(var, map.get(var));
			}
		}
		return a;
	}
	
	/**
	 * Returns a trimmed version of the assignment, where only the
	 * variables NOT given as parameters are considered
	 * 
	 * @param variables the variables to remove
	 * @return a new, trimmed assignment
	 */
	
	public Assignment getTrimmedInverse (Collection<String> variables) {
		Assignment a = copy();
		a.removePairs(variables);
		return a;
	}
	
	/**
	 * Returns a trimmed version of the assignment, where only the
	 * variables given as parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmed(String... variables) {
		Assignment a = new Assignment();
		for (String var : variables) {
			if (map.containsKey(var)) {
				a.addPair(var, map.get(var));
			}
		}
		return a;
	}
	
	/**
	 * Returns a trimmed version of the assignment, where only the
	 * variables NOT given as parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmedInverse(String... variables) {
		Assignment a = copy();
		for (String var : variables) {
			a.removePair(var);
		}
		return a;
	}
	
	
	/**
	 * Returns a copy of the assignment
	 * 
	 * @return the copy
	 */
	public Assignment copy() {
		return new Assignment(this);
	}
	
	
	/**
	 * Returns the list of variables used 
	 * 
	 * @return variables list
	 */
	public Set<String> getVariables() {
		return map.keySet();
	} 
	

	/**
	 * Returns the entry set for the assignment
	 * 
	 * @return the entry set
	 */
	public Set<Entry<String,Value>> getEntrySet() {
		return map.entrySet();
	}
	
	
	/**
	 * Get the entry at the given position in the hash, according
	 * to the alphabetical ordering of the keys
	 * 
	 * @param index the given position
	 * @return the corresponding entry
	 */
	public Entry<String,Value> getEntry(int index) {
		int count = 0;
		Iterator<Entry<String,Value>> it = new TreeSet<Entry<String,Value>>(map.entrySet()).iterator();
		while (count < index && it.hasNext()) {
			it.next();
			count++;
		}
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}
	
	/**
	 * Returns the value associated with the variable in the assignment,
	 * if one is specified.  Else, returns null.
	 * 
	 * @param var the variable
	 * @return the associated value
	 */
	public Value getValue(String var) {
		return map.get(var);
	}
	

	
	/**
	 * Returns the values corresponding to the variable labels given as argument.
	 * If the variable is not in the assignment, the value is null.
	 * 
	 * @param vars the variable labels
	 * @return the corresponding values
	 */
	public List<Value> getValues(Iterable<String> vars) {
		List<Value> values = new ArrayList<Value>();
		for (String var : vars) {
			values.add(map.get(var));
		}
		return values;
	}
	
	

	/**
	 * Returns all the values contained in the assignment
	 * 
	 * @return the collection of values
	 */
	public Collection<Value> getValues() {
		return map.values();
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
			if (map.containsKey(key)) {
				Value val = a.getValue(key);
				if (map.get(key) == null) {
					if (val != null) {
						return false;
					}
				}
				else if (!map.get(key).equals(val)) {
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
			if (map.containsKey(evidenceVar)) {
				if (map.get(evidenceVar) == null) {
					if (a.getValue(evidenceVar) != null) {
					return false;
					}
				}
				else if (!map.get(evidenceVar).equals(a.getValue(evidenceVar))) {
					return false;
				}
			}
		}
		return true;
	}

	
	/**
	 * Returns true if the assignment only contains none values for all variables,
	 * and false if at least one has a different value.
	 * 
	 * @return true if all variables have none values, false otherwise
	 */
	public boolean isDefault() {
		for (String var : map.keySet()) {
			if (!map.get(var).equals(ValueFactory.none())) {
				return false;
			}
		}
		return true;
	}



	public boolean isDiscrete() {
		for (String var : map.keySet()) {
			Value val = map.get(var);
			if (val instanceof DoubleVal || val instanceof VectorVal) {
				return false;
			}
		}
		return true;
	}
	


	public Assignment getDiscrete() {
		Assignment discrete = new Assignment();
		for (String var : map.keySet()) {
			Value val = map.get(var);
			if (!(val instanceof DoubleVal) && !(val instanceof VectorVal)) {
				discrete.addPair(var, val);
			}
		}
		return discrete;
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
		if (hashCodeCache == Integer.MAX_VALUE) {
			 hashCodeCache = map.hashCode();
		}
		return hashCodeCache;
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
				return map.equals(((Assignment)o).getPairs());
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
		if (map.keySet().isEmpty()) {
			return "~";
		}
		List<String> keyList = new ArrayList<String>(map.keySet());
		for (String key: keyList) {
			if (map.get(key) == null) {
				str += key + "=null";
			}
			else if (map.get(key).equals(Boolean.TRUE)) {
				str += key;
			}
			else if (map.get(key).equals(Boolean.FALSE)) {
				str += "!" + key;
			}
			else {
				str += key + "=" + map.get(key) ;
			}
			if (!key.equals(keyList.get(map.keySet().size() - 1))) {
				str += " ^ ";
			}
		}
		return str;
		
	}
	
	

	// ===================================
	//  PRIVATE FUNCTIONS
	// ===================================

	private void resetHashCodeCache() {
		hashCodeCache = Integer.MAX_VALUE;;
	}


	
}
