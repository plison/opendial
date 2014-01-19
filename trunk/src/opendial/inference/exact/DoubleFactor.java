// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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
	public static Logger log = new Logger("DoubleFactor", Logger.Level.NORMAL);

	// the matrix, mapping each assignment to two double values
	// (the probability and the utility)
	Map<Assignment, Double[]> matrix;

	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================


	
	/**
	 * Creates a new, empty factor, and a set of head variables
	 */
	public DoubleFactor() {
		matrix = new HashMap<Assignment,Double[]>();
	}
	

	/**
	 * Creates a new factor out of an existing one
	 * @param existingFactor
	 */
	public DoubleFactor(DoubleFactor existingFactor) {
		matrix = new HashMap<Assignment,Double[]>(existingFactor.getMatrix());
	}


	/**
	 * Adds a new entry to the matrix
	 * 
	 * @param a the assignment
	 * @param value the probability value
	 */
	public void addProbEntry (Assignment a, double value) {
		if (matrix.containsKey(a)) {
			matrix.put(a, new Double[]{value, matrix.get(a)[1]});
		}
		else {
			matrix.put(a, new Double[]{value, 0.0});
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
			matrix.put(a, new Double[]{matrix.get(a)[0], value});
		}
		else {
			matrix.put(a, new Double[]{1.0, value});
		}
	}

	public void addEntry(Assignment a, double probValue, double utilityValue) {
		matrix.put(a, new Double[]{probValue, utilityValue});
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
			matrix.put(a, new Double[]{getProbEntry(a) + probIncr, getUtilityEntry(a) + utilIncr});
		}
		else {
			matrix.put(a, new Double[]{probIncr, utilIncr});
		}
	}
	
	
	
	/**
	 * Normalises the factor, assuming no conditional variables in the factor.
	 * 
	 */
	public void normalise () {
		Map<Assignment,Double> probMatrix = InferenceUtils.normalise(getProbMatrix());
		Map<Assignment,Double> utilityMatrix = getUtilityMatrix();
		
		matrix = new HashMap<Assignment,Double[]>(probMatrix.size());
		if (probMatrix.size() != utilityMatrix.size()) {
			log.warning("prob. and utility matrices have different sizes");
		}
		for (Assignment a : probMatrix.keySet()) {
			matrix.put(a, new Double[]{probMatrix.get(a), utilityMatrix.get(a)});
		}
	}
	
	
	/**
	 * Normalises the factor, with the conditional variables as argument.
	 * 
	 */
	
	public void normalise(Collection<String> condVars) {
		Map<Assignment,Double> probMatrix = InferenceUtils.normalise(getProbMatrix(), condVars);
		Map<Assignment,Double> utilityMatrix = getUtilityMatrix();
		
		matrix = new HashMap<Assignment,Double[]>(probMatrix.size());
		if (probMatrix.size() != utilityMatrix.size()) {
			log.warning("prob. and utility matrices have different sizes");
		}
		for (Assignment a : probMatrix.keySet()) {
			matrix.put(a, new Double[]{probMatrix.get(a), utilityMatrix.get(a)});
		}
	}
	
	public void trim(Collection<String> headVars) {
		Map<Assignment,Double[]> newMatrix = new HashMap<Assignment,Double[]>(matrix.size());		
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
	public Map<Assignment, Double[]> getMatrix() {
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
	public String toString() {
		String str = "";
		for (Assignment a : matrix.keySet()) {
			str += "P(" + a + ")=" + matrix.get(a)[0];
			if (matrix.get(a)[1]!=0) {
				str += " and Q(" + a + ")=" + matrix.get(a)[1];				
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
