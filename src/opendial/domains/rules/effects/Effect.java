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

package opendial.domains.rules.effects;

import java.util.Set;

import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.Template;

/**
 * Generic representation of an effect. An effect is used to create an output
 * object, which can be parametrised using additional input variables.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Effect {
	
	/**
	 * Returns the additional input variables for the effect (can be an empty set)
	 * 
	 * @return the labels for the input variables
	 */
	public Set<String> getAdditionalInputVariables();
	
	/**
	 * Returns the output variables that the effect is based on.
	 * 
	 * @return the output variables
	 */
	public Set<Template> getOutputVariables();
	
	
	/**
	 * Returns the output, parametrised with some (possibly empty) input.
	 * 
	 * @param additionalInput the additional input assignment
	 * @return the corresponding output for the effect
	 */
	public Output createOutput(Assignment additionalInput);
	
}
