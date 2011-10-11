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

package opendial.inference.bn.distribs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.Assignment;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProbabilityTable {

	static Logger log = new Logger("ProbabilityTable", Logger.Level.DEBUG);
	
	Map<Assignment, Float> table;
	
	public ProbabilityTable() {
		table = new HashMap<Assignment,Float>();
	}
	
	public void addRow(Assignment assignment, float prob) {
		table.put(assignment, prob);
	}
	
	public float getProb (Assignment assignment) {
		Float result = table.get(assignment);
		if (result != null) {
			return result.floatValue();
		}
		else {
			return 0.0f;
		}
	}
	
	public boolean hasProb (Assignment assignment) {
		boolean result = table.containsKey(assignment);
		return result;
	}

	/**
	 * 
	 * @param subAss
	 * @return
	 */
	public List<Assignment> getIncludingAssignments(Assignment subAss) {
		List<Assignment> result = new LinkedList<Assignment>();
		for (Assignment ass: table.keySet()) {
			if (ass.contains(subAss)) {
				result.add(ass);
			}
		}
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public Set<Assignment> getAllAssignments() {
		return table.keySet();
	}
}
