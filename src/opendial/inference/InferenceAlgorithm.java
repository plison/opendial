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

package opendial.inference;

import java.util.Arrays;
import java.util.Collection;

import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.datastructs.Assignment;

/**
 * Generic interface for probabilistic inference algorithms. Three distinct types of
 * queries are possible:
 * <ul>
 * <li>probability queries of the form P(X1,...,Xn)
 * <li>utility queries of the form U(X1,...,Xn)
 * <li>reduction queries where a Bayesian network is reduced to a subset of variables
 * X1,...,Xn
 * </ul>
 * 
 * The interface contains 3 abstract methods (queryProb, queryUtil and reduce) that
 * must be specified by all implementing classes. In addition, a set of default
 * methods provide alternative ways to call the inference process (e.g. with one or
 * several query variables, with or without evidence, etc.).
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface InferenceAlgorithm {

	// ===================================
	// PROBABILITY QUERIES
	// ===================================

	/**
	 * Computes the probability distribution for the query variables given the
	 * provided evidence, all specified in the query
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution failed to deliver a result
	 */
	public MultivariateDistribution queryProb(Query.ProbQuery query);

	/**
	 * Computes the probability distribution for the query variables given the
	 * provided evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVars the collection of query variables
	 * @param evidence the evidence
	 * @return the resulting probability distribution failed to deliver a result
	 */
	public default MultivariateDistribution queryProb(BNetwork network,
			Collection<String> queryVars, Assignment evidence) {
		return queryProb(new Query.ProbQuery(network, queryVars, evidence));
	}

	/**
	 * Computes the probability distribution for the query variables, assuming no
	 * additional evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVars the collection of query variables
	 * @return the resulting probability distribution failed to deliver a result
	 */
	public default MultivariateDistribution queryProb(BNetwork network,
			Collection<String> queryVars) {
		return queryProb(new Query.ProbQuery(network, queryVars, new Assignment()));
	}

	/**
	 * Computes the probability distribution for the query variable given the
	 * provided evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVar the (unique) query variable
	 * @param evidence the evidence
	 * @return the resulting probability distribution for the variable inference
	 *         process failed to deliver a result
	 */
	public default IndependentDistribution queryProb(BNetwork network,
			String queryVar, Assignment evidence) {
		return queryProb(
				new Query.ProbQuery(network, Arrays.asList(queryVar), evidence))
						.getMarginal(queryVar);
	}

	/**
	 * Computes the probability distribution for the query variable, assuming no
	 * additional evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVar the (unique) query variable
	 * @return the resulting probability distribution for the variable inference
	 *         process failed to deliver a result
	 */
	public default IndependentDistribution queryProb(BNetwork network,
			String queryVar) {
		return queryProb(network, queryVar, new Assignment());
	}

	// ===================================
	// UTILITY QUERIES
	// ===================================

	/**
	 * Computes the utility table for the query variables (typically action
	 * variables), given the provided evidence.
	 * 
	 * @param query the full query
	 * @return the resulting utility table deliver a result
	 */
	public UtilityTable queryUtil(Query.UtilQuery query);

	/**
	 * Computes the utility table for the query variables (typically action
	 * variables), given the provided evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVars the query variables (usually action variables)
	 * @param evidence the additional evidence
	 * @return the resulting utility table for the query variables process failed to
	 *         deliver a result
	 */
	public default UtilityTable queryUtil(BNetwork network,
			Collection<String> queryVars, Assignment evidence) {
		return queryUtil(new Query.UtilQuery(network, queryVars, evidence));
	}

	/**
	 * Computes the utility table for the query variables (typically action
	 * variables), assuming no additional evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVars the query variables (usually action variables)
	 * @return the resulting utility table for the query variables process failed to
	 *         deliver a result
	 */
	public default UtilityTable queryUtil(BNetwork network,
			Collection<String> queryVars) {
		return queryUtil(new Query.UtilQuery(network, queryVars, new Assignment()));
	}

	/**
	 * Computes the utility table for the query variable (typically an action
	 * variable), assuming no additional evidence.
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVar the query variable
	 * @return the resulting utility table for the query variable process failed to
	 *         deliver a result
	 */
	public default UtilityTable queryUtil(BNetwork network, String queryVar) {
		return queryUtil(new Query.UtilQuery(network, Arrays.asList(queryVar),
				new Assignment()));
	}

	/**
	 * Computes the utility table for the query variable (typically an action
	 * variable), given the provided evidence
	 * 
	 * @param network the Bayesian network on which to perform the inference
	 * @param queryVar the query variable
	 * @param evidence the additional evidence
	 * @return the resulting utility table for the query variable process failed to
	 *         deliver a result
	 */
	public default UtilityTable queryUtil(BNetwork network, String queryVar,
			Assignment evidence) {
		return queryUtil(
				new Query.UtilQuery(network, Arrays.asList(queryVar), evidence));
	}

	// ===================================
	// REDUCTION QUERIES
	// ===================================

	/**
	 * Generates a new Bayesian network that only contains a subset of variables in
	 * the original network and integrates the provided evidence.
	 * 
	 * @param query the full reduction query
	 * @return the reduced Bayesian network deliver a result
	 */
	public BNetwork reduce(Query.ReduceQuery query);

	/**
	 * Generates a new Bayesian network that only contains a subset of variables in
	 * the original network and integrates the provided evidence.
	 * 
	 * @param network the original Bayesian network
	 * @param queryVars the variables to retain
	 * @param evidence the additional evidence
	 * @return the new, reduced Bayesian network deliver a result
	 */
	public default BNetwork reduce(BNetwork network, Collection<String> queryVars,
			Assignment evidence) {
		return reduce(new Query.ReduceQuery(network, queryVars, evidence));
	}

	/**
	 * Generates a new Bayesian network that only contains a subset of variables in
	 * the original network, assuming no additional evidence.
	 * 
	 * @param network the original Bayesian network
	 * @param queryVars the variables to retain
	 * @return the new, reduced Bayesian network deliver a result
	 */
	public default BNetwork reduce(BNetwork network, Collection<String> queryVars) {
		return reduce(new Query.ReduceQuery(network, queryVars, new Assignment()));
	}

}
