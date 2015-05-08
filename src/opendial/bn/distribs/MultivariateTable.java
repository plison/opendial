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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.utils.InferenceUtils;
import opendial.utils.StringUtils;

/**
 * Representation of a multivariate categorical table P(X1,...Xn), where X1,...Xn are
 * random variables.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class MultivariateTable implements MultivariateDistribution {

	// logger
	public static Logger log = new Logger("MultivariateTable", Logger.Level.DEBUG);

	// the head variables
	Set<String> headVars;

	// the probability table
	Map<Assignment, Double> table;

	// probability intervals (used for binary search in sampling)
	Intervals<Assignment> intervals;

	// sampler
	Random sampler;

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new probability table, with no values
	 */
	public MultivariateTable() {
		table = new HashMap<Assignment, Double>(5);
		headVars = new HashSet<String>();
		sampler = new Random();
	}

	/**
	 * Constructs a new probability table with a mapping between head variable
	 * assignments and probability values. The construction assumes that the
	 * distribution does not have any conditional variables.
	 * 
	 * @param headTable the mapping to fill the table
	 */
	public MultivariateTable(Map<Assignment, Double> headTable) {
		this();
		double totalProb = 0.0;
		for (Assignment a : headTable.keySet()) {
			addRow(a, headTable.get(a));
			totalProb += headTable.get(a);
		}
		if (totalProb < 0.99999) {
			incrementRow(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
	}

	/**
	 * Constructs a new multivariate table from a univariate table.
	 * 
	 * @param headTable the univariate table.
	 */
	public MultivariateTable(CategoricalTable headTable) {
		this();
		double totalProb = 0.0;
		String variable = headTable.getVariable();
		for (Value a : headTable.getValues()) {
			double prob = headTable.getProb(a);
			addRow(new Assignment(variable, a), prob);
			totalProb += prob;
		}
		if (totalProb < 0.99999) {
			incrementRow(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
	}

	/**
	 * Create a categorical table with a unique value with probability 1.0.
	 * 
	 * @param uniqueValue the unique value for the table
	 */
	public MultivariateTable(Assignment uniqueValue) {
		this();
		addRow(uniqueValue, 1.0);
	}

	/**
	 * Adds a new row to the probability table, assuming no conditional assignment.
	 * If the table already contains a probability, it is erased.
	 * 
	 * @param head the assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public synchronized void addRow(Assignment head, double prob) {

		if (prob < 0.0f || prob > 1.02f) {
			return;
		}

		headVars.addAll(head.getVariables());

		table.put(head, prob);

		double totalProb = countTotalProb();
		if (totalProb < 0.98) {
			table.put(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
		else {
			table.remove(Assignment.createDefault(headVars));
		}
	}

	/**
	 * Increments the probability specified in the table for the given head
	 * assignment. If none exists, simply assign the probability.
	 * 
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Assignment head, double prob) {
		if (table.containsKey(head)) {
			if (head.equals(Assignment.createDefault(headVars))) {
				return;
			}
			addRow(head, table.get(head) + prob);
		}
		else {
			addRow(head, prob);
		}
	}

	/**
	 * Add a new set of rows to the probability table.
	 * 
	 * @param heads the mappings (head assignment, probability value)
	 */
	public synchronized void addRows(Map<Assignment, Double> heads) {
		for (Assignment head : heads.keySet()) {
			addRow(head, heads.get(head));
		}
	}

	/**
	 * Extend all rows in the table with the given value assignment
	 * 
	 * @param assign the value assignment
	 */
	public synchronized void extendRows(Assignment assign) {
		Map<Assignment, Double> newTable = new HashMap<Assignment, Double>();
		for (Assignment row : table.keySet()) {
			newTable.put(new Assignment(row, assign), table.get(row));
		}
		table = newTable;
	}

	/**
	 * Removes a row from the table.
	 * 
	 * @param head head assignment
	 */
	public synchronized void removeRow(Assignment head) {

		table.remove(head);

		double totalProb = countTotalProb();
		if (totalProb < 0.99999 && !head.isDefault()) {
			table.put(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the rows of the table
	 * 
	 * @return the table rows
	 */
	@Override
	public Set<Assignment> getValues() {
		return table.keySet();
	}

	/**
	 * Returns the probability P(head).
	 * 
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	@Override
	public double getProb(Assignment head) {
		if (headVars.isEmpty() && !head.isEmpty()) {
			return 0.0;
		}
		Assignment trimmedHead = head.getTrimmed(headVars);

		if (table.containsKey(trimmedHead)) {
			return table.get(trimmedHead);
		}

		return 0.0f;
	}

	/**
	 * Returns the marginal distribution P(Xi) for a random variable Xi in X1,...Xn.
	 * 
	 * @param variable the variable Xi
	 * @return the distribution P(Xi).
	 */
	@Override
	public CategoricalTable getMarginal(String variable) {
		CategoricalTable marginal = new CategoricalTable(variable);

		for (Assignment row : getValues()) {
			double prob = table.get(row);
			if (prob > 0.0) {
				marginal.addRow(row.getValue(variable), prob);
			}
		}
		return marginal;
	}

	/**
	 * returns true if the table contains a probability for the given assignment
	 * 
	 * @param head the assignment
	 * @return true if the table contains a row for the assignment, false otherwise
	 */
	public boolean hasProb(Assignment head) {
		Assignment trimmedHead = head.getTrimmed(headVars);
		return table.containsKey(trimmedHead);
	}

	/**
	 * Sample an assignment from the distribution. If no assignment can be sampled
	 * (due to e.g. an ill-formed distribution), returns an empty assignment.
	 * 
	 * @return the sampled assignment
	 * @throws DialException if no assignment could be sampled
	 */
	@Override
	public Assignment sample() throws DialException {

		if (intervals == null) {
			intervals = new Intervals<Assignment>(table);
		}
		if (intervals.isEmpty()) {
			log.warning("interval is empty, table: " + table);
			return new Assignment();
		}

		return intervals.sample();
	}

	/**
	 * Returns the set of variable labels used in the table
	 * 
	 * @return the variable labels in the table
	 */
	@Override
	public Set<String> getVariables() {
		return new HashSet<String>(headVars);
	}

	/**
	 * Returns true if the table is empty (or contains only a default assignment),
	 * false otherwise
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		if (table.isEmpty()) {
			return true;
		}
		else
			return (table.size() == 1 && table.keySet().iterator().next()
					.equals(Assignment.createDefault(headVars)));
	}

	/**
	 * Returns a subset of the N values in the table with the highest probability.
	 * 
	 * @param nbest the number of values to select
	 * @return the distribution with the subset of values
	 */
	public MultivariateTable getNBest(int nbest) {

		Map<Assignment, Double> filteredTable = InferenceUtils
				.getNBest(table, nbest);
		return new MultivariateTable(filteredTable);
	}

	/**
	 * Returns the most likely assignment of values in the table. If none could be
	 * found, returns an empty assignment.
	 * 
	 * @return the assignment with highest probability
	 */
	@Override
	public Assignment getBest() {
		if (table.size() > 0) {
			MultivariateTable nbest = getNBest(1);
			if (nbest.getValues().size() > 1) {
				nbest.removeRow(Assignment.createDefault(nbest.getVariables()));
			}
			return nbest.getValues().iterator().next();
		}
		else {
			log.warning("table is empty, cannot extract best value");
			return new Assignment();
		}
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Modifies the variable identifiers.
	 * 
	 * @param oldVarId the old identifier to replace
	 * @param newVarId the new identifier
	 */
	@Override
	public void modifyVariableId(String oldVarId, String newVarId) {
		Map<Assignment, Double> newTable = new HashMap<Assignment, Double>();

		for (Assignment head : table.keySet()) {
			Assignment newHead = head.copy();
			if (head.containsVar(oldVarId)) {
				Value condVal = newHead.removePair(oldVarId);
				newHead.addPair(newVarId, condVal);
			}
			newTable.put(newHead, table.get(head));
		}

		if (headVars.contains(oldVarId)) {
			headVars.remove(oldVarId);
			headVars.add(newVarId);
		}

		table = newTable;
		intervals = null;
	}

	/**
	 * Returns the hashcode for the table.
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	/**
	 * Returns true if the probability table is well-formed. The method checks that
	 * all possible assignments for the condition and head parts are covered in the
	 * table, and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {

		// checks that the total probability is roughly equal to 1.0f
		double totalProb = countTotalProb()
				+ getProb(Assignment.createDefault(headVars));
		if (totalProb < 0.9f || totalProb > 1.1f) {
			log.debug("total probability is " + totalProb);
			return false;
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

		Map<Assignment, Double> sortedTable = InferenceUtils.getNBest(table,
				Math.max(table.size(), 1));

		String str = "";
		for (Entry<Assignment, Double> entry : sortedTable.entrySet()) {
			String prob = StringUtils.getShortForm(entry.getValue());
			str += "P(" + entry.getKey() + "):=" + prob + "\n";
		}

		return (str.length() > 0) ? str.substring(0, str.length() - 1) : str;
	}

	/**
	 * Prunes all table values that have a probability lower than the threshold.
	 * 
	 * @param threshold the threshold
	 */
	@Override
	public boolean pruneValues(double threshold) {
		boolean changed = false;
		Map<Assignment, Double> newTable = new HashMap<Assignment, Double>();
		for (Assignment row : table.keySet()) {
			double prob = table.get(row);
			if (prob >= threshold) {
				newTable.put(row, prob);
			}
			else {
				changed = true;
			}
		}
		table = newTable;
		return changed;
	}

	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public MultivariateTable copy() {
		MultivariateTable tableCopy = new MultivariateTable();
		for (Assignment head : table.keySet()) {
			tableCopy.addRow(head.copy(), table.get(head));
		}
		return tableCopy;
	}

	/**
	 * Returns itself.
	 */
	@Override
	public MultivariateTable toDiscrete() {
		return this;
	}

	// ===================================
	// PRIVATE METHODS
	// ===================================

	/**
	 * Returns the total accumulated probability for the distribution P(.|condition)
	 * 
	 * @return the total probability
	 */
	private double countTotalProb() {
		double totalProb = 0.0f;
		Assignment defaultA = Assignment.createDefault(headVars);
		for (Assignment head : table.keySet()) {
			if (!defaultA.equals(head)) {
				totalProb += table.get(head);
			}
		}
		return totalProb;
	}

}
