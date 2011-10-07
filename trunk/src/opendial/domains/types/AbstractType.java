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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class AbstractType {

	
	static Logger log = new Logger("AbstractType", Logger.Level.NORMAL);

	
	String name;
	
	// list of values for the entity
	Map<String,Value> values;
	
	// list of features for the entity
	Map<String,FeatureType> features;
	
	/**
	 * @param name
	 */
	public AbstractType(String name) {
		this.name = name;
		values = new HashMap<String,Value>();
		
		features = new HashMap<String,FeatureType>();
	}


	public String getName(){
		return name;
	}
	/**
	 * Add a new value to the entity
	 * 
	 * @param value the new value to add
	 */
	protected void internalAddValue(Value value) {
		values.put(value.getLabel(), value);
	}
	
	/**
	 * Add a list of values to the entity
	 * (if the value already exists, it is overwritten)
	 * 
	 * @param actionValues the list of values to add
	 */
	protected void internalAddValues(List<Value> values2) {
		for (Value value: values2) {
			internalAddValue(value);
		}
	}
	
	
	
	public void addFeature(FeatureType feature) {
		features.put(feature.getName(), feature);
	}
	
	/**
	 * Add a list of features to the entity (if a feature 
	 * with identical name already exists, it is overwritten)
	 * 
	 * @param features the list of features to add
	 */
	public void addFeatures(List<FeatureType> features) {
		for (FeatureType f : features) {
			String name = f.getName();
			if (this.features.containsKey(name)) {
				log.warning("feature name " + name + " already in entity " + name);
			}
			this.features.put(f.getName(), f);
		}
	}
	
	
	
	// ===================================
	//  GETTERS
	// ===================================

	
	/**
	 * Returns true if the entity contains the given value,
	 * false otherwise

	 * @return true if value present, false otherwise
	 */
	public boolean acceptsValue(String value) {
		if (values.containsKey(value)) {
			return true;
		}
		else {
			for (Value v : values.values()) {
				if (v instanceof RangeValue) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns the list of values contained in the entity
	 * 
	 * @return the list of values
	 */
	public List<Value> getValues() {
		return new ArrayList<Value>(values.values());
	}
	
	
	/**
	 * Returns true if the entity contains the given feature,
	 * false otherwise
	 * 
	 * @param featureName the feature name
	 * @return true if feature present, false otherwise
	 */
	public boolean hasFeature(String featureName) {
		return features.containsKey(featureName);
	}
	
	/**
	 * Returns true if the entity contains the given feature,
	 * false otherwise
	 * 
	 * @param featureName the feature name
	 * @return true if feature present, false otherwise
	 */
	public boolean hasFeatureForBaseValue(String featureName, String baseValue) {
		if (features.containsKey(featureName)) {
			FeatureType feat = features.get(featureName);
			return feat.isDefinedForBaseValue(baseValue);
		}
		return false;
	}
	
	/**
	 * Gets the feature associated with the name, if any
	 * is present. Else, return null.  
	 * 
	 * @param featureName the feature name
	 * @return the entity if the feature is present, or null otherwise.
	 */
	public FeatureType getFeature(String featureName) {
		
		if (features.containsKey(featureName)) {
			return features.get(featureName);
		}
		return null;
	}

	 public List<FeatureType> getPartialFeatures(String baseValue) {
		 List<FeatureType> partialFeatures = new LinkedList<FeatureType>();
		 for (FeatureType feat: features.values()) {
			 if (feat.isDefinedForBaseValue(baseValue)) {
				 partialFeatures.add(feat);
			 }
		 }
		 return partialFeatures;
	 }
	 
		/**
		 * 
		 * @return
		 */
		public List<FeatureType> getFullFeatures() {
			 List<FeatureType> fullFeatures = new LinkedList<FeatureType>();
			 for (FeatureType feat: features.values()) {
				 if (!feat.isPartial()) {
					 fullFeatures.add(feat);
				 }
			 }
			 return fullFeatures;
		}

	/**
	 * 
	 * @return
	 */
	public List<FeatureType> getFeatures() {
		return new ArrayList<FeatureType>(features.values());
	}
}
