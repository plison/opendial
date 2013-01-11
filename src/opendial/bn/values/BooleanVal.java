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

package opendial.bn.values;


/**
 * Representation of a boolean value.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
 public final class BooleanVal implements Value {
	
	 // the boolean
	boolean b;
	
	/**
	 * Creates the boolean value
	 * (protected, use the ValueFactory to create it)
	 * 
	 * @param b the boolean
	 */
	protected BooleanVal(boolean b) { this.b = b; };
	
	/**
	 * Returns the hashcode of the boolean
	 *
	 * @return hashcode
	 */
	@Override
	public int hashCode() { return new Boolean(b).hashCode(); }
	
	/**
	 * Returns true if the boolean value is similar, false otherwise
	 *
	 * @param o the value to compare
	 * @return true if similar, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return (o instanceof BooleanVal && ((BooleanVal)o).getBoolean() == getBoolean());
	}
	
	/**
	 * Returns the boolean value 
	 * 
	 * @return
	 */
	public boolean getBoolean() {return b; }
	
	/**
	 * Copies the boolean value
	 *
	 * @return
	 */
	@Override
	public BooleanVal copy() { return new BooleanVal(b); }
	
	/**
	 * Returns a string representation of the boolean value
	 *
	 * @return
	 */
	@Override
	public String toString() { return ""+b; }

	 
	/**
	 * Compares the boolean to another value
	 * 
	 * @return usual ordering, or hashcode difference if the value is not a boolean
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof BooleanVal) {
			return (new Boolean(b)).compareTo(((BooleanVal)o).getBoolean());
		}
		else {
			return hashCode() - o.hashCode();
		}
	}
	
}