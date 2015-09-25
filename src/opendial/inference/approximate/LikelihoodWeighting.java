package opendial.inference.approximate;

import java.util.logging.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.inference.Query;

/**
 * Sampling process (based on likelihood weighting) for a particular query.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class LikelihoodWeighting {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// actual number of samples for the algorithm
	int nbSamples;

	public static double WEIGHT_THRESHOLD = 0.0001f;

	// the stack of weighted samples which have been collected so far
	Stack<Sample> samples = new Stack<Sample>();

	// the query
	Query query;
	Collection<String> queryVars;
	Assignment evidence;

	// sorted nodes in the network
	List<BNode> sortedNodes;

	// termination status
	boolean isTerminated = false;

	// scheduled thread pool to terminate sampling once the time limit is
	// reached
	static ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

	// ===================================
	// PUBLIC METHODS
	// ===================================

	/**
	 * Creates a new sampling query with the given arguments and starts sampling
	 * (using parallel streams).
	 * 
	 * @param query the query to answer
	 * @param nbSamples the number of samples to collect
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public LikelihoodWeighting(Query query, int nbSamples, long maxSamplingTime) {
		this.query = query;
		this.evidence = query.getEvidence();
		this.queryVars = query.getQueryVars();

		this.nbSamples = nbSamples;
		sortedNodes = query.getFilteredSortedNodes();
		Collections.reverse(sortedNodes);
		service.schedule(() -> isTerminated = true, maxSamplingTime,
				TimeUnit.MILLISECONDS);
		Stream.generate(() -> this)
				// creates infinite stream
				.parallel()
				// parallelise
				.map(p -> p.sample())
				// generate a sample
				.limit(nbSamples)
				// stop when nbSamples are collected
				.filter(s -> s.getWeight() > WEIGHT_THRESHOLD)
				.filter(s -> !s.isEmpty()) // discard empty samples
				.forEach(s -> samples.add(s)); // makes a list of samples
	}

	/**
	 * Returns a string representation of the query and number of collected samples
	 */
	@Override
	public String toString() {
		return query.toString() + " (" + samples.size()
				+ " samples already collected)";
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

	/**
	 * Runs the sample collection procedure until termination (either due to a
	 * time-out or the collection of a number of samples = nbSamples). The method
	 * loops until terminate() is called, or enough samples have been collected.
	 * 
	 * @return the resulting sample
	 * 
	 */
	protected Sample sample() {
		Sample sample = new Sample();
		if (isTerminated) {
			return sample;
		}
		try {
			for (BNode n : sortedNodes) {
				String id = n.getId();

				// if the node is an evidence node and has no input nodes
				if (n.getInputNodeIds().isEmpty() && evidence.containsVar(id)) {
					sample.addPair(id, evidence.getValue(id));

				}
				else if (n instanceof ChanceNode) {
					sampleChanceNode((ChanceNode) n, sample);
				}

				// if the node is an action node
				else if (n instanceof ActionNode) {
					sampleActionNode((ActionNode) n, sample);
				}

				// finally, if the node is a utility node, calculate the utility
				else if (n instanceof UtilityNode) {
					double newUtil = ((UtilityNode) n).getUtility(sample);
					sample.addUtility(newUtil);
				}
			}
			sample.trim(queryVars);
		}
		catch (RuntimeException e) {
			log.warning("exception caught: " + e);
			e.printStackTrace();
		}
		return sample;
	}

	// ===================================
	// PRIVATE METHODS
	// ===================================

	/**
	 * Samples the given chance node and add it to the sample. If the variable is
	 * part of the evidence, updates the weight.
	 * 
	 * @param n the chance node to sample
	 */
	private void sampleChanceNode(ChanceNode n, Sample sample) {

		String id = n.getId();
		// if the node is chance node and not evidence, sample from the values
		if (!evidence.containsVar(id)) {
			Value newVal = n.sample(sample);
			sample.addPair(id, newVal);
		}

		// if the node is an evidence node, update the weights
		else {
			Value evidenceValue = evidence.getValue(id);
			ProbDistribution distrib = n.getDistrib();
			double evidenceProb = 1.0;
			if (distrib instanceof ContinuousDistribution) {
				evidenceProb = ((ContinuousDistribution) distrib)
						.getProbDensity(evidenceValue);
			}
			else {
				evidenceProb = n.getProb(sample, evidenceValue);
			}
			sample.addLogWeight(Math.log(evidenceProb));
			sample.addPair(id, evidenceValue);
		}
	}

	/**
	 * Samples the action node. If the node is part of the evidence, simply add it to
	 * the sample. Else, samples an action at random.
	 * 
	 * @param n the action node
	 * @param sample the weighted sample to extend
	 */
	private void sampleActionNode(ActionNode n, Sample sample) {

		String id = n.getId();
		if (!evidence.containsVar(id) && n.getInputNodeIds().isEmpty()) {
			Value newVal = n.sample();
			sample.addPair(id, newVal);
		}
		else {
			Value evidenceValue = evidence.getValue(id);
			sample.addPair(id, evidenceValue);
		}
	}

	/**
	 * Redraw the samples according to their weight. The number of redrawn samples is
	 * the same as the one given as argument.
	 * 
	 * @param samples the initial samples (with their weight)
	 * @return the redrawn samples given their weight redrawn.
	 */
	private void redrawSamples() {
		try {
			Intervals<Sample> intervals =
					new Intervals<Sample>(samples, s -> s.getWeight());
			Stack<Sample> newSamples = new Stack<Sample>();
			int sampleSize = samples.size();
			for (int j = 0; j < sampleSize; j++) {
				newSamples.add(intervals.sample());
			}
			samples = newSamples;
		}
		catch (RuntimeException e) {
			log.warning("could not redraw samples: " + e);
			e.printStackTrace();
		}
	}

}
