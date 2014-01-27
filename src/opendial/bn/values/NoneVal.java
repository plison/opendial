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
 * "None" value (describing the lack of value, or an empty assignment)
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public final class NoneVal implements Value {
	
	/**
	 * Creates the none value 
	 * (protected, use the value factory)
	 * 
	 */
	protected NoneVal() { }
	
	/**
	 * Returns a hashcode for the value
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return 346; }
	
	/**
	 * Returns true if both values are none
	 *
	 * @param o the object
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return (o instanceof NoneVal);
	}
		
	
	/**
	 * Returns its own instance
	 *
	 * @return its own instance
	 */
	@Override
	public NoneVal copy() { return this; }
	
	
	/**
	 * Returns the string "None
	 *
	 * @return the string
	 */
	@Override
	public String toString() { return "None"; }
	
	
	/**
	 * Compares the none value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	
	
	/**
	 * True if subvalue is contained in the current instance, and false
	 * otherwise
	 * 
	 * @param subvalue the possibly contained value
	 * @return true if contained, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}
	
}