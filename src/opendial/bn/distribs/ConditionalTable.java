// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.bn.distribs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.StringUtils;

/**
 * Traditional probability distribution represented as a discrete probability
 * table. The table expresses a generic distribution of type P(X | Y1, ..., Yn),
 * where X is called the "head" random variable, and Y1, ..., Yn the conditional
 * random variables.
 * 
 * <p>
 * Technically, the table is expressed as a double mapping, where each
 * assignment for Y1, ..., Yn is mapped to a categorical table where the
 * assignments for X are associated with a given probability value.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ConditionalTable extends ConditionalDistribution<CategoricalTable> {

	// logger
	public static Logger log = new Logger("ConditionalTable",
			Logger.Level.DEBUG);

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new conditional categorical table with the given variable
	 * name.
	 * 
	 * @param headVar the variable name
	 */
	public ConditionalTable(String headVar) {
		super(headVar);
	}

	/**
	 * Adds a new row to the probability table, given the conditional
	 * assignment, the head assignment and the probability value. If the table
	 * already contains a probability, it is erased.
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the value for the head variable
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, Value head, double prob) {

		if (prob < 0.0f || prob > 1.05f) {
			log.warning("probability is not well-formed: " + prob);
			return;
		}

		if (!table.containsKey(condition)) {
			table.put(condition, new CategoricalTable(headVar));
		}

		conditionalVars.addAll(condition.getVariables());

		table.get(condition).addRow(head, prob);
	}

	/**
	 * Adds a new row to the probability table, given the conditional
	 * assignment, the head assignment and the probability value. If the table
	 * already contains a probability, it is erased.
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the value for the head variable (as a string)
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, String head, double prob) {
		addRow(condition, ValueFactory.create(head), prob);
	}

	/**
	 * Adds a new row to the probability table, given the conditional
	 * assignment, the head assignment and the probability value. If the table
	 * already contains a probability, it is erased.
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the value for the head variable (as a double)
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, double head, double prob) {
		addRow(condition, ValueFactory.create(head), prob);
	}

	/**
	 * Adds a new row to the probability table, given the conditional
	 * assignment, the head assignment and the probability value. If the table
	 * already contains a probability, it is erased.
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param head the value for the head variable (as a boolean)
	 * @param prob the associated probability
	 */
	public void addRow(Assignment condition, boolean head, double prob) {
		addRow(condition, ValueFactory.create(head), prob);
	}

	/**
	 * Increments the probability specified in the table for the given condition
	 * and head assignments. If none exists, simply assign the probability.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Assignment condition, Value head, double prob) {
		if (table.containsKey(condition) && table.get(condition).hasProb(head)) {
			addRow(condition, head, table.get(condition).getProb(head) + prob);
		} else {
			addRow(condition, head, prob);
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
		conditionalVars.addAll(condition.getVariables());
		table.put(condition, heads);
	}

	/**
	 * Add a new set of rows to the probability table, given the mapping between
	 * conditional assignments and (value assignment, prob) pairs
	 * 
	 * @param fullTable the full probability table to insert
	 */
	public void addRows(Map<Assignment, Map<Value, Double>> fullTable) {
		for (Assignment cond : fullTable.keySet()) {
			addRows(cond, new CategoricalTable(headVar,fullTable.get(cond)));
		}
	}

	/**
	 * Removes a row from the table, given the condition and the head
	 * assignments.
	 * 
	 * @param condition conditional assignment
	 * @param head head assignment
	 */
	public void removeRow(Assignment condition, Value head) {
		if (table.containsKey(condition)) {
			table.get(condition).removeRow(head);
		} else {
			log.debug("cannot remove row: condition " + condition
					+ " is not present");
		}
	}

	/**
	 * Fill the "conditional holes" of the distribution -- that is, the possible
	 * conditional assignments Y1,..., Yn that are not associated with any
	 * distribution P(X1,...,Xn | Y1,...,Yn) in the table. The method create a
	 * default assignment X1=None,... Xn=None with probability 1.0 for these
	 * cases.
	 */
	public void fillConditionalHoles() {
		if (conditionalVars.size() <= 1) {
			return;
		}
		Map<String, Set<Value>> possibleCondPairs = CombinatoricsUtils
				.extractPossiblePairs(table.keySet());
		if (CombinatoricsUtils.getNbCombinations(possibleCondPairs) < 500) {
			Set<Assignment> possibleCondAssignments = CombinatoricsUtils
					.getAllCombinations(possibleCondPairs);
			possibleCondAssignments.remove(new Assignment());

			for (Assignment possibleCond : possibleCondAssignments) {
				if (!table.containsKey(possibleCond)) {
					addRow(possibleCond, ValueFactory.none(), 1.0);
				}
			}
		}
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the probability P(head|condition), if any is specified. Else,
	 * returns 0.0f.
	 *
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	@Override
	public double getProb(Assignment condition, Value head) {

		Assignment trimmed = condition.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed).getProb(head);
		} else {
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
	 * Returns the probability P(head). If the specified distribution contains
	 * conditional variables, they are marginalised.
	 * 
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	public double getMarginalProb(Value head) {
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
	public boolean hasProb(Assignment condition, Value head) {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		boolean result = (table.containsKey(trimmed) && table.get(trimmed)
				.hasProb(head));
		return result;
	}

	/**
	 * Returns true if the table contains a distribution for the given
	 * assignment of conditional variables, and false otherwise
	 * 
	 * @param condition the conditional assignment
	 * @return true if the table contains a distribution, false otherwise
	 */
	public boolean hasProbTable(Assignment condition) {
		return table.containsKey(condition);
	}

	/**
	 * Returns a flat table mapping complete variable assignments (for both the
	 * conditional and the head variables) to a probability value.
	 * 
	 * <p>
	 * This flat table corresponds to the "factor matrix" structure used in e.g.
	 * variable elimination.
	 * 
	 * @return the flattened probability table
	 */
	public Map<Assignment, Double> getFlatTable() {
		Map<Assignment, Double> flatTable = new HashMap<Assignment, Double>();
		for (Assignment condition : table.keySet()) {
			for (Value head : table.get(condition).getValues()) {
				Assignment flatValue = new Assignment(condition, headVar, head);
				flatTable.put(flatValue, table.get(condition).getProb(head));
			}
		}
		return flatTable;
	}

	/**
	 * Returns the posterior distribution obtained by integrating the (possibly
	 * partial) conditional assignment.
	 * 
	 * @param condition assignment on a subset of the conditional variables
	 * @return the resulting probability distribution.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition)
			throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}

		ConditionalTable newDistrib = new ConditionalTable(headVar);
		for (Assignment a : table.keySet()) {
			if (a.consistentWith(condition)) {
				Assignment remaining = a.getTrimmedInverse(condition
						.getVariables());
				if (!newDistrib.table.containsKey(remaining)) {
					newDistrib.addDistrib(remaining, table.get(a));
				} else {
					log.warning("inconsistent results for partial posterior");
				}
			}
		}
		return newDistrib;
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns a string representation of the probability table
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment cond : table.keySet()) {
			for (Value head : table.get(cond).getValues()) {
				String prob = StringUtils.getShortForm(table.get(cond).getProb(
						head));
				if (cond.size() > 0) {
					str += "P(" + headVar + "=" + head + " | " + cond + "):="
							+ prob + "\n";
				} else {
					str += "P(" + headVar + "=" + head + "):=" + prob + "\n";
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
	 * Copies the distribution
	 */
	@Override
	public ConditionalTable copy() {
		ConditionalTable newC = new ConditionalTable(headVar);
		for (Assignment cond : table.keySet()) {
			newC.addRows(cond, table.get(cond).copy());
		}
		return newC;
	}

}
