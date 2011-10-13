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

package opendial.inference.bn;

import java.util.List;
import java.util.SortedMap;
import java.util.Set;
import java.util.TreeMap;

import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Assignment {

	static Logger log = new Logger("Assignment", Logger.Level.DEBUG);
	
	SortedMap<String,Object> pairs;
	
	public Assignment() {
		pairs = new TreeMap<String,Object>();
	}
	
	public Assignment(Assignment a) {
		pairs = a.copy().getPairs();
	}
	
	
	public Assignment(String id, Object val) {
		this();
		addPair(id,val);
	}
	
	public Assignment(String id) {
		this();
		addAssignment(id);
	}
	
	public Assignment(List<String> ids) {
		this();
		for (String id: ids) {
			addAssignment(id);
		}
	}
	
	/**
	 * @param ass
	 * @param id
	 * @param val
	 */
	public Assignment(Assignment ass, String id, Object val) {
		pairs = ass.copy().getPairs();
		addPair(id, val);
	}
	
	
	public Assignment(Assignment ass1, Assignment ass2) {
		pairs = ass1.copy().getPairs();
		addAssignments(ass2.copy().getPairs());
	}

	public void addPair(String id, Object val) {
		pairs.put(id, val);
	}
	
	public void addAssignment(String id) {
		if (!id.startsWith("!")) {
			addPair(id, Boolean.TRUE);
		}
		else {
			addPair(id.substring(1,id.length()), Boolean.FALSE);
		}
	}
	
	public void addAssignments (SortedMap<String,Object> assignments) {
		this.pairs.putAll(assignments);
	}
	
	
	
	@Override
	public int hashCode() {
		double hash = 0;
		int counter = 1;
		
		for (String key: pairs.keySet()) {
			hash += counter*key.hashCode()*pairs.get(key).hashCode()/100;
			if (hash == 0) {
				log.warning("hash value for assignment has dropped to 0, might cause problems");
			}
			counter++;
		}
		if (hash < Integer.MIN_VALUE || hash > Integer.MAX_VALUE) {
			hash = hash / 10000;
		}
 		return (int)hash;
	}
	
	
	@Override
	public boolean equals(Object o) {
		return (hashCode() == o.hashCode());
	}
	
	
	public SortedMap<String,Object> getPairs() {
		return pairs;
	}

	/**
	 * 
	 * @return
	 */
	public int getSize() {
		return pairs.size();
	}
	
	
	public Assignment copy() {
		Assignment ass = new Assignment();
		ass.addAssignments(pairs);
		return ass;
	}
	
	
	public String toString() {
		String str = "";
		for (String key: pairs.keySet()) {
			str += key + "=" + pairs.get(key) ;
			if (!key.equals(pairs.lastKey())) {
				str += " ^ ";
			}
		}
		return str;
		
	}

	/**
	 * 
	 * @param subAss
	 * @return
	 */
	public boolean contains(Assignment subAss) {	
		for (String key : subAss.getPairs().keySet()) {
			if (pairs.containsKey(key)) {
				Object val = subAss.getPairs().get(key);
				if (!pairs.get(key).equals(val)) {
					return false;
				}
			}
			else {
				return false;
			}	
		}
		return true;
	}
	
	
	public boolean consistentWith(Assignment evidence) {
		for (String evidenceVar : evidence.getPairs().keySet()) {
			if (pairs.containsKey(evidenceVar)) {
				if (!pairs.get(evidenceVar).equals(evidence.getPairs().get(evidenceVar))) {
					return false;
				}
			}
		}
		return true;
	}



	/**
	 * 
	 * @param id
	 * @return
	 */
	public void removePair(String id) {
		pairs.remove(id);
	}
	
	public void removePairs(Set<String> ids) {
		for (String id: ids) {
			pairs.remove(id);
		}
	}
}
