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

import opendial.arch.Logger;
import opendial.datastructs.Template;

/**
 * String value.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */

public final class StringVal implements Value {
	
	public static Logger log = new Logger("StringVal", Logger.Level.DEBUG);

	// the string
	String str;
	
	/**
	 * Creates a new string value
	 * (protected, use the ValueFactory instead)
	 * 
	 * @param str the string
	 */
	protected StringVal(String str) { 
		this.str = str.trim(); 
	//	StringUtils.checkForm(str); 
	};
	
	
	/**
	 * Returns the hashcode for the string
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return str.hashCode(); }
	
	
	/**
	 * Returns true if the strings are equals, false otherwise
	 *
	 * @param o the object to compare
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		if (o instanceof StringVal) {
			if (((StringVal)o).getString().equalsIgnoreCase(getString())) {
				return true;
			}
			else if (((StringVal)o).getString().contains("*")) {
				return new Template(((StringVal)o).getString()).match(str, true).isMatching();
			}
			else if (str.contains("*")) {
				return new Template(str).match(((StringVal)o).getString(), true).isMatching();
			}
		}
		return false;
	}
	
	/**
	 * Returns the string itself
	 * 
	 * @return
	 */
	public String getString() {return str; }
	
	/**
	 * Returns a copy of the string value
	 *
	 * @return the copy
	 */
	@Override
	public StringVal copy() { return new StringVal(str); }
	
	/**
	 * Returns the string itself
	 *
	 * @return the string
	 */
	@Override
	public String toString() { return str; }
	
	
	/**
	 * Compares the string value to another value
	 * 
	 * @return usual ordering, or hashcode if the value is not a string
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof StringVal) {
			return str.compareTo(((StringVal)o).getString());
		}
		else {
			return hashCode() - o.hashCode();
		}
	}


	/**
	 * Returns true if subvalue is a substring of the current StringVal, and
	 * false otherwise
	 * 
	 * @return true is subvalue is a substring of the object, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		if (subvalue instanceof StringVal) {
			return (new Template(((StringVal)subvalue).getString()).match(str,false)).isMatching();
		}
		return false;
	}
	
}
