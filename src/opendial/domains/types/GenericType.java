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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.Value;
import opendial.utils.Logger;

/**
 * Representation of a generic variable type.  A variable type is constituted
 * by a name, a (possibly empty) set of values, and a (possibly empty) set of features.
 * 
 * <p>In addition, a type can denote either a fixed variable (representing a fluent
 * which remains present and fixed in the dialogue state), or an entity (which can
 * possess 0...n instances in the dialogue state).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GenericType {

	// logger
	static Logger log = new Logger("GenericType", Logger.Level.DEBUG);

	// the type name
	String name;

	// whether the type is fixed or variable
	boolean isFixed = false;


	// list of values for the entity
	Set<Value> values;

	// list of full features for the entity
	Map<String,FeatureType> features;

	// ===================================
	//  TYPE CONSTRUCTION METHODS
	// ===================================


	/**
	 * Creates a new generic type, with the given name
	 * 
	 * @param name the type name
	 */
	public GenericType(String name) {
		this.name = name;
		values = new HashSet<Value>();

		features = new HashMap<String,FeatureType>();
	}


	/**
	 * Adds a new value to the type
	 * 
	 * @param value the new value to add
	 */
	public void addValue(Value value) {
		values.add(value);
	}


	/**
	 * Adds a list of values to the type
	 * (if the value already exists, it is overwritten)
	 * 
	 * @param values the list of values to add
	 */
	public void addValues(Collection<? extends Value> values) {
		this.values.addAll(values);
	}



	/**
	 * Adds a new feature to the type (if the feature with identical 
	 * name already exists, it is overwritten)
	 * 
	 * @param feature the feature to add
	 */
	public void addFeature(FeatureType feature) {
		features.put(feature.getName(), feature);
	}


	/**
	 * Add a list of features to the type (if a feature 
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

	/**
	 * Sets the type as fixed or not
	 * 
	 * @param isFixed whether the type is fixed or not
	 */
	public void setAsFixed(boolean isFixed) {
		this.isFixed = isFixed;
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the type name
	 * 
	 * @return the type name
	 */
	public String getName(){
		return name;
	}


	/** 
	 * Returns true if the type contains the given value,
	 * false otherwise

	 * @return true if value present, false otherwise
	 */
	public boolean containsValue(Object value) {
		for (Value val : values) {
			if (val.containsValue(value)){
				return true;
			}
		}
		return false;
	}



	/**
	 * Returns the value denoted by the label, if it exists in the type.
	 * Else, returns null.
	 * 
	 * @param valueLabel the label for the value
	 * @return the value
	 */
	public BasicValue<?> getValue(Object valueLabel) {
		for (Value val: values) {
			if (val instanceof BasicValue && val.containsValue(valueLabel)) {
				return (BasicValue<?>)val;
			}
		}
		return null;
	}


	/**
	 * Returns the list of values contained in the type
	 * 
	 * @return the list of values
	 */
	public List<Value> getAllValues() {
		return new ArrayList<Value>(values);
	}


	/**
	 * Returns the list of basic values contained in the type
	 * (e.g. excluding all range values)
	 * 
	 * @return the list of values
	 */
	public List<BasicValue<?>> getBasicValues() {
		List<BasicValue<?>> basicValues = new LinkedList<BasicValue<?>>();
		for (Value v: values) {
			if (v instanceof BasicValue) {
				basicValues.add((BasicValue<?>)v);
			}
		}
		return basicValues;
	}



	/**
	 * Returns true if the type contains the given feature,
	 * false otherwise
	 * 
	 * @param featureName the feature name
	 * @return true if feature present, false otherwise
	 */
	public boolean hasFeature(String featureName) {
		return features.containsKey(featureName);
	}

	/**
	 * Returns true if the type contains the a (full or partial) feature
	 * of the given name, for the specific base value
	 * 
	 * @param featureName the feature name
	 * @param baseValue the type value
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
	 * Gets the feature associated with the name, if any is present. 
	 * Else, return null.  
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


	/**
	 * Returns the list of partial features defined for the base value
	 * 
	 * @param baseValue the base value
	 * @return the partial features defined
	 */ 
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
	 * Returns the list of full features for the type
	 * 
	 * @return the list of full features
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
	 * Returns the list of (full or partial) features
	 * 
	 * @return the list of all features
	 */
	public List<FeatureType> getFeatures() {
		return new ArrayList<FeatureType>(features.values());
	}


	/**
	 * Returns true if the type is fixed, false otherwise
	 * 
	 * @return true if type is fixed, false otherwise
	 */
	public boolean isFixed() {
		return isFixed;
	}
}
