// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.inference.algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.algorithms.datastructs.Factor;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * Implementation of the Variable Elimination algorithm
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VariableElimination {

	static Logger log = new Logger("VariableElimination", Logger.Level.DEBUG);

	/**
	 * Computes the probability distribution for the query variables in the Bayesian 
	 * Network, given the evidence
	 * 
	 * @param bn the Bayesian Network
	 * @param queryVars the list of query variables (must be included in the BN)
	 * @param evidence the evidence
	 * @return the probability distribution for the query
	 */
	public static Map<Assignment,Float> query
	(BNetwork bn, List<String> queryVars, Assignment evidence) {

		List<Factor> factors = new LinkedList<Factor>();

		// sort the nodes
		for (BNode n: bn.getSortedNodes()) {

			// create the basic factor for every variable
			Factor basicFactor = makeFactor(n, evidence);
			factors.add(basicFactor);

			// if the variable is hidden, we sum it out
			if (!queryVars.contains(n.getId()) && ! evidence.getPairs().containsKey(n.getId())) {
				Factor sumFactor = sumOut(n, factors);
				factors.clear();
				factors.add(sumFactor);
			}
		}

		// compute the final product, and normalise
		Factor finalProduct = pointwiseProduct(factors);
		Map<Assignment,Float> normalisedProbs = InferenceUtils.normalise(finalProduct.getMatrix());

		return normalisedProbs;
	}


	/**
	 * Sums out the variable from the pointwise product of the factors, 
	 * and returns the result
	 * 
	 * @param the Bayesian node corresponding to the variable
	 * @param factors the factors to sum out
	 * @return the summed out factor
	 */
	private static Factor sumOut(BNode node, List<Factor> factors) {	

		// we divide the factors into two lists: the factors which are
		// independent of the variable, and those who aren't
		List<Factor> independentFactors = new LinkedList<Factor>();
		List<Factor> dependentFactors = new LinkedList<Factor>();

		for (Factor f: factors) {
			if (!f.getVariables().contains(node.getId())) {
				independentFactors.add(f);
			}
			else {
				dependentFactors.add(f);
			}
		}

		// we compute the product of the independent factors
		Factor productIndependentFactors = pointwiseProduct(independentFactors);

		// we compute the product of the dependent factors
		Factor productDependentFactors = pointwiseProduct(dependentFactors);

		// we sum out the dependent factors
		Factor sumDependentFactors = sumOutDependent(node, productDependentFactors);

		// finally, we compute the product of all factors
		Factor finalFactor = pointwiseProduct(Arrays.asList(productIndependentFactors, sumDependentFactors));

		return finalFactor;

	}



	/**
	 * Sums out the variable from the given factor, and returns the result
	 * 
	 * @param node the Bayesian node corresponding to the variable
	 * @param factor the factor to sum out
	 * @return the summed out factor
	 */
	private static Factor sumOutDependent(BNode node, Factor factor) {

		// generate the list of sub-factors for each possible instantiation of 
		// the variable value
		List<Factor> subFactors = new LinkedList<Factor>();
		for (Object v: node.getValues())  {
			Factor subFactor = reduceFactor(factor, node.getId(), v);
			subFactors.add(subFactor);
		}

		// create the new factor
		Factor sumFactor = new Factor();

		// compute the sum and add it to the new factor
		for (Assignment a : subFactors.get(0).getMatrix().keySet()) {
			float sum = 0.0f;
			for (Factor f : subFactors) {
				sum += f.getEntry(a);
			}
			sumFactor.addEntry(a, sum);
		}

		return sumFactor;
	}


	/**
	 * Returns the reduced factor obtained by fixing a variable to a given value
	 * 
	 * @param factor the factor to reduce
	 * @param var the variable to fix
	 * @param val the value to fix
	 * @return the reduced factor
	 */
	private static Factor reduceFactor(Factor factor, String var, Object val) {

		// create reduced factor
		Factor subFactor = new Factor();

		// search for factor assignments which matches var=val, and adding
		// their content to the reduced factor
		Map<Assignment, Float> matrix = factor.getMatrix();
		for (Assignment a : matrix.keySet()) {
			if (a.getPairs().get(var).equals(val)) {
				Assignment reducedA = new Assignment(a);
				reducedA.removePair(var);
				subFactor.addEntry(reducedA, matrix.get(a));
			}
		}

		return subFactor;
	}


	/**
	 * Computes the pointwise matrix product of the list of factors
	 * 
	 * @param factors the factors
	 * @return the pointwise product of the factors
	 */
	private static Factor pointwiseProduct (List<Factor> factors) {

		Factor productFactor = new Factor();

		// compute the union of the variables expressed in each factor
		Map<String,Set<Object>> unionVars = new HashMap<String,Set<Object>>();
		for (Factor f: factors) {
			unionVars.putAll(f.getPossibleValues());
		}
		
		// generates the alternative assignments combination for the variables
		List<Assignment> combinations = InferenceUtils.getAllCombinations(unionVars);

		// calculate the product for each assignment
		for (Assignment a : combinations) {
			float product = 1.0f;
			for (Factor f: factors) {
				Assignment reducedAssignment = InferenceUtils.trimAssignment(a, f.getVariables());
				product = product * f.getEntry(reducedAssignment);
			}
			productFactor.addEntry(a, product);
		}


		return productFactor;
	}


	/**
	 * Creates a new factor given the probability distribution defined in the Bayesian
	 * node, and the evidence (which needs to be matched)
	 * 
	 * @param node the Bayesian node 
	 * @param evidence the evidence
	 * @return the factor for the node
	 */
	private static Factor makeFactor(BNode node, Assignment evidence) {

		Factor f = new Factor();

		// generates all possible assignments for the node content
		for (Assignment a: node.getAllPossibleAssignments()) {
			
			// verify that the assignment is consistent with the evidence
			if (a.consistentWith(evidence)) {
				
				// adding a new entry to the factor
				Assignment a2 = new Assignment(a);
				a2.removePairs(evidence.getPairs().keySet());
				f.addEntry(a2, node.getProb(a));
			}
		}

		return f;
	}
}
