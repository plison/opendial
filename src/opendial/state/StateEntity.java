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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.domains.types.EntityType;
import opendial.domains.types.StandardType;
import opendial.utils.Logger;

/**
 * TODO: should add a reference to the entity type here
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StateEntity {

	static Logger log = new Logger("StateVariable", Logger.Level.NORMAL);
	
	StandardType type;
	
	String label;
	private static int entityCounter = 1;
	
	SortedMap<String,Float> values;
	
	Map<StandardType,StateEntity> features;
	
	
	public StateEntity(StandardType type2) {
		this.type = type2;
		values = new TreeMap<String,Float>();
		features = new HashMap<StandardType,StateEntity>();
		label = type2.getName() + entityCounter;
		entityCounter++;
	}
	
	public void addValue(String value, float prob) {
		values.put(value, prob);
	}

	/**
	 * 
	 * @param values2
	 */
	public void addValues(Map<String, Float> values2) {
		values.putAll(values2);
	}

	/**
	 * 
	 * @param features2
	 */
	public void addFeatures(List<StateEntity> features2) {
		for (StateEntity feat : features2) {
			features.put(feat.getType(), feat);
		}
	}

	/**
	 * 
	 * @return
	 */
	public StandardType getType() {
		return type;
	}
	
	public String getLabel() {
		return label;
	}

	/**
	 * 
	 * @return
	 */
	public SortedMap<String,Float> getValues() {
		return values;
	}

	/**
	 * 
	 * @return
	 */
	public List<StateEntity> getFeatures() {
		return new ArrayList<StateEntity>(features.values());
	}
}
