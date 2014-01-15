// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.bn.distribs.discrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.other.ConditionalDistribution;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.DistanceUtils;
import opendial.utils.StringUtils;

/**
 * Traditional probability distribution represented as a discrete probability table.  
 * The table expresses a generic distribution of type P(X1, ..., Xn | Y1, ..., Yn), 
 * where X1, ..., Xn is called the "head" part of the distribution, and Y1, ..., Yn 
 * the conditional part.
 * 
 * <p>Technically, the table is expressed as a double mapping, where each assignment for
 * Y1, ..., Yn is mapped to a table where the assignments for X1...Xn are associated with 
 * a given probability value.  If the distribution contains no conditional part, the 
 * assignment for Y1...Yn is empty.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class ConditionalCategoricalTable extends ConditionalDistribution<CategoricalTable> 
	implements DiscreteDistribution  {

	// logger
	public static Logger log = new Logger("ConditionalCategoricalTable", Logger.Level.DEBUG);

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================


	/**
	 * Adds a new row to the probability table, given the conditional assignment,
	 * the head assignment and the probability value.
	 * If the table already contains a probability, it is erased.

	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the head assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, Assignment head, double prob) {

		if (prob < 0.0f || prob > 1.05f) {
			log.warning("probability is not well-formed: " + prob);
			return;
		}

		if (!table.containsKey(condition)) {
			table.put(condition, new CategoricalTable());
		}

		conditionalVars.addAll(condition.getVariables());

		table.get(condition).addRow(head, prob);

	}

	/**
	 * Increments the probability specified in the table for the given head 
	 * assignment.  If none exists, simply assign the probability.
	 * 
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Assignment head, double prob) {
		incrementRow(new Assignment(), head, prob);
	}


	/**
	 * Increments the probability specified in the table for the given condition and
	 * head assignments.  If none exists, simply assign the probability.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Assignment condition, Assignment head, double prob) {
		if (table.containsKey(condition) && table.get(condition).hasProb(head)) {
			addRow(condition,head, table.get(condition).getProb(head) + prob);
		}
		else {
			addRow(condition,head,prob);
		}
	}


	/**
	 * Add a new set of rows to the probability table, given the conditional 
	 * assignment and the mappings (head assignment, probability value).
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param heads the simple table with (head assignment, probability value)
	 */
	public void addRows(Assignment condition, CategoricalTable heads) {
		for (Assignment head : heads.getRows()) {
			addRow(condition, head, heads.getProb(head));
		}
	}


	/**
	 * Add a new set of rows to the probability table, given the mapping between
	 * conditional assignments and (value assignment, prob) pairs
	 * 
	 * @param fullTable the full probability table to insert
	 */
	public void addRows (Map<Assignment, Map<Assignment,Double>> fullTable) {
		for (Assignment cond : fullTable.keySet()) {
			for (Assignment head : fullTable.get(cond).keySet()) {
				addRow(cond, head, fullTable.get(cond).get(head));
			}
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
			table.get(condition).removeRow(head);
		}
		else {
			log.debug("cannot remove row: condition " + condition + " is not present");
		}
	}


	/**
	 * Fill the "conditional holes" of the distribution -- that is, the possible conditional
	 * assignments Y1,..., Yn that are not associated with any distribution P(X1,...,Xn | Y1,...,Yn)
	 * in the table. The method create a default assignment X1=None,... Xn=None with probability
	 * 1.0 for these cases.
	 */
	public void fillConditionalHoles() {
		Map<String,Set<Value>> possibleCondPairs = 
				CombinatoricsUtils.extractPossiblePairs(table.keySet());
		if (CombinatoricsUtils.getEstimatedNbCombinations(possibleCondPairs) < 400)  {				
			Set<Assignment> possibleCondAssignments = 
					CombinatoricsUtils.getAllCombinations(possibleCondPairs);
			possibleCondAssignments.remove(new Assignment());

			Assignment defaultA = new Assignment();
			if (!table.isEmpty()) {
				defaultA = Assignment.createDefault(table.values().iterator().next().getHeadVariables());
			}
			for (Assignment possibleCond: possibleCondAssignments) {
				if (!table.containsKey(possibleCond)) {
					addRow(possibleCond, defaultA, 1.0);
				}
			}
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
	public double getProb(Assignment condition, Assignment head) {

		Assignment trimmed = condition.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed).getProb(head);
		}
		else  {
			for (String condVar : conditionalVars) {
				if (!trimmed.containsVar(condVar)) {
					trimmed.addPair(condVar, ValueFactory.none());
				}
			}
			if (table.containsKey(trimmed)) {
				return table.get(trimmed).getProb(head);
			}
		}
		return 0.0f;
	}


	/**
	 * Returns the probability P(head).  If the specified distribution contains
	 * conditional variables, they are marginalised.
	 * 
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	public double getProb(Assignment head) {
		double totalProb = 0.0f;
		for (Assignment condition : table.keySet()) {
			totalProb = getProb(condition, head);
		}
		return totalProb;
	}


	/**
	 * Returns whether the distribution has a well-defined probability for the
	 * given conditional and head assignments
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return true if the distribution defines a probability for the value, and
	 *         false otherwise
	 */
	public boolean hasProb(Assignment condition, Assignment head) {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		boolean result = (table.containsKey(trimmed) && table.get(trimmed).hasProb(head));
		return result;
	}



	/**
	 * Returns true if the table contains a distribution for the given assignment of
	 * conditional variables, and false otherwise
	 * 
	 * @param condition the conditional assignment
	 * @return true if the table contains a distribution, false otherwise
	 */
	public boolean hasProbTable(Assignment condition) {
		return table.containsKey(condition);
	}


	/**
	 * Returns whether the distribution has a well-defined probability for the
	 * given assignment (assuming no conditional variables)
	 * 
	 * @param head the head assignment
	 * @return true if the distribution defines a probability for the value, and
	 *         false otherwise
	 */
	public boolean hasProb(Assignment head) {
		return (table.containsKey(new Assignment()) && table.get(new Assignment()).hasProb(head));
	}



	/**
	 * Returns a flat table mapping complete variable assignments (for both the
	 * conditional and the head variables) to a probability value. 
	 * 
	 * <p>This flat table corresponds to the "factor matrix" structure used in 
	 * e.g. variable elimination.
	 * 
	 * @return the flattened probability table
	 */
	public Map<Assignment,Double> getFlatTable() {
		Map<Assignment,Double> flatTable = new HashMap<Assignment,Double>();
		for (Assignment condition : table.keySet()) {
			for (Assignment head : table.get(condition).getRows()) {
				Assignment flatValue = new Assignment(condition, head);
				flatTable.put(flatValue, table.get(condition).getProb(head));
			}
		}
		return flatTable;
	}
	
	

	public ProbDistribution getPartialPosterior (Assignment condition) {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}
		
		ConditionalCategoricalTable newDistrib = new ConditionalCategoricalTable();
		for (Assignment a : table.keySet()) {
			if (a.consistentWith(condition)) {
				Assignment remaining = a.getTrimmedInverse(condition.getVariables());
				if (!newDistrib.table.containsKey(remaining)) {
					newDistrib.addDistrib(remaining, table.get(a));
				}
				else {
					log.warning("inconsistent results for partial posterior");
				}
			}
		}
		return newDistrib;
	}
	

	// ===================================
	//  UTILITIES
	// ===================================


	/**
	 * Returns a string representation of the probability table
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment cond: table.keySet()) {
			for (Assignment head: table.get(cond).getRows()) {
				String prob = StringUtils.getShortForm(table.get(cond).getProb(head));
				if (cond.size() > 0) {
					str += "P(" + head + " | " + cond 
							+ "):="  + prob + "\n";
				}
				else {
					str += "P(" + head + "):="  + prob + "\n";
				}
			}
		}

		return str;
	}
	
	/**
	 * Returns the hashcode for the distribution
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	/**
	 * Returns the distribution
	 */
	public ConditionalCategoricalTable toDiscrete() {
		return this;
	}
		
	
	/**
	 * Copies the distribution
	 */
	public ConditionalCategoricalTable copy() {
		ConditionalCategoricalTable newC = new ConditionalCategoricalTable();
		try {
		for (Assignment a : table.keySet()) {
			newC.addRows(a, getPosterior(a));
		}
		}
		catch (DialException e) {
			log.warning("copy problem: " + e);
		}
		return newC;
	}

}
