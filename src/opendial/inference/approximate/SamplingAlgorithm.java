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

package opendial.inference.approximate;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import opendial.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.EmpiricalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.Query;

/**
 * Sampling-based inference algorithm for Bayesian networks. The class provides a set
 * of functionalities for performing inference operations based on a particular
 * sampling algorithm (e.g. likelihood weighting).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class SamplingAlgorithm implements InferenceAlgorithm {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public int nbSamples = Settings.nbSamples;

	long maxSamplingTime = Settings.maxSamplingTime;

	// ===================================
	// CONSTRUCTORS
	// ===================================

	/**
	 * Creates a new likelihood weighting algorithm with the specified number of
	 * samples and sampling time
	 * 
	 * @param nbSamples the maximum number of samples to collect
	 * @param maxSamplingTime the maximum sampling time
	 */
	public SamplingAlgorithm(int nbSamples, long maxSamplingTime) {
		this.nbSamples = nbSamples;
		this.maxSamplingTime = maxSamplingTime;
	}

	/**
	 * Creates a new likelihood weighting algorithm with the specified number of
	 * samples and sampling time
	 * 
	 */
	public SamplingAlgorithm() {
	}

	// ===================================
	// PUBLIC METHODS
	// ===================================

	/**
	 * Queries for the probability distribution of the set of random variables in the
	 * Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution failed
	 */
	@Override
	public EmpiricalDistribution queryProb(Query.ProbQuery query) {

		// creates a new query thread
		LikelihoodWeighting isquery =
				new LikelihoodWeighting(query, nbSamples, maxSamplingTime);

		// extract and redraw the samples according to their weight.
		List<Sample> samples = isquery.getSamples();
		// creates an empirical distribution from the samples
		return new EmpiricalDistribution(samples);
	}

	/**
	 * Extracts a unique (non reweighted) sample for the query.
	 * 
	 * @param network the network on which to extract the sample
	 * @param queryVars the variables to extract
	 * @return the extracted sample
	 */
	public static Assignment extractSample(BNetwork network,
			Collection<String> queryVars) {
		// creates a new query thread
		Query query = new Query.ProbQuery(network, queryVars, new Assignment());
		LikelihoodWeighting isquery =
				new LikelihoodWeighting(query, 1, Settings.maxSamplingTime);

		// extract and redraw the samples according to their weight.
		List<Sample> samples = isquery.getSamples();
		if (samples.isEmpty()) {
			throw new RuntimeException("could not extract sample");
		}
		else {
			return samples.get(0).getTrimmed(query.getQueryVars());
		}
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

		try {
			// creates a new query thread
			LikelihoodWeighting isquery =
					new LikelihoodWeighting(query, nbSamples, maxSamplingTime);

			// extract and redraw the samples
			List<Sample> samples = isquery.getSamples();

			// creates the utility table from the samples
			UtilityTable utilityTable = new UtilityTable();
			samples.stream()
					.forEach(s -> utilityTable.incrementUtil(s, s.getUtility()));

			return utilityTable;
		}
		catch (Exception e) {
			e.printStackTrace();
			return new UtilityTable();
		}
	}

	/**
	 * Queries for the utility without any particular query variable
	 * 
	 * @param network the graphical model
	 * @return the utility
	 */

	public double queryUtil(BNetwork network) {

		// creates a new query thread
		Query query = new Query.UtilQuery(network, network.getChanceNodeIds(),
				new Assignment());
		LikelihoodWeighting isquery =
				new LikelihoodWeighting(query, nbSamples, maxSamplingTime);

		// extract and redraw the samples
		List<Sample> samples = isquery.getSamples();

		double total =
				samples.stream().parallel().mapToDouble(s -> s.getUtility()).sum();
		return total / samples.size();
	}

	/**
	 * Reduces the Bayesian network to a subset of its variables and returns the
	 * result.
	 * 
	 * <p>
	 * NB: the equivalent "reduce" method includes additional speed-up methods to
	 * simplify the reduction process.
	 * 
	 * @param query the reduction query
	 * @return the reduced Bayesian network
	 */
	@Override
	public BNetwork reduce(Query.ReduceQuery query) {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		// creates a new query thread
		LikelihoodWeighting isquery =
				new LikelihoodWeighting(query, nbSamples, maxSamplingTime);

		// extract and redraw the samples
		List<Sample> samples = isquery.getSamples();

		EmpiricalDistribution fullDistrib = new EmpiricalDistribution(samples);

		// create the reduced network
		BNetwork reduced = new BNetwork();
		for (String var : query.getSortedQueryVars()) {

			Set<String> inputNodesIds =
					network.getNode(var).getAncestorsIds(queryVars);
			for (String inputNodeId : new ArrayList<String>(inputNodesIds)) {

				// remove the continuous nodes from the inputs (as a conditional
				// probability
				// distribution with a continuous dependent variable is hard to
				// construct)
				ChanceNode inputNode = reduced.getChanceNode(inputNodeId);
				if (inputNode.getDistrib() instanceof ContinuousDistribution) {
					inputNodesIds.remove(inputNodeId);
				}
			}

			ProbDistribution distrib = fullDistrib.getMarginal(var, inputNodesIds);
			// creating the node
			ChanceNode node = new ChanceNode(var, distrib);
			for (String inputId : inputNodesIds) {
				node.addInputNode(reduced.getNode(inputId));
			}
			reduced.addNode(node);
		}

		return reduced;
	}

	/**
	 * Returns an empirical distribution for the particular query, after reweighting
	 * each samples based on the provided weighting scheme.
	 * 
	 * @param query the query
	 * @param weightScheme the weighting scheme for the samples
	 * @return the resulting empirical distribution for the query variables, after
	 *         reweigthing
	 */
	public EmpiricalDistribution getWeightedSamples(Query query,
			Consumer<Collection<Sample>> weightScheme) {

		LikelihoodWeighting isquery =
				new LikelihoodWeighting(query, nbSamples, maxSamplingTime);
		List<Sample> samples = isquery.getSamples();
		weightScheme.accept(samples);
		Intervals<Sample> intervals =
				new Intervals<Sample>(samples, s -> s.getWeight());

		EmpiricalDistribution distrib = new EmpiricalDistribution();

		int sampleSize = samples.size();
		for (int j = 0; j < sampleSize; j++) {
			distrib.addSample(intervals.sample());
		}
		return distrib;
	}

}
