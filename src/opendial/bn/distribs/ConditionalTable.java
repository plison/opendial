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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.utils.StringUtils;

/**
 * Conditional probability distribution represented as a probability table. The table
 * expresses a generic distribution of type P(X|Y1...Yn), where X is called the
 * "head" random variable, and Y1...Yn the conditional random variables.
 * 
 * <p>
 * Constructing a conditional table should be done via its Builder class: builder =
 * new ConditionalTable.Builder("variable name"); builder.addRow(...); table =
 * builder.build();
 * 
 * <p>
 * This class represent a generic conditional distribution in which the distribution
 * for the head variable X can be represented using arbitrary distributions of type
 * IndependentProbDistribution.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ConditionalTable implements ProbDistribution {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// head variable
	String headVar;

	// conditional variables
	protected Set<String> conditionalVars;

	// the probability table
	protected HashMap<Assignment, IndependentDistribution> table;

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new probability table, with no values
	 * 
	 * @param headVar the name of the random variable
	 */
	public ConditionalTable(String headVar) {
		table = new HashMap<Assignment, IndependentDistribution>();
		this.headVar = headVar;
		conditionalVars = new HashSet<String>();
	}

	/**
	 * Constructs a new probability table, with the values in distribs
	 * 
	 * @param headVar the name of the random variable
	 * @param distribs the distribs (one for each conditional assignment)
	 */
	public ConditionalTable(String headVar,
			Map<Assignment, IndependentDistribution> distribs) {
		table = new HashMap<Assignment, IndependentDistribution>();
		this.headVar = headVar;
		conditionalVars = new HashSet<String>();
		for (Assignment condition : distribs.keySet()) {
			addDistrib(condition, distribs.get(condition));
		}
	}

	/**
	 * Modifies the distribution table by replace the old variable identifier by the
	 * new one
	 * 
	 * @param oldVarId the old variable label
	 * @param newVarId the new variable label
	 */
	@Override
	public void modifyVariableId(String oldVarId, String newVarId) {

		for (Assignment condition : new ArrayList<Assignment>(table.keySet())) {
			table.get(condition).modifyVariableId(oldVarId, newVarId);
			if (condition.containsVar(oldVarId)) {
				IndependentDistribution distrib = table.remove(condition);
				Value v = condition.removePair(oldVarId);
				condition.addPair(newVarId, v);
				table.put(condition, distrib);
			}
		}

		if (conditionalVars.contains(oldVarId)) {
			conditionalVars.remove(oldVarId);
			conditionalVars.add(newVarId);
		}

		if (this.headVar.equals(oldVarId)) {
			this.headVar = newVarId;
		}
	}

	/**
	 * Adds a new continuous probability distribution associated with the given
	 * conditional assignment
	 * 
	 * @param condition the conditional assignment
	 * @param distrib the distribution (in a continuous, function-based
	 *            representation) @ if distrib relates to a different random variable
	 */
	public void addDistrib(Assignment condition, IndependentDistribution distrib) {
		table.put(condition, distrib);
		if (!distrib.getVariable().equals(this.headVar)) {
			throw new RuntimeException("Variable is " + this.headVar + ", not "
					+ distrib.getVariable());
		}
		conditionalVars.addAll(condition.getVariables());
	}

	/**
	 * Prunes from the table all values whose probability falls below the threshold
	 * 
	 * @param threshold the threshold to apply
	 */
	@Override
	public boolean pruneValues(double threshold) {
		boolean changed = false;
		for (Assignment condition : table.keySet()) {
			changed = changed || table.get(condition).pruneValues(threshold);
		}
		return changed;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the name of the random variable
	 * 
	 * @return the (head) random variable
	 */
	@Override
	public String getVariable() {
		return this.headVar;
	}

	/**
	 * Sample a head assignment from the distribution P(head|condition), given the
	 * condition. If no assignment can be sampled (due to e.g. an ill-formed
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment the condition
	 */
	@Override
	public Value sample(Assignment condition) {

		if (condition.size() != conditionalVars.size()) {
			condition = condition.getTrimmed(conditionalVars);
		}
		IndependentDistribution subdistrib = table.get(condition);
		if (subdistrib != null) {
			return subdistrib.sample();
		}

		// log.fine("could not find the distribution for " + condition + " (vars: "
		// + conditionalVars + ", distribution is " + toString() + ")");

		return ValueFactory.none();
	}

	/**
	 * Returns the probability of the head assignment given the conditional
	 * assignment. The method assumes that the posterior distribution has a discrete
	 * form.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Value head) {
		if (condition.size() > conditionalVars.size()) {
			condition = condition.getTrimmed(conditionalVars);
		}
		if (table.containsKey(condition)) {
			return table.get(condition).getProb(head);
		}
		else if (condition.isDefault()) {
			log.warning("void condition cannot be found in " + toString());
			double total = 0.0;
			for (Assignment c : table.keySet()) {
				total += table.get(c).getProb(head);
			}
			return total;
		}
		else {
			// log.warning("could not find the corresponding condition for "
			// + condition + ")");
			return 0.0;
		}
	}

	/**
	 * Returns the (unconditional) probability distribution P(X) given the
	 * conditional assignment.
	 * 
	 * @param condition the conditional assignment
	 * @return the corresponding probability distribution
	 */
	@Override
	public IndependentDistribution getProbDistrib(Assignment condition) {
		if (table.containsKey(condition)) {
			return table.get(condition);
		}
		else {
			return new SingleValueDistribution(headVar, ValueFactory.none());
		}
	}

	/**
	 * Returns the posterior distribution obtained by integrating the (possibly
	 * partial) conditional assignment.
	 * 
	 * @param condition the assignment on a subset of the conditional variables
	 * @return the resulting posterior distribution.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition) {
		if (table.containsKey(condition)) {
			return table.get(condition);
		}
		ConditionalTable newDistrib = new ConditionalTable(headVar);
		for (Assignment a : table.keySet()) {
			if (a.consistentWith(condition)) {
				Assignment remaining = a.getPruned(condition.getVariables());
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

	/**
	 * Returns all possible values specified in the table. The input values are here
	 * ignored (for efficiency reasons), so the method simply extracts all possible
	 * head rows in the table.
	 * 
	 * @return the possible values for the head variables.
	 */
	@Override
	public Set<Value> getValues() {
		Set<Value> headRows = new HashSet<Value>();
		for (Assignment condition : table.keySet()) {
			headRows.addAll(table.get(condition).getValues());
		}
		return headRows;
	}

	/**
	 * Returns the set of possible conditional assignments in the table.
	 * 
	 * @return the set of conditional assignments
	 */
	public Set<Assignment> getConditions() {
		return table.keySet();
	}

	/**
	 * Returns the conditional variables of the table
	 * 
	 * @return the set of conditional variables
	 */
	@Override
	public Set<String> getInputVariables() {
		return conditionalVars;
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns the hashcode for the table.
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	/**
	 * Returns a copy of the probability table
	 * 
	 * @return the copy
	 */
	@Override
	public ConditionalTable copy() {
		ConditionalTable newTable = new ConditionalTable(headVar);
		for (Assignment condition : table.keySet()) {
			try {
				newTable.addDistrib(condition, table.get(condition).copy());
			}
			catch (RuntimeException e) {
				log.warning("Copy error: " + e);
			}
		}
		return newTable;
	}

	/**
	 * Returns a pretty print of the distribution
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		String s = "";
		for (Assignment condition : table.keySet()) {
			IndependentDistribution distrib = table.get(condition);
			for (Value head : distrib.getValues()) {
				String prob = StringUtils.getShortForm(distrib.getProb(head));
				if (condition.size() > 0) {
					s += "P(" + headVar + "=" + head + " | " + condition + "):="
							+ prob + "\n";
				}
				else {
					s += "P(" + headVar + "=" + head + "):=" + prob + "\n";
				}
			}
		}
		return s;
	}

	/**
	 * Returns true if the object o is a conditional distribution with the same
	 * content
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConditionalTable) {
			Set<Assignment> conditions = ((ConditionalTable) o).table.keySet();
			if (!conditions.equals(table.keySet())) {
				return false;
			}
			for (Assignment c : conditions) {
				IndependentDistribution distrib = table.get(c);
				IndependentDistribution distrib2 =
						((ConditionalTable) o).table.get(c);
				if (!distrib.equals(distrib2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Builder class for the conditional table. The builder allows you to add rows to
	 * the table. Once all rows have been added, the resulting table can be created
	 * using the build() method.
	 * 
	 *
	 */
	public static class Builder {

		String headVar;
		Map<Assignment, CategoricalTable.Builder> table;

		/**
		 * Constructs a new conditional categorical table with the given variable
		 * name.
		 * 
		 * @param headVar the variable name
		 */
		public Builder(String headVar) {
			this.headVar = headVar;
			table = new HashMap<Assignment, CategoricalTable.Builder>();
		}

		/**
		 * Adds a new row to the probability table, given the conditional assignment,
		 * the head assignment and the probability value. If the table already
		 * contains a probability, it is erased.
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
				table.put(condition, new CategoricalTable.Builder(headVar));
			}

			table.get(condition).addRow(head, prob);
		}

		/**
		 * Adds a new row to the probability table, given the conditional assignment,
		 * the head assignment and the probability value. If the table already
		 * contains a probability, it is erased.
		 * 
		 * @param condition the conditional assignment for Y1...Yn
		 * @param head the value for the head variable (as a string)
		 * @param prob the associated probability
		 */
		public void addRow(Assignment condition, String head, double prob) {
			addRow(condition, ValueFactory.create(head), prob);
		}

		/**
		 * Adds a new row to the probability table, given the conditional assignment,
		 * the head assignment and the probability value. If the table already
		 * contains a probability, it is erased.
		 * 
		 * @param condition the conditional assignment for Y1...Yn
		 * @param head the value for the head variable (as a double)
		 * @param prob the associated probability
		 */
		public void addRow(Assignment condition, double head, double prob) {
			addRow(condition, ValueFactory.create(head), prob);
		}

		/**
		 * Adds a new row to the probability table, given the conditional assignment,
		 * the head assignment and the probability value. If the table already
		 * contains a probability, it is erased.
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
			if (table.containsKey(condition)) {
				table.get(condition).incrementRow(head, prob);
			}
			else {
				addRow(condition, head, prob);
			}
		}

		/**
		 * Add rows to the conditional table
		 * 
		 * @param condition the condition
		 * @param subtable the table of values for the head distribution
		 */
		public void addRows(Assignment condition, Map<Value, Double> subtable) {
			CategoricalTable.Builder builder = table.computeIfAbsent(condition,
					k -> new CategoricalTable.Builder(headVar));
			builder.addRows(subtable);
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
			}
			else {
				log.fine("cannot remove row: condition " + condition
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
			ValueRange possibleCondPairs = new ValueRange(table.keySet());
			if (possibleCondPairs.getNbCombinations() < 500) {
				Set<Assignment> possibleCondAssignments =
						possibleCondPairs.linearise();
				possibleCondAssignments.remove(new Assignment());

				for (Assignment possibleCond : possibleCondAssignments) {
					if (!table.containsKey(possibleCond)) {
						addRow(possibleCond, ValueFactory.none(), 1.0);
					}
				}
			}
		}

		/**
		 * Returns true if the probability table is well-formed. The method checks
		 * that all possible assignments for the condition and head parts are covered
		 * in the table, and that the probabilities add up to 1.0f.
		 * 
		 * @return true if the table is well-formed, false otherwise
		 */
		public boolean isWellFormed() {

			// checks that all possible assignments are covered in the table
			ValueRange possibleCondPairs = new ValueRange(table.keySet());

			if (possibleCondPairs.getNbCombinations() < 100) {
				Set<Assignment> possibleCondAssignments =
						possibleCondPairs.linearise();
				possibleCondAssignments.remove(new Assignment());
				if (possibleCondAssignments.size() != table.keySet().size()
						&& possibleCondAssignments.size() > 1) {
					log.warning("number of possible conditional assignments: "
							+ possibleCondAssignments.size()
							+ ", but number of actual conditional assignments: "
							+ table.keySet().size());
					log.fine("possible conditional assignments: "
							+ possibleCondAssignments);
					log.fine("actual assignments: " + table.keySet());
					return false;
				}
			}

			return true;
		}

		/**
		 * Normalises the conditional table
		 */
		public void normalise() {
			for (Assignment cond : table.keySet()) {
				table.get(cond).normalise();
			}
		}

		/**
		 * Builds the corresponding probability table. If some conditional tables
		 * have a total probability mass that is less than 1.0, creates a default
		 * None value to cover the remaining mass.
		 * 
		 * @return the corresponding conditional table
		 */
		public ConditionalTable build() {
			Map<Assignment, IndependentDistribution> table2 =
					new HashMap<Assignment, IndependentDistribution>();
			for (Assignment cond : table.keySet()) {
				table2.put(cond, table.get(cond).build());
			}
			return new ConditionalTable(headVar, table2);
		}

	}

}
