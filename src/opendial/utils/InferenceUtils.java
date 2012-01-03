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

import opendial.arch.DialException;
import opendial.inference.bn.Assignment;
import opendial.utils.Logger;

/**
 * General utilities for probabilistic inference
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class InferenceUtils {

	// logger
	static Logger log = new Logger("InferenceUtils", Logger.Level.DEBUG);
	
	public static long totalTimeForCombinatorics = 0;

	
	/**
	 * Generates all possible assignment combinations from the set of values
	 * provided as parameters -- each variable being associated with a set of
	 * alternative values.
	 * 
	 * <p>NB: use with caution, computational complexity is exponential!
	 * 
	 * @param valuesMatrix the set of values to combine 
	 * @return the list of all possible combinations
	 */
	public static List<Assignment> getAllCombinations(Map<String,Set<Object>> valuesMatrix) {
			
		long initTime = System.currentTimeMillis();
		// start with a single, empty assignment
		List<Assignment> assignments = new LinkedList<Assignment>();
		assignments.add(new Assignment());
		
		// at each iterator, we expand each assignment with a new combination
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
	//	if (assignments.size() == 1 && assignments.get(0).getSize() == 0) {
	//		return new LinkedList<Assignment>();
	//	}
		
		totalTimeForCombinatorics += (System.currentTimeMillis() - initTime);
		return assignments;
	}
	
	
	/**
	 * Generates all combinations of assignments from the list
	 * 
	 * <p>NB: use with caution, computational complexity is exponential!

	 * @param allAssignments list of alternative assignments
	 * @return the generated combination of assignments
	 */
	public static List<Assignment> getAllCombinations(List<Set<Assignment>> allAssignments) {
		
		long initTime = System.currentTimeMillis();
		
		// start with a single, empty assignment
		List<Assignment> assignments = new LinkedList<Assignment>();
		assignments.add(new Assignment());
		
		// incrementally combines and expands the assignments
		for (Set<Assignment> list1: allAssignments) {

			List<Assignment> assignments2 = new LinkedList<Assignment>();

			for (Assignment a : list1) {
				
				for (Assignment ass: assignments) {
					if (ass.consistentWith(a)) {
					Assignment ass2 = new Assignment(ass, a);
					assignments2.add(ass2);
					}
				}
			}
			assignments = assignments2;
		}		
		
		totalTimeForCombinatorics += (System.currentTimeMillis() - initTime);

		return assignments;
	}


	/**
	 * Normalise the given probability distribution.
	 * 
	 * @param distrib the distribution to normalise
	 * @return the normalised distribution
	 */
	public static Map<Assignment, Float> normalise (Map<Assignment, Float> distrib) throws DialException {
		float total = 0.0f;
		for (Assignment a : distrib.keySet()) {
			total += distrib.get(a);
		}
		if (total == 0.0f) {
	//		log.debug("distribution: " + distrib);
			throw new DialException("all assignments in the distribution have a zero probability, cannot be normalised");
		}
		Map<Assignment,Float> normalisedDistrib = new HashMap<Assignment,Float>();
		for (Assignment a: distrib.keySet()) {
			normalisedDistrib.put(a, distrib.get(a)/ total);
		}
		return normalisedDistrib;
	}

	
}
