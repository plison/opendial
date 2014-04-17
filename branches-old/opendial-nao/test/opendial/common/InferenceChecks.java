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

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.NaiveInference;
import opendial.inference.exact.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;

public class InferenceChecks {


	// logger
	public static Logger log = new Logger("InferenceChecks", Logger.Level.DEBUG);
	
	
	VariableElimination ve;
	LikelihoodWeighting is;
	LikelihoodWeighting is2;
	NaiveInference naive;

	boolean includeNaive = false;

	Map<InferenceAlgorithm, Long> timings;
	Map<InferenceAlgorithm, Integer> numbers;


	public double EXACT_THRESHOLD = 0.01;
	public double SAMPLING_THRESHOLD = 0.1;
	
	
	public InferenceChecks() {

		ve = new VariableElimination();
		is = new LikelihoodWeighting();
		is2 = new LikelihoodWeighting(4000, 500);
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


	public void checkProb (ProbQuery query) throws DialException {
		
		DiscreteDistribution distrib1 = compute(query, ve).toDiscrete();
		DiscreteDistribution distrib2 = compute(query, is).toDiscrete();
	
		try { compareDistributions(distrib1, distrib2, 0.1); }
		catch (AssertionError e) {
			distrib2 = compute(query, is2).toDiscrete();
			log.debug("resampling for query " + query);
			compareDistributions(distrib1, distrib2, 0.1); 
		}
		if (includeNaive) {
			DiscreteDistribution distrib3 = compute(query, naive).toDiscrete();	
			compareDistributions(distrib1, distrib3, 0.01); 
		}
	}
	
	

	public void checkProb (ProbQuery query, Assignment a, double expected) throws DialException {

		CategoricalTable distrib1 = compute(query, ve).toDiscrete();
		CategoricalTable distrib2 = compute(query, is).toDiscrete();

		assertEquals(expected, distrib1.getProb(a), EXACT_THRESHOLD);
		
			try { assertEquals(expected, distrib2.getProb(a), SAMPLING_THRESHOLD);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2).toDiscrete();
				assertEquals(expected, distrib2.getProb(a), SAMPLING_THRESHOLD);
			}

			if (includeNaive) {
				CategoricalTable distrib3 = compute(query, naive).toDiscrete();
				assertEquals(expected, distrib3.toDiscrete().getProb(a), EXACT_THRESHOLD);
			}
	}


	
	public void checkCDF (ProbQuery query, Assignment a, double expected) throws DialException {

		ContinuousDistribution distrib1 = compute(query, ve).toContinuous();
		ContinuousDistribution distrib2 = compute(query, is).toContinuous();

		assertEquals(expected, distrib1.getCumulativeProb(a), EXACT_THRESHOLD);
		
			try { assertEquals(expected, distrib2.getCumulativeProb(a), SAMPLING_THRESHOLD);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2).toContinuous();
				assertEquals(expected, distrib2.getCumulativeProb(a), SAMPLING_THRESHOLD);
			}

			if (includeNaive) {
				ContinuousDistribution distrib3 = compute(query, naive).toContinuous();
				assertEquals(expected, distrib3.toDiscrete().getProb(new Assignment(), a), EXACT_THRESHOLD);
			}
	} 

	

	public void checkUtil(UtilQuery query, Assignment a, double expected) throws DialException {
			
		UtilityDistribution distrib1 = compute(query, ve);
		UtilityDistribution distrib2 = compute(query, is);

			assertEquals(expected, distrib1.getUtil(a), EXACT_THRESHOLD);
			try { assertEquals(expected, distrib2.getUtil(a), SAMPLING_THRESHOLD * 5);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2);
				assertEquals(expected, distrib2.getUtil(a), SAMPLING_THRESHOLD * 5);
			}

			if (includeNaive) {
				UtilityDistribution distrib3 = compute(query, naive);
				assertEquals(expected, distrib3.getUtil(a), EXACT_THRESHOLD);
			}
		}
	
	

	
	private IndependentProbDistribution compute(ProbQuery query, 
			InferenceAlgorithm algo) throws DialException {
		

			long time1 = System.nanoTime();
			IndependentProbDistribution distrib = algo.queryProb(query);
			long inferenceTime = System.nanoTime() - time1;
			numbers.put(algo, numbers.get(algo) + 1);
			timings.put(algo, timings.get(algo) + inferenceTime);
			return distrib;
	}
	
	
	private UtilityDistribution compute(UtilQuery query, InferenceAlgorithm algo) throws DialException {
		
			long time1 = System.nanoTime();
			UtilityDistribution distrib = algo.queryUtil(query);
			long inferenceTime = System.nanoTime() - time1;
			numbers.put(algo, numbers.get(algo) + 1);
			timings.put(algo, timings.get(algo) + inferenceTime);
	
			return distrib;
		
	}
	

	
	private void compareDistributions(DiscreteDistribution distrib1, 
			DiscreteDistribution distrib2, double margin) throws DialException {
		
		Collection<Assignment> rows = distrib1.getPosterior(new Assignment()).getRows();
		for (Assignment value : rows) {
			assertEquals(distrib1.getProb(new Assignment(), value), 
					distrib2.getProb(new Assignment(), value), margin);
		}
	}

	public void showPerformance() {
		log.info("--------------------");
		if (includeNaive) {
		log.info("Average time for naive inference: " +
				(timings.get(naive) / (1000000.0 * numbers.get(naive))) + " ms.");
		}
		log.info("Average time for variable elimination: " +
				(timings.get(ve) / (1000000.0 * numbers.get(ve))) + " ms.");
		log.info("Average time for importance sampling: " + 
				((timings.get(is) + timings.get(is2)) / (1000000.0 * numbers.get(is))) + " ms. (with " +
				(numbers.get(is2) *100 / numbers.get(is)) + "% of repeats)");
		log.info("--------------------");
	}
	
}
