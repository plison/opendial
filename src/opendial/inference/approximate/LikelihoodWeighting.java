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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.EmpiricalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.Query;

/**
 * Inference algorithm for Bayesian networks based on normalised importance sampling 
 * (likelihood weighting).  The algorithm relies on the selection of samples from the
 * network, which are weighted according to the specified evidence.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class LikelihoodWeighting implements InferenceAlgorithm {

	// logger
	public static Logger log = new Logger("LikelihoodWeighting", Logger.Level.DEBUG);

	int nbSamples = Settings.nbSamples;

	long maxSamplingTime = Settings.maxSamplingTime;


	// ===================================
	//  CONSTRUCTORS
	// ===================================
	
	/**
	 * Creates a new likelihood weighting algorithm with the specified number of 
	 * samples and sampling time
	 * 
	 * @param nbSamples the maximum number of samples to collect
	 * @param maxSamplingTime the maximum sampling time
	 */
	public LikelihoodWeighting(int nbSamples, long maxSamplingTime) {
		this.nbSamples = nbSamples;
		this.maxSamplingTime = maxSamplingTime;
	}


	/**
	 * Creates a new likelihood weighting algorithm with the specified number of 
	 * samples and sampling time
	 * 
	 */
	public LikelihoodWeighting() { }

	

	// ===================================
	//  PUBLIC METHODS
	// ===================================
	

	/**
	 * Queries for the probability distribution of the set of random variables in 
	 * the Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution
	 * @throws DialException if the inference operation failed
	 */
	@Override
	public EmpiricalDistribution queryProb(Query.ProbQuery query) throws DialException {

		// creates a new query thread
		SamplingProcess isquery = new SamplingProcess(query, nbSamples, maxSamplingTime);
		
		// extract and redraw the samples according to their weight.
		Stack<WeightedSample> samples = isquery.getSamples();
		samples = redrawSamples(samples);

		// creates an empirical distribution from the samples
		EmpiricalDistribution empiricalDistrib = new EmpiricalDistribution();
		for (WeightedSample sample : samples) {
			sample.trim(query.getQueryVars());
			empiricalDistrib.addSample(sample);
		}

		return empiricalDistrib;
	}

	
	/**
	 * Extracts a unique (non reweighted) sample for the query.
	 * 
	 * @param query the query
	 * @return the extracted sample
	 * @throws DialException
	 */
	public static Assignment extractSample(BNetwork network, Collection<String> queryVars) 
			throws DialException {
		// creates a new query thread
		Query query = new Query.ProbQuery(network, queryVars, new Assignment());
		SamplingProcess isquery = new SamplingProcess(query, 1, Settings.maxSamplingTime);
		
		// extract and redraw the samples according to their weight.
		Stack<WeightedSample> samples = isquery.getSamples();
		if (samples.isEmpty()) {
			throw new DialException("could not extract sample");
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
	 * @throws DialException if the inference operation failed
	 */
	@Override
	public UtilityTable queryUtil(Query.UtilQuery query) throws DialException {

		// creates a new query thread
		SamplingProcess isquery = new SamplingProcess(query, nbSamples, maxSamplingTime);
		
		// extract and redraw the samples
		Stack<WeightedSample> samples = isquery.getSamples();
		samples = redrawSamples(samples);

		// creates the utility table from the samples
		UtilityTable utilityTable = new UtilityTable();

		for (WeightedSample sample : samples) {
			sample.trim(query.getQueryVars());
			utilityTable.incrementUtil(sample, sample.getUtility());
		} 

		return utilityTable;
	}

	
	/**
	 * Queries for the utility without any particular query variable
	 * 
	 * @param network the graphical model
	 * @return the utility
	 * @throws DialException if the inference operation failed
	 */
	
	public double queryUtil(BNetwork network) throws DialException {

		// creates a new query thread
		Query query = new Query.UtilQuery(network, network.getChanceNodeIds(), new Assignment());
		SamplingProcess isquery = new SamplingProcess(query, nbSamples, maxSamplingTime);
		
		
		// extract and redraw the samples
		Stack<WeightedSample> samples = isquery.getSamples();

		samples = redrawSamples(samples);

		double total = 0.0;
		for (WeightedSample sample : samples) {
			total += sample.getUtility();
		} 

		return total / samples.size();
	}


	/**
	 * Reduces the Bayesian network to a subset of its variables and returns the result.
	 * 
	 * <p>NB: the equivalent "reduce" method includes additional speed-up methods to
	 * simplify the reduction process.
	 * 
	 * @param query the reduction query
	 * @return the reduced Bayesian network
	 */
	@Override
	public BNetwork reduce(Query.ReduceQuery query) throws DialException {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		
		// creates a new query thread
		SamplingProcess isquery = new SamplingProcess(query, nbSamples, maxSamplingTime);
		
		// extract and redraw the samples
		Stack<WeightedSample> samples = isquery.getSamples();
		samples = redrawSamples(samples);
		EmpiricalDistribution fullDistrib = new EmpiricalDistribution();
		for (WeightedSample sample : samples) {
			sample.trim(query.getQueryVars());
			fullDistrib.addSample(sample);
		}

		// create the reduced network
		BNetwork reduced = new BNetwork();
		for (String var: query.getSortedQueryVars()) {
			
			Set<String> inputNodesIds = network.getNode(var).getAncestorsIds(queryVars);
			for (String inputNodeId : new ArrayList<String>(inputNodesIds)) {
				
				// remove the continuous nodes from the inputs (as a conditional probability
				// distribution with a continuous dependent variable is hard to construct)
				ChanceNode inputNode = reduced.getChanceNode(inputNodeId);
				if (inputNode.getDistrib() instanceof ContinuousDistribution) {
					inputNodesIds.remove(inputNodeId);
				}
			}
			
			ProbDistribution distrib = fullDistrib.getMarginal(var, inputNodesIds);

			// creating the node
			ChanceNode node = new ChanceNode(var);
			node.setDistrib(distrib);
			for (String inputId : inputNodesIds) {
				node.addInputNode(reduced.getNode(inputId));
			}
			reduced.addNode(node);
		}

		return reduced;
	}

	

	// ===================================
	//  PRIVATE METHODS
	// ===================================
	


	/**
	 * Redraw the samples according to their weight.  The number of redrawn samples is the same 
	 * as the one given as argument.
	 * 
	 * @param samples the initial samples (with their weight)
	 * @return the redrawn samples given their weight
	 * @throws DialException
	 */
	public static Stack<WeightedSample> redrawSamples(Stack<WeightedSample> samples)
			throws DialException {

		int sampleSize = samples.size();
		WeightedSample[] sampleArray = new WeightedSample[sampleSize];
		double[] weightArray = new double[sampleSize];
		for (int i = 0 ; i < sampleSize ; i++) {
			WeightedSample sample = samples.pop();
			sampleArray[i] = sample;
			weightArray[i] = sample.getWeight();
		}

		Intervals<WeightedSample> intervals = new Intervals<WeightedSample>(sampleArray, weightArray);
		Stack<WeightedSample> reweightedSamples = new Stack<WeightedSample>();

		for (int j = 0 ; j < sampleSize; j++) {
			WeightedSample sample = intervals.sample();
			reweightedSamples.add(sample);
		}
		return reweightedSamples;
	}


	


}
