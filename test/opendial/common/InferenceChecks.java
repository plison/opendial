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

package opendial.common;

import java.util.logging.*;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import opendial.bn.BNetwork;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.UtilityFunction;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.Query;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.inference.exact.NaiveInference;
import opendial.inference.exact.VariableElimination;

public class InferenceChecks {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	VariableElimination ve;
	SamplingAlgorithm is;
	SamplingAlgorithm is2;
	NaiveInference naive;

	boolean includeNaive = false;

	Map<InferenceAlgorithm, Long> timings;
	Map<InferenceAlgorithm, Integer> numbers;

	public double EXACT_THRESHOLD = 0.01;
	public double SAMPLING_THRESHOLD = 0.1;

	public InferenceChecks() {

		ve = new VariableElimination();
		is = new SamplingAlgorithm(2000, 200);
		is2 = new SamplingAlgorithm(15000, 1500);
		naive = new NaiveInference();

		timings = new HashMap<InferenceAlgorithm, Long>();
		numbers = new HashMap<InferenceAlgorithm, Integer>();
		timings.put(ve, 0l);
		numbers.put(ve, 0);
		timings.put(is, 0l);
		numbers.put(is, 0);
		timings.put(is2, 0l);
		numbers.put(is2, 0);
		timings.put(naive, 0l);
		numbers.put(naive, 0);
	}

	public void includeNaive(boolean includeNaive) {
		this.includeNaive = includeNaive;
	}

	public void checkProb(BNetwork network, Collection<String> queryVars,
			Assignment evidence) {

		Query.ProbQuery query =
				new Query.ProbQuery(network, queryVars, new Assignment());
		MultivariateDistribution distrib1 = computeProb(query, ve);
		MultivariateDistribution distrib2 = computeProb(query, is);

		try {
			compareDistributions(distrib1, distrib2, 0.1);
		}
		catch (AssertionError e) {
			distrib2 = computeProb(query, is2);
			log.fine("resampling for query "
					+ new Query.ProbQuery(network, queryVars, evidence));
			compareDistributions(distrib1, distrib2, 0.1);
		}
		if (includeNaive) {
			MultivariateDistribution distrib3 = computeProb(query, naive);
			compareDistributions(distrib1, distrib3, 0.01);
		}
	}

	public void checkProb(BNetwork network, String queryVar, String a,
			double expected) {
		checkProb(network, Arrays.asList(queryVar), new Assignment(queryVar, a),
				expected);
	}

	public void checkProb(BNetwork network, String queryVar, Value a,
			double expected) {
		checkProb(network, Arrays.asList(queryVar), new Assignment(queryVar, a),
				expected);
	}

	public void checkProb(BNetwork network, Collection<String> queryVars,
			Assignment a, double expected) {

		Query.ProbQuery query =
				new Query.ProbQuery(network, queryVars, new Assignment());

		MultivariateDistribution distrib1 = computeProb(query, ve);
		MultivariateDistribution distrib2 = computeProb(query, is);

		assertEquals(expected, distrib1.getProb(a), EXACT_THRESHOLD);

		try {
			assertEquals(expected, distrib2.getProb(a), SAMPLING_THRESHOLD);
		}
		catch (AssertionError e) {
			distrib2 = computeProb(query, is2);
			assertEquals(expected, distrib2.getProb(a), SAMPLING_THRESHOLD);
		}

		if (includeNaive) {
			MultivariateDistribution distrib3 = computeProb(query, naive);
			assertEquals(expected, distrib3.getProb(a), EXACT_THRESHOLD);
		}
	}

	public void checkCDF(BNetwork network, String variable, double value,
			double expected) {

		Query.ProbQuery query = new Query.ProbQuery(network, Arrays.asList(variable),
				new Assignment());
		ContinuousDistribution distrib1 =
				computeProb(query, ve).getMarginal(variable).toContinuous();
		ContinuousDistribution distrib2 =
				computeProb(query, is).getMarginal(variable).toContinuous();

		assertEquals(expected, distrib1.getCumulativeProb(value), EXACT_THRESHOLD);

		try {
			assertEquals(expected, distrib2.getCumulativeProb(value),
					SAMPLING_THRESHOLD);
		}
		catch (AssertionError e) {
			distrib2 = computeProb(query, is2).getMarginal(variable).toContinuous();
			assertEquals(expected, distrib2.getCumulativeProb(value),
					SAMPLING_THRESHOLD);
		}

		if (includeNaive) {
			ContinuousDistribution distrib3 =
					computeProb(query, naive).getMarginal(variable).toContinuous();
			assertEquals(expected, distrib3.toDiscrete().getProb(value),
					EXACT_THRESHOLD);
		}
	}

	public void checkUtil(BNetwork network, String queryVar, String a,
			double expected) {
		checkUtil(network, Arrays.asList(queryVar), new Assignment(queryVar, a),
				expected);

	}

	public void checkUtil(BNetwork network, Collection<String> queryVars,
			Assignment a, double expected) {

		Query.UtilQuery query =
				new Query.UtilQuery(network, queryVars, new Assignment());
		UtilityFunction distrib1 = computeUtil(query, ve);
		UtilityFunction distrib2 = computeUtil(query, is);

		assertEquals(expected, distrib1.getUtil(a), EXACT_THRESHOLD);
		try {
			assertEquals(expected, distrib2.getUtil(a), SAMPLING_THRESHOLD * 5);
		}
		catch (AssertionError e) {
			distrib2 = computeUtil(query, is2);
			assertEquals(expected, distrib2.getUtil(a), SAMPLING_THRESHOLD * 5);
		}

		if (includeNaive) {
			UtilityFunction distrib3 = computeUtil(query, naive);
			assertEquals(expected, distrib3.getUtil(a), EXACT_THRESHOLD);
		}
	}

	private MultivariateDistribution computeProb(Query.ProbQuery query,
			InferenceAlgorithm algo) {

		long time1 = System.nanoTime();
		MultivariateDistribution distrib = algo.queryProb(query);
		long inferenceTime = System.nanoTime() - time1;
		numbers.put(algo, numbers.get(algo) + 1);
		timings.put(algo, timings.get(algo) + inferenceTime);
		return distrib;
	}

	private UtilityFunction computeUtil(Query.UtilQuery query,
			InferenceAlgorithm algo) {

		long time1 = System.nanoTime();
		UtilityFunction distrib = algo.queryUtil(query);
		long inferenceTime = System.nanoTime() - time1;
		numbers.put(algo, numbers.get(algo) + 1);
		timings.put(algo, timings.get(algo) + inferenceTime);

		return distrib;

	}

	private void compareDistributions(MultivariateDistribution distrib1,
			MultivariateDistribution distrib2, double margin) {

		Collection<Assignment> rows = distrib1.getValues();
		for (Assignment value : rows) {
			assertEquals(distrib1.getProb(value), distrib2.getProb(value), margin);
		}
	}

	public void showPerformance() {
		if (includeNaive) {
			log.info("Average time for naive inference: "
					+ (timings.get(naive) / (1000000.0 * numbers.get(naive)))
					+ " ms.");
		}
		log.info("Average time for variable elimination: "
				+ (timings.get(ve) / (1000000.0 * numbers.get(ve))) + " ms.");
		log.info("Average time for importance sampling: "
				+ ((timings.get(is) + timings.get(is2))
						/ (1000000.0 * numbers.get(is)))
				+ " ms. (with " + (numbers.get(is2) * 100 / numbers.get(is))
				+ "% of repeats)");
	}

}
