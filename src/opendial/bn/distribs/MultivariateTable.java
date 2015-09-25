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

import java.util.logging.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.Intervals;
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
	final static Logger log = Logger.getLogger("OpenDial");

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
	 * Constructs a new probability table with a mapping between head variable
	 * assignments and probability values. The construction assumes that the
	 * distribution does not have any conditional variables.
	 * 
	 * @param headVars the variables in the table
	 * @param headTable the mapping to fill the table
	 */
	protected MultivariateTable(Set<String> headVars,
			Map<Assignment, Double> headTable) {
		this.headVars = headVars;
		this.table = headTable;
	}

	/**
	 * Constructs a new multivariate table from a univariate table.
	 * 
	 * @param headTable the univariate table.
	 */
	public MultivariateTable(CategoricalTable headTable) {
		this.headVars = new HashSet<String>(Arrays.asList(headTable.getVariable()));
		this.table = new HashMap<Assignment, Double>();
		String variable = headTable.getVariable();
		for (Value a : headTable.getValues()) {
			double prob = headTable.getProb(a);
			table.put(new Assignment(variable, a), prob);
		}
	}

	/**
	 * Create a categorical table with a unique value with probability 1.0.
	 * 
	 * @param uniqueValue the unique value for the table
	 */
	public MultivariateTable(Assignment uniqueValue) {
		this.headVars = uniqueValue.getVariables();
		this.table = new HashMap<Assignment, Double>();
		this.table.put(uniqueValue, 1.0);
	}

	/**
	 * Extend all rows in the table with the given value assignment
	 * 
	 * @param assign the value assignment
	 */
	public void extendRows(Assignment assign) {
		Map<Assignment, Double> newTable = new HashMap<Assignment, Double>();
		for (Assignment row : table.keySet()) {
			newTable.put(new Assignment(row, assign), table.get(row));
		}
		table = newTable;
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
	public IndependentDistribution getMarginal(String variable) {
		CategoricalTable.Builder marginal = new CategoricalTable.Builder(variable);

		for (Assignment row : getValues()) {
			double prob = table.get(row);
			if (prob > 0.0) {
				marginal.addRow(row.getValue(variable), prob);
			}
		}
		return marginal.build();
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
	 */
	@Override
	public Assignment sample() {

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

		Map<Assignment, Double> filteredTable =
				InferenceUtils.getNBest(table, nbest);
		return new MultivariateTable(headVars, filteredTable);
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
			double maxprob = -10;
			Assignment maxVal = null;
			for (Assignment v : table.keySet()) {
				double prob = table.get(v);
				if (prob > maxprob) {
					maxprob = prob;
					maxVal = v;
				}
			}
			return (maxVal != null) ? maxVal : Assignment.createDefault(headVars);
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
	 * Returns a string representation of the probability table
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {

		Map<Assignment, Double> sortedTable =
				InferenceUtils.getNBest(table, Math.max(table.size(), 1));

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
		Builder tableCopy = new Builder();
		for (Assignment head : table.keySet()) {
			tableCopy.addRow(head.copy(), table.get(head));
		}
		return tableCopy.build();
	}

	/**
	 * Returns itself.
	 */
	@Override
	public MultivariateTable toDiscrete() {
		return this;
	}

	// ===================================
	// BUILDER CLASS
	// ===================================

	/**
	 * Builder for the multivariate table.
	 *
	 */
	public static class Builder {

		// the head variables
		Set<String> headVars;

		// the probability table
		Map<Assignment, Double> table;

		public Builder() {
			table = new HashMap<Assignment, Double>(5);
			headVars = new HashSet<String>();
		}

		/**
		 * Adds a new row to the probability table, assuming no conditional
		 * assignment. If the table already contains a probability, it is erased.
		 * 
		 * @param head the assignment for X1...Xn
		 * @param prob the associated probability
		 */
		public void addRow(Assignment head, double prob) {

			if (prob < 0.0f || prob > 1.02f) {
				return;
			}

			headVars.addAll(head.getVariables());

			table.put(head, prob);
		}

		/**
		 * Increments the probability specified in the table for the given head
		 * assignment. If none exists, simply assign the probability.
		 * 
		 * @param head the head assignment
		 * @param prob the probability increment
		 */
		public void incrementRow(Assignment head, double prob) {
			addRow(head, table.getOrDefault(head, 0.0) + prob);
		}

		/**
		 * Add a new set of rows to the probability table.
		 * 
		 * @param heads the mappings (head assignment, probability value)
		 */
		public void addRows(Map<Assignment, Double> heads) {
			for (Assignment head : heads.keySet()) {
				addRow(head, heads.get(head));
			}
		}

		/**
		 * Removes a row from the table.
		 * 
		 * @param head head assignment
		 */
		public void removeRow(Assignment head) {
			table.remove(head);
		}

		/**
		 * Returns true if the probability table is well-formed. The method checks
		 * that all possible assignments for the condition and head parts are covered
		 * in the table, and that the probabilities add up to 1.0f.
		 * 
		 * @return true if the table is well-formed, false otherwise
		 */
		public boolean isWellFormed() {

			// checks that the total probability is roughly equal to 1.0f
			double totalProb = table.values().stream().mapToDouble(d -> d).sum();
			if (totalProb < 0.9f || totalProb > 1.1f) {
				log.fine("total probability is " + totalProb);
				return false;
			}

			return true;
		}

		/**
		 * Normalises the table
		 */
		public void normalise() {
			table = InferenceUtils.normalise(table);
		}

		/**
		 * Builds the multivariate table
		 * 
		 * @return the corresponding table
		 */
		public MultivariateTable build() {
			double totalProb = table.values().stream().mapToDouble(d -> d).sum();
			if (totalProb < 0.99) {
				Assignment def = Assignment.createDefault(headVars);
				incrementRow(def, (1 - totalProb));
			}
			else {
				table = InferenceUtils.normalise(table);
			}
			return new MultivariateTable(headVars, table);
		}

	}
}
