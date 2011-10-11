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

package opendial.arch;

/**
 * Placeholder for generic constants and enumerations used in openDial
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */ 
public class DialConstants {
	
	/**
	 * Binary logical operators (used to express combination of several
	 * conditions or effects)
	 */
	public static enum BinaryOperator {AND, OR}
	
	
	/**
	 * Equality/inequality relations (used in conditions)
	 */
	public static enum Relation {EQUAL, UNEQUAL}

	
	/**
	 * Enumeration of possible model types
	 */
	public static enum ModelGroup {
		USER_REALISATION,
		USER_PREDICTION,
		USER_TRANSITION,
		SYSTEM_ACTIONVALUE,
		SYSTEM_REALISATION,
		SYSTEM_TRANSITION
	}
	
	
	/**
	 * Primitive data types
	 */
	public static enum PrimitiveType {
		BOOLEAN,
		INTEGER,
		STRING,
		FLOAT
	}
	
}
