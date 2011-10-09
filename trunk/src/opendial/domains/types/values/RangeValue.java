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


/**
 * TODO: implement range value correctly!
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RangeValue extends Value {

	// static Logger log = new Logger("RangeValue", Logger.Level.NORMAL);
	
	PrimitiveType range;
	
	public RangeValue(PrimitiveType range) {
		this.range = range;
	}
	
	public PrimitiveType getRange() {
		return range;
	}
	
	
	public boolean acceptsValue(Object val) {
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
				if (((String)val).equals("true") || ((String)val).equals("false")) {
					return true;
				}
				else {
					return false;
				}
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


}
