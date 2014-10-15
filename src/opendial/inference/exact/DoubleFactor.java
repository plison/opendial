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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.utils.InferenceUtils;

/**
 * Double factor, combining probability and utility distributions
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DoubleFactor {

	// logger
	public static Logger log = new Logger("DoubleFactor", Logger.Level.DEBUG);

	// the matrix, mapping each assignment to two double values
	// (the probability and the utility)
	Map<Assignment, double[]> matrix;

	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================


	
	/**
	 * Creates a new, empty factor, and a set of head variables
	 */
	public DoubleFactor() {
		matrix = new HashMap<Assignment,double[]>();
	}
	

	/**
	 * Creates a new factor out of an existing one
	 * @param existingFactor
	 */
	public DoubleFactor(DoubleFactor existingFactor) {
		matrix = new HashMap<Assignment,double[]>(existingFactor.getMatrix());
	}


	/**
	 * Adds a new entry to the matrix
	 * 
	 * @param a the assignment
	 * @param value the probability value
	 */
	public void addProbEntry (Assignment a, double value) {
		if (matrix.containsKey(a)) {
			matrix.put(a, new double[]{value, matrix.get(a)[1]});
		}
		else {
			matrix.put(a, new double[]{value, 0.0});
		}
	}


	/**
	 * Adds a new entry to the matrix
	 * 
	 * @param a the assignment
	 * @param value the probability value
	 */
	public void addUtilityEntry (Assignment a, double value) {
		if (matrix.containsKey(a)) {
			matrix.put(a, new double[]{matrix.get(a)[0], value});
		}
		else {
			matrix.put(a, new double[]{1.0, value});
		}
	}

	public void addEntry(Assignment a, double probValue, double utilityValue) {
		matrix.put(a, new double[]{probValue, utilityValue});
	}


	/**
	 * Increment the entry with new probability and utility values
	 * 
	 * @param a the assignment
	 * @param probIncr probability increment
	 * @param utilIncr utility increment
	 */
	public void incrementEntry(Assignment a, double probIncr,
			double utilIncr) {
		if (matrix.containsKey(a)) {
			matrix.put(a, new double[]{getProbEntry(a) + probIncr, getUtilityEntry(a) + utilIncr});
		}
		else {
			matrix.put(a, new double[]{probIncr, utilIncr});
		}
	}
	
	
	/**
	 * Removes an entry from the matrix
	 * 
	 * @param a the entry to remove
	 */
	public void removeEntry(Assignment a) {
		matrix.remove(a);
	}
	
	
	/**
	 * Normalises the factor, assuming no conditional variables in the factor.
	 * 
	 */
	public void normalise () {
		Map<Assignment,Double> probMatrix = InferenceUtils.normalise(getProbMatrix());
		Map<Assignment,Double> utilityMatrix = getUtilityMatrix();
		
		matrix = new HashMap<Assignment,double[]>(probMatrix.size());
		if (probMatrix.size() != utilityMatrix.size()) {
			log.warning("prob. and utility matrices have different sizes");
			log.debug("prob matrix: " + probMatrix);
			log.debug("utility matrix: " + utilityMatrix);
		}
		for (Assignment a : probMatrix.keySet()) {
			matrix.put(a, new double[]{probMatrix.get(a), utilityMatrix.get(a)});
		}
	}
	
	/**
	 * Normalise the utilities with respect to the probabilities in the double factor.
	 */
	public void normaliseUtil() {
		Map<Assignment,double[]> newMatrix = new HashMap<Assignment, double[]>();
		for (Assignment a : matrix.keySet()) {
			double[] entries = matrix.get(a);
			if (entries[0] > 0.0) {
				newMatrix.put(a, new double[]{entries[0], entries[1] / entries[0]});
			}
		}
		matrix = newMatrix;
	}
	
	/**
	 * Normalises the factor, with the conditional variables as argument.
	 * 
	 */
	
	public void normalise(Collection<String> condVars) {
		Map<Assignment,Double> probMatrix = InferenceUtils.normalise(getProbMatrix(), condVars);
		Map<Assignment,Double> utilityMatrix = getUtilityMatrix();
		
		matrix = new HashMap<Assignment,double[]>(probMatrix.size());
		if (probMatrix.size() != utilityMatrix.size()) {
			log.warning("prob. and utility matrices have different sizes");
		}
		for (Assignment a : probMatrix.keySet()) {
			matrix.put(a, new double[]{probMatrix.get(a), utilityMatrix.get(a)});
		}
	}
	
	public void trim(Collection<String> headVars) {
		Map<Assignment,double[]> newMatrix = new HashMap<Assignment,double[]>(matrix.size());		
		for (Assignment a : matrix.keySet()) {
			newMatrix.put(a.getTrimmed(headVars), matrix.get(a));
		}
		matrix = newMatrix;
	}


	// ===================================
	//  GETTERS 
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
		boolean containsRealAssign = false;
		Iterator<Assignment> it = matrix.keySet().iterator();
		while (it.hasNext() && !containsRealAssign) {
			Assignment a = it.next();
			if (!a.isEmpty()) {
				containsRealAssign = true;
			}
		}
		
		return !containsRealAssign;
	}


	
	/**
	 * Returns the probability for the assignment, if it is
	 * encoded in the matrix.  Else, returns null
	 * 
	 * @param a the assignment
	 * @return probability of the assignment
	 */
	public double getProbEntry(Assignment a) {
		if (!matrix.containsKey(a)) {
			log.debug("assignment FAILURE: " + a);
		}
		return matrix.get(a)[0];
	}


	/**
	 * Returns the utility for the assignment, if it is encoded
	 * in the matrix. Else, returns null
	 * 
	 * @param a the assignment
	 * @return utility for the assignment
	 */
	public double getUtilityEntry(Assignment a) {
		if (!matrix.containsKey(a)) {
			log.debug("assignment FAILURE: " + a);
		}
		return matrix.get(a)[1];
	}


	/**
	 * Returns the matrix included in the factor
	 * 
	 * @return the matrix
	 */
	public Map<Assignment, double[]> getMatrix() {
		return matrix;
	}

	/**
	 * Returns the probability matrix for the factor
	 * 
	 * @return the probability matrix
	 */
	public Map<Assignment,Double> getProbMatrix() {
		Map<Assignment,Double> probMatrix = new HashMap<Assignment,Double>();
		for (Assignment a : matrix.keySet()) {
			probMatrix.put(a, getProbEntry(a));
		}
		return probMatrix;
	}


	/**
	 * Returns the utility matrix for the factor
	 * 
	 * @return the utility matrix
	 */
	public Map<Assignment,Double> getUtilityMatrix() {
		Map<Assignment,Double> utilityMatrix = new HashMap<Assignment,Double>();
		for (Assignment a : matrix.keySet()) {
			utilityMatrix.put(a, getUtilityEntry(a));
		}
		return utilityMatrix;
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
	 * Returns the set of variables used in the assignment.  It assumes
	 * that at least one entry exists in the matrix.  Else, returns
	 * an empty list
	 * 
	 * @return the set of variables
	 */
	public Set<String> getVariables() {
		if (!matrix.isEmpty()) {
			return matrix.keySet().iterator().next().getVariables();
		}
		else {
			return new HashSet<String>();
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


	// ===================================
	//  UTILITIES
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
			if (matrix.get(a)[1]!=0) {
				str += " and U(" + a + ")=" + matrix.get(a)[1];				
			}
			str +="\n";
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
