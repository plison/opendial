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

import java.util.Set;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;


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
	public Set<Template> getInputVariables() {
		return initCondition.getInputVariables();
	}

	@Override
	public ValueRange getGroundings(Assignment input) {
		return initCondition.getGroundings(input);
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
	

	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================

	
	/**
	 * Returns the hashcode for the condition
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return - initCondition.hashCode();
	}
	
	/**
	 * Returns the string representation of the condition
	 */
	@Override
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
	@Override
	public boolean equals(Object o) {
		if (o instanceof NegatedCondition) {
			return ((NegatedCondition)o).getInitCondition().equals(initCondition);
		}
		return false;
	}
}
