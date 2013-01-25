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

package opendial.domains.rules.conditions.checks;


import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.Value;

public abstract class AbstractCheck {

	// logger
	public static Logger log = new Logger("Check", Logger.Level.NORMAL);
	
	/**
	 * Returns whether the check is satisfied with the given value
	 * 
	 * @param input
	 * @return
	 */
	public abstract boolean isSatisfied (Assignment input);

	/**
	 * Returns the (optional) local output produced by the check 
	 * given the value.
	 * 
	 * <p>Example: if the condition is "u_m matches "take the ${X}"
	 * and u_m = "take the box", the condition will produce a local
	 * output "X=box".
	 * 
	 * @param input
	 * @return the local output for the condition
	 */
	public Assignment getLocalOutput(Assignment input) {
		return new Assignment();
	}

}

