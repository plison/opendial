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
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.checks.AbstractCheck;
import opendial.domains.rules.conditions.checks.CheckFactory;
import opendial.domains.rules.quantification.LabelPredicate;
import opendial.domains.rules.quantification.UnboundPredicate;
import opendial.domains.rules.quantification.ValuePredicate;

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
	public static enum Relation {EQUAL, UNEQUAL, CONTAINS, NOT_CONTAINS,
		GREATER_THAN, LOWER_THAN}
	
	// variable label (can include slots to fill)
	Template variable;

	// expected variable value (can include slots to fill)
	Template expectedValue;

	// set of input and local output variables
	Set<Template> inputVariables;
	
	// the relation which needs to hold between the variable and the value
	// (default is EQUAL)
	Relation relation = Relation.EQUAL;
	
	AbstractCheck check;
	
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
		this.variable = new Template(variable);
		this.expectedValue = new Template(value);
		this.relation = relation;
		
		// fill the input and local output variables
		inputVariables = new HashSet<Template>(Arrays.asList(this.variable));
		for (String slot : expectedValue.getSlots()) {
			inputVariables.add(new Template(slot));
		}
		check = CheckFactory.createCheck(this.variable, this.expectedValue, relation);
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
	public Template getVariable() {
		return variable;
	}
	
	
	/**
	 * Returns the expected variable value for the basic condition
	 * 
	 * @return the expected variable value
	 */
	public Template getValue() {
		return expectedValue;
	}
	

	/**
	 * Returns the set of unbound predicates for the basic condition, which could
	 * be either associated with the variable label or its content.
	 * 
	 * @return the set of unbound predicates
	 */
	@Override
	public Set<UnboundPredicate> getUnboundPredicates() {
		Set<UnboundPredicate> predicates = new HashSet<UnboundPredicate>();
		if (!variable.getSlots().isEmpty()) {
			predicates.add(new LabelPredicate(variable));
		}
		if (!expectedValue.getSlots().isEmpty()) {
			predicates.add(new ValuePredicate(variable.toString(), expectedValue));
		}
		return predicates;
	}
	
	/**
	 * Returns the input variables for the condition (the main variable
	 * itself, plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<Template> getInputVariables() {
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
		return check.isSatisfied(input);		
	}

	
	/**
	 * Return the local output produced by the condition, if any
	 * 
	 * @param input the actual assignment of values
	 * @return a value assignment (that can be empty)
	 */
	@Override
	public Assignment getLocalOutput(Assignment input) {	
	
		return check.getLocalOutput(input);
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
