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

package opendial.domains.rules.conditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.conditions.checks.ConditionCheck;
import opendial.domains.rules.conditions.checks.ConditionCheckFactory;

/**
 * Basic condition between a variable and a value
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicCondition implements Condition {

	static Logger log = new Logger("BasicCondition", Logger.Level.DEBUG);

	// possible relations used in a basic condition
	public static enum Relation {EQUAL, UNEQUAL, EXACT_MATCH, PARTIAL_MATCH, 
		GREATER_THAN, LOWER_THAN, CONTAINS, NOT_CONTAINS}
	
	// variable label (can include slots to fill)
	TemplateString variable;

	// expected variable value (can include slots to fill)
	TemplateString expectedValue;

	// set of input and local output variables
	Set<TemplateString> inputVariables;
	Set<String> localOutputVariables;

	// the relation which needs to hold between the variable and the value
	// (default is EQUAL)
	Relation relation = Relation.EQUAL;
	
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
	public BasicCondition(String variable, String value, Relation relation) {
		this.variable = new TemplateString(variable);
		this.expectedValue = new TemplateString(value);
		setRelation(relation);
		
		// fill the input and local output variables
		inputVariables = new HashSet<TemplateString>(Arrays.asList(this.variable));
		localOutputVariables = new HashSet<String>();
		if (relation == Relation.PARTIAL_MATCH || relation == Relation.EXACT_MATCH) {
			for (String slot : this.expectedValue.getSlots()) {
				localOutputVariables.add(slot);
			}
		}
		else {
			for (String slot : this.expectedValue.getSlots()) {
				inputVariables.add(new TemplateString(slot));
			}
		}
	}

	/**
	 * Changes the relation in place in the condition
	 * 
	 * @param relation the relation
	 */
	public void setRelation(Relation relation) {
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
	public TemplateString getVariable() {
		return variable;
	}
	
	
	/**
	 * Returns the expected variable value for the basic condition
	 * 
	 * @return the expected variable value
	 */
	public TemplateString getValue() {
		return expectedValue;
	}
	
	/**
	 * Returns the input variables for the condition (the main variable
	 * itself, plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<TemplateString> getInputVariables() {
		return inputVariables;
	}


	/**
	 * Returns the local output variables that can be produced by the
	 * condition
	 * 
	 * @return the local output variables
	 */
	@Override
	public Set<String> getLocalOutputVariables() {
		return localOutputVariables;
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

		ConditionCheck check = ConditionCheckFactory.createCheck(expectedValue, relation, input);
		
		Value value = ValueFactory.none();
		TemplateString instantiatedVar = variable.fillSlotsPartial(input);
		if (instantiatedVar.getSlots().isEmpty() 
				&& input.containsVar(instantiatedVar.getRawString())) {
			value = input.getValue(instantiatedVar.getRawString());
		}
		
		return check.isSatisfied(value);		
	}

	
	/**
	 * Return the local output produced by the condition, if any
	 * 
	 * @param input the actual assignment of values
	 * @return a value assignment (that can be empty)
	 */
	@Override
	public Assignment getLocalOutput(Assignment input) {	
	
		ConditionCheck check = ConditionCheckFactory.createCheck(expectedValue, relation, input);
		
		Value value = ValueFactory.none();
		TemplateString instantiatedVar = variable.fillSlotsPartial(input);
		if (instantiatedVar.getSlots().isEmpty() 
				&& input.containsVar(instantiatedVar.getRawString())) {
			value = input.getValue(instantiatedVar.getRawString());
		}
		
		return check.getLocalOutput(value);	
	}



	
	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	/**
	 * Returns a string representation of the condition
	 * 
	 * @return the string representation
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
		case EXACT_MATCH: return variable + " matches exact pattern \"" + expectedValue + "\"";
		case PARTIAL_MATCH: return variable + " matches partial pattern \"" + expectedValue + "\"";
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
					((BasicCondition)o).getValue().equals(expectedValue));
		}
		return false;
	}
	

}
