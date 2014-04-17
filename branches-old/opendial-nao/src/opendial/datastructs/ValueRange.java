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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;


/**
 * Representation of a range of alternative values for a set of variables.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ValueRange {

	// logger
	public static Logger log = new Logger("ValueRange", Logger.Level.NORMAL);
	
	Map<String,Set<Value>> range;
	
	
	/**
	 * Constructs a new, empty range of values.
	 */
	public ValueRange() {
		range = new HashMap<String,Set<Value>>();
	}
	
	/**
	 * Constructs a new range that is the union of two existing ranges
	 * 
	 * @param range1 the first range of values
	 * @param range2 the second range of values.
	 */
	public ValueRange(ValueRange range1, ValueRange range2) {
		this();
		addRange(range1);
		addRange(range2);
	}
	
	
	/**
	 * Constructs a range of values based on the mapping between variables
	 * and sets of values
	 * 
	 * @param range the range (as a map)
	 */
	public ValueRange(Map<String,Set<Value>> range) {
		this.range = new HashMap<String,Set<Value>>(range);
	}

	/**
	 * Adds a value for the variable in the range.
	 * 
	 * @param variable the variable
	 * @param val the value
	 */
	public void addValue(String variable, Value val) {
		if (!range.containsKey(variable)) {
			range.put(variable, new HashSet<Value>());
		}
		range.get(variable).add(val);
	}

	
	/**
	 * Adds a set of values for the variable
	 * @param variable the variable
	 * @param values the values to add
	 */
	public void addValues(String variable, Set<Value> values) {
		for (Value val : values) {
			addValue(variable, val);
		}
	}

	/**
	 * Adds the values defined in the assignment to the range
	 * 
	 * @param assignment the value assignments
	 */
	public void addAssign(Assignment assignment) {
		for (String var : assignment.getVariables()) {
			addValue(var, assignment.getValue(var));
		}
	}

	/**
	 * Adds the range of values to the existing one.
	 * 
	 * @param newRange the new range
	 */
	public void addRange(ValueRange newRange) {
		for (String var : newRange.getVariables()) {
			addValues(var, newRange.getValues(var));
		}
	}
	
	
	/**
	 * Extracts all alternative assignments of values for the variables
	 * in the range.  This operation can be computational expensive,
	 * use with caution.
	 * 
	 * @return the set of alternative assignments
	 */
	public Set<Assignment> linearise() {
		return CombinatoricsUtils.getAllCombinations(range);
	}


	/**
	 * Returns the set of variables with a non-empty range of values
	 * 
	 * @return the set of variables
	 */
	public Set<String> getVariables() {
		return range.keySet();
	}
	
	
	/**
	 * Returns the set of values for the variable in the range (if defined).
	 * If the variable is not defined in the range, returns null
	 * 
	 * @param variable the variable
	 * @return its set of alternative values
	 */
	public Set<Value> getValues(String variable) {
		return range.get(variable);
	}

	
	/**
	 * Returns a string representation for the range
	 */
	@Override
	public String toString() {
		return range.toString();
	}
	
	
	/**
	 * Returns the hashcode for the range of values
	 */
	@Override
	public int hashCode() {
		return range.hashCode() - 1;
	}

	
	/**
	 * Returns true if the range is empty (contains no variables).
	 * 
	 * @return true if empty, else false.
	 */
	public boolean isEmpty() {
		return range.isEmpty();
	}

	/**
	 * Intersects the range with the existing one (only retains
	 * the values defined in both ranges).
	 * 
	 * @param otherRange the range to intersect with the existing one
	 */
	public void intersectRange(ValueRange otherRange) {
		for (String id : otherRange.getVariables()) {
			if (range.containsKey(id)) {
				range.get(id).retainAll(otherRange.getValues(id));
			}
			else {
				addValues(id, otherRange.getValues(id));
			}
		}
	}
}

