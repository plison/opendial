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
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProbabilityTable extends GenericDistribution {

	static Logger log = new Logger("ProbabilityTable", Logger.Level.DEBUG);
	
	Map<Assignment, Float> table;
		
	
	public ProbabilityTable() {
		super();
		table = new HashMap<Assignment,Float>();
	}

	
	public void addRow(Assignment assignment, float prob) {
		if (isValidAssignment(assignment)) {
			table.put(assignment, prob);
		}
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
	
	
	public boolean hasDefinedProb (Assignment assignment) {
		boolean result = table.containsKey(assignment);
		return result;
	}

	
	public void completeProbabilityTable() {
				
		for (Assignment condAss : getConditionalAssignments()) {
			List<Assignment> uncovered = getUncoveredAssignments(condAss);
			if (uncovered.size() == 1) {
				Assignment lastAss = new Assignment(condAss, uncovered.get(0));  // hack!
				float remainingProb = 1 - getTotalProbability(condAss);
				addRow(lastAss, remainingProb);
		//		log.debug("Completing table with: P(" + lastAss + ") = " + remainingProb );
			}
		}
	}
	

	
	public boolean isWellFormed() {
		
		List<Assignment> condAssignments = getConditionalAssignments();
		for (Assignment condAss : condAssignments) {
			List<Assignment> uncovered = getUncoveredAssignments(condAss);
			if (uncovered.size() > 0) {
				for (Assignment uncoveredHead : uncovered) {
					log.warning("Uncovered assignments in the probability table: P(" + uncoveredHead + " | " + condAss + ") = ?");
				}
				return false;
			}
			
			float totalProbability = getTotalProbability(condAss);
			if (totalProbability < 0.999f || totalProbability > 1.0001f) {
				log.warning("total probability for conditional assignment " + condAss + " = " + totalProbability);
				return false;
			}
		}
		List<Assignment> possibleCondAssignments = InferenceUtils.getAllCombinations(depValues);
		if (possibleCondAssignments.size() != condAssignments.size()) {
			for (Assignment p : possibleCondAssignments) {
				if (!condAssignments.contains(p)) {
					log.warning("No distribution is declared for conditional assignment " + p);
					return false;
				}
			}
		}
		return true;
	}
	
	

	/**
	 * 
	 * @return
	 */
	public Set<Assignment> getDefinedAssignments() {
		return table.keySet();
	}
	
	
	public String toString() {
		String str = "";
		for (Assignment assign: table.keySet()) {
			str += "P(" + assign + ")=" + table.get(assign) + "\n";
		}
		return str;
	}
}
