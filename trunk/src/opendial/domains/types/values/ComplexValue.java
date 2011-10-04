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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.domains.types.FeatureType;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexValue extends BasicValue {

	Map<String,FeatureType> features;

	/**
	 * @param label
	 */
	public ComplexValue(String label) {
		super(label);
		features = new HashMap<String,FeatureType>();
	}

	static Logger log = new Logger("ComplexValue", Logger.Level.NORMAL);

	/**
	 * 
	 * @return
	 */
	public List<FeatureType> getFeatures() {
		return new ArrayList<FeatureType>(features.values());
	}

	/**
	 * 
	 * @param features2
	 */
	public void addFeatures(List<FeatureType> features2) {
		for (FeatureType feat : features2) {
			features.put(feat.getName(), feat);
		}
	}
}
