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

package opendial.domains.values;



/**
 * Representation of a basic value for a variable type.  The class of the
 * value is parametrised with T.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicValue<T> implements Value {
	
	// the value
	T value;
		
	/**
	 * Creates a new basic value
	 * 
	 * @param value the value
	 */
	public BasicValue(T value) {
		this.value = value;
	}
	
	/**
	 * Returns the value
	 * 
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	
	/**
	 * Returns true if the object is equivalent to the value,
	 * and false otherwise
	 *
	 * @param val the object to compare
	 * @return true if the value is equivalent, false otherwise
	 */
	@Override
	public boolean containsValue(Object val) {
		return (val.equals(value));
	}
	
	
	/**
	 * Returns a string representation of the value
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return value.toString();
	}
	
	
	@Override 
	public boolean equals (Object o) {
		if (!(o instanceof BasicValue)) {
			return false;
		}
		else {
			return ((BasicValue<?>)o).getValue().equals(value);
		}
	}
}
