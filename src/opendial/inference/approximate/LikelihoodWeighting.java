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

package opendial.inference.approximate;

import java.util.Arrays;
import java.util.Set;
import java.util.Stack;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.other.EmpiricalDistribution;
import opendial.bn.distribs.other.MarginalEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

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

	/** geometric factor used in supervised learning from Wizard-of-Oz data */
	public static final double GEOMETRIC_FACTOR = 0.5;

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


	/**
	 * Queries for the probability distribution of the set of random variables in 
	 * the Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution
	 * @throws DialException if the inference operation failed
	 */
	@Override
	public EmpiricalDistribution queryProb(ProbQuery query) throws DialException {

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
	public static Assignment extractSample(ProbQuery query) throws DialException {
		// creates a new query thread
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
	public UtilityTable queryUtil(UtilQuery query) throws DialException {

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
	 * @param BNetwork network
	 * @return the utility
	 * @throws DialException if the inference operation failed
	 */
	
	public double queryUtil(BNetwork network) throws DialException {

		// creates a new query thread
		SamplingProcess isquery = new SamplingProcess(new UtilQuery(
				network, network.getChanceNodeIds()), nbSamples, maxSamplingTime);
		
		
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
	public BNetwork reduce(ReductionQuery query) throws DialException {

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
		BNetwork network = new BNetwork();
		for (String var: query.getSortedQueryVars()) {
			Set<String> directAncestors = query.getInputNodes(var);

			// creating the empirical distribution
			ProbDistribution distrib = new MarginalEmpiricalDistribution
					(Arrays.asList(var), directAncestors, fullDistrib);

			// creating the node
			ChanceNode node = new ChanceNode(var);
			node.setDistrib(distrib);
			for (String ancestor : directAncestors) {
				node.addInputNode(network.getNode(ancestor));
			}
			network.addNode(node);
		}

		return network;
	}


	/**
	 * Queries for the probability distribution of the set of random variables in 
	 * the Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution
	 * @throws DialException if the inference operation failed
	 */
	public EmpiricalDistribution queryWizard(UtilQuery query, Assignment wizardAction) throws DialException {

		// creates a new query thread
		SamplingProcess isquery = new SamplingProcess(query, nbSamples, maxSamplingTime);
				
		// extract and redraw the samples according to their weight.
		Stack<WeightedSample> samples = isquery.getSamples();
		reweightSamples(samples, wizardAction);
		samples = redrawSamples(samples);

		// creates an empirical distribution from the samples
		EmpiricalDistribution empiricalDistrib = new EmpiricalDistribution();
		for (WeightedSample sample : samples) {
			sample.trim(query.getQueryVars());
			empiricalDistrib.addSample(sample);
		}

		return empiricalDistrib;
	}


	private void reweightSamples(Stack<WeightedSample> samples,
			Assignment wizardAction) {

		UtilityTable averages = new UtilityTable();

		Set<String> actionVars = wizardAction.getVariables();
		for (WeightedSample sample : samples) {
			Assignment action = sample.getTrimmed(actionVars);
			averages.incrementUtil(action, sample.getUtility());
		}
		if (averages.getTable().size() == 1) {
			return;
		}

		log.debug("Utilities : " + averages.toString().replace("\n", ", ") + " ==> gold action = " + wizardAction);

		for (WeightedSample sample : samples) {

			UtilityTable copy = averages.copy();
			Assignment sampleAssign = sample.getTrimmed(actionVars);
			copy.setUtil(sampleAssign, sample.getUtility());
			int ranking = copy.getRanking(wizardAction);
			if (ranking != -1) {
				sample.addLogWeight(Math.log((GEOMETRIC_FACTOR 
						* Math.pow(1-GEOMETRIC_FACTOR, ranking)) + 0.00001));
			}				
		}
	}



	/**
	 * Redraw the samples according to their weight.  The number of redrawn samples is the same 
	 * as the one given as argument.
	 * 
	 * @param samples the initial samples (with their weight)
	 * @return the redrawn samples given their weight
	 * @throws DialException
	 */
	private Stack<WeightedSample> redrawSamples(Stack<WeightedSample> samples) throws DialException {

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
