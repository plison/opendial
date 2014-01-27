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

package opendial.bn.distribs.discrete.functions;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Represents a deterministic function of its input.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public interface DeterministicFunction {

	/**
	 * Returns the unique value corresponding to the input assignment
	 * 
	 * @param input the input assignment
	 * @return the corresponding output
	 */
	public Value getValue(Assignment input);
	
	/**
	 * Returns a copy of the function
	 * 
	 * @return the copy
	 */
	public DeterministicFunction copy();

	/**
	 * Modify all occurrences of the old variable identifier oldId 
	 * by newId
	 * 
	 * @param oldId the old identifier
	 * @param newId the new identifier
	 */
	public void modifyVarId(String oldId, String newId);
}

