// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.util.Log;
import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.values.Value;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.NaiveInference;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.UtilQuery;

public class InferenceChecks {


	// logger
	public static Logger log = new Logger("InferenceChecks", Logger.Level.DEBUG);
	
	
	VariableElimination ve;
	ImportanceSampling is;
	ImportanceSampling is2;
	NaiveInference naive;

	boolean includeNaive = false;

	Map<Query, Map<InferenceAlgorithm, ProbDistribution>> probQueryCache;
	Map<Query, Map<InferenceAlgorithm, UtilityDistribution>> utilQueryCache;
	
	Map<InferenceAlgorithm, Long> timings;
	Map<InferenceAlgorithm, Integer> numbers;


	public double EXACT_THRESHOLD = 0.01;
	public double SAMPLING_THRESHOLD = 0.08;
	
	
	public InferenceChecks() {

		ve = new VariableElimination();
		is = new ImportanceSampling(400, 250);
		is2 = new ImportanceSampling(4000, 500);
		naive = new NaiveInference();
		probQueryCache = new HashMap<Query, Map<InferenceAlgorithm, ProbDistribution>>();
		utilQueryCache = new HashMap<Query, Map<InferenceAlgorithm, UtilityDistribution>>();
		
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
		
		DiscreteProbDistribution distrib1 = compute(query, ve).toDiscrete();
		DiscreteProbDistribution distrib2 = compute(query, is).toDiscrete();
	
		try { compareDistributions(distrib1, distrib2, 0.1); }
		catch (AssertionError e) {
			distrib2 = compute(query, is2).toDiscrete();
			log.debug("resampling for query " + query);
			compareDistributions(distrib1, distrib2, 0.1); 
		}
		if (includeNaive) {
			DiscreteProbDistribution distrib3 = compute(query, naive).toDiscrete();	
			compareDistributions(distrib1, distrib3, 0.01); 
		}
	}
	
	

	public void checkProb (ProbQuery query, Assignment a, double expected) throws DialException {

		DiscreteProbDistribution distrib1 = compute(query, ve).toDiscrete();
		DiscreteProbDistribution distrib2 = compute(query, is).toDiscrete();

		assertEquals(expected, distrib1.getProb(new Assignment(), a), EXACT_THRESHOLD);
		
			try { assertEquals(expected, distrib2.getProb(new Assignment(), a), SAMPLING_THRESHOLD);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2).toDiscrete();
				assertEquals(expected, distrib2.getProb(new Assignment(), a), SAMPLING_THRESHOLD);
			}

			if (includeNaive) {
				DiscreteProbDistribution distrib3 = compute(query, naive).toDiscrete();
				assertEquals(expected, distrib3.toDiscrete().getProb(new Assignment(), a), EXACT_THRESHOLD);
			}
	}


	public void checkCDF (ProbQuery query, Assignment a, double expected) throws DialException {

		ContinuousProbDistribution distrib1 = compute(query, ve).toContinuous();
		ContinuousProbDistribution distrib2 = compute(query, is).toContinuous();

		assertEquals(expected, distrib1.getCumulativeProb(new Assignment(), a), EXACT_THRESHOLD);
		
			try { assertEquals(expected, distrib2.getCumulativeProb(new Assignment(), a), SAMPLING_THRESHOLD);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2).toContinuous();
				assertEquals(expected, distrib2.getCumulativeProb(new Assignment(), a), SAMPLING_THRESHOLD);
			}

			if (includeNaive) {
				ContinuousProbDistribution distrib3 = compute(query, naive).toContinuous();
				assertEquals(expected, distrib3.toDiscrete().getProb(new Assignment(), a), EXACT_THRESHOLD);
			}
	}

	

	public void checkUtil(UtilQuery query, Assignment a, double expected) throws DialException {
			
		UtilityDistribution distrib1 = compute(query, ve);
		UtilityDistribution distrib2 = compute(query, is);

			assertEquals(expected, distrib1.getUtility(a), EXACT_THRESHOLD);
			try { assertEquals(expected, distrib2.getUtility(a), SAMPLING_THRESHOLD * 5);	}
			catch (AssertionError e) {
				distrib2 = compute(query, is2);
				assertEquals(expected, distrib2.getUtility(a), SAMPLING_THRESHOLD * 5);
			}

			if (includeNaive) {
				UtilityDistribution distrib3 = compute(query, naive);
				assertEquals(expected, distrib3.getUtility(a), EXACT_THRESHOLD);
			}
		}
	
	

	
	private ProbDistribution compute(ProbQuery query, 
			InferenceAlgorithm algo) throws DialException {
		
		if (!probQueryCache.containsKey(query)) {
			probQueryCache.put(query, new HashMap<InferenceAlgorithm, ProbDistribution>());			
		}
		

		if (probQueryCache.get(query).containsKey(algo)) {
			return probQueryCache.get(query).get(algo);
		}

		else {
			long time1 = System.nanoTime();
			ProbDistribution distrib = algo.queryProb(query);
			long inferenceTime = System.nanoTime() - time1;
			numbers.put(algo, numbers.get(algo) + 1);
			timings.put(algo, timings.get(algo) + inferenceTime);
			probQueryCache.get(query).put(algo, distrib);
			return distrib;
		}
	}
	
	
	private UtilityDistribution compute(UtilQuery query, InferenceAlgorithm algo) throws DialException {
		
		if (!utilQueryCache.containsKey(query)) {
			utilQueryCache.put(query, new HashMap<InferenceAlgorithm, UtilityDistribution>());			
		}
		
		if (utilQueryCache.get(query).containsKey(algo)) {
			return utilQueryCache.get(query).get(algo);
		}
		else {
			long time1 = System.nanoTime();
			UtilityDistribution distrib = algo.queryUtility(query);
			long inferenceTime = System.nanoTime() - time1;
			numbers.put(algo, numbers.get(algo) + 1);
			timings.put(algo, timings.get(algo) + inferenceTime);
	
			utilQueryCache.get(query).put(algo, distrib);
			return distrib;
		}
		
	}
	

	
	private void compareDistributions(DiscreteProbDistribution distrib1, 
			DiscreteProbDistribution distrib2, double margin) throws DialException {
		
		Set<Assignment> rows = distrib1.getProbTable(new Assignment()).getRows();
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
