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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.utils.InferenceUtils;
import opendial.utils.StringUtils;

/**
 * Utility table that is empirically constructed from a set of samples. The table is
 * defined via a mapping from assignment to utility estimates.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class UtilityTable implements UtilityFunction {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// mapping between assignments and estimates of the utility value
	Map<Assignment, UtilityEstimate> table;

	// the variables of the table
	Set<String> variables;

	// ===================================
	// CONSTRUCTION METHODS
	// ===================================

	/**
	 * Creates a new, empty empirical utility table
	 */
	public UtilityTable() {
		table = new HashMap<Assignment, UtilityEstimate>();
		variables = new HashSet<String>();
	}

	/**
	 * Constructs a new utility distribution, given the values provided as argument
	 * 
	 * @param values the values
	 */
	public UtilityTable(Map<Assignment, Double> values) {
		this();
		for (Assignment a : values.keySet()) {
			setUtil(a, values.get(a));
		}
	}

	/**
	 * Adds a new utility value to the estimated table
	 * 
	 * @param sample the sample assignment
	 * @param utility the utility value for the sample
	 */
	public void incrementUtil(Assignment sample, double utility) {
		if (!table.containsKey(sample)) {
			table.put(new Assignment(sample), new UtilityEstimate(utility));
		}
		else {
			table.get(new Assignment(sample)).update(utility);
		}
		variables.addAll(sample.getVariables());
	}

	/**
	 * Sets the utility associated with a value assignment
	 * 
	 * @param input the value assignment for the input nodes
	 * @param utility the resulting utility
	 */
	public void setUtil(Assignment input, double utility) {
		table.put(input, new UtilityEstimate(utility));
		variables.addAll(input.getVariables());
	}

	/**
	 * Removes a utility from the utility distribution
	 * 
	 * @param input the assignment associated with the utility to be removed
	 */
	public void removeUtil(Assignment input) {
		table.remove(input);
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the estimated utility for the given assignment
	 * 
	 * @param input the assignment
	 * @return the utility for the assignment
	 */
	@Override
	public double getUtil(Assignment input) {

		if (input.size() != variables.size()) {
			input = input.getTrimmed(variables);
		}
		if (table.containsKey(input)) {
			return table.get(input).getValue();
		}
		return 0.0f;
	}

	/**
	 * Returns the table reflecting the estimated utility values for each assignment
	 * 
	 * @return the (assignment,utility) table
	 */
	public Map<Assignment, Double> getTable() {
		Map<Assignment, Double> averageUtils =
				new LinkedHashMap<Assignment, Double>();
		for (Assignment a : table.keySet()) {
			averageUtils.put(a, getUtil(a));
		}
		return averageUtils;
	}

	/**
	 * Creates a table with a subset of the utility values, namely the N-best highest
	 * ones.
	 * 
	 * @param nbest the number of values to keep in the filtered table
	 * @return the table of values, of size nbest
	 */
	public UtilityTable getNBest(int nbest) {
		Map<Assignment, Double> filteredTable =
				InferenceUtils.getNBest(getTable(), nbest);
		return new UtilityTable(filteredTable);
	}

	/**
	 * Returns the ranking of the given input sorted by utility
	 * 
	 * @param input the input to rank
	 * @param minDifference the minimum difference between utilities
	 * @return the rank of the given assignment in the utility table
	 */
	public int getRanking(Assignment input, double minDifference) {
		return InferenceUtils.getRanking(getTable(), input, minDifference);
	}

	/**
	 * Returns the entry with the highest utility in the table
	 * 
	 * @return the entry with highest utility
	 */
	public Map.Entry<Assignment, Double> getBest() {
		if (table.isEmpty()) {
			Map<Assignment, Double> newTable = new HashMap<Assignment, Double>();
			newTable.put(new Assignment(), 0.0);
			return newTable.entrySet().iterator().next();
		}
		return getNBest(1).getTable().entrySet().iterator().next();
	}

	/**
	 * Returns the rows of the table
	 * 
	 * @return the raws
	 */
	public Set<Assignment> getRows() {
		return table.keySet();
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns a copy of the utility table
	 * 
	 * @return the copy
	 */
	@Override
	public UtilityTable copy() {
		return new UtilityTable(getTable());
	}

	/**
	 * Returns the hashcode for the utility table
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	/**
	 * Returns a string representation for the distribution
	 * 
	 * @return the string representation for the table.
	 */
	@Override
	public String toString() {

		Map<Assignment, Double> sortedTable =
				InferenceUtils.getNBest(getTable(), table.size());

		String str = "";
		for (Entry<Assignment, Double> entry : sortedTable.entrySet()) {
			str += "U(" + entry.getKey() + "):="
					+ StringUtils.getShortForm(entry.getValue()) + "\n";
		}
		return (str.length() > 0) ? str.substring(0, str.length() - 1) : "";
	}

	/**
	 * Modifies a variable label with a new one
	 * 
	 * @param nodeId the old variable label
	 * @param newId the new label
	 */
	@Override
	public void modifyVariableId(String nodeId, String newId) {
		Map<Assignment, UtilityEstimate> utilities2 =
				new HashMap<Assignment, UtilityEstimate>();
		for (Assignment a : table.keySet()) {
			Assignment b = new Assignment();
			for (String var : a.getVariables()) {
				String newVar = (var.equals(nodeId)) ? newId : var;
				b.addPair(newVar, a.getValue(var));
			}
			utilities2.put(b, table.get(a));
		}
		table = utilities2;
	}

	/**
	 * Estimate of a utility value, defined by the averaged estimate itself and the
	 * number of values that have contributed to it (in order to correctly compute
	 * the average)
	 */
	private class UtilityEstimate {

		// averaged estimate for the utility
		double average = 0.0;

		// number of values used for the average
		int nbValues = 0;

		/**
		 * Creates a new utility estimate, with a first value
		 * 
		 * @param firstValue the first value
		 */
		public UtilityEstimate(double firstValue) {
			update(firstValue);
		}

		/**
		 * Updates the current estimate with a new value
		 * 
		 * @param newValue the new value
		 */
		public void update(double newValue) {
			double prevUtil = average;
			nbValues++;
			average = prevUtil + (newValue - prevUtil) / (nbValues);
		}

		/**
		 * Returns the current (averaged) estimate for the utility
		 * 
		 * @return the estimate
		 */
		public double getValue() {
			if (nbValues > 0) {
				return average;
			}
			else {
				return 0.0;
			}
		}

		/**
		 * Returns the average (as a string)
		 */
		@Override
		public String toString() {
			return "" + average;
		}

		/**
		 * Returns the hashcode for the average.
		 */
		@Override
		public int hashCode() {
			return new Double(average).hashCode();
		}

	}

}
