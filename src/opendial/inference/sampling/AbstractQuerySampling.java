package opendial.inference.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Timer;

import opendial.arch.Logger;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.Query;

public abstract class AbstractQuerySampling extends Thread {

	// logger
	public static Logger log = new Logger("AbstractQuerySampling", Logger.Level.DEBUG);
	

	// number of sampling threads to run in parallel
	public static int NB_THREADS = 2;

	// actual number of samples for the algorithm
	int nbSamples = 300;

	// the list of threads currently running
	List<SampleCollector> threads = new ArrayList<SampleCollector>();

	// the stack of weighted samples which have been collected so far
	Stack<WeightedSample> samples;

	// the total weight accumulated by the samples
	double totalWeight = 0.0f;

	// the query
	Query query;
	
	boolean inCompilation = false;
	
	

	/**
	 * Creates a new sampling query with the given arguments
	 * 
	 * @param network the Bayesian network
	 * @param query the query to answer
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public AbstractQuerySampling(Query query,int nbSamples, long maxSamplingTime) {

		this.query = query;
		samples = new Stack<WeightedSample>();

		// creates the sampling threads
		for (int i =0 ; i < NB_THREADS ; i++) {
			SampleCollector newThread = new SampleCollector(this, query);
			threads.add(newThread);
		}

		this.nbSamples = nbSamples;

		Timer timer = new Timer();
		timer.schedule(new StopSamplingTask(this, maxSamplingTime), maxSamplingTime);
	}



	/**
	 * Starts the sampling threads 
	 */
	public void run() {
		for (SampleCollector thread : threads) {
			thread.start();
		}
	}


	/**
	 * Adds a sample to the stack of collected samples.  If the desired
	 * number of samples is achieved, compile the results and notifies waiting
	 * threads that the results can be collected
	 * 
	 * @param sample
	 */
	protected void addSample (WeightedSample sample) {
		if (samples.size() < nbSamples) {
			if (!inCompilation) {
			samples.add(sample);
			totalWeight += sample.getWeight();
			//	log.debug("adding sample " + samples.size());
			}
		}
		else {
			terminateThreads();
		}
	}

	

	/**
	 * Terminates all sampling threads, compile their results, and notifies
	 * the sampling algorithm.
	 */
	public synchronized void terminateThreads() {
		inCompilation = true;
		if (!isTerminated()) {
			for (SampleCollector thread : new ArrayList<SampleCollector>(threads)) {
				thread.terminate();
			}
			compileResults();
			notifyAll(); 
		}
		inCompilation = false;
	}

	protected abstract void compileResults();


	public abstract  boolean isTerminated();



	public Query getQuery() {
		return query;
	}



	public Stack<WeightedSample> getSamples() {
		return samples;
	}
}
