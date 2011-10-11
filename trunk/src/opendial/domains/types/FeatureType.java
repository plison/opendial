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

package opendial.domains.types;

import java.util.LinkedList;
import java.util.List;

import opendial.utils.Logger;

/**
 * Representation of a feature type.  It extends the generic type class,
 * adding the definition of <i>base values</i>, that is: values from the 
 * including type for which the feature is defined.
 * 
 * <p>The use of base values is motivated by the wish to express <i>partial
 * features</i>: features which are only defined for a subset of the type
 * values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FeatureType extends GenericType {

	// logger
	static Logger log = new Logger("FeatureType", Logger.Level.NORMAL);
	
	// base values (from top type) in case the feature is partial
	// NB: by convention, an empty list signifies that the feature is full
	List<String> baseValues;		
	
	/**
	 * Creates a new feature type, with the given name
	 * 
	 * @param name
	 */
	public FeatureType(String name) {
		super(name);
		baseValues = new LinkedList<String>();
	}
	
	/**
	 * Adds a base value to the type
	 * 
	 * @param baseVal the base value
	 */
	public void addBaseValue(String baseVal) {
		baseValues.add(baseVal);
	}
	
	/**
	 * Adds a list of base values to the type
	 * 
	 * @param baseVals the base values
	 */
	public void addBaseValues(List<String> baseVals) {
		baseValues.addAll(baseVals);
	}
	
	
	/**
	 * Returns true if the feature is defined for the given
	 * base value, false otherwise
	 * 
	 * @param baseVal the base value to check
	 * @return true is feature is defined, false otherwise
	 */
	public boolean isDefinedForBaseValue(String baseVal) {
		if (baseValues.isEmpty()) {
			return true;
		}
		else if (baseValues.contains(baseVal)) {
			return true;
		}
		return false;
	}

	
	/**
	 * Returns true if the feature is partially defined, and false
	 * otherwise.
	 * 
	 * @return true if partial feature, false otherwise
	 */
	public boolean isPartial() {
		return (!baseValues.isEmpty());
	}

	/**
	 * Returns the list of base values for the type
	 * 
	 * @return the base values
	 */
	public List<String> getBaseValues() {
		return baseValues;
	}
}
