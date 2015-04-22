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

package opendial.domains.rules.conditions;

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.RuleGrounding;


/**
 * Basic condition between a variable and a value
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class BasicCondition implements Condition {

	static Logger log = new Logger("BasicCondition", Logger.Level.DEBUG);

	// possible relations used in a basic condition
	public static enum Relation {EQUAL, UNEQUAL, CONTAINS, NOT_CONTAINS,
		GREATER_THAN, LOWER_THAN, IN, NOT_IN, LENGTH}

	// variable label (can include slots to fill)
	final String variable;

	// expected variable value (can include slots to fill)
	final Value expectedValue;

	// the relation which needs to hold between the variable and the value
	// (default is EQUAL)
	final Relation relation;

	// ===================================
	//  CONDITION CONSTRUCTION
	// ===================================


	/**
	 * Creates a new basic condition, given a variable label, an expected value,
	 * and a relation to hold between the variable and its value
	 * 
	 * @param variable the variable
	 * @param value the value
	 * @param relation the relation to hold
	 */
	public BasicCondition(String variable, Value value, Relation relation) {
		this.variable = variable;
		this.expectedValue = value;
		this.relation = relation;
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the relation in place for the condition
	 * 
	 * @return the relation
	 */
	public Relation getRelation() {
		return relation;
	}


	/**
	 * Returns the variable label for the basic condition
	 * 
	 * @return the variable label
	 */
	public String getVariable() {
		return variable;
	}


	/**
	 * Returns the expected variable value for the basic condition
	 * 
	 * @return the expected variable value
	 */
	public Value getValue() {
		return expectedValue;
	}
	
	
	/**
	 * Returns an empty list
	 * 
	 * @return an empty list
	 */
	@Override
	public Set<String> getSlots() {
		return new HashSet<String>();
	}



	/**
	 * Returns the input variables for the condition (the main variable
	 * itself, plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<Template> getInputVariables() {
		Set<Template> inputVariables = new HashSet<Template>();
		inputVariables.add(new Template(variable));
		return inputVariables;
	}




	/**
	 * Returns true if the condition is satisfied by the value assignment
	 * provided as argument, and false otherwise
	 * 
	 * <p>This method uses an external ConditionCheck object to ease the
	 * process.
	 *
	 * @param input the actual assignment of values
	 * @return true if the condition is satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		Value actualValue = input.getValue(variable);
		switch (relation) {	
		case EQUAL: return actualValue.equals(expectedValue);
		case UNEQUAL: return !actualValue.equals(expectedValue); 
		case GREATER_THAN: return (actualValue.compareTo(expectedValue) > 0); 
		case LOWER_THAN: return (actualValue.compareTo(expectedValue) < 0);
		case CONTAINS: return actualValue.contains(expectedValue); 
		case NOT_CONTAINS: return !actualValue.contains(expectedValue); 
		case LENGTH: return actualValue.length() == expectedValue.length();
		case IN: return expectedValue.contains(actualValue); 
		case NOT_IN: return !expectedValue.contains(actualValue); 
		}
		return false;
	}


	/**
	 * Returns the set of possible groundings for the given input assignment
	 * 
	 * @param input the input assignment
	 * @return the set of possible (alternative) groundings for the condition
	 */
	@Override
	public RuleGrounding getGroundings(Assignment input) {	
		if (relation == Relation.IN && expectedValue instanceof SetVal) {
			return new RuleGrounding(variable, ((SetVal)expectedValue).getSet());
		}
		else if (isSatisfiedBy(input)){
			return new RuleGrounding();
		}
		else {
			return new RuleGrounding.Failed();
		}
	}




	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	/**
	 * Returns a string representation of the condition
	 */
	@Override
	public String toString() {
		switch (relation) {
		case EQUAL: return variable + "=" + expectedValue ; 
		case UNEQUAL: return variable + "!=" + expectedValue ; 
		case GREATER_THAN: return variable + ">" + expectedValue; 
		case LOWER_THAN : return variable + "<" + expectedValue; 
		case CONTAINS: return expectedValue + " in " + variable; 
		case NOT_CONTAINS: return expectedValue + " !in " + variable;
		case LENGTH: return "length("+variable+")=" + expectedValue;
		case IN: return variable + " in " + expectedValue; 
		case NOT_IN: return variable + " not in " + expectedValue;
		default: return ""; 
		}
	}

	/**
	 * Returns the hashcode for the condition
	 *
	 * @return the hashcode
	 */
	@Override 
	public int hashCode() {
		return variable.hashCode() + expectedValue.hashCode() - 3*relation.hashCode();
	}


	/**
	 * Returns true if the given object is a condition identical to the current
	 * instance, and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if the condition are equals, and false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		if (o instanceof BasicCondition) {
			return (((BasicCondition)o).getVariable().equals(variable) && 
					((BasicCondition)o).getValue().equals(expectedValue) && 
					relation == ((BasicCondition)o).getRelation());
		}
		return false;
	}


}
