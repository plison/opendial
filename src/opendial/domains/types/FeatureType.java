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
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FeatureType extends GenericType {

	static Logger log = new Logger("FeatureType", Logger.Level.NORMAL);

	boolean isPartial = false;
	List<String> baseValues;		// base values (from top type) in case the feature is partial
	
	/**
	 * @param name
	 */
	public FeatureType(String name) {
		super(name);
		baseValues = new LinkedList<String>();
	}
	
	
	public void addBaseValue(String baseVal) {
		isPartial = true;
		baseValues.add(baseVal);
	}
	
	public void addBaseValues(List<String> baseVals) {
		isPartial = true;
		baseValues.addAll(baseVals);
	}
	
	public boolean isDefinedForBaseValue(String baseVal) {
		if (!isPartial) {
			return true;
		}
		else if (baseValues.contains(baseVal)) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isPartial() {
		return isPartial;
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getBaseValues() {
		return baseValues;
	}
}
