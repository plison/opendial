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

package opendial.inference.algorithms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNode;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VariableElimination {

	static Logger log = new Logger("VariableElimination", Logger.Level.NORMAL);
	
	
	
	private static List<Factor> sumOut(BNode node, List<Factor> factors) {
		return factors;
		
	}
	
	
	private static Factor pointwiseProduct (List<Factor> factors) {
		
		Map<String,Set<Object>> unionVars = new HashMap<String,Set<Object>>();
		for (Factor f: factors) {
			for (String var : f.getVariables()) {
				unionVars.put(var, f.getValues(var));
			}
		}
		
		Factor productFactor = new Factor(unionVars);
		
		List<Assignment> combinations = InferenceUtils.getAllCombinations(unionVars);
		
		for (Assignment a : combinations) {
			float product = 1.0f;
			for (Factor f: factors) {
				Assignment reducedAssignment = InferenceUtils.trimAssignment(a, f.getVariables());
				product = product * f.getEntry(reducedAssignment);
			}
			productFactor.addEntry(a, product);
		}
		
		
		return productFactor;
	}
	
	
	 private static Factor makeFactor(BNode node, Assignment evidence) {
		 
		 Factor f = new Factor(node.getAllValues());
		 
		 for (Assignment a: node.getAllPossibleAssignments()) {
			 if (a.contains(evidence)) {
				 a.removePairs(evidence.getPairs().keySet());
				 f.addEntry(a, node.getProb(a));
			 }
		 }
		 return f;
	}
}


 final class Factor {
	
	Map<Assignment, Float> matrix;
	
	Map<String,Set<Object>> values;
	
	public Factor(Map<String,Set<Object>> values) {
		matrix = new HashMap<Assignment,Float>();
		this.values = values;
	}
	
	public void addEntry (Assignment a, float value) {
		matrix.put(a, value);
	}
	
	public float getEntry(Assignment a) {
		return matrix.get(a);
	}
	
	public Set<String> getVariables() {
		return values.keySet();
	}
	
	public Set<Object> getValues(String var) {
		return values.get(var);
	}
}
