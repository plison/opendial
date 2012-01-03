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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.arch.DialConstants;
import opendial.arch.DialConstants.BinaryOperator;
import opendial.domains.rules.variables.Variable;
import opendial.inference.bn.Assignment;
import opendial.utils.Logger;

/**
 * Representation of a complex condition, made of several sub-conditions
 * connected by a logical operator (AND or OR)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexCondition implements Condition {

	// logger
	static Logger log = new Logger("ComplexCondition", Logger.Level.DEBUG);
	
	// the list of sub-conditions
	List<Condition> subconditions;

	// the binary logical operator
	BinaryOperator binaryOp = BinaryOperator.AND;
	
	
	/**
	 * Creates a new complex condition, with an empty list of sub-conditions,
	 * connected by AND
	 */
	public ComplexCondition() {
		subconditions = new LinkedList<Condition>();
	}
	
	/**
	 * Creates a new complex condition, with an empty list of sub-conditions,
	 * connected by the given operator
	 * 
	 * @param binaryOp the binary operator
	 */
	public ComplexCondition(BinaryOperator binaryOp) {
		this();
		setOperator(binaryOp);
	}
	
	/**
	 * Sets the logical operator for the complex condition
	 * 
	 * @param binaryOp the logical operator
	 */
	public void setOperator(BinaryOperator binaryOp) {
		this.binaryOp = binaryOp;
	}
	
	
	/**
	 * Adds a sub-condition to the current list of sub-conditions
	 * 
	 * @param subcondition the sub-condition to add
	 */
	public void addSubcondition(Condition subcondition) {
		subconditions.add(subcondition);
	}

	
	/**
	 * Adds a list of sub-conditions to the current list
	 * 
	 * @param subconditions the sub-conditions to add
	 */
	public void addSubconditions(List<Condition> subconditions) {
		this.subconditions.addAll(subconditions);
	}

	/**
	 * Returns the current list of sub-conditions
	 * 
	 * @return the sub-conditions
	 */
	public List<Condition> getSubconditions() {
		return subconditions;
	}

	
	/**
	 * Returns the logical operator for the complex condition
	 * 
	 * @return the logical operator
	 */
	public BinaryOperator getBinaryOperator() {
		return binaryOp;
	}

	
	/**
	 * Returns true is the condition is satisfied by the following
	 * assignment, and false otherwise
	 * 
	 * @param input the assignment to check
	 * @return true if the condition is satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input, Map<Variable,String> anchors) {
		if (binaryOp.equals(BinaryOperator.AND)) {
			for (Condition subcondition : subconditions) {
				if (!subcondition.isSatisfiedBy(input, anchors)) {
					return false;
				}
			}
			return true;
		}
		else if (binaryOp.equals(BinaryOperator.OR)) {
	//		log.debug("number of subconditions: " + subconditions);
			for (Condition subcondition: subconditions) {
	//			log.debug("is subcond : "+ subcondition+ " satisfied by input " + input + "?" + subcondition.isSatisfiedBy(input));
				if (subcondition.isSatisfiedBy(input, anchors)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	
	
	
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		if (binaryOp.equals(BinaryOperator.AND)) {
			for (Condition subcondition : subconditions) {
				if (!subcondition.isSatisfiedBy(input)) {
					return false;
				}
			}
			return true;
		}
		else if (binaryOp.equals(BinaryOperator.OR)) {
	//		log.debug("number of subconditions: " + subconditions);
			for (Condition subcondition: subconditions) {
	//			log.debug("is subcond : "+ subcondition+ " satisfied by input " + input + "?" + subcondition.isSatisfiedBy(input));
				if (subcondition.isSatisfiedBy(input)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	
	
	@Override
	public String toString() {
		String str = "";
		for (Condition subcondition : subconditions) {
			str += subcondition + DialConstants.toString(binaryOp);
		}
		return str.substring(0, str.length() - 3);
	}
}
