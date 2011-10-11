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

package opendial.state;

import java.util.HashMap;
import java.util.Map;

import opendial.arch.DialException;
import opendial.domains.types.GenericType;
import opendial.utils.Logger;

/**
 * Representation of a fluent for an entity.  The main extension is the use
 * of an "existence probability", for cases of uncertainty about the existence
 * of the entity itself (independently of uncertainty on its content).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class EntityFluent extends Fluent {

	// logger
	static Logger log = new Logger("EntityFluent", Logger.Level.NORMAL);
	
	// existence probability
	float existenceProb = 1.0f;
	
	// entity counter for each entity
	private static Map<String,Integer> entityCounter = new HashMap<String,Integer>();


	/**
	 * Creates a new entity fluent.  The label is forged by adding the type label 
	 * + a counter on the number of entities 
	 * 
	 * @param type
	 */
	public EntityFluent(GenericType type) {
		super(type);
		label = forgeNewLabel();
	}
	

	/**
	 * Sets the existence probability of the entity
	 * 
	 * @param existenceProb existence probability
	 */
	public void setExistenceProb(float existenceProb) {
		this.existenceProb = existenceProb;
	}
	
	
	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================

	
	/**
	 * Copies the fluent
	 */
	public EntityFluent copy() {
		EntityFluent copy = new EntityFluent(type);
		copy.setLabel(label);
		
		for (String v : values.keySet()) {
			try {
			copy.addValue(v, values.get(v));
			}
			catch (DialException e) {
				log.warning("Strange problem copying a fluent, aborting copy operation");
			}
		}
		for (ConditionalFluent f : features.values()) {
			copy.addFeature(f.copy());
		}
		copy.setExistenceProb(existenceProb);
		return copy;
	}
	
	/**
	 * Forge a new label, by concatenating the type label + a counter
	 * on the number of entities
	 * 
	 * @return
	 */
	private String forgeNewLabel() {
		String typeName = type.getName();
		if (!entityCounter.containsKey(typeName)) {
			entityCounter.put(typeName, 1);
		}
		else {
			entityCounter.put(typeName, entityCounter.get(typeName) + 1);
		}
		return typeName + entityCounter.get(typeName);
	}
	
}
