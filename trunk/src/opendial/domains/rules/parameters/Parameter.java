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

package opendial.domains.rules.parameters;

import java.util.Collection;
import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.Assignment;



/**
 * Interface for a parameter associated with an effect
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Parameter {

	
	/**
	 * Returns the actual parameter value given the inputs provided as arguments.
	 * If the actual value cannot be retrieved (missing information), throws 
	 * an exception.
	 * 
	 * @param input the input assignment
	 * @return the actual parameter value
	 * @throws DialException if the value cannot be retrieved
	 */
	public double getParameterValue (Assignment input) throws DialException;
	
	
	/**
	 * Returns the (possibly empty) set of parameter identifiers used in the 
	 * parameter object.
	 * 
	 * @return the collection of parameter labels 
	 */
	public Collection<String> getParameterIds();

	
	/**
	 * Copies the parameter
	 * 
	 * @return the copy
	 */
	public Parameter copy();
}
