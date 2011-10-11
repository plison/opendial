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

import opendial.domains.types.GenericType;

/**
 * Standard representation for a variable defined as input or output
 * inside a rule.  A variable is primarily defined by its type (e.g. "intent"),
 * and its identifier (as used in the rule)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StandardVariable {

	// static Logger log = new Logger("Variable", Logger.Level.NORMAL);

	// the identifier for the variable
	String identifier;

	// the type of the variable
	GenericType type;

	/**
	 * Creates a new standard variable, based on the given type.  The default
	 * variable identifier will be the type name
	 * 
	 * @param type the variable type
	 */
	public StandardVariable (GenericType type) {
		this.identifier = type.getName();
		this.type = type;
	}

	/**
	 * Creates a new standard variable, based on the given type and identifier
	 * 
	 * @param identifier the identifier for the variable
	 * @param type the variable type
	 */
	public StandardVariable(String identifier, GenericType type) {
		this.identifier = identifier;
		this.type = type;
	}


	/**
	 * Returns the identifier for the variable
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	
	/**
	 * Sets the identifier of the variable
	 * 
	 * @param id the identifier
	 */
	public void setIdentifier(String id) {
		this.identifier = id;
	}

	/**
	 * Sets the type of the variable
	 * 
	 * @param type the type
	 */
	public void setType(GenericType type) {
		this.type = type;
	}

	
	/**
	 * Returns the type of the variable
	 * 
	 * @return the variable type
	 */
	public GenericType getType() {
		return type;
	}


	/**
	 * Returns a string representation of the variable
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return identifier + " (" + type.getName() + ")";
	}


}
