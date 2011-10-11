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

import opendial.domains.types.FeatureType;

/**
 * Representation of a feature variable.  It is defined as an extension of the
 * standard variable, and as such, contains both an identifier and a type.  
 * 
 * <p>In addition, a feature variable must also define a "base variable", which is the
 * variable on which the feature is attached (i.e. the variable it is a feature of).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FeatureVariable extends StandardVariable {

	// the base variable for the feature
	StandardVariable baseVariable;
	
	
	/**
	 * Creates a new feature variable, defined by an identifier, a type and a base variable
	 * 
	 * @param identifier the identifier
	 * @param type the type
	 * @param baseVariable the base variable
	 */
	public FeatureVariable(String identifier, FeatureType type, StandardVariable baseVariable) {
		super(identifier, type);
		this.baseVariable = baseVariable;
	}
	
	
	/**
	 * Returns the base variable for the feature
	 * 
	 * @return the base variable
	 */
	public StandardVariable getBaseVariable() {
		return baseVariable;
	}
	
	
	/**
	 * Returns a string representation of the variable
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = identifier + "=" + type.getName() + "(" + baseVariable + ")";
		return str;
	}

}
