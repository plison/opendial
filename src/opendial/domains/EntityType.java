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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.utils.Logger;

/**
 * Representation of an entity type
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class EntityType {

	static Logger log = new Logger("Entity", Logger.Level.NORMAL);
	
	// name of the entity
	String entityName;
	
	// list of values for the entity
	List<String> values;
	
	// list of features for the entity
	Map<String,EntityType> features;
	
	boolean isFixed = false;
	
	
	// ===================================
	//  ENTITY CONSTRUCTION METHODS
	// ===================================

	/**
	 * Create a new entity with the given name
	 * 
	 * @param entityName the entity name
	 */
	public EntityType (String entityName) {
		this.entityName= entityName;
		values = new ArrayList<String>();
		features = new HashMap<String,EntityType>();
	}


	/**
	 * Change the entity name
	 * 
	 * @param entityName the new name of the entity
	 */
	public void setName(String entityName) {
		this.entityName = entityName;
	}
	
	/**
	 * Add a new value to the entity
	 * 
	 * @param value the new value to add
	 */
	public void addValue(String value) {
		values.add(value);
	}
	
	/**
	 * Add a list of values to the entity
	 * (if the value already exists, it is overwritten)
	 * 
	 * @param values the list of values to add
	 */
	public void addValues(List<String> values2) {
		values.addAll(values2);
	}
	
	/**
	 * Add a list of features to the entity (if a feature 
	 * with identical name already exists, it is overwritten)
	 * 
	 * @param features the list of features to add
	 */
	public void addFeatures(List<EntityType> features) {
		for (EntityType f : features) {
			String name = f.getName();
			if (this.features.containsKey(name)) {
				log.warning("feature name " + name + " already in entity " + entityName);
			}
			this.features.put(f.getName(), f);
		}
	}
	
	/**
	 * Set the entity as being of fixed arity ("fixed variable")
	 */
	public void setAsFixed() {
		isFixed = true;
	}

	
	// ===================================
	//  GETTERS
	// ===================================

	
	/**
	 * Returns true if the entity contains the given value,
	 * false otherwise

	 * @return true if value present, false otherwise
	 */
	public boolean hasValue(String value) {
		return values.contains(value);
	}
	
	/**
	 * Returns the list of values contained in the entity
	 * 
	 * @return the list of values
	 */
	public List<String> getValues() {
		return values;
	}
	
	/**
	 * Returns the name of the entity
	 * 
	 * @return name of entity
	 */
	public String getName() {
		return entityName;
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
	 * Gets the feature associated with the name, if any
	 * is present. Else, return null.  
	 * 
	 * @param featureName the feature name
	 * @return the entity if the feature is present, or null otherwise.
	 */
	public EntityType getFeature(String featureName) {
		if (!features.containsKey(featureName)) {
			log.warning("feature name " + featureName + " absent from entity " + entityName);		
		}
		return features.get(featureName);
	}
	
	/**
	 * Returns true is the entity is fixed, and false otherwise
	 * 
	 * @return true is entity is fixed, false otherwise
	 */
	public boolean isFixed() {
		return isFixed;
	}
}
