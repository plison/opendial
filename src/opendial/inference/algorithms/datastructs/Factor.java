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

package opendial.inference.algorithms.datastructs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.Assignment;

/**
 * Representation of a factor matrix, used to perform probabilistic inference
 * with the Variable Elimination algorithm
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Factor {
	
		// the matrix
		Map<Assignment, Float> matrix;

		/**
		 * Creates a new, empty factor
		 */
		public Factor() {
			matrix = new HashMap<Assignment,Float>();
		}

		/**
		 * Adds a new entry to the matrix
		 * 
		 * @param a the assignment
		 * @param value the probability value
		 */
		public void addEntry (Assignment a, float value) {
			matrix.put(a, value);
		}
		

		/**
		 * Returns the probability for the assignment, if it is
		 * encoded in the matrix.  Else, returns null
		 * 
		 * @param a the assignment
		 * @return probability of the assignment
		 */
		public float getEntry(Assignment a) {
			return matrix.get(a);
		}

		/**
		 * Returns the matrix included in the factor
		 * 
		 * @return the matrix
		 */
		public Map<Assignment, Float> getMatrix() {
			return matrix;
		}

		/**
		 * Returns the set of variables used in the assignment.  It assumes
		 * that at least one entry exists in the matrix.  Else, returns
		 * an empty list
		 * 
		 * @return
		 */
		public Set<String> getVariables() {
			if (!matrix.isEmpty()) {
				return matrix.keySet().iterator().next().getVariables();
			}
			else {
				return new HashSet<String>();
			}
		}
		

		/**
		 * Returns a string representation of the factor
		 *
		 * @return the string representation
		 */
		public String toString() {
			String str = "";
			for (Assignment a : matrix.keySet()) {
				str += "P(" + a + ")=" + matrix.get(a) + "\n";
			}
			return str;
		}

}
