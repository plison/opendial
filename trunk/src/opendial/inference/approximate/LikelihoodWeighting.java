package opendial.inference.approximate;

import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.inference.Query;

/**
 * Sampling process (based on likelihood weighting) for a particular query.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class LikelihoodWeighting implements AnytimeProcess {

	// logger
	public static Logger log = new Logger("SamplingProcess", Logger.Level.DEBUG);

	// actual number of samples for the algorithm
	int nbSamples;

	public static double WEIGHT_THRESHOLD = 0.00001f;

	// the stack of weighted samples which have been collected so far
	List<Sample> samples;

	// the query
	Query query;

	// sorted nodes in the network
	List<BNode> sortedNodes;

	// termination status
	boolean isTerminated = false; 


	// ===================================
	//  PUBLIC METHODS
	// ===================================


	/**
	 * Creates a new sampling query with the given arguments and starts
	 * sampling (using parallel streams). 
	 * 
	 * @param query the query to answer
	 * @param nbSamples the number of samples to collect
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public LikelihoodWeighting(Query query,int nbSamples, long maxSamplingTime) {
		this.query = query;
		samples = new Stack<Sample>();
		this.nbSamples = nbSamples;
		sortedNodes = query.getFilteredSortedNodes();
		Collections.reverse(sortedNodes);
		setTimeout(maxSamplingTime);
		
		samples = Stream.generate(() -> this)  	// creates infinite stream
				.filter(p -> !p.isTerminated)  	// stop when process is terminated
				.parallel()						// parallelise
				.map(p -> p.sample())			// generate a sample
				.filter(s -> !s.isEmpty())		// discard empty samples	
				.limit(nbSamples)				// stop when nbSamples are collected
				.collect(Collectors.toList());	// makes a list of samples
	}


	/**
	 * Terminates all sampling threads, compile their results, and notifies
	 * the sampling algorithm.
	 */
	@Override
	public void terminate() {
		if (!isTerminated) {
			if (samples.size() == 0) {
				log.debug("no samples for query: " + query);
				query.getEvidence().clear();
			}
			else {
				isTerminated = true;
			}
		}
	}


	/**
	 * Returns a string representation of the query and number of collected samples
	 */
	@Override
	public String toString() {
		return query.toString() + " (" + samples.size() + " samples already collected)";
	}


	/**
	 * Returns the collected samples
	 * 
	 * @return the collected samples
	 */
	public List<Sample> getSamples() {
		redrawSamples();
		return samples;
	}



	@Override
	public boolean isTerminated() {
		return isTerminated;
	}


	/**
	 * Runs the sample collection procedure until termination (either due to a time-out 
	 * or the collection of a number of samples = nbSamples).  The method loops until
	 * terminate() is called, or enough samples have been collected. 
	 * 
	 */
	protected Sample sample() {

		Sample sample = new Sample();
		try {
			for (BNode n : sortedNodes) {

				// if the value is already part of the sample, skip to next one
				if (sample.containsVar(n.getId())) {
					continue;
				}

				// if the node is an evidence node and has no input nodes
				else if (n.getInputNodeIds().isEmpty() 
						&& query.getEvidence().containsVar(n.getId())) {
					sample.addPair(n.getId(), query.getEvidence().getValue(n.getId()));
				}
				else if (n instanceof ChanceNode) {
					sampleChanceNode((ChanceNode)n, sample);
				}

				// if the node is an action node
				else if (n instanceof ActionNode) {
					sampleActionNode((ActionNode)n, sample);
				}

				// finally, if the node is a utility node, calculate the utility
				else if (n instanceof UtilityNode) {
					double newUtil = ((UtilityNode)n).getUtility(sample);
					sample.addUtility(newUtil);
				}
			}

			// we only add the sample if the weight is larger than a given threshold
			if (sample.getWeight() < WEIGHT_THRESHOLD) {
				sample.clear();
			}

			sample.trim(query.getQueryVars());

		}
		catch (DialException e) {
			log.info("exception caught: " + e);
		}
		return sample;
	}



	// ===================================
	//  PRIVATE METHODS
	// ===================================



	/**
	 * Samples the given chance node and add it to the sample.  If the variable is part
	 * of the evidence, updates the weight.
	 * 
	 * @param n the chance node to sample
	 * @param sample to weighted sample to extend
	 * @throws DialException if the sampling operation failed
	 */
	private void sampleChanceNode(ChanceNode n, Sample sample) throws DialException {

		// if the node is a chance node and is not evidence, sample from the values
		if (!query.getEvidence().containsVar(n.getId())) {
			Value newVal = n.sample(sample);
			sample.addPair(n.getId(), newVal);				
		}

		// if the node is an evidence node, update the weights
		else {
			Value evidenceValue = query.getEvidence().getValue(n.getId());
			double evidenceProb = 1.0;
			if (n.getDistrib() instanceof ContinuousDistribution) {
				evidenceProb = ((ContinuousDistribution)n.getDistrib()).
						getProbDensity(evidenceValue);
			}
			else {
				evidenceProb = n.getProb(sample, evidenceValue);	
			}
			sample.addLogWeight(Math.log(evidenceProb));						
			sample.addPair(n.getId(), evidenceValue);
		}
	}


	/**
	 * Samples the action node.  If the node is part of the evidence, simply add it to 
	 * the sample. Else, samples an action at random.
	 * 
	 * @param n the action node 
	 * @param sample the weighted sample to extend
	 */
	private void sampleActionNode(ActionNode n, Sample sample) {

		if (!query.getEvidence().containsVar(n.getId()) && 
				n.getInputNodeIds().isEmpty()) {
			Value newVal = n.sample(sample);
			sample.addPair(n.getId(), newVal);
		}
		else {
			Value evidenceValue = query.getEvidence().getValue(n.getId());
			double evidenceProb = n.getProb(evidenceValue);
			sample.addLogWeight(Math.log(evidenceProb));						
			sample.addPair(n.getId(), evidenceValue);
		}
	}


	/**
	 * Redraw the samples according to their weight.  The number of redrawn samples is the same 
	 * as the one given as argument.
	 * 
	 * @param samples the initial samples (with their weight)
	 * @return the redrawn samples given their weight
	 * @throws DialException if the samples could not be redrawn.
	 */
	private void redrawSamples() {

		try {
			Intervals<Sample> intervals = new Intervals<Sample>(samples, s -> s.getWeight());

			Stack<Sample> newSamples = new Stack<Sample>();
			int sampleSize = samples.size();
			for (int j = 0 ; j < sampleSize; j++) {
				newSamples.add(intervals.sample());
			}
			samples = newSamples;
		}
		catch (DialException e) {
			log.warning("could not redraw samples: "  +e );
		}
	}




}
