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

package opendial.domains.rules.variables;


/**
 * Representation of a pointer variable, i.e. a variable which is only
 * a pointer/reference for another variable already defined.
 * 
 * Pointer variables are primarily used to define output variables which
 * are already specified as input variables.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PointerVariable extends StandardVariable {

	// the variable which is pointed at
	StandardVariable targetVariable ;
	
	
	/**
	 * Creates a new pointer variable, based on the provided target variable
	 * (i.e. the variable for which the pointer is a reference)
	 * 
	 * @param targetVariable the variable which is pointed at
	 */
	public PointerVariable(StandardVariable targetVariable) {
		super(targetVariable.getIdentifier(), targetVariable.getType());
		this.targetVariable = targetVariable;
	}
	
	
	/**
	 * Returns the target variable
	 * 
	 * @return the target variable
	 */
	public StandardVariable getTarget() {
		return targetVariable;
	}
	

}
