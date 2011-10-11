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
	
	SortedMap<String,Object> assignments;
	
	public Assignment() {
		assignments = new TreeMap<String,Object>();
	}
	
	public Assignment(String id, Object val) {
		this();
		addAssignment(id,val);
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
		assignments = ass.getAssignments();
		addAssignment(id, val);
	}

	public void addAssignment(String id, Object val) {
		assignments.put(id, val);
	}
	
	public void addAssignment(String id) {
		if (!id.startsWith("!")) {
			addAssignment(id, Boolean.TRUE);
		}
		else {
			addAssignment(id.substring(1,id.length()), Boolean.FALSE);
		}
	}
	
	public void addAssignments (SortedMap<String,Object> assignments) {
		this.assignments.putAll(assignments);
	}
	
	
	
	@Override
	public int hashCode() {
		double hash = 0;
		int counter = 1;
		for (String key: assignments.keySet()) {
			hash += 2*counter*key.hashCode() + 3*counter*assignments.get(key).hashCode();
			counter++;
		}
		if (hash < Integer.MIN_VALUE || hash > Integer.MAX_VALUE) {
			log.debug("Hashcode exceeds min or max value, scaling down");
			hash = hash / 10000;
		}
 		return (int)hash;
	}
	
	
	@Override
	public boolean equals(Object o) {
		return (hashCode() == o.hashCode());
	}
	
	
	public SortedMap<String,Object> getAssignments() {
		return assignments;
	}

	/**
	 * 
	 * @return
	 */
	public int getSize() {
		return assignments.size();
	}
	
	
	public Assignment copy() {
		Assignment ass = new Assignment();
		ass.addAssignments(assignments);
		return ass;
	}
	
	
	public String toString() {
		String str = "";
		for (String key: assignments.keySet()) {
			str += key + "=" + assignments.get(key) ;
			if (!key.equals(assignments.lastKey())) {
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
		for (String key : subAss.getAssignments().keySet()) {
			if (assignments.containsKey(key)) {
				Object val = subAss.getAssignments().get(key);
				if (!assignments.get(key).equals(val)) {
					return false;
				}
			}
			else {
				return false;
			}	
		}
		return true;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Object getSubAssignment(String id) {
		return assignments.get(id);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public void removeAssignment(String id) {
		assignments.remove(id);
	}
}
