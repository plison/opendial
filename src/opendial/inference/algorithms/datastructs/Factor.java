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
import opendial.utils.Logger;

/**
 * Representation of a factor used in the Variable Elimination algorithm
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Factor {

	 static Logger log = new Logger("Factor", Logger.Level.NORMAL);
	
		Map<Assignment, Float> matrix;
		
		Map<String,Set<Object>> possibleValues;


		public Factor() {
			matrix = new HashMap<Assignment,Float>();
			possibleValues = new HashMap<String,Set<Object>>();
		}


		public void addEntry (Assignment a, float value) {
			matrix.put(a, value);
			fillPossibleValues(a);
		}
		
		
		public void fillPossibleValues(Assignment a) {
			for (String var : a.getPairs().keySet()) {
				if (!possibleValues.containsKey(var)) {
					Set<Object> vals = new HashSet<Object>();
					vals.add(a.getPairs().get(var));
					possibleValues.put(var, vals);
				}
				else {
					possibleValues.get(var).add(a.getPairs().get(var));
				}
			}
		}

		public float getEntry(Assignment a) {
			return matrix.get(a);
		}

		public Map<Assignment, Float> getMatrix() {
			return matrix;
		}

		public Set<String> getVariables() {
			if (!matrix.isEmpty()) {
				return matrix.keySet().iterator().next().getPairs().keySet();
			}
			else {
				return new HashSet<String>();
			}
		}
		
		public Map<String,Set<Object>> getPossibleValues() {
			return possibleValues;
		}


		public String toString() {
			String str = "";
			for (Assignment a : matrix.keySet()) {
				str += "P(" + a + ")=" + matrix.get(a) + "\n";
			}
			return str;
		}

}
