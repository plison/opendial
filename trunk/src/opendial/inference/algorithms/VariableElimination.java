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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.gui.NetworkVisualisation;
import opendial.inference.algorithms.datastructs.Factor;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.inference.distribs.Distribution;
import opendial.inference.distribs.ProbabilityTable;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * Implementation of the Variable Elimination algorithm
 *
 * NB: make this more efficient by discarding irrelevant variables!
 * also see Koller's book to compare the algorithm
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VariableElimination {

	static Logger log = new Logger("VariableElimination", Logger.Level.DEBUG);
	
	public static int nbQueries = 0 ;
	public static long totalQueryTime = 0L;
	public static boolean activateProfiling = false;
	
	public static Distribution query (BNetwork bn, List<String> queryVars) {
		return query(bn, queryVars, new Assignment(), bn.getSortedNodes());
	}
	
	public static Distribution query (BNetwork bn, List<String> queryVars, Assignment evidence) {
		return query(bn, queryVars, evidence, bn.getSortedNodes());
	}
	
	
	public static Distribution query (BNetwork bn, List<String> queryVars, List<BNode> sortedNodes) {
		return query(bn, queryVars, new Assignment(), sortedNodes);
	}
	

	
	/**
	 * Computes the probability distribution for the query variables in the Bayesian 
	 * Network, given the evidence
	 * 
	 * @param bn the Bayesian Network
	 * @param queryVars the list of query variables (must be included in the BN)
	 * @param evidence the evidence
	 * @return the probability distribution for the query
	 * @throws DialException 
	 */
	public static Distribution query (BNetwork bn, List<String> queryVars, Assignment evidence, List<BNode> sortedNodes) {

	//	log.debug("query vars: " + queryVars);
	//	log.debug("sorted nodes: " + sortedNodes);	

		long initTime = System.currentTimeMillis();
		
		List<Factor> factors = new LinkedList<Factor>();
		
		// sort the nodes
		for (BNode n: sortedNodes) {

			if (true) {

				// create the basic factor for every variable
				Factor basicFactor = makeFactor(n, evidence);

				factors.add(basicFactor);

				// if the variable is hidden, we sum it out
				if (!queryVars.contains(n.getId()) && 
						!evidence.getPairs().containsKey(n.getId())) {
					factors = sumOut(n, factors);
				}
			}
		}

		// compute the final product, and normalise
		Factor finalProduct = pointwiseProduct(factors);

		Map<Assignment, Float> normalisedProbs = normalise(finalProduct, queryVars);

		normalisedProbs = addEvidencePairs (bn, normalisedProbs, queryVars, evidence);

		Distribution distrib = new ProbabilityTable(normalisedProbs);

		if (activateProfiling) {
			nbQueries++;
			totalQueryTime += (System.currentTimeMillis() - initTime);
		}
		return distrib;

	}


	/**
	 * Sums out the variable from the pointwise product of the factors, 
	 * and returns the result
	 * 
	 * @param the Bayesian node corresponding to the variable
	 * @param factors the factors to sum out
	 * @return the summed out factor
	 */
	private static List<Factor> sumOut(BNode node, List<Factor> factors) {	

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

		// we compute the product of the dependent factors
		Factor productDependentFactors = pointwiseProduct(dependentFactors);

		// we sum out the dependent factors
		Factor sumDependentFactors = sumOutDependent(node, productDependentFactors);

		List<Factor> finalFactors = new LinkedList<Factor>();
		finalFactors.addAll(independentFactors);
		finalFactors.add(sumDependentFactors);

		return finalFactors;

	}


	/**
	 * Sums out the variable from the given factor, and returns the result
	 * 
	 * @param node the Bayesian node corresponding to the variable
	 * @param factor the factor to sum out
	 * @return the summed out factor
	 */
	private static Factor sumOutDependent(BNode node, Factor factor) {

		// create the new factor
		Factor sumFactor = new Factor();		

		for (Assignment a : factor.getMatrix().keySet()) {
			Assignment reducedA = new Assignment(a);
			reducedA.removePair(node.getId());
			if (sumFactor.hasAssignment(reducedA)) {
				sumFactor.addEntry(reducedA, sumFactor.getEntry(reducedA) + factor.getEntry(a));
			}
			else {
				sumFactor.addEntry(reducedA, factor.getEntry(a));
			}	
		}

		return sumFactor;
	}



	/**
	 * Computes the pointwise matrix product of the list of factors
	 * 
	 * @param factors the factors
	 * @return the pointwise product of the factors
	 */
	private static Factor pointwiseProduct (List<Factor> factors) {

		Map<Assignment, Float> productFactor = new HashMap<Assignment, Float>();
		productFactor.put(new Assignment(), 1.0f);

		for (Factor f: factors) {

			Map<Assignment, Float> tempFactor = new HashMap<Assignment,Float>();
			for (Assignment a : f.getMatrix().keySet()) {

				for (Assignment b: productFactor.keySet()) {
					if (b.consistentWith(a)) {
						float product = f.getEntry(a) * productFactor.get(b);
						tempFactor.put(new Assignment(a,b), product);
					}
				}
			}
			productFactor = tempFactor;
		}

		return new Factor(productFactor);
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
				a2.removePairs(evidence.getVariables());
				f.addEntry(a2, node.getProb(a));
			}
		}

		return f;
	}



	/**
	 * In case of overlap between the query variables and the evidence (this happens
	 * when a variable specified in the evidence also appears in the query), extends 
	 * the distribution to add the evidence assignment pairs.
	 * 
	 * @param bn the Bayesian network
	 * @param distribution the computed distribution
	 * @param queryVars the query variables
	 * @param evidence the evidence
	 * @return the extended distribution
	 */
	private static Map<Assignment, Float> addEvidencePairs(BNetwork bn,
			Map<Assignment, Float> distribution, List<String> queryVars,
			Assignment evidence) {

		// first, check if there is an overlap between the query variables and
		// the evidence variables
		Map<String,Set<Object>> valuesToAdd = new HashMap<String,Set<Object>>();
		for (String queryVar : queryVars) {
			if (evidence.getPairs().containsKey(queryVar)) {
				valuesToAdd.put(queryVar, bn.getNode(queryVar).getValues());
			}
		}

		// in case of overlap, extend the distribution
		if (!valuesToAdd.isEmpty()) {
			List<Assignment> possibleExtensions = InferenceUtils.getAllCombinations(valuesToAdd);

			Map<Assignment,Float> extendedDistribution = new HashMap<Assignment,Float>();
			for (Assignment a : distribution.keySet()) {
				for (Assignment b: possibleExtensions) {

					// if the assignment b agrees with the evidence, reuse the probability value
					if (evidence.contains(b)) {
						extendedDistribution.put(new Assignment(a, b), distribution.get(a));
					}

					// else, set the probability value to 0.0f
					else {
						extendedDistribution.put(new Assignment(a, b), 0.0f);				
					}
				}
			}

			return extendedDistribution;
		}
		return distribution;
	}
	
	
	public static Map<Assignment, Float> normalise (Factor factor, List<String> queryVars) {
		Map<Assignment,Float> result = new HashMap<Assignment, Float>();
		try {
			result = InferenceUtils.normalise(factor.getMatrix());
		} catch (DialException e) {
			//		NetworkVisualisation.showBayesianNetwork(bn);
	//		log.warning("normalisation failed for query P( " + queryVars + "), replacing with default value...");
			Assignment a = new Assignment();
			for (String q : queryVars) {
				a.addPair(q, "None");
			}
			result.putAll(factor.getMatrix());
			result.put(a, 1.0f);
		}
		return result;
	}
}
