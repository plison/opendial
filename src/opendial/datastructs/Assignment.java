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

package opendial.datastructs;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Representation of an assignment of variables (expressed via their unique
 * identifiers) to specific values. The assignment is logically represented as a
 * conjunction of (variable,value) pairs.
 * 
 * <p>
 * Technically, the assignment is encoded as a map between the variable identifiers
 * and their associated value. This class offers various methods are provided for
 * creating, comparing and manipulating such assignments.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Assignment {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the hashmap encoding the assignment
	protected final Map<String, Value> map;

	// the cached value for the hash
	int cachedHash = 0;

	// ===================================
	// CONSTRUCTORS
	// ===================================

	/**
	 * Creates a new, empty assignment
	 */
	public Assignment() {
		map = new HashMap<String, Value>();
	}

	/**
	 * Creates a copy of the assignment
	 * 
	 * @param a the assignment to copy
	 */
	public Assignment(Assignment a) {
		map = new HashMap<String, Value>(a.map);
	}

	/**
	 * Creates an assignment with a single (var,value) pair
	 * 
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(String var, Value val) {
		map = new HashMap<String, Value>();
		map.put(var, val);
	}

	/**
	 * Creates a new assignment, with a single (var,value) pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a string)
	 */
	public Assignment(String var, String val) {
		this();
		map.put(var, ValueFactory.create(val));
	}

	/**
	 * Creates a new assignment, with a single (var,value) pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a double)
	 */
	public Assignment(String var, double val) {
		this();
		map.put(var, ValueFactory.create(val));
	}

	/**
	 * Creates a new assignment, with a single (var,value) pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a boolean)
	 */
	public Assignment(String var, boolean val) {
		this();
		map.put(var, ValueFactory.create(val));
	}

	/**
	 * Creates a new assignment, with a single (var,value) pair
	 * 
	 * @param var the variable label
	 * @param val the value (as a double array)
	 */
	public Assignment(String var, double[] val) {
		this();
		map.put(var, ValueFactory.create(val));
	}

	/**
	 * Creates an assignment with a list of sub assignments
	 * 
	 * @param assignments the assignments to combine
	 */
	public Assignment(Assignment... assignments) {
		this();
		Arrays.asList(assignments).stream().forEach(a -> addAssignment(a));
	}

	/**
	 * Creates an assignment with a single pair, given a boolean assignment such as
	 * "Variable" or "!Variable". If booleanAssign start with an exclamation mark,
	 * the value is set to false, else the value is set to true.
	 * 
	 * @param booleanAssign the boolean assignment
	 */
	public Assignment(String booleanAssign) {
		this();
		addPair(booleanAssign);
	}

	/**
	 * Creates an assignment with a list of boolean assignments (cf. method above).
	 * 
	 * @param booleanAssigns the list of boolean assignments
	 */
	public Assignment(List<String> booleanAssigns) {
		this();
		booleanAssigns.stream().forEach(b -> addPair(b));
	}

	/**
	 * Creates an assignment with a map of (var,value) pairs
	 * 
	 * @param pairs the pairs
	 */
	public Assignment(Map<String, Value> pairs) {
		this();
		addPairs(pairs);
	}

	/**
	 * Creates an assignment from an existing one (which is copied), plus a single
	 * (var,value) pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, Value val) {
		this();
		addAssignment(ass);
		addPair(var, val);
	}

	/**
	 * Creates an assignment from an existing one (which is copied), plus a single
	 * (var,value) pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, String val) {
		this();
		addAssignment(ass);
		addPair(var, val);
	}

	/**
	 * Creates an assignment from an existing one (which is copied), plus a single
	 * (var,val) pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, double val) {
		this();
		addAssignment(ass);
		addPair(var, val);
	}

	/**
	 * Creates an assignment from an existing one (which is copied), plus a single
	 * (var, val) pair
	 * 
	 * @param ass the assignment to copy
	 * @param var the variable label
	 * @param val the value
	 */
	public Assignment(Assignment ass, String var, boolean val) {
		this();
		addAssignment(ass);
		addPair(var, val);
	}

	/**
	 * Creates an assignment by adding a set of map entries
	 * 
	 * @param entries the entries to add
	 */
	public Assignment(Set<Entry<String, Value>> entries) {
		this();
		for (Entry<String, Value> entry : entries) {
			addPair(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Creates a new assignment with two pairs of (variable,value)
	 * 
	 * @param var1 label of first variable
	 * @param val1 value of first variable
	 * @param var2 label of second variable
	 * @param val2 value of second variable
	 */
	public Assignment(String var1, Value val1, String var2, Value val2) {
		this();
		map.put(var1, val1);
		map.put(var2, val2);
	}

	/**
	 * Creates an assignment with only none values for the variable labels given as
	 * argument.
	 * 
	 * @param variables the collection of variable labels
	 * @return the resulting default assignment
	 */
	public static Assignment createDefault(Collection<String> variables) {
		Assignment a = new Assignment();
		variables.stream().forEach(v -> a.addPair(v, ValueFactory.none()));
		return a;
	}

	/**
	 * Creates an assignment where all variable share a single common value.
	 * 
	 * @param variables the variables of the assignment
	 * @param string the single value for all variables
	 * @return the corresponding assignment
	 */
	public static Assignment createOneValue(Collection<String> variables,
			String string) {
		Assignment a = new Assignment();
		variables.stream().forEach(v -> a.addPair(v, string));
		return a;
	}

	/**
	 * Creates an assignment with only none values for the variable labels given as
	 * argument.
	 * 
	 * @param variables the collection of variable labels
	 * @return the resulting default assignment
	 */
	public static Assignment createDefault(String... variables) {
		return createDefault(Arrays.asList(variables));
	}

	public static Assignment createFromString(String str) {
		Assignment a = new Assignment();
		for (int i = 0; i < str.split("\\^").length; i++) {
			String substr = str.split("\\^")[i];
			if (substr.contains("=")) {
				String var = substr.split("=")[0].trim();
				String value = substr.split("=")[1].trim();
				a.addPair(var, ValueFactory.create(value));
			}
			else if (substr.contains("!")) {
				String woNeg = substr.replace("!", "").trim();
				a.addPair(woNeg, ValueFactory.create(false));
			}
			else {
				a.addPair(substr.trim(), ValueFactory.create(true));
			}
		}
		return a;
	}

	// ===================================
	// SETTERS
	// ===================================

	/**
	 * Adds a new (var,value) pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value
	 */
	public void addPair(String var, Value val) {
		map.put(var, val);
		cachedHash = 0;
	}

	/**
	 * Adds a new (var, value) pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a string
	 */
	public void addPair(String var, String val) {
		map.put(var, ValueFactory.create(val));
		cachedHash = 0;
	}

	/**
	 * Adds a new (var, value) pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a double
	 */
	public void addPair(String var, double val) {
		map.put(var, ValueFactory.create(val));
		cachedHash = 0;
	}

	/**
	 * Adds a new (var, value) pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a boolean
	 */
	public void addPair(String var, boolean val) {
		map.put(var, ValueFactory.create(val));
		cachedHash = 0;
	}

	/**
	 * Adds a new (var, value) pair to the assignment
	 * 
	 * @param var the variable
	 * @param val the value, as a double array
	 */
	public void addPair(String var, double[] val) {
		map.put(var, ValueFactory.create(val));
		cachedHash = 0;
	}

	/**
	 * Adds a new (var,value) pair as determined by the form of the argument. If the
	 * argument starts with an exclamation mark, the value is set to False, else the
	 * value is set to True.
	 * 
	 * @param booleanAssign the pair to add
	 */
	public void addPair(String booleanAssign) {
		if (!booleanAssign.startsWith("!")) {
			addPair(booleanAssign, ValueFactory.create(true));
		}
		else {
			addPair(booleanAssign.substring(1, booleanAssign.length()),
					ValueFactory.create(false));
		}
	}

	/**
	 * Adds a set of (var,value) pairs to the assignment
	 * 
	 * @param pairs the pairs to add
	 */
	public void addPairs(Map<String, Value> pairs) {
		map.putAll(pairs);
		cachedHash = 0;
	}

	/**
	 * Add a new set of pairs defined in the assignment given as argument (i.e. merge
	 * the given assignment into the present one).
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
	 * @return the removed value
	 */
	public Value removePair(String var) {
		Value v = map.remove(var);
		cachedHash = 0;
		return v;
	}

	/**
	 * Remove the pairs associated with the labels
	 * 
	 * @param vars the variable labels to remove
	 */
	public void removePairs(Collection<String> vars) {
		vars.stream().forEach(v -> map.remove(v));
		cachedHash = 0;
	}

	/**
	 * Remove all pairs whose value equals toRemove
	 * 
	 * @param toRemove the value to remove
	 * @return the resulting assignment
	 */
	public Assignment removeValues(Value toRemove) {
		Assignment a = new Assignment();
		for (String var : map.keySet()) {
			Value v = map.get(var);
			if (!v.equals(toRemove)) {
				a.addPair(var, v);
			}
		}
		cachedHash = 0;
		return a;
	}

	public void clear() {
		map.clear();
		cachedHash = 0;
	}

	/**
	 * Trims the assignment, where only the variables given as parameters are
	 * considered
	 * 
	 * @param variables the variables to consider
	 */
	public void trim(Collection<String> variables) {
		map.keySet().retainAll(variables);
		cachedHash = 0;
	}

	/**
	 * Trims the assignment, where only the variables given as parameters are
	 * considered
	 * 
	 * @param variables the variables to consider
	 */
	public void removeAll(Collection<String> variables) {
		map.keySet().removeAll(variables);
		cachedHash = 0;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the intersection of the two assignments
	 * 
	 * @param assign second assignment
	 * @return the intersection
	 */
	public Assignment intersect(Assignment assign) {
		Assignment intersect = new Assignment();
		for (String var : map.keySet()) {
			Value v = map.get(var);
			if (assign.getValue(var).equals(v)) {
				intersect.addPair(var, v);
			}
		}
		return intersect;
	}

	/**
	 * Returns a new assignment with the primes removed.
	 * 
	 * @return a new assignment, without the accessory specifiers
	 */
	public Assignment removePrimes() {
		Assignment a = new Assignment();
		for (String var : map.keySet()) {
			if (!map.containsKey(var + "'")) {
				boolean hasPrime = (var.charAt(var.length() - 1) == '\'');
				String newVar =
						(hasPrime) ? var.substring(0, var.length() - 1) : var;
				a.addPair(newVar, map.get(var));
			}
		}

		return a;
	}

	/**
	 * Filter the assignment by removing all pairs that do not satisfy the given
	 * predicate
	 * 
	 * @param predicate the predicate to apply for the filtering
	 */
	public void filterValues(Predicate<Value> predicate) {
		Stack<String> toRemove = new Stack<String>();
		for (String var : map.keySet()) {
			if (!predicate.test(map.get(var))) {
				toRemove.add(var);
			}
		}
		while (!toRemove.isEmpty()) {
			map.remove(toRemove.pop());
		}
		cachedHash = 0;
	}

	/**
	 * Returns a new assignment where the variable name is replaced.
	 * 
	 * @param oldVar old variable name
	 * @param newVar new variable name
	 * @return the new assignment with the renamed variable
	 */
	public Assignment renameVar(String oldVar, String newVar) {
		Assignment newAssign = copy();
		if (containsVar(oldVar)) {
			Value condVal = newAssign.removePair(oldVar);
			newAssign.addPair(newVar, condVal);
		}
		return newAssign;
	}

	public Assignment addPrimes() {
		Assignment a = new Assignment();
		map.entrySet().stream()
				.forEach(e -> a.addPair(e.getKey() + "'", e.getValue()));
		return a;
	}

	/**
	 * Returns whether the assignment is empty.
	 * 
	 * @return true if the assignment is empty, else false.
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns the pairs of the assignment
	 * 
	 * @return all pairs
	 */
	public Map<String, Value> getPairs() {
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
	 * Returns true if the assignment contains the given variable, and false
	 * otherwise
	 * 
	 * @param var the variable label
	 * @return true if the variable is included, false otherwise
	 */
	public boolean containsVar(String var) {
		return map.containsKey(var);
	}

	/**
	 * Returns true if the assignment contains the given entry
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return true if the assignment contains the pair, false otherwise
	 */
	public boolean containsPair(String variable, Value value) {
		Value v = map.get(variable);
		return v != null && v.equals(value);
	}

	/**
	 * Returns true if the assignment contains all of the given variables, and false
	 * otherwise
	 * 
	 * @param vars the variable labels
	 * @return true if all variables are included, false otherwise
	 */
	public boolean containsVars(Collection<String> vars) {
		return map.keySet().containsAll(vars);
	}

	/**
	 * Returns true if the assignment contains at least one of the given variables,
	 * and false otherwise
	 * 
	 * @param vars the variable labels
	 * @return true if at least one variable are included, false otherwise
	 */
	public boolean containsOneVar(Set<String> vars) {
		return !Collections.disjoint(map.keySet(), vars);
	}

	/**
	 * Returns a trimmed version of the assignment, where only the variables given as
	 * parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmed(Collection<String> variables) {
		Assignment a = new Assignment();
		int trimmedHash = 0;
		for (Entry<String, Value> e : map.entrySet()) {
			String var = e.getKey();
			if (variables.contains(var)) {
				Value val = e.getValue();
				a.addPair(var, val);
				trimmedHash += var.hashCode() ^ val.hashCode();
			}
		}
		a.cachedHash = trimmedHash;
		return a;
	}

	/**
	 * Returns a prunes version of the assignment, where the the variables given as
	 * parameters are pruned out of the assignment
	 * 
	 * @param variables the variables to remove
	 * @return a new, pruned assignment
	 */
	public Assignment getPruned(Collection<String> variables) {
		Assignment a = new Assignment();
		int trimmedHash = 0;
		for (Entry<String, Value> e : map.entrySet()) {
			String var = e.getKey();
			if (!variables.contains(var)) {
				Value val = e.getValue();
				a.addPair(var, val);
				trimmedHash += var.hashCode() ^ val.hashCode();
			}
		}
		a.cachedHash = trimmedHash;
		return a;
	}

	/**
	 * Returns a trimmed version of the assignment, where only the variables given as
	 * parameters are considered
	 * 
	 * @param variables the variables to consider
	 * @return a new, trimmed assignment
	 */
	public Assignment getTrimmed(String... variables) {
		return getTrimmed(Arrays.asList(variables));
	}

	/**
	 * Returns a copy of the assignment
	 * 
	 * @return the copy
	 */
	public Assignment copy() {
		Assignment c = new Assignment(this);
		c.cachedHash = cachedHash;
		return c;
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
	public Set<Entry<String, Value>> getEntrySet() {
		return map.entrySet();
	}

	/**
	 * Returns the value associated with the variable in the assignment, if one is
	 * specified. Else, returns the none value.
	 * 
	 * @param var the variable
	 * @return the associated value
	 */
	public Value getValue(String var) {
		return map.getOrDefault(var, ValueFactory.none());
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
	 * Returns the list of values corresponding to a subset of variables in the
	 * assignment (in the same order)
	 * 
	 * @param subsetVars the subset of variable labels
	 * @return the corresponding values
	 */
	public List<Value> getValues(List<String> subsetVars) {
		List<Value> vals = new ArrayList<Value>();
		for (String var : subsetVars) {
			Value v = map.getOrDefault(var, ValueFactory.none());
			vals.add(v);
		}
		return vals;
	}

	/**
	 * Returns true if the assignment contains all pairs specified in the assignment
	 * given as parameter (both the label and its value must match for all pairs).
	 * 
	 * @param a the assignment
	 * @return true if a is contained in assignment, false otherwise
	 */
	public boolean contains(Assignment a) {
		for (String key : a.getVariables()) {
			if (map.containsKey(key)) {
				Value val = a.getValue(key);
				Value val2 = map.get(key);
				if (val2 == null) {
					if (val != null) {
						return false;
					}
				}
				else if (!val2.equals(val)) {
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
	 * Returns true if the two assignments are mutually consistent, i.e. if there is
	 * a label l which appears in both assignment, then their value must be equal.
	 * 
	 * @param a the second assignment
	 * @return true if assignments are consistent, false otherwise
	 */
	public boolean consistentWith(Assignment a) {

		Map<String, Value> firstMap = (a.size() < map.size()) ? a.map : map;
		Map<String, Value> secondMap = (a.size() < map.size()) ? map : a.map;

		for (String evidenceVar : firstMap.keySet()) {
			Value v2 = secondMap.get(evidenceVar);
			if (v2 == null) {
				continue;
			}
			Value v1 = firstMap.get(evidenceVar);
			if (!v1.equals(v2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the two assignments are mutually consistent, i.e. if there is
	 * a label l which appears in both assignment, then their value must be equal.
	 * The checks are here only done on the subset of variables subvars
	 * 
	 * @param a the second assignment
	 * @param subvars the subset of variables to check
	 * @return true if assignments are consistent, false otherwise
	 */
	public boolean consistentWith(Assignment a, Set<String> subvars) {
		for (String subvar : subvars) {
			Value v2 = map.get(subvar);
			Value v1 = a.getValue(subvar);
			if (v1 == null || v2 == null || !v1.equals(v2)) {
				return false;
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

	public boolean containContinuousValues() {
		for (String var : map.keySet()) {
			if (map.get(var) instanceof DoubleVal
					|| map.get(var) instanceof ArrayVal) {
				return true;
			}
		}
		return false;
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns the hashcode associated with the assignment. The hashcode is
	 * calculated from the hashmap corresponding to the assignment.
	 * 
	 * @return the corresponding hashcode
	 */
	@Override
	public int hashCode() {
		if (cachedHash == 0) {
			cachedHash = map.hashCode();
		}
		return cachedHash;
	}

	/**
	 * Returns true if the object given as argument is an assignment identical to the
	 * present one
	 *
	 * @param o the object to compare
	 * @return true if the assignments are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Assignment) {
			return map.equals(((Assignment) o).map);
		}
		return false;
	}

	public Element generateXML(Document doc) {

		Element root = doc.createElement("assignment");

		for (String varId : map.keySet()) {
			Element var = doc.createElement("variable");
			Attr id = doc.createAttribute("id");
			id.setValue(varId);
			var.setAttributeNode(id);
			Element value = doc.createElement("value");
			value.setTextContent(map.get(varId).toString());
			var.appendChild(value);
			root.appendChild(var);
		}
		return root;
	}

	/**
	 * Returns a string representation of the assignment
	 */
	@Override
	public String toString() {
		String str = "";
		if (map.keySet().isEmpty()) {
			return "~";
		}
		List<String> keyList = new ArrayList<String>(map.keySet());
		for (String key : keyList) {
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
				str += key + "=" + map.get(key);
			}
			if (!key.equals(keyList.get(map.keySet().size() - 1))) {
				str += " ^ ";
			}
		}
		return str;

	}

}
