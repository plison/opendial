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
import java.util.Set;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.utils.StringUtils;

/**
 * Conditional probability distribution represented as a probability table. The table
 * expresses a generic distribution of type P(X|Y1...Yn), where X is called the
 * "head" random variable, and Y1...Yn the conditional random variables..
 * 
 * <p>
 * This class represent a generic conditional distribution in which the distribution
 * for the head variable X can be represented using arbitrary distributions of type
 * T.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ConditionalDistribution<T extends IndependentProbDistribution>
		implements ProbDistribution {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// head variable
	String headVar;

	// conditional variables
	protected Set<String> conditionalVars;

	// the probability table
	protected HashMap<Assignment, T> table;

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new probability table, with no values
	 * 
	 * @param headVar the name of the random variable
	 */
	public ConditionalDistribution(String headVar) {
		table = new HashMap<Assignment, T>();
		this.headVar = headVar;
		conditionalVars = new HashSet<String>();
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
				T distrib = table.remove(condition);
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
	 *            representation)
	 * @throws RuntimeException if distrib relates to a different random variable
	 */
	public void addDistrib(Assignment condition, T distrib) throws RuntimeException {
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
	 * @return the sampled assignment
	 * @throws RuntimeException if the sample could not be extracted given the condition
	 */
	@Override
	public Value sample(Assignment condition) throws RuntimeException {

		Assignment trimmed = condition.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed).sample();
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
	 * @throws RuntimeException if the probability could not be extracted
	 */
	@Override
	public double getProb(Assignment condition, Value head) throws RuntimeException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed).getProb(head);
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
	public IndependentProbDistribution getProbDistrib(Assignment condition)
			throws RuntimeException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
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
	public ProbDistribution getPosterior(Assignment condition) throws RuntimeException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}

		ConditionalDistribution<T> newDistrib =
				new ConditionalDistribution<T>(headVar);
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
	 * @return
	 */
	public Set<Assignment> getConditions() {
		return table.keySet();
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
	@SuppressWarnings("unchecked")
	@Override
	public ConditionalDistribution<T> copy() {
		ConditionalDistribution<T> newTable =
				new ConditionalDistribution<T>(headVar);
		for (Assignment condition : table.keySet()) {
			try {
				newTable.addDistrib(condition, (T) table.get(condition).copy());
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
			T distrib = table.get(condition);
			for (Value head : distrib.getValues()) {
				String prob = StringUtils.getShortForm(distrib.getProb(head));
				if (condition.size() > 0) {
					s +=
							"P(" + headVar + "=" + head + " | " + condition + "):="
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
	 * Returns true if the probability table is well-formed. The method checks that
	 * all possible assignments for the condition and head parts are covered in the
	 * table, and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	public boolean isWellFormed() {

		// checks that all possible assignments are covered in the table
		ValueRange possibleCondPairs = new ValueRange(table.keySet());

		if (possibleCondPairs.getNbCombinations() < 100) {
			Set<Assignment> possibleCondAssignments = possibleCondPairs.linearise();
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
	 * Returns true if the object o is a conditional distribution with the same
	 * content
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConditionalDistribution) {
			Set<Assignment> conditions =
					((ConditionalDistribution) o).table.keySet();
			if (!conditions.equals(table.keySet())) {
				return false;
			}
			for (Assignment c : conditions) {
				T distrib = table.get(c);
				T distrib2 = (T) ((ConditionalDistribution) o).table.get(c);
				if (!distrib.equals(distrib2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
