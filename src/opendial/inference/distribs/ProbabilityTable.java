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

package opendial.inference.distribs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNode;
import opendial.utils.Logger;

/**
 * Implementation of a probability distribution as a look-up table.  The
 * table directly maps possible variable assignments to probability values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProbabilityTable extends GenericDistribution implements Distribution {

	// logger
	static Logger log = new Logger("ProbabilityTable", Logger.Level.DEBUG);
	
	// the look-up table with probabilities
	Map<Assignment, Float> table;
		
	
	/**
	 * Creates a new, empty probability table
	 */
	public ProbabilityTable() {
		super();
		table = new HashMap<Assignment,Float>();
	}
	
	
	/**
	 * Creates a new probability table, based on the values and input
	 * nodes specified in the Bayesian node.  
	 * 
	 * @param node the Bayesian node
	 */
	public ProbabilityTable(BNode node) {
		super(node);
		table = new HashMap<Assignment,Float>();		
	}
	
	
	/**
	 * Creates a probability table with the initial elements already 
	 * specified as parameter
	 * 
	 * @param table the initial table
	 */
	public ProbabilityTable(Map<Assignment, Float> table) {
		super();
		for (Assignment a : table.keySet()) {
			for (String avar : a.getVariables()) {
				addHeadVariable(avar, Arrays.asList(a.getValue(avar)));
			}
		}
		this.table = table;
	}

	
	
	// ===================================
	// SETTERS
	// ===================================



	/**
	 * Adds a row to the table, with the <assignment, prob> pair
	 * 
	 * @param assignment the assignment
	 * @param prob the associated probability
	 */
	public void addRow(Assignment assignment, float prob) {
	//	if (isValid(assignment)) {
			table.put(assignment, prob);
	//	}
	}
	
	

	
	/**
	 * Complete the probability table, if all but 1 possible assignments have
	 * been defined in the table.  In this case, add the last assignment, with
	 * a probability = 1.0f - sum(other assignments).
	 */
	public void completeProbabilityTable() {
			
		if (!getConditions().isEmpty()) {
		for (Assignment condAss : getConditions()) {
			
			// search for combinations of head variables not covered in the table
			List<Assignment> uncoveredHeads = new LinkedList<Assignment>();				
			for (Assignment comb : getHeads()) {
				if (!hasProb(new Assignment(condAss, comb))) {
					uncoveredHeads.add(comb);
				}
			}
			
			if (uncoveredHeads.size() == 1) {
				Assignment lastAss = new Assignment(condAss, uncoveredHeads.get(0));  
				float remainingProb = 1 - getTotalProbilityForCondition(condAss);
				addRow(lastAss, remainingProb);
				log.debug("Completing table with: P(" + lastAss + ") = " + remainingProb );
			}
			
			else if (uncoveredHeads.size() > 1) {
				log.warning("cannot complete probability table, more than one missing assignment");
			}
		}
		}
	}
	

	
	// ===================================
	// GETTERS
	// ===================================


	/**
	 * Returns the look-up table
	 *
	 * @return the look-up table
	 */
	@Override
	public List<Assignment> getTable() {
		return new ArrayList<Assignment>(table.keySet());
	}
	
	
	/**
	 * Returns the probability of the assignment, if a row exists for it.
	 * Else, returns 0.0f.
	 *
	 * @param assignment the assignment
	 * @return its probability, if one is defined.
	 */
	@Override
	public float getProb (Assignment assignment) {
		Float result = table.get(assignment);
		if (result != null) {
			return result.floatValue();
		}
		else {
			return 0.0f;
		}
	}
	
	
	
	/**
	 * Returns true if a probability is defined in the table for the
	 * given assignment.  Else returns false.
	 *
	 * @param assignment the assignment
	 * @return true if a probability is defined, false otherwise.
	 */
	@Override
	public boolean hasProb (Assignment assignment) {
		boolean result = table.containsKey(assignment);
		return result;
	}
	
	
	/**
	 * Returns true is the probability table is well-formed, and false otherwise.
	 * The table is said to be well-formed if (1) all possible assignments for the
	 * variables are covered, (2) the sum over all assignments = 1.0f.
	 *
	 * @return true if 
	 */
	@Override
	public boolean isWellFormed() {
		
		// check presence of all possible assignments
		for (Assignment a : getAllPossibleAssignments()) {
			if (!table.containsKey(a)) {
				log.warning("No distribution is declared for assignment " + a);
				log.debug("all keys: " + table.keySet());
				return false;			
			}
		}
 
		// ensure probabilities sum up to 1.0f
		for (Assignment condAss : getConditions()) {
			float totalProbability = getTotalProbilityForCondition(condAss);
			if (totalProbability < 0.999f || totalProbability > 1.0001f) {
				log.warning("total probability for P(.|" + condAss + ") = " + totalProbability);
				log.debug("full table:" + table);
				return false;
			}
		}		
		
		return true;
	}
	
		
	
	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns the total probability for a given conditional assignment
	 * 
	 * @param cond the conditional assignment
	 * @return the sum of all probabilities for this condition
	 */
	protected float getTotalProbilityForCondition(Assignment cond) {
		float total = 0.0f;
		for (Assignment a : getTable()) {
			if (a.contains(cond)) {
				total += getProb(a);
			}
		}
		return total;
	}
	
	
	/**
	 * Returns a string representation of the probability table
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment cond: getConditions()) {
			for (Assignment head: this.getHeads()) {
				if (cond.getSize() > 0) {
				str += "P(" + head + " | " + cond + "):="  + table.get(new Assignment(cond, head)) + "\n";
				}
				else {
					str += "P(" + head + "):="  + table.get(new Assignment(head)) + "\n";
				}
			}
		}
		
		return str;
	}

}
