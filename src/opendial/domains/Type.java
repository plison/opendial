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

package opendial.domains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.domains.values.BasicValue;
import opendial.domains.values.Value;
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
public class Type {

	// logger
	static Logger log = new Logger("Type", Logger.Level.NORMAL);

	// the type name
	String name;

	// whether the type is fixed or variable
	boolean isFixed = false;


	// list of values
	Set<Value> values;

	// list of features
	Map<String, Type> features;

	// base values for partially defined features
	Map<String, Value> baseValuesForFeats;
	
	
	// ===================================
	//  TYPE CONSTRUCTION METHODS
	// ===================================


	/**
	 * Creates a new generic type, with the given name
	 * 
	 * @param name the type name
	 */
	public Type(String name) {
		this.name = name;
		values = new HashSet<Value>();

		features = new HashMap<String, Type>();
		baseValuesForFeats = new HashMap<String,Value>();
	}
	
	public <T> Type(String name, Collection<T> values) {
		this(name);
		for (T val : values) {
			this.values.add(new BasicValue<T>(val));
		}
	}

	/**
	 * Adds a new value to the type
	 * 
	 * @param value the new value to add
	 */
	public <T> void addValue(T value) {
		values.add(new BasicValue<T>(value));
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
	public <T> void addValues(Collection<T> values) {
		for (T value : values) {
			if (value instanceof Value) {
				this.values.add((Value)value);
			}
			else {
			this.values.add(new BasicValue<T>(value));
			}
		}
	} 



	/**
	 * Adds a new full feature to the type (if the feature with identical 
	 * name already exists, it is overwritten).  A full feature is a feature
	 * defined for all values of the type
	 * 
	 * @param feature the feature to add
	 */
	public void addFullFeature(Type feature) {
		features.put(feature.getName(), feature);
		feature.setAsFixed(true);
	}
	
	
	
	/**
	 * Adds a new partial feature to the type, which is defined to be valid
	 * only for the given value
	 * 
	 * @param feature the feature to add
	 * @param baseValue the value for which it is value
	 */
	public <T> void addPartialFeature(Type feature, T baseValue) {
		features.put(feature.getName(), feature);
		if (baseValue instanceof Value) {
			baseValuesForFeats.put(feature.getName(), (Value)baseValue);
		}
		else {
			baseValuesForFeats.put(feature.getName(), new BasicValue<T>(baseValue));
		}
		feature.setAsFixed(true);
	}
	
	
	

	/**
	 * Add a list of features to the type (if a feature 
	 * with identical name already exists, it is overwritten)
	 * 
	 * @param features the list of features to add
	 */
	public void addFullFeatures(List<Type> features) {
		for (Type f : features) {
			String name = f.getName();
			if (this.features.containsKey(name)) {
				log.warning("feature name " + name + " already in entity " + name);
			}
			this.features.put(f.getName(), f);
			f.setAsFixed(true);
		}
	}
	
	
	/**
	 * Adds a list of partial features to the type. The list of partial features
	 * is defined as a map between the feature and the base values for which they
	 * are defined.
	 * 
	 * @param partialFeats the partial features to add
	 */
	public void addPartialFeatures(Map<Type, Value> partialFeats) {
		for (Type pfeat : partialFeats.keySet()) {
			log.debug("here, adding partial feature: " + pfeat);
			addPartialFeature(pfeat, partialFeats.get(pfeat));
			pfeat.setAsFixed(true);
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
	 * Returns the list of basic values content contained in the type
	 * (e.g. excluding all range values)
	 * 
	 * @return the list of values content
	 */
	public List<Object> getBasicValuesContent() {
		List<Object> basicValues = new LinkedList<Object>();
		for (Value v: values) {
			if (v instanceof BasicValue) {
				basicValues.add(((BasicValue<?>)v).getValue());
			}
		}
		return basicValues;
	}


	/**
	 * Returns true if the type contains the given full or partial feature,
	 * false otherwise
	 * 
	 * @param featureName the feature name
	 * @return true if  feature present, false otherwise
	 */
	public boolean hasFeature(String featureName) {
		return features.containsKey(featureName);
	}
	

	/**
	 * Returns true if the type contains the given full feature,
	 * false otherwise
	 * 
	 * @param featureName the feature name
	 * @return true if full feature present, false otherwise
	 */
	public boolean hasFullFeature(String featureName) {
		return (features.containsKey(featureName) &&
				!baseValuesForFeats.containsKey(featureName));
	}

	
	/**
	 * Returns true if the type contains the given partial feature
	 * 
	 * @param featureName the feature name
	 * @return true if partial feature present, false otherwise
	 */
	public boolean hasPartialFeature(String featureName) {
		return (features.containsKey(featureName) &&
				baseValuesForFeats.containsKey(featureName));
	}
	
	
	/**
	 * Returns true if the type contains the partial feature
	 * of the given name, for the specific base value
	 * 
	 * @param featureName the feature name
	 * @param baseValue the type value
	 * @return true if partial feature present, false otherwise
	 */
	public <T> boolean hasPartialFeature(String featureName, T baseValue) {
		if (features.containsKey(featureName) &&
				baseValuesForFeats.containsKey(featureName)) {
			if (baseValue instanceof Value) {
				return baseValuesForFeats.get(featureName).equals(baseValue);
			}
			else {
				return baseValuesForFeats.get(featureName).equals(new BasicValue<T>(baseValue));
			}
		}
		return false;
	}
	
	
	/**
	 * Returns true if the type contains the a (full or partial) feature
	 * of the given name, for the specific base value
	 * 
	 * @param featureName the feature name
	 * @param baseValue the type value
	 * @return true if feature present, false otherwise
	 */
	public <T> boolean hasFeature(String featureName, T baseValue) {
		return (hasFullFeature(featureName) || hasPartialFeature(featureName, baseValue));
	}

	
	/**
	 * Gets the full feature associated with the name, if any is present. 
	 * Else, return null.  
	 * 
	 * @param featureName the feature name
	 * @return the entity if the feature is present, or null otherwise.
	 */
	public Type getFullFeature(String featureName) {
		if (!baseValuesForFeats.containsKey(featureName)) {
			return features.get(featureName);
		}
		return null;
	}


	/**
	 * Gets the partial feature associated with the name, if any is present. 
	 * Else, return null.  
	 * 
	 * @param featureName the feature name
	 * @return the entity if the feature is present, or null otherwise.
	 */
	public Type getPartialFeature(String featureName) {
		if (baseValuesForFeats.containsKey(featureName)) {
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
	public <T> List<Type> getPartialFeatures(T baseValue) {
		List<Type> partialFeats = new LinkedList<Type>();
		for (String feat : baseValuesForFeats.keySet()) {
			
			if (baseValue instanceof Value && baseValuesForFeats.get(feat).equals(baseValue)) {
				partialFeats.add(features.get(feat));
			}
			else if (baseValuesForFeats.get(feat).equals(new BasicValue<T>(baseValue))) {
				partialFeats.add(features.get(feat));
			}
		}
		return partialFeats;
	}

	
	/**
	 * Returns the base value for the partial feature, if one is defined.
	 * Else, returns null.
	 * 
	 * @param featName the feature name
	 * @return the value
	 */
	public Value getBaseValueForPartialFeature(String featName) {
		return baseValuesForFeats.get(featName);
	}
	
	/**
	 * Returns the list of full features for the type
	 * 
	 * @return the list of full features
	 */
	public List<Type> getFullFeatures() {
		List<Type> fullFeats = new LinkedList<Type>();
		for (String feat : features.keySet()) {
			if (!baseValuesForFeats.containsKey(feat)) {
				fullFeats.add(features.get(feat));
			}
		}
		return fullFeats;
	}

	
	/**
	 * Returns the list of (full or partial) features.  Note that some
	 * of these features might only be defined for specific values
	 * of the base!
	 * 
	 * @return the list of all features
	 */
	public List<Type> getFeatures() {
		return new ArrayList<Type>(features.values());
	}
	
	
	/**
	 * Returns the partial or full feature associated with the name
	 * 
	 * @param featureName the feature name
	 * @return
	 */
	public Type getFeature(String featureName) {
		return features.get(featureName);
	}


	/**
	 * Returns true if the type is fixed, false otherwise
	 * 
	 * @return true if type is fixed, false otherwise
	 */
	public boolean isFixed() {
		return isFixed;
	}
	
	
	/**
	 * Returns a string representation of the type
	 *
	 * @return the string representation
	 */
	public String toString() {
		String str = name;
		str += " = " + values;
		if (!features.isEmpty()) {
			str += " [" + features.values() + "]";
		}
		return str;
	}
	
	
	/**
	 * Checks whether two types are identical.  Important note: we only
	 * check the name of the type, which means it is crucial to ensure that
	 * each type has a unique identifier!
	 *
	 * @param o the object to compare
	 * @return true if they are identical, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		if (o instanceof Type) {
			return (name.equals(((Type)o).getName()));
		}
		else {
			return false;
		}
	}


}
