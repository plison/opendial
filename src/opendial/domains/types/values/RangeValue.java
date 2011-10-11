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

package opendial.domains.types.values;

import opendial.arch.DialConstants.PrimitiveType;
import opendial.utils.Logger;


/**
 * Representation of a type value encoded as a "range" (such as "string",
 * "integer", "float" or "boolean").
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RangeValue implements Value {

	// logger
	 static Logger log = new Logger("RangeValue", Logger.Level.DEBUG);
	
	 // the range (can be one of the 4 declared primitive types)
	PrimitiveType range;
	
	/**
	 * Creates a new range value, with the given range
	 * 
	 * @param range the range
	 */
	public RangeValue(PrimitiveType range) {
		this.range = range;
	}
	
	
	/**
	 * Returns the range for the value
	 * 
	 * @return the range
	 */
	public PrimitiveType getRange() {
		return range;
	}
	
	
	/**
	 * Returns true is the value provided as parameter falls within 
	 * the specified range
	 *
	 * @param val the value to check
	 * @return true if the value falls within the range, false otherwise
	 */
	public boolean containsValue(Object val) {

		if (range.equals(PrimitiveType.STRING)) {
			return (val instanceof String);
		}
		else if (range.equals(PrimitiveType.INTEGER)) {
			if (val instanceof Integer) {
				return true;
			}
			else if (val instanceof String){
				try {
					Integer.parseInt((String)val);
					return true;
				}
				catch (NumberFormatException e) { return false; }	
			}
		}
		else if (range.equals(PrimitiveType.BOOLEAN)) {
			if (val instanceof Integer) {
				return true;
			}
			else if (val instanceof String){
				return (((String)val).toLowerCase().equals("true") ||
						((String)val).toLowerCase().equals("false"));
			}

		}
		else if (range.equals(PrimitiveType.FLOAT)) {
			if (val instanceof Float) {
				return true;
			}
			else if (val instanceof String){
				try {
					Float.parseFloat((String)val);
					return true;
				}
				catch (NumberFormatException e) { return false; }	
			}
		}
		return false;
	}
	


	/**
	 * Returns a string representation of the range value
	 *
	 * @return the string representation
	 */
	public String toString() {
		if (range.equals(PrimitiveType.STRING)) {
			return "string";
		}
		else if (range.equals(PrimitiveType.INTEGER)) {
			return "integer";
		}
		else if (range.equals(PrimitiveType.BOOLEAN)) {
			return "boolean";
		}
		else if (range.equals(PrimitiveType.FLOAT)) {
			return "float";
		}
		return "";
	}

}
