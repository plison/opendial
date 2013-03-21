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

package opendial.bn.values;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Value that is defined as a mapping of values.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public final class MapVal implements Value {
	
	// the set of values
	Map<String,Value> map;
	
	
	protected MapVal() {
		this.map = new HashMap<String,Value>();
	}
	
	/**
	 * Creates the map of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected MapVal(Map<String,Value> values) { this.map = new HashMap<String,Value>(values); };

	
	/**
	 * Returns the hashcode for the map
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return map.hashCode(); }
	
	/**
	 * Returns true if the maps are equals (contain the same elements), false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return ((o instanceof MapVal && ((MapVal)o).getMap().equals(getMap())));
	}
	
	
	/**
	 * Returns the set of values
	 *  
	 * @return the set
	 */
	public Map<String,Value> getMap() {return map; }
	
	/**
	 * Returns a copy of the map
	 *
	 * @return the copy
	 */
	@Override
	public MapVal copy() { return new MapVal(map); }
	
	/**
	 * Returns a string representation of the map
	 *
	 * @return the string
	 */
	@Override
	public String toString() { 
		String s = "<";
		for (String key : map.keySet()) {
			s += key + ":"+map.get(key)+";";
		}
		return s.substring(0,s.length()-1)+ ">";
	}

	
	public void put(String key, Value value) {
		map.put(key, value);
	}
	
	/**
	 * Adds all the values in the given MapVal to this value
	 * 
	 * @param values the mapVal with the values to add
	 */
	public void putAll(MapVal values) {
		map.putAll(values.getMap());
	}
	
	public void removeAll(List<String> discardValues) {
		for (String discardValue : discardValues) {
			map.remove(discardValue);
		}
	}
	
	/**
	 * Compares the map value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	public void remove(String key) {
		map.remove(key);
	}


	
}