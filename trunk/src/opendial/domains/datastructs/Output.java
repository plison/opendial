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

package opendial.domains.datastructs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

/**
 * Representation of an "output", from a rule effect or an external module. 
 * An output defines how to set or modify a set of particular variables in 
 * the dialogue state. 
 * 
 * More specifically, an output can specify: <ol>
 * <li> a set of values to set for specific variables;
 * <li> a set of values to discard for specific variables;
 * <li> a set of values to add for specific variables;
 * <li> whether specific variables should be cleared (remove all values).
 * </ol>
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Output implements Value {

	// logger
	public static Logger log = new Logger("Output", Logger.Level.DEBUG);
	
	// a value to set for a variable
	// (there can only at most one per variable)
	Map<String,Value> setValues;
	
	// sets of values to discard 
	Map<String,Set<Value>> discardValues;
	
	// sets of values to add
	Map<String,Set<Value>> addValues;

	// variables to clear
	Set<String> toClear;
	
	// whether the output is "broken" (incomplete fillers)
	boolean broken = false;
	
	
	// ===================================
	//  OUTPUT CONSTRUCTION
	// ===================================

	
	/**
	 * Creates a new output
	 */
	public Output() {
		setValues = new HashMap<String,Value>();
		discardValues = new HashMap<String,Set<Value>>();
		addValues = new HashMap<String,Set<Value>>();
		toClear = new HashSet<String>();
	}
	
	/**
	 * Sets a specific value for a specific variable
	 * 
	 * @param variable the variable label
	 * @param value the value to set
	 */
	public void setValueForVariable(String variable, Value value) {
		if (setValues.containsKey(variable)) {
			log.info(variable + ":=" + setValues.get(variable) 
					+ " overriden by " + variable +":=" + value);
		}
		if (addValues.containsKey(variable)) {
			log.warning("output should not contain both set and add values for variable " + variable);
		}
		setValues.put(variable,value);
	}
	
	
	/**
	 * Sets a value to discard for a specific variable
	 * 
	 * @param variable the variable label
	 * @param value the value to discard
	 */
	public void discardValueForVariable(String variable, Value value) {
		if (!discardValues.containsKey(variable)) {
			discardValues.put(variable, new HashSet<Value>());
		}
		discardValues.get(variable).add(value);
	}
	
	
	/**
	 * Sets a value to add for a specific variable
	 * (if the variable domain is defined as a set)
	 * 
	 * @param variable the variable label
	 * @param value the value to add
	 */
	public void addValueForVariable(String variable, Value value) {
		if (!addValues.containsKey(variable)) {
			addValues.put(variable, new HashSet<Value>());
		}
		if (setValues.containsKey(variable)) {
			log.warning("output should not contain both set and add values for variable " + variable);
		}
		addValues.get(variable).add(value);
	}
	
	
	/**
	 * Sets the variable as variable to clear (remove all values).
	 * 
	 * @param variable the variable to clear
	 */
	public void clearVariable(String variable) {
		toClear.add(variable);
	}
	
	/**
	 * Sets the values for specific variables
	 * 
	 * @param newSetValues the (variable,value) pairs
	 */
	public void setValuesForVariables(Map<String,Value> newSetValues) {
		setValues.putAll(newSetValues);
	}

	/**
	 * Sets the values for specific variables
	 * 
	 * @param newSetValues the (variable,value) pairs as an assignment
	 */
	public void setValuesForVariables(Assignment newSetValues) {
		setValues.putAll(newSetValues.getPairs());
	}
	
	
	/**
	 * Sets values to discard for specific variables 
	 * 
	 * @param newDiscardValues (variable, values) pairs
	 */
	public void discardValuesForVariables(Map<String,Set<Value>> newDiscardValues) {
		for (String variable: newDiscardValues.keySet()) {
			if (!discardValues.containsKey(variable)) {
				discardValues.put(variable, new HashSet<Value>());
			}
			discardValues.get(variable).addAll(newDiscardValues.get(variable));
		}
	}
	
	
	/**
	 * Sets values to add for specific variables 
	 * 
	 * @param newAddValues (variable, values) pairs
	 */
	public void addValuesForVariables(Map<String,Set<Value>> newAddValues) {
		for (String variable: newAddValues.keySet()) {
			if (!addValues.containsKey(variable)) {
				addValues.put(variable, new HashSet<Value>());
			}
			addValues.get(variable).addAll(newAddValues.get(variable));
		}
	}
	
	
	/**
	 * Sets variables as variables to clear (remove all values)
	 * 
	 * @param variables variables to clear
	 */
	public void clearVariables(Set<String> variables) {
		toClear.addAll(variables);
	}
	
	

	/**
	 * Includes the content of the output given as argument into the 
	 * current instance
	 * 
	 * @param output the output to integrate
	 */
	public void includeOutput(Output output) {
		setValuesForVariables(output.getAllSetValues());
		discardValuesForVariables(output.getAllDiscardValues());
		addValuesForVariables(output.getAllAddValues());
		clearVariables(output.getVariablesToClear());
		if (output.isBroken()) {
			broken = true;
		}
	}
	
	
	/**
	 * Adds an ending to all the variables used in the output
	 * 
	 * @param ending the ending to add
	 */
	public void addEndingToVariables(String ending) {
		Map<String,Value> oldSetValues = new HashMap<String,Value>(setValues);
		setValues.clear();
		for (String setVar : oldSetValues.keySet()) {
			setValues.put(setVar+ending, oldSetValues.get(setVar));
		}
		Map<String,Set<Value>> oldDiscardValues = new HashMap<String,Set<Value>>(discardValues);
		discardValues.clear();
		for (String discardVar : oldDiscardValues.keySet()) {
			discardValues.put(discardVar+ending, oldDiscardValues.get(discardVar));
		}
		Map<String,Set<Value>> oldAddValues = new HashMap<String,Set<Value>>(addValues);
		addValues.clear();
		for (String addVar : oldAddValues.keySet()) {
			addValues.put(addVar+ending, oldAddValues.get(addVar));
		}
		Set<String> oldToClear = new HashSet<String>(toClear);
		toClear.clear();
		for (String toClearVar : oldToClear) {
			toClear.add(toClearVar+ending);
		}
	}
	
	
	/**
	 * Sets the output as "broken", in case a slot could not be filled in an 
	 * effect.  This is useful for decision rules.
	 * 
	 * @param broken whether the output is broken
	 */
	public void setAsBroken(boolean broken) {
		this.broken = broken;
	}
	
	
	// ===================================
	//  GETTERS
	// ===================================

	
	/**
	 * Returns true is the output does not specify anything, and false otherwise
	 * 
	 * @return true if the output does not include any change
	 */
	public boolean isVoid() {
		return (setValues.isEmpty() && discardValues.isEmpty() && addValues.isEmpty() && toClear.isEmpty());
	}

	/**
	 * Returns whether the output is "broken" or not (incomplete filler).
	 * 
	 * @return true if broken, false otherwise.
	 */
	public boolean isBroken() {
		return broken;
	}
	

	/**
	 * Returns the value to set for the variable (if any).  If no
	 * value has been specified, returns a None value.
	 * 
	 * @param variable the variable label
	 * @return the value to set for the variable (if any)
	 */
	public Value getSetValue (String variable) {
		if (setValues.containsKey(variable)) {
			return setValues.get(variable);
		}
		return ValueFactory.none();
	}
	
	
	/**
	 * Returns true if there are values to discard for the variable.
	 * 
	 * @param variable the variable label
	 * @return true if there are values to discard, false otherwise
	 */
	public boolean hasValuesToDiscard(String variable) {
		return discardValues.containsKey(variable);
	}
	
	
	/**
	 * Returns the (possibly empty) set of values to discard for the variable .
	 * 
	 * @param variable the variable label
	 * @return the set of values to discard
	 */
	public Set<Value> getValuesToDiscard(String variable) {
		if (discardValues.containsKey(variable)) {
			return new HashSet<Value>(discardValues.get(variable));
		}
		else {
			return new HashSet<Value>();
		}
	}
	

	/**
	 * Returns true if there are values to add for the variable.
	 * 
	 * @param variable the variable label
	 * @return true if there are values to add, false otherwise
	 */
	public boolean hasValuesToAdd(String variable) {
		return addValues.containsKey(variable);
	}
	
	
	/**
	 * Returns the (possibly empty) set of values to add for the variable .
	 * 
	 * @param variable the variable label
	 * @return the set of values to add
	 */
	public Set<Value> getValuesToAdd(String variable) {
		if (addValues.containsKey(variable)) {
			return new HashSet<Value>(addValues.get(variable));
		}
		else {
			return new HashSet<Value>();
		}
	}
	
	
	/**
	 * Returns true if the variable should be cleared of all its values,
	 * and false otherwise
	 * 
	 * @param variable the variable label
	 * @return true if variable should be cleared, false otherwise
	 */
	public boolean mustBeCleared(String variable) {
		return toClear.contains(variable);
	}
	
	
	/**
	 * Returns a mapping with all values to set for the variables in the output
	 * 
	 * @return all the values to set
	 */
	public Map<String,Value> getAllSetValues() {
		return new HashMap<String,Value>(setValues);
	}
	

	/**
	 * Returns a mapping with all values to discard for the variables in the output
	 * 
	 * @return all the values to discard
	 */
	public Map<String,Set<Value>> getAllDiscardValues() {
		return new HashMap<String,Set<Value>>(discardValues);
	}
	
	/**
	 * Returns a mapping with all values to add for the variables in the output
	 * 
	 * @return all the values to add
	 */
	public Map<String,Set<Value>> getAllAddValues() {
		return new HashMap<String,Set<Value>>(addValues);
	}
	
	
	/**
	 * Returns all variables to clear 
	 * 
	 * @return the variables to clear
	 */
	public Set<String> getVariablesToClear() {
		return new HashSet<String>(toClear);
	}
	
	

	/**
	 * Returns all the variable labels influenced by the output
	 * 
	 * @return all the variables influenced by the output
	 */
	public Set<String> getVariables() {
		Set<String> allVariables = new HashSet<String>();
		allVariables.addAll(setValues.keySet());
		allVariables.addAll(discardValues.keySet());
		allVariables.addAll(addValues.keySet());
		allVariables.addAll(toClear);
		return allVariables;
	}
	
	
	/**
	 * Returns true if the output is compatible with the assignment of values
	 * (typically actions) given as argument
	 * 
	 * @param actions the assignment of values
	 * @return true if compatible, false otherwise
	 */
	public boolean isCompatibleWith(Assignment actions) {
		
		// checks whether the output is broken
		if (broken) {
			return false;
		}
		// all the set values must be satisfied
		for (String var : setValues.keySet()) {
			if (!actions.containsVar(var)) { return false; }
			else {
				Value expectedVal = setValues.get(var);
				if (expectedVal instanceof StringVal) {
					Template expectedValStr = new Template(expectedVal.toString());
					if (!expectedValStr.isMatching(actions.getValue(var).toString(), false)) {
						return false;
					}
				}
				else if	(!actions.getValue(var).equals(expectedVal)) { return false; }
			}
		}
		// all the discard values must be satisfied
		for (String var : discardValues.keySet()) {
			if (actions.containsVar(var) && discardValues.get(var).
					contains(actions.getValue(var))) {
				return false; 
			}
		}
		
		// finally, all the other action assignment must be equal to none
		/** Assignment actionsCopy = new Assignment(actions);
		actionsCopy.removePairs(getVariables());
		for (String remainingVar : actionsCopy.getVariables()) {
			if (!actionsCopy.getValue(remainingVar).equals(ValueFactory.none())) {
				return false;
			}
		} */
		
		return true;
	}
	

	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	
	/**
	 * Parses the string representing the output, and returns the output object.
	 * 
	 * @param outputString the string representing the output
	 * @return the corresponding output
	 */
	public static Output parseOutput(String outputString) {
		Output o = new Output();
		if (outputString.contains(" ^ ")) {
			for (String split : outputString.split(" \\^ ")) {
				Output subOutput = parseOutput (split);
				o.includeOutput(subOutput);
			}
		}
		else {
			if (outputString.contains("Void")) {
				return new Output();
			}
			if (outputString.contains(":=") && outputString.contains("{}")) {
				String var = outputString.split(":=")[0];
				o.clearVariable(var);
			}
			else if (outputString.contains(":=")) {
				String var = outputString.split(":=")[0];
				Value val = ValueFactory.create(outputString.split(":=")[1]);
				o.setValueForVariable(var, val);
			}
			else if (outputString.contains("!=")) {
				String var = outputString.split("!=")[0];
				Value val = ValueFactory.create(outputString.split("!=")[1]);
				o.discardValueForVariable(var, val);
			}
			else if (outputString.contains("+=")) {
				String var = outputString.split("\\+=")[0];
				Value val = ValueFactory.create(outputString.split("\\+=")[1]);
				o.addValueForVariable(var, val);
			}
		}	
		return o;
	}
	
	/**
	 * Returns the hashcode for the output
	 * 
	 * @return the hashcode
	 */
	public int hashCode() {
		return setValues.hashCode() + 2*addValues.hashCode() 
		- 3*discardValues.hashCode() +7 * toClear.hashCode();
	}
	
	/**
	 * Returns a string representation for the output
	 *
	 * @return the string representation
	 */
	public String toString() {
		String str = "";
		for (String var : toClear) {
			str += var + ":= {}" + " ^ ";
		}
		for (String var : setValues.keySet()) {
			str += var + ":=" + setValues.get(var) + " ^ ";
		}
		for (String var : discardValues.keySet()) {
			for (Value val : discardValues.get(var)) {
				str += var + "!=" + val + " ^ ";
			}
		}
		for (String var : addValues.keySet()) {
			for (Value val : addValues.get(var)) {
				str += var + "+=" + val + " ^ ";
			}
		}
		if (str.contains("^")) {
			str = str.substring(0, str.length()-3);
		}
		
		if (str.equals("")) {
			str = "Void";
		}
		return str;
	}
	
	
	/**
	 * Returns true if o is also an output with identical content, and false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if the objects are identical, and false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof Output) {
			if (((Output)o).getAllSetValues().equals(setValues)
				&& ((Output)o).getAllDiscardValues().equals(discardValues)
				&& ((Output)o).getAllAddValues().equals(addValues)
				&& ((Output)o).getVariablesToClear().equals(toClear)) {
				return true;		
				}	
		}
		return false;
	}
	
	
	/**
	 * Creates a copy of the current output
	 * 
	 * @return the copy
	 */
	public Output copy() {
		Output copy = new Output();
		copy.setValuesForVariables(setValues);
		copy.discardValuesForVariables(discardValues);
		copy.addValuesForVariables(addValues);
		copy.clearVariables(toClear);
		return copy;
	}

	/**
	 * Compares the output value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}
	

}
