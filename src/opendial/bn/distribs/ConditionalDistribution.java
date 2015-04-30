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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.utils.CombinatoricsUtils;

/**
 * Conditional probability distribution represented as a probability table. The
 * table expresses a generic distribution of type P(X|Y1...Yn), where X is
 * called the "head" random variable, and Y1...Yn the conditional random
 * variables..
 * 
 * <p>
 * This class represent a generic conditional distribution in which the
 * distribution for the head variable X can be represented using arbitrary
 * distributions of type T.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ConditionalDistribution<T extends IndependentProbDistribution>
		implements ProbDistribution {

	// logger
	public static Logger log = new Logger("ConditionalDistribution",
			Logger.Level.DEBUG);

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
	 * Modifies the distribution table by replace the old variable identifier by
	 * the new one
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
	 * @throws DialException if distrib relates to a different random variable
	 */
	public void addDistrib(Assignment condition, T distrib)
			throws DialException {
		table.put(condition, distrib);
		if (!distrib.getVariable().equals(this.headVar)) {
			throw new DialException("Variable is " + this.headVar + ", not "
					+ distrib.getVariable());
		}
		conditionalVars.addAll(condition.getVariables());
	}

	/**
	 * Prunes from the table all values whose probability falls below the
	 * threshold
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
	 * Sample a head assignment from the distribution P(head|condition), given
	 * the condition. If no assignment can be sampled (due to e.g. an ill-formed
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 * @throws DialException if the sample could not be extracted given the
	 *             condition
	 */
	@Override
	public Value sample(Assignment condition) throws DialException {

		Assignment trimmed = (conditionalVars.containsAll(condition
				.getVariables())) ? condition : condition
				.getTrimmed(conditionalVars);

		if (table.containsKey(trimmed)) {
			return table.get(trimmed).sample();
		}

		log.debug("could not find the distribution for " + condition
				+ " (vars: " + conditionalVars + ", distribution is "
				+ toString() + ")");

		return ValueFactory.none();
	}

	/**
	 * Returns the probability of the head assignment given the conditional
	 * assignment. The method assumes that the posterior distribution has a
	 * discrete form.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 * @throws DialException if the probability could not be extracted
	 */
	@Override
	public double getProb(Assignment condition, Value head)
			throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed).getProb(head);
		} else {
			log.warning("could not find the corresponding condition for "
					+ condition + ")");
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
	public T getProbDistrib(Assignment condition) throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		} else {
			throw new DialException("could not find the corresponding "
					+ "condition for " + condition + " in " + toString());
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
	public ProbDistribution getPosterior(Assignment condition)
			throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}

		ConditionalDistribution<T> newDistrib = new ConditionalDistribution<T>(
				headVar);
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

	/**
	 * Returns all possible values specified in the table. The input values are
	 * here ignored (for efficiency reasons), so the method simply extracts all
	 * possible head rows in the table.
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
		ConditionalDistribution<T> newTable = new ConditionalDistribution<T>(
				headVar);
		for (Assignment condition : table.keySet()) {
			try {
				newTable.addDistrib(condition, (T) table.get(condition).copy());
			} catch (DialException e) {
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
			String distribString = table.get(condition).toString();
			Pattern p = Pattern.compile("PDF\\((.)*\\)=");
			Matcher m = p.matcher(distribString);
			while (m.find()) {
				String toreplace = m.group();
				distribString = distribString.replace(toreplace,
						toreplace.substring(0, toreplace.length() - 2) + "|"
								+ condition + ")=");
			}
			s += distribString + "\n";
		}
		return s;
	}

	/**
	 * Returns true if the probability table is well-formed. The method checks
	 * that all possible assignments for the condition and head parts are
	 * covered in the table, and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {

		// checks that all possible conditional assignments are covered in the
		// table
		Map<String, Set<Value>> possibleCondPairs = CombinatoricsUtils
				.extractPossiblePairs(table.keySet());

		if (CombinatoricsUtils.getNbCombinations(possibleCondPairs) < 100) {
			// Note that here, we only check on the possible assignments in the
			// distribution itself
			// but a better way to do it would be to have it on the actual
			// values given by the input nodes
			// but would require the distribution to have some access to it.
			Set<Assignment> possibleCondAssignments = CombinatoricsUtils
					.getAllCombinations(possibleCondPairs);
			possibleCondAssignments.remove(new Assignment());
			if (possibleCondAssignments.size() != table.keySet().size()
					&& possibleCondAssignments.size() > 1) {
				log.warning("number of possible conditional assignments: "
						+ possibleCondAssignments.size()
						+ ", but number of actual conditional assignments: "
						+ table.keySet().size());
				log.debug("possible conditional assignments: "
						+ possibleCondAssignments);
				log.debug("actual assignments: " + table.keySet());
				return false;
			}
		}

		for (Assignment condition : table.keySet()) {
			if (!table.get(condition).isWellFormed()) {
				log.debug(table.get(condition) + " is ill-formed");
				return false;
			}
		}

		return true;
	}

}
