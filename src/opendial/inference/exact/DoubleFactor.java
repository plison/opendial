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

package opendial.inference.exact;

import java.util.logging.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Double factor, combining probability and utility distributions
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class DoubleFactor {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the matrix, mapping each assignment to two double values
	// (the probability and the utility)
	Map<Assignment, double[]> matrix;

	// ===================================
	// CONSTRUCTION METHODS
	// ===================================

	/**
	 * Creates a new, empty factor, and a set of head variables
	 */
	public DoubleFactor() {
		matrix = new HashMap<Assignment, double[]>();
	}

	/**
	 * Creates a new factor out of an existing one
	 * 
	 * @param existingFactor the existing factor
	 */
	public DoubleFactor(DoubleFactor existingFactor) {
		matrix = new HashMap<Assignment, double[]>(existingFactor.matrix);
	}

	public void addEntry(Assignment a, double probValue, double utilityValue) {
		matrix.put(a, new double[] { probValue, utilityValue });
	}

	/**
	 * Increment the entry with new probability and utility values
	 * 
	 * @param a the assignment
	 * @param probIncr probability increment
	 * @param utilIncr utility increment
	 */
	public void incrementEntry(Assignment a, double probIncr, double utilIncr) {
		double[] old = matrix.getOrDefault(a, new double[] { 0.0, 0.0 });
		double[] val = new double[] { old[0] + probIncr, old[1] + utilIncr };
		matrix.put(a, val);
	}

	/**
	 * Normalises the factor, assuming no conditional variables in the factor.
	 * 
	 */
	public void normalise() {
		double total = matrix.values().stream().mapToDouble(v -> v[0]).sum();
		for (Entry<Assignment, double[]> e : matrix.entrySet()) {
			double[] old = e.getValue();
			e.setValue(new double[] { old[0] / total, old[1] });
		}
	}

	/**
	 * Normalise the utilities with respect to the probabilities in the double
	 * factor.
	 */
	public void normaliseUtil() {
		for (Entry<Assignment, double[]> e : matrix.entrySet()) {
			double[] old = e.getValue();
			if (old[0] > 0.0 && old[1] != 0 && old[0] != 1) {
				e.setValue(new double[] { old[0], old[1] / old[0] });
			}
		}
	}

	/**
	 * Normalises the factor, with the conditional variables as argument.
	 * 
	 * @param condVars the conditional variables
	 */
	public void normalise(Collection<String> condVars) {

		Map<Assignment, Double> totals = new HashMap<Assignment, Double>();
		for (Assignment a : matrix.keySet()) {
			Assignment cond = a.getTrimmed(condVars);
			double prob = totals.getOrDefault(cond, 0.0);
			totals.put(cond, prob + matrix.get(a)[0]);
		}
		for (Entry<Assignment, double[]> e : matrix.entrySet()) {
			Assignment cond = e.getKey().getTrimmed(condVars);
			double[] old = e.getValue();
			e.setValue(new double[] { old[0] / totals.get(cond), old[1] });
		}
	}

	/**
	 * Trims the factor to the variables provided as argument.
	 * 
	 * @param headVars the variables to retain.
	 */
	public void trim(Collection<String> headVars) {
		Map<Assignment, double[]> matrix2 = new HashMap<Assignment, double[]>();
		for (Entry<Assignment, double[]> e : matrix.entrySet()) {
			Assignment a = e.getKey();
			a.trim(headVars);
			double[] val = e.getValue();
			matrix2.put(a, val);
		}
		matrix = matrix2;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns true if the factor is empty, e.g. either really empty, or containing
	 * only empty assignments.
	 * 
	 * @return true if the factor is empty, false otherwise
	 */
	public boolean isEmpty() {
		if (matrix == null) {
			return true;
		}
		for (Assignment a : matrix.keySet()) {
			if (!a.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public double[] getEntry(Assignment a) {
		return matrix.get(a);
	}

	/**
	 * Returns the probability for the assignment, if it is encoded in the matrix.
	 * Else, returns null
	 * 
	 * @param a the assignment
	 * @return probability of the assignment
	 */
	public double getProbEntry(Assignment a) {
		return matrix.get(a)[0];
	}

	/**
	 * Returns the utility for the assignment, if it is encoded in the matrix. Else,
	 * returns null
	 * 
	 * @param a the assignment
	 * @return utility for the assignment
	 */
	public double getUtilityEntry(Assignment a) {
		return matrix.get(a)[1];
	}

	/**
	 * Returns the matrix included in the factor
	 * 
	 * @return the matrix
	 */
	public Set<Assignment> getAssignments() {
		return matrix.keySet();
	}

	/**
	 * Returns the probability matrix for the factor
	 * 
	 * @return the probability matrix
	 */
	public Map<Assignment, Double> getProbTable() {
		return matrix.keySet().stream()
				.collect(Collectors.toMap(a -> a, a -> matrix.get(a)[0]));
	}

	/**
	 * Returns the utility matrix for the factor
	 * 
	 * @return the utility matrix
	 */
	public Map<Assignment, Double> getUtilTable() {
		return matrix.keySet().stream()
				.collect(Collectors.toMap(a -> a, a -> matrix.get(a)[1]));
	}

	/**
	 * Returns the set of assignments in the factor
	 * 
	 * @return the set of assignments
	 */
	public Set<Assignment> getValues() {
		return matrix.keySet();
	}

	/**
	 * Returns the set of variables used in the assignment. It assumes that at least
	 * one entry exists in the matrix. Else, returns an empty list
	 * 
	 * @return the set of variables
	 */
	public Set<String> getVariables() {
		if (!matrix.isEmpty()) {
			return matrix.keySet().iterator().next().getVariables();
		}
		else {
			return Collections.emptySet();
		}
	}

	/**
	 * Returns true if the factor contains the assignment, and false otherwise
	 * 
	 * @param a the assignment
	 * @return true if assignment is included, false otherwise
	 */
	public boolean hasAssignment(Assignment a) {
		return matrix.containsKey(a);
	}

	/**
	 * Returns the set of possible values for the given variable
	 * 
	 * @param variable the variable label
	 * @return the set of possible values
	 */
	public Set<Value> getValues(String variable) {
		return matrix.keySet().stream().map(a -> a.getValue(variable))
				.collect(Collectors.toSet());
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns a copy of the double factor
	 * 
	 * @return the copy
	 */
	public DoubleFactor copy() {
		return new DoubleFactor(this);
	}

	/**
	 * Returns a string representation of the factor
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment a : matrix.keySet()) {
			str += "P(" + a + ")=" + matrix.get(a)[0];
			if (matrix.get(a)[1] != 0) {
				str += " and U(" + a + ")=" + matrix.get(a)[1];
			}
			str += "\n";
		}
		return str;
	}

	/**
	 * Returns the size of the factor
	 * 
	 * @return the factor size
	 */
	public int size() {
		return matrix.size();
	}

}
