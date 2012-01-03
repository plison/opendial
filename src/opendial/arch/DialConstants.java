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
 * Placeholder for generic constants and enumerations used in openDial.
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
	
	public static String toString(BinaryOperator binaryOp) {
		if (binaryOp.equals(BinaryOperator.AND)) {
			return " ^ ";
		}
		else if (binaryOp.equals(BinaryOperator.OR)) {
			return " v ";
		}
		return "";
	}
	
	/**
	 * Equality/inequality relations (used in conditions)
	 */
	public static enum Relation {EQUAL, UNEQUAL}

	public static String toString(Relation rel) {
		if (rel.equals(Relation.EQUAL)) {
			return "=";
		}
		else if (rel.equals(Relation.UNEQUAL)) {
			return "!=";
		}
		return "";
	}

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
	 * String representation of the model groups
	 * 
	 * @param mg the model group
	 * @return the string representation
	 */
	public static String toString(ModelGroup mg) {
		if (mg == ModelGroup.USER_REALISATION) {
			return "userRealisation"; 
		}
		else if (mg == ModelGroup.USER_PREDICTION) {
			return "userPrediction";
		}
		else if (mg == ModelGroup.USER_TRANSITION) {
			return "userPrediction";
		}
		else if (mg == ModelGroup.SYSTEM_ACTIONVALUE) {
			return "systemActionValue";
		}
		else if (mg == ModelGroup.SYSTEM_REALISATION) {
			return "systemRealisation";
		}
		else if (mg == ModelGroup.SYSTEM_TRANSITION) {
			return "systemTransition";
		}
		return "";
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
	
	/**
	 * Maximum path length when processing a Bayesn Network
	 */
	public static final int MAX_PATH_LENGTH = 100;

	
}
