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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.distribs.ConditionalTable.Builder;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.Query;

/**
 * Implementation of the Variable Elimination algorithm.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class VariableElimination implements InferenceAlgorithm {

	final static Logger log = Logger.getLogger("OpenDial");

	// ===================================
	// MAIN QUERY METHODS
	// ===================================

	/**
	 * Queries for the probability distribution of the set of random variables in the
	 * Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @return the corresponding categorical table failed
	 */
	@Override
	public MultivariateTable queryProb(Query.ProbQuery query) {
		DoubleFactor queryFactor = createQueryFactor(query);
		MultivariateTable.Builder builder = new MultivariateTable.Builder();
		builder.addRows(queryFactor.getProbTable());
		builder.normalise();
		return builder.build();
	}

	/**
	 * Queries for the utility of a particular set of (action) variables, given the
	 * provided evidence
	 * 
	 * @param query the full query
	 * @return the utility distribution
	 */
	@Override
	public UtilityTable queryUtil(Query.UtilQuery query) {
		DoubleFactor queryFactor = createQueryFactor(query);
		queryFactor.normalise();
		return new UtilityTable(queryFactor.getUtilTable());
	}

	// ===================================
	// INFERENCE OPERATION METHODS
	// ===================================

	/**
	 * Generates the full double factor associated with the query variables, using
	 * the variable-elimination algorithm.
	 * 
	 * @param query the query
	 * @return the full double factor containing all query variables occurred during
	 *         the inference
	 */
	private DoubleFactor createQueryFactor(Query query) {

		List<DoubleFactor> factors = new LinkedList<DoubleFactor>();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();

		for (BNode n : query.getFilteredSortedNodes()) {
			// create the basic factor for every variable
			DoubleFactor basicFactor = makeFactor(n, evidence);
			if (!basicFactor.isEmpty()) {
				factors.add(basicFactor);
				// if the variable is hidden, we sum it out
				if (!queryVars.contains(n.getId())) {
					factors = sumOut(n.getId(), factors);
				}
			}
		}
		// compute the final product, and normalise
		DoubleFactor finalProduct = pointwiseProduct(factors);
		finalProduct = addEvidencePairs(finalProduct, query);
		finalProduct.trim(queryVars);
		return finalProduct;
	}

	/**
	 * Sums out the variable from the pointwise product of the factors, and returns
	 * the result
	 * 
	 * @param nodeId the Bayesian node corresponding to the variable
	 * @param factors the factors to sum out
	 * @return the summed out factor
	 */
	private List<DoubleFactor> sumOut(String nodeId, List<DoubleFactor> factors) {

		// we divide the factors into two lists: the factors which are
		// independent of the variable, and those who aren't
		List<DoubleFactor> dependentFactors = new LinkedList<DoubleFactor>();
		List<DoubleFactor> remainingFactors = new LinkedList<DoubleFactor>();

		for (DoubleFactor f : factors) {
			if (!f.getVariables().contains(nodeId)) {
				remainingFactors.add(f);
			}
			else {
				dependentFactors.add(f);
			}
		}

		// we compute the product of the dependent factors
		DoubleFactor productDependentFactors = pointwiseProduct(dependentFactors);

		// we sum out the dependent factors
		DoubleFactor sumDependentFactors =
				sumOutDependent(nodeId, productDependentFactors);

		if (!sumDependentFactors.isEmpty()) {
			remainingFactors.add(sumDependentFactors);
		}

		return remainingFactors;
	}

	/**
	 * Sums out the variable from the given factor, and returns the result
	 * 
	 * @param node the Bayesian node corresponding to the variable
	 * @param factor the factor to sum out
	 * @return the summed out factor
	 */
	private DoubleFactor sumOutDependent(String nodeId, DoubleFactor factor) {

		// create the new factor
		DoubleFactor sumFactor = new DoubleFactor();

		for (Assignment a : factor.getValues()) {
			Assignment reducedA = new Assignment(a);
			reducedA.removePair(nodeId);
			double[] entry = factor.getEntry(a);
			double prob = entry[0];
			double util = entry[1];
			sumFactor.incrementEntry(reducedA, prob, prob * util);
		}

		sumFactor.normaliseUtil();

		return sumFactor;
	}

	/**
	 * Computes the pointwise matrix product of the list of factors
	 * 
	 * @param factors the factors
	 * @return the pointwise product of the factors
	 */
	private DoubleFactor pointwiseProduct(List<DoubleFactor> factors) {

		if (factors.isEmpty()) {
			DoubleFactor factor = new DoubleFactor();
			factor.addEntry(new Assignment(), 1.0, 0.0);
			return factor;
		}
		else if (factors.size() == 1) {
			return factors.get(0);
		}
		DoubleFactor factor = factors.get(0);
		factors.remove(0);
		for (DoubleFactor f : factors) {

			DoubleFactor tempFactor = new DoubleFactor();
			Set<String> sharedVars = new HashSet<String>(f.getVariables());
			sharedVars.retainAll(factor.getVariables());

			for (Assignment a : f.getValues()) {

				double[] entry = f.getEntry(a);
				double prob = entry[0];
				double util = entry[1];

				for (Assignment b : factor.getValues()) {
					if (b.consistentWith(a, sharedVars)) {
						double[] entry2 = factor.getEntry(b);
						double prob2 = entry2[0];
						double util2 = entry2[1];
						double product = prob * prob2;
						double sum = util + util2;

						tempFactor.addEntry(new Assignment(a, b), product, sum);
					}
				}
			}
			factor = tempFactor;
		}

		return factor;
	}

	/**
	 * Creates a new factor given the probability distribution defined in the
	 * Bayesian node, and the evidence (which needs to be matched)
	 * 
	 * @param node the Bayesian node
	 * @param evidence the evidence
	 * @return the factor for the node
	 */
	private DoubleFactor makeFactor(BNode node, Assignment evidence) {

		DoubleFactor factor = new DoubleFactor();

		// generates all possible assignments for the node content
		Map<Assignment, Double> flatTable = node.getFactor();
		for (Assignment a : flatTable.keySet()) {

			// verify that the assignment is consistent with the evidence
			if (a.consistentWith(evidence)) {
				// adding a new entry to the factor
				Assignment a2 = new Assignment(a);
				a2.removePairs(evidence.getVariables());

				if (node instanceof ChanceNode || node instanceof ActionNode) {
					factor.addEntry(a2, flatTable.get(a), 0.0f);
				}
				else if (node instanceof UtilityNode) {
					factor.addEntry(a2, 1.0f, flatTable.get(a));
				}
			}
		}

		return factor;
	}

	/**
	 * In case of overlap between the query variables and the evidence (this happens
	 * when a variable specified in the evidence also appears in the query), extends
	 * the distribution to add the evidence assignment pairs.
	 * 
	 * @param query the query
	 * @param distribution the computed distribution
	 */
	private DoubleFactor addEvidencePairs(DoubleFactor factor, Query query) {

		Set<String> inter = new HashSet<String>(query.getQueryVars());
		inter.retainAll(query.getEvidence().getVariables());
		Assignment evidence = query.getEvidence().getTrimmed(inter);
		if (!inter.isEmpty()) {
			DoubleFactor newFactor = new DoubleFactor();
			for (Assignment a : factor.getAssignments()) {
				Assignment assign = new Assignment(a, evidence);
				double[] entry = factor.getEntry(a);
				newFactor.addEntry(assign, entry[0], entry[1]);
			}
			return newFactor;
		}
		else {
			return factor;
		}
	}

	// ===================================
	// NETWORK REDUCTION METHODS
	// ===================================

	/**
	 * Reduces the Bayesian network by retaining only a subset of variables and
	 * marginalising out the rest.
	 * 
	 * @param query the query containing the network to reduce, the variables to
	 *            retain, and possible evidence.
	 * @return the probability distributions for the retained variables reduction
	 *         operation failed
	 */
	@Override
	public BNetwork reduce(Query.ReduceQuery query) {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();

		// create the query factor
		DoubleFactor queryFactor = createQueryFactor(query);
		BNetwork reduced = new BNetwork();

		List<String> sortedNodesIds = network.getSortedNodesIds();
		sortedNodesIds.retainAll(queryVars);
		Collections.reverse(sortedNodesIds);

		for (String var : sortedNodesIds) {

			Set<String> directAncestors =
					network.getNode(var).getAncestorsIds(queryVars);
			// create the factor and distribution for the variable
			DoubleFactor factor =
					getRelevantFactor(queryFactor, var, directAncestors);
			ProbDistribution distrib = createProbDistribution(var, factor);

			// create the new node
			ChanceNode cn = new ChanceNode(var, distrib);
			for (String ancestor : directAncestors) {
				cn.addInputNode(reduced.getNode(ancestor));
			}
			reduced.addNode(cn);
		}

		return reduced;
	}

	/**
	 * Returns the factor associated with the probability/utility distribution for
	 * the given node in the Bayesian network. If the factor encode more than the
	 * needed distribution, the surplus variables are summed out.
	 * 
	 * @param factors the collection of factors in which to search
	 * @param toEstimate the variable to estimate
	 * @return the relevant factor associated with the node could be found
	 */
	private DoubleFactor getRelevantFactor(DoubleFactor fullFactor, String headVar,
			Set<String> inputVars) {

		// summing out unrelated variables
		DoubleFactor factor = fullFactor.copy();
		for (String otherVar : new ArrayList<String>(factor.getVariables())) {
			if (!otherVar.equals(headVar) && !inputVars.contains(otherVar)) {
				List<DoubleFactor> summedOut =
						sumOut(otherVar, Arrays.asList(factor));
				if (!summedOut.isEmpty()) {
					factor = summedOut.get(0);
				}
			}
		}

		return factor;
	}

	/**
	 * Creates the probability distribution for the given variable, as described by
	 * the factor. The distribution is normalised, and encoded as a table.
	 * 
	 * @param headVar the variable
	 * @param factor the double factor
	 * @return the resulting probability distribution
	 */
	public static ProbDistribution createProbDistribution(String headVar,
			DoubleFactor factor) {

		Set<String> variables = factor.getVariables();

		// if the factor does not have dependencies, create a simple distribution
		if (variables.size() == 1) {

			factor.normalise();
			CategoricalTable.Builder builder = new CategoricalTable.Builder(headVar);
			for (Assignment a : factor.getAssignments()) {
				builder.addRow(a.getValue(headVar), factor.getProbEntry(a));
			}
			return builder.build();
		}

		// else, create a full probability table
		else {
			Set<String> depVariables = new HashSet<String>(variables);
			depVariables.remove(headVar);

			factor.normalise(depVariables);
			Builder builder = new ConditionalTable.Builder(headVar);
			for (Assignment a : factor.getAssignments()) {
				Assignment cond = a.getTrimmed(depVariables);
				builder.addRow(cond, a.getValue(headVar), factor.getProbEntry(a));
			}
			return builder.build();
		}

	}
}
