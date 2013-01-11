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

/**
 * Representation of a void condition, which is always true.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VoidCondition implements Condition {

	// logger
	static Logger log = new Logger("VoidCondition", Logger.Level.NORMAL);

	/**
	 * Return an empty set 
	 * 
	 * @return an empty set
	 */
	@Override
	public Set<TemplateString> getInputVariables() {
		return new HashSet<TemplateString>();
	}
	
	/**
	 * Returns an empty set
	 *
	 * @return an empty set
	 */
	@Override
	public Set<String> getLocalOutputVariables() {
		return new HashSet<String>();
	}


	/**
	 * Returns true (condition is always trivially satisfied)
	 *
	 * @param input the input assignment (ignored)
	 * @return true
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		return true;
	}
	
	/**
	 * Returns an empty assignment (no local output)
	 *
	 * @param input the input assignment (ignored)
	 * @return an empty assignment
	 */
	@Override
	public Assignment getLocalOutput(Assignment input) {
		return new Assignment();
	}
	
	/**
	 * Returns the string "true" indicating that the condition is
	 * always trivially true
	 *
	 * @return true
	 */
	@Override
	public String toString() {
		return "true";
	}
	
	/**
	 * Returns a constant representing the hashcode for the void condition
	 *
	 * @return 36
	 */
	@Override
	public int hashCode() {
		return 36;
	}
	
	/**
	 * Returns true if o is also a void condition
	 *
	 * @param o the object to compare
	 * @return true if o is also a void condition
	 */
	public boolean equals(Object o) {
		return (o instanceof VoidCondition);
	}
}
