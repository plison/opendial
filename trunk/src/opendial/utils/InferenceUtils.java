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

package opendial.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNode;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class InferenceUtils {

	static Logger log = new Logger("InferenceUtils", Logger.Level.DEBUG);
	
	
	public static List<Assignment> getAllCombinations(Map<String,Set<Object>> valuesMatrix) {
				
		List<Assignment> assignments = new LinkedList<Assignment>();
		assignments.add(new Assignment());
		
		for (String label : valuesMatrix.keySet()) {

			List<Assignment> assignments2 = new LinkedList<Assignment>();

			for (Object val: valuesMatrix.get(label)) {
				
				for (Assignment ass: assignments) {
					Assignment ass2 = new Assignment(ass, label, val);
					assignments2.add(ass2);
				}
			}
			assignments = assignments2;
		}		
		return assignments;
	}
	
	


	public static Assignment trimAssignment(Assignment fullAssign, Set<String> variables) {
		Assignment trimedAssign = new Assignment();
		for (String key : fullAssign.getPairs().keySet()) {
			if (variables.contains(key)) {
				trimedAssign.addPair(key, fullAssign.getPairs().get(key));	
			}
		}
		return trimedAssign;
	}
	
	public static Map<Assignment, Float> normalise (Map<Assignment, Float> distrib) {
		float total = 0.0f;
		for (Assignment a : distrib.keySet()) {
			total += distrib.get(a);
		}
		Map<Assignment,Float> normalisedDistrib = new HashMap<Assignment,Float>();
		for (Assignment a: distrib.keySet()) {
			normalisedDistrib.put(a, distrib.get(a)/ total);
		}
		return normalisedDistrib;
	}
	
}
