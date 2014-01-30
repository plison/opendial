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

package opendial.domains.rules.conditions;

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.SetVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.Template.MatchResult;
import opendial.datastructs.ValueRange;


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
	public BasicCondition(Template variable, Template value, Relation relation) {
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
	 * Returns the input variables for the condition (the main variable
	 * itself, plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<Template> getInputVariables() {
		Set<Template> inputVariables = new HashSet<Template>();
		inputVariables.add(variable);
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
		
		if (!input.containsVars(variable.getSlots())
				|| !input.containsVars(expectedValue.getSlots())) {
			return false;
		}

		String filledVar = variable.fillSlots(input).getRawString();
		Value filledValue = ValueFactory.create(expectedValue.fillSlots(input).getRawString());


		Value actualValue = input.getValue(filledVar);
		switch (relation) {	
		case EQUAL: return actualValue.equals(filledValue);
		case UNEQUAL: return !actualValue.equals(filledValue); 
		case GREATER_THAN: return (actualValue.compareTo(filledValue) > 0); 
		case LOWER_THAN: return (actualValue.compareTo(filledValue) < 0);
		case CONTAINS: return actualValue.contains(filledValue); 
		case NOT_CONTAINS: return !actualValue.contains(filledValue); 
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
	public ValueRange getGroundings(Assignment input) {	

		ValueRange groundings = new ValueRange();

		if (variable.fillSlots(input).isUnderspecified()) {
			for (String inputVar : input.getVariables()) {
				MatchResult m = variable.match(inputVar, false);
				if (m.isMatching()) {
					groundings.addAssign(m.getFilledSlots());
					Assignment newInput = new Assignment(input, m.getFilledSlots());
					groundings.addRange(getGroundings(newInput));
				}
			}
			return groundings;
		}

		Template expectedValue2 = expectedValue.fillSlots(input);
		if (expectedValue2.isUnderspecified()) {

				String filledVar = variable.fillSlots(input).getRawString();
				Value actualValue = input.getValue(filledVar);

				if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
					MatchResult m = expectedValue2.match(actualValue.toString(), true);
					if (m.isMatching()) {
						Assignment possGrounding = m.getFilledSlots().removeValues
								(ValueFactory.none()).getTrimmedInverse(input.getVariables());
						groundings.addAssign(possGrounding);
					}
				}
				else if (relation == Relation.CONTAINS && actualValue instanceof SetVal) {
					for (Value subval : ((SetVal)actualValue).getSet()) {
						MatchResult m2 = expectedValue2.match(subval.toString(), true);
						Assignment possGrounding = m2.getFilledSlots().removeValues
								(ValueFactory.none()).getTrimmedInverse(input.getVariables());
						groundings.addAssign(possGrounding);
					}
				}
				else if (relation == Relation.CONTAINS && actualValue instanceof StringVal) {
					MatchResult m2 = expectedValue2.match(actualValue.toString(), false);
					Assignment possGrounding = m2.getFilledSlots().removeValues
							(ValueFactory.none()).getTrimmedInverse(input.getVariables());
					groundings.addAssign(possGrounding);
				}
		}
		return groundings;
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
