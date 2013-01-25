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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.quantification.UnboundPredicate;


/**
 * Negated condition, which is satisfied when the included condition is not.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NegatedCondition implements Condition {

	// logger
	public static Logger log = new Logger("NegatedCondition", Logger.Level.NORMAL);
	
	// condition to negate
	Condition initCondition;
	
	

	// ===================================
	//  CONDITION CONSTRUCTIOn
	// ===================================

	
	/**
	 * Creates a new negated condition with the condition provided as argument
	 * 
	 * @param initCondition the condition to negate
	 */
	public NegatedCondition (Condition initCondition) {
		this.initCondition = initCondition;
	}


	// ===================================
	//  GETTERS
	// ===================================

	
	/**
	 * Returns the input variables for the condition (which are the same as
	 * the ones for the condition to negate)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<TemplateString> getInputVariables() {
		return initCondition.getInputVariables();
	}

	/**
	 * Returns the set of unbound predicates for the basic condition, which could
	 * be either associated with the variable label or its content.
	 * 
	 * @return the set of unbound predicates
	 */
	@Override
	public Set<UnboundPredicate> getUnboundPredicates() {
		return initCondition.getUnboundPredicates();
	}

	
	/**
	 * Returns true if the condition to negate is *not* satisfied, and 
	 * false if it is satisfied
	 * 
	 * @param input the input assignment to verify
	 * @return true if the included condition is false, and vice versa
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		return !initCondition.isSatisfiedBy(input);
	}

	/**
	 * Returns the condition to negate
	 * 
	 * @return the condition to negate
	 */
	public Condition getInitCondition() {
		return initCondition;
	}
	
	
	/**
	 * Returns the local output for the condition, if any
	 * 
	 * @param input the input assignment in which to extract the local output
	 * @return the extracted local output
	 */
	@Override
	public Assignment getLocalOutput(Assignment input) {
		return initCondition.getLocalOutput(input);
	}
	

	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================

	
	/**
	 * Returns the hashcode for the condition
	 * 
	 * @return the hashcode
	 */
	public int hashCode() {
		return - initCondition.hashCode();
	}
	
	/**
	 * Returns the string representation of the condition
	 *
	 * @return the string representation
	 */
	public String toString() {
		return "!"+ initCondition.toString();
	}
	
	
	/**
	 * Returns true if the current instance and the object are identical,
	 * and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof NegatedCondition) {
			return ((NegatedCondition)o).getInitCondition().equals(initCondition);
		}
		return false;
	}
}
