package opendial.inference.approximate;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.inference.Query;

/**
 * Sampling process for a particular query
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class SamplingProcess extends AnytimeProcess {

	// logger
	public static Logger log = new Logger("SamplingProcess", Logger.Level.DEBUG);

	// actual number of samples for the algorithm
	int nbSamples;

	public static double WEIGHT_THRESHOLD = 0.00001f;

	// the stack of weighted samples which have been collected so far
	Stack<WeightedSample> samples;

	// the total weight accumulated by the samples
	double totalWeight = 0.0f;

	// the query
	Query query;

	// termination status
	boolean isTerminated = false; 


	// ===================================
	//  PUBLIC METHODS
	// ===================================


	/**
	 * Creates a new sampling query with the given arguments
	 * 
	 * @param query the query to answer
	 * @param nbSamples the number of samples to collect
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public SamplingProcess(Query query,int nbSamples, long maxSamplingTime) {
		super(maxSamplingTime);
		this.query = query;
		samples = new Stack<WeightedSample>();
		this.nbSamples = nbSamples;
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
	 * Runs the sample collection procedure until termination (either due to a time-out 
	 * or the collection of a number of samples = nbSamples).  The method loops until
	 * terminate() is called, or enough samples have been collected. 
	 * 
	 */
	@Override
	public void run() {
		List<BNode> sortedNodes = query.getFilteredSortedNodes();
		Collections.reverse(sortedNodes);

		// continue until the thread is marked as finished
		while (!isTerminated) {
			try {
				WeightedSample sample = new WeightedSample();

				for (BNode n : sortedNodes) {

					// if the value is already part of the sample, skip to next one
					if (sample.containsVar(n.getId())) {
						continue;
					}

					// if the node is an evidence node and has no input nodes
					else if (n.getInputNodeIds().isEmpty() && query.getEvidence().containsVar(n.getId())) {
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
				if (sample.getWeight() > WEIGHT_THRESHOLD) {
					addSample(sample);
				}

			}
			catch (DialException e) {
				log.info("exception caught: " + e);
			}
		}
	}



	/**
	 * Returns the collected samples
	 * 
	 * @return the collected samples
	 */
	public Stack<WeightedSample> getSamples() {
		start();
		try {	join(); } 
		catch (InterruptedException e) { e.printStackTrace(); }
		return samples;
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Adds a sample to the stack of collected samples.  If the desired
	 * number of samples is achieved, terminate the sample collection.
	 * 
	 * @param sample the sample to add
	 */
	private void addSample (WeightedSample sample) {
		if (samples.size() < nbSamples) {
			if (!isTerminated) {
				samples.add(sample);
				totalWeight += sample.getWeight();
			}
		}
		else {
			terminate();
		}
	}


	/**
	 * Samples the given chance node and add it to the sample.  If the variable is part
	 * of the evidence, updates the weight.
	 * 
	 * @param n the chance node to sample
	 * @param sample to weighted sample to extend
	 * @throws DialException if the sampling operation failed
	 */
	private void sampleChanceNode(ChanceNode n, WeightedSample sample) throws DialException {

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
				evidenceProb = ((ContinuousDistribution)n.getDistrib()).getProbDensity(evidenceValue);
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
	private void sampleActionNode(ActionNode n, WeightedSample sample) {

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

	@Override
	public boolean isTerminated() {
		return isTerminated;
	}


}
