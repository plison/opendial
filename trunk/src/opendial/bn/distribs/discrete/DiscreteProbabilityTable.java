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
import opendial.bn.Assignment;
import opendial.bn.distribs.AbstractProbabilityTable;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.DistanceUtils;

/**
 * Traditional probability distribution represented as a discrete probability table.  
 * The table expresses a generic distribution of type P(X1...Xn|Y1...Yn), where X1...Xn 
 * is called the "head" part of the distribution, and Y1...Yn the conditional part.
 * 
 * <p>Technically, the table is expressed as a double mapping, where each assignment for
 * Y1...Yn is mapped to a table where the assignments for X1...Xn are associated with a 
 * given probability value.  If the distribution contains no conditional part, the assignment
 * for Y1...Yn is empty.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class DiscreteProbabilityTable extends AbstractProbabilityTable<SimpleTable>
implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("DiscreteProbabilityTable", Logger.Level.DEBUG);

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================


	/**
	 * Constructs a new probability table, with no values
	 */
	public DiscreteProbabilityTable() {
		table = new HashMap<Assignment,SimpleTable>();
		conditionalVars = new HashSet<String>();
	}


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
			table.put(condition, new SimpleTable());
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
	public void addRows(Assignment condition, SimpleTable heads) {
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



	public void fillConditionalHoles() {
		Map<String,Set<Value>> possibleCondPairs = 
				CombinatoricsUtils.extractPossiblePairs(table.keySet());
		if (CombinatoricsUtils.getEstimatedNbCombinations(possibleCondPairs) < 100)  {				
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
	@Override
	public double getProb(Assignment condition, Assignment head) {

		Assignment trimmed = condition.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed).getProb(head);
		}
		else  {
			Assignment closest = DistanceUtils.getClosestElement(table.keySet(), trimmed);		
			if (!closest.isEmpty()) {
				return table.get(closest).getProb(head);
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
	 * Returns the probability table associated with the conditional assignment
	 * given as argument. If none can be found, return an empty table.
	 * 
	 * @param condition the assignment on the conditional variables
	 * @return the associated probability table
	 */
	@Override
	public SimpleTable getProbTable(Assignment condition)  {

		Assignment trimmed = condition.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}	
		else {
			Assignment closest = DistanceUtils.getClosestElement(table.keySet(), trimmed);		
			if (!closest.isEmpty()) {
				return table.get(closest);
			}	
		}
		SimpleTable defaultTable = new SimpleTable();
		if (!table.isEmpty()) {
			defaultTable.addRow(Assignment.createDefault(table.values().iterator().next().getHeadVariables()), 1.0);
		}
		return defaultTable;
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


	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>();
		for (SimpleTable subdistrib : table.values()) {
			headVars.addAll(subdistrib.getHeadVariables());
		}
		return headVars;
	}


	// ===================================
	//  UTILITIES
	// ===================================


	/**
	 * Returns a string representation of the probability table
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment cond: table.keySet()) {
			for (Assignment head: table.get(cond).getRows()) {
				double prob = DistanceUtils.shorten(table.get(cond).getProb(head));
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
			return "(probability table too big to be shown)" + toString();
		}
	}



	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public DiscreteProbabilityTable copy() {
		DiscreteProbabilityTable tableCopy = new DiscreteProbabilityTable();
		for (Assignment condition : table.keySet()) {
			SimpleTable newTable = table.get(condition).copy();
			tableCopy.addRows(condition.copy(), newTable);
		}
		return tableCopy;
	}

	/**
	 * Returns the same distribution instance
	 * 
	 * @return the same instance
	 */
	@Override
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}


	/**
	 * Returns the continuous equivalent of the distribution
	 *
	 * @return the continuous distribution
	 * @throws DialException 
	 */
	@Override
	public ContinuousProbabilityTable toContinuous() throws DialException {
		ContinuousProbabilityTable newTable = new ContinuousProbabilityTable();
		for (Assignment a : table.keySet()) {
			newTable.addDistrib(a, table.get(a).toContinuous());
		}
		return newTable;
	}




}
