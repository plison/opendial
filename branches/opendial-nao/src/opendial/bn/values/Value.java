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

package opendial.bn.values;

/**
 * Generic interface for a variable value, that can be copied.
 * 
 * <p>The classes implementing this interface should also 
 * implement the hashcode(), equals() and toString() methods
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Value extends Comparable<Value> {
	
	/**
	 * Copies the value
	 * 
	 * @return the value
	 */
	public abstract Value copy();

	
	/**
	 * Return true if the value contains the value given as argument
	 * 
	 * @param subvalue the value to check for inclusion
	 * @return true if the value is contained, false otherwise
	 */
	public abstract boolean contains(Value subvalue);
	
}
