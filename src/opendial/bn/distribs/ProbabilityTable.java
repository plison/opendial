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

package opendial.bn.distribs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.utils.InferenceUtils;

/**
 * Traditional probability distribution represented as a probability table.  The table
 * expresses a generic distribution of type P(X1...Xn|Y1...Yn), where X1...Xn is called
 * the "head" part of the distribution, and Y1...Yn the conditional part.
 * 
 * <p>Technically, the table is expressed as a double mapping, where each assignment for
 * Y1...Yn is mapped to a table where the assignments for X1...Xn are associated with a 
 * given probability value.  If the distribution contains no conditional part, the assignment
 * for Y1...Yn is empty.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProbabilityTable implements ProbDistribution {

	// logger
	public static Logger log = new Logger("ProbabilityTable", Logger.Level.DEBUG);
	
	// the probability table
	Map<Assignment,Map<Assignment,Float>> table;
	
	

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================

	
	/**
	 * Constructs a new probability table, with no values
	 */
	public ProbabilityTable() {
		table = new HashMap<Assignment,Map<Assignment,Float>>();
	}
	
	
	/**
	 * Adds a new row to the probability table, assuming no conditional assignment.
	 * If the table already contains a probability, it is erased.
	 * 
	 * @param head the assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public void addRow (Assignment head, float prob) {
		addRow(new Assignment(), head, prob);
	}
	
	
	
	/**
	 * Adds a new row to the probability table, given the conditional assignment,
	 * the head assignment and the probability value.
	 * If the table already contains a probability, it is erased.

	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the head assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, Assignment head, float prob) {
		
		if (!table.containsKey(condition)) {
			table.put(condition, new HashMap<Assignment, Float>());
		}
		
		if (prob < 0.0f || prob > 1.0f) {
			log.warning("probability is not well-formed: " + prob);
			return;
		}
		
		if (table.get(condition).containsKey(head)) {
			log.debug("probability for P("+head+"|" + condition+ ") already exists, replacing " 
					+ table.get(condition).get(head) + " by " + prob);
		}
		
		table.get(condition).put(head, prob);
		
	}
	
	/**
	 * Add a new set of rows to the probability table, given the conditional 
	 * assignment and the mappings (head assignment, probability value).
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param heads the mappings (head assignment, probability value)
	 */
	public void addRows(Assignment condition, Map<Assignment,Float> heads) {
		for (Assignment head : heads.keySet()) {
			addRow(condition, head, heads.get(head));
		}
	}
	
	
	/**
	 * Removes a row from the table, given the condition and the head assignments.
	 * 
	 * @param condition conditional assignment
	 * @param head head assignment
	 */
	public void removeRow(Assignment condition, Assignment head) {
		if (table.containsKey(condition)) {
			table.get(condition).remove(head);
		}
		else {
			log.debug("cannot remove row: condition " + condition + " is not present");
		}
	}
	
	

	// ===================================
	//  GETTERS
	// ===================================

	
	
	/**
	 * Returns the probability P(head|condition), if any is specified.  Else,
	 * returns 0.0f.
	 *
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	@Override
	public float getProb(Assignment condition, Assignment head) {
		if (table.containsKey(condition)) {
			Map<Assignment,Float> headTable = table.get(condition);
			if (headTable.containsKey(head)) {
				return headTable.get(head);
			}
			return 0.0f;
		}
		return 0.0f;
	}
	
	
	/**
	 * Sample a head assignment from the distribution P(head|condition), given the
	 * condition.  If no assignment can be sampled (due to e.g. an ill-formed 
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 */
	@Override
	public Assignment sample(Assignment condition) {
		
		for (int i = 0 ; i < 10 ; i++) {
		float rand = (new Random()).nextFloat();
		float total = 0.0f;
		Map<Assignment,Float> subtable = table.get(condition);
		for (Assignment a : subtable.keySet()) {
			total += subtable.get(a);
			if (rand < total) {
				return a;
			}
		}
		log.debug("sampling didn't work (try " + i + "), random number: " + rand);
		}
		return new Assignment();
	}
	


	/**
	 * Returns the probability table associated with the conditional assignment
	 * given as argument.
	 * 
	 * @param condition the assignment on the conditional variables
	 * @return the associated probability table
	 */
	@Override
	public Map<Assignment, Float> getProbTable(Assignment condition) {
		if (table.containsKey(condition)) {
			return table.get(condition);
		}
		else {	
			log.warning("probability table for the condition "  + condition + " not defined");
			return new HashMap<Assignment,Float>();
		}
	}
	

	// ===================================
	//  UTILITIES
	// ===================================

	/**
	 * Returns the hashcode for the table.
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	
	
	/**
	 * Returns true if the probability table is well-formed.  The method checks that all 
	 * possible assignments for the condition and head parts are covered in the table, 
	 * and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		
		// checks that all possible conditional assignments are covered in the table
		Map<String,Set<Object>> possibleCondPairs = 
			InferenceUtils.extractPossiblePairs(table.keySet());
		List<Assignment> possibleCondAssignments = 
			InferenceUtils.getAllCombinations(possibleCondPairs);
		possibleCondAssignments.remove(new Assignment());
		if (possibleCondAssignments.size() != table.keySet().size() && possibleCondAssignments.size() > 1) {
			log.warning("number of possible conditional assignments: " 
					+ possibleCondAssignments.size() 
					+ ", but number of actual conditional assignments: " 
					+ table.keySet().size());
			log.debug("possible conditional assignments: " + possibleCondAssignments);
			log.debug("actual assignments: "+ table.keySet());
			return false;
		}
		
		// checks that all possible head assignments are covered, for each condition
		Map<String,Set<Object>> possibleHeadPairs = new HashMap<String,Set<Object>>();	
		for (Assignment condition : table.keySet()) {
			possibleHeadPairs.putAll(
					InferenceUtils.extractPossiblePairs(table.get(condition).keySet()));
		}
		List<Assignment> possibleHeadAssignments = 
			InferenceUtils.getAllCombinations(possibleHeadPairs);
		possibleHeadAssignments.remove(new Assignment());
		for (Assignment condition : table.keySet()) {
			if (table.get(condition).keySet().size() != possibleHeadAssignments.size()) {
				log.warning("for condition = " + condition 
						+", number of possible head assignments: " 
						+ possibleHeadAssignments.size() 
						+ ", but number of actual head assignments: "
						+ table.get(condition).keySet().size());
				return false;
			}
			
			// checks that the total probability is roughly equal to 1.0f
			float totalProb = countTotalProb(condition);
			if (totalProb < 0.9999f || totalProb > 1.0001f) {
				log.warning("total probability for the condition = " + condition 
						+ " is " + totalProb);
				return false;
			}
		}
		
		return true;
	}
	

	
	/**
	 * Returns a string representation of the probability table
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment cond: table.keySet()) {
			for (Assignment head: table.get(cond).keySet()) {
				if (cond.getSize() > 0) {
					str += "P(" + head + " | " + cond 
					+ "):="  + table.get(cond).get(head) + "\n";
				}
				else {
					str += "P(" + head + "):="  
					+ table.get(new Assignment()).get(head) + "\n";
				}
			}
		}

		return str;
	}
	
	

	/**
	 * Returns a pretty print representation of the table, which is identical 
	 * to the toString() representation if the total number of lines is < 20.
	 * 
	 * <p>Else, returns the message "(probability table too big to be shown)".
	 * 
	 * @return the pretty print representation of the table
	 */
	@Override
	public String prettyPrint() {
		if (getFlatTable().size() < 20) {
			return toString();
		}
		else {
			return "(probability table too big to be shown)";
		}
	}
	
	

	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public ProbabilityTable copy() {
		ProbabilityTable tableCopy = new ProbabilityTable();
		for (Assignment condition : table.keySet()) {
			Map<Assignment,Float> headTable = table.get(condition);
			for (Assignment head : headTable.keySet()) {
				tableCopy.addRow(condition, head, headTable.get(head));
			}
		}
		return tableCopy;
	}
	
	
	// ===================================
	//  PRIVATE METHODS
	// ===================================

	

	/**
	 * Returns the total accumulated probability for the distribution P(.|condition)
	 * 
	 * @param condition the conditional assignment
	 * @return the total probability
	 */
	private float countTotalProb(Assignment condition) {
		float totalProb = 0.0f;
		for (Assignment head : table.get(condition).keySet()) {
			totalProb += table.get(condition).get(head);
		}
		return totalProb;
	}
	
	
	private Map<Assignment,Float> getFlatTable() {
		Map<Assignment,Float> flatTable = new HashMap<Assignment,Float>();
		for (Assignment condition : table.keySet()) {
			for (Assignment head : table.get(condition).keySet()) {
				Assignment flatValue = new Assignment(condition, head);
				flatTable.put(flatValue, table.get(condition).get(head));
			}
		}
		return flatTable;
	}

	
	
}
