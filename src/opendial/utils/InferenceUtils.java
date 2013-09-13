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

package opendial.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.continuous.functions.ProductKernelDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;

/**
 * Utility functions for inference operations.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class InferenceUtils {

	// logger
	public static Logger log = new Logger("InferenceUtils", Logger.Level.DEBUG);

	static Random sampler = new Random();

	/**
	 * Normalise the given probability distribution (assuming no conditional variables).
	 * 
	 * @param distrib the distribution to normalise
	 * @return the normalised distribution
	 */
	public static Map<Assignment, Double> normalise (Map<Assignment, Double> distrib) {
		double total = 0.0f;
		for (Assignment a : distrib.keySet()) {
			total += distrib.get(a);
		}
		if (total == 0.0f) {
			//		log.debug("distribution: " + distrib);
			log.warning("all assignments in the distribution have a zero " +
					"probability, cannot be normalised");
			total = 1.0f;
		}

		Map<Assignment,Double> normalisedDistrib = new HashMap<Assignment,Double>();
		for (Assignment a: distrib.keySet()) {
			double prob = distrib.get(a)/ total;
			//	if (prob > 0.0) {
			normalisedDistrib.put(a, prob);
			//	}
		}
		return normalisedDistrib;
	}


	/**
	 * Normalises the given distribution, assuming a set of conditional variables.
	 * 
	 * @param distrib the distribution to normalise
	 * @param condVars the conditional variables
	 * @return the normalised distribution
	 */
	public static Map<Assignment, Double> normalise (Map<Assignment, Double> distrib, 
			Collection<String> condVars) {

		Map<Assignment, Double> totals = new HashMap<Assignment,Double>();
		for (Assignment a : distrib.keySet()) {
			Assignment condition = a.getTrimmed(condVars);
			if (!totals.containsKey(condition)) {
				totals.put(condition, 0.0);
			}
			totals.put(condition, totals.get(condition) + distrib.get(a));
		}

		Map<Assignment,Double> normalisedDistrib = new HashMap<Assignment,Double>();
		for (Assignment a : distrib.keySet()) {
			Assignment condition = a.getTrimmed(condVars);
			double total = totals.get(condition);
			if (total == 0) {
				log.warning("all assignments in the distribution have a zero " +
						"probability, cannot be normalised");
				total = 1.0f;
			}
			normalisedDistrib.put(a, distrib.get(a)/total);
		}
		return normalisedDistrib;
	}




	/**
	 * Flattens a probability table, i.e. converts a double mapping into
	 * a single one, by creating every possible combination of assignments.
	 * 
	 * @param table the table to flatten
	 * @return the flattened table
	 */
	public static Map<Assignment, Double> flattenTable(
			Map<Assignment, Map<Assignment, Double>> table) {
		Map<Assignment,Double> flatTable = new HashMap<Assignment,Double>();
		for (Assignment condition : table.keySet()) {
			for (Assignment head : table.get(condition).keySet()) {
				flatTable.put(new Assignment(condition, head), table.get(condition).get(head));
			}
		}
		return flatTable;
	}


	public static Double[] normalise(Double[] initProbs) {
		for (int i = 0 ; i < initProbs.length; i++) {
			if (initProbs[i] < 0) {
				initProbs[i] = 0.0;
			}
		}
		double sum = 0.0;
		for (double prob: initProbs) {
			sum += prob;
		}

		Double[] result = new Double[initProbs.length];

		if (sum > 0.001) {
			for (int i = 0 ; i < initProbs.length; i++) {
				result[i] = initProbs[i] / sum;
			}
		}
		else {
			for (int i = 0 ; i < initProbs.length; i++) {
				result[i] = 1.0 / initProbs.length;
			}
		}

		return result;
	}

	public static SimpleTable createTable(Collection<String> headVars, Collection<Assignment> samples) {
		SimpleTable table = new SimpleTable();

		Map<Assignment, Integer> counts = new HashMap<Assignment,Integer>();

		for (Assignment sample : samples) {
			Assignment trimmed = sample.getTrimmed(headVars);
			if (counts.containsKey(trimmed)) {
				counts.put(trimmed, counts.get(trimmed) + 1);
			}
			else {
				counts.put(trimmed,1);
			}
		}
		for (Assignment value : counts.keySet()) {
			table.addRow(value, 1.0 * counts.get(value) / samples.size());
		}
		return table;
	}


	public static DiscreteProbabilityTable createTable(Set<String> condVars,
			Set<String> headVars, Collection<Assignment> samples) {

		DiscreteProbabilityTable table = new DiscreteProbabilityTable();

		Map<Assignment,SimpleEmpiricalDistribution> temp = 
				new HashMap<Assignment,SimpleEmpiricalDistribution>();

		for (Assignment sample: samples) {
			Assignment condition = sample.getTrimmed(condVars);
			Assignment head = sample.getTrimmed(headVars);
			if (!temp.containsKey(condition)) {
				temp.put(condition, new SimpleEmpiricalDistribution());
			}

			temp.get(condition).addSample(head);
		}

		for (Assignment condition : temp.keySet()) {
			table.addRows(condition, (SimpleTable)(temp.get(condition).toDiscrete()));
		}
		table.fillConditionalHoles();
		return table;
	}


	public static ContinuousProbDistribution createContinuousDistrib 
	(String headVar, Collection<Assignment> samples) throws DialException {
		
		Assignment firstSample = samples.iterator().next();
		if (firstSample.getValue(headVar) instanceof DoubleVal) {
			return extractUnivariateDistribution(headVar, samples);
		}
		else if (firstSample.getValue(headVar) instanceof VectorVal) {
			return extractMultivariateDistribution(headVar, samples);				
		}
		else {
			throw new DialException ("empirical distribution for " + headVar + " could not be " +
					"converted to a continuous distribution: value is " + 
					firstSample.getValue(headVar).getClass().getSimpleName());
		}
	}


	private static  UnivariateDistribution extractUnivariateDistribution 
	(String headVar, Collection<Assignment> samples) throws DialException {

		List<Double> values = new ArrayList<Double>(samples.size());
		for (Assignment sample : samples) {
			Value value = sample.getValue(headVar);
			if (value instanceof DoubleVal) {
				values.add(((DoubleVal)sample.getValue(headVar)).getDouble());
			}
			else {
				throw new DialException ("value type is not allowed in " +
						"continuous distribution: " + value.getClass().getName());
			}
		}
		return new UnivariateDistribution(headVar, new KernelDensityFunction(values));
	}


	private static MultivariateDistribution extractMultivariateDistribution 
	(String headVar, Collection<Assignment> samples) throws DialException {

		List<Double[]> values = new ArrayList<Double[]>(samples.size());
		for (Assignment sample : samples) {
			Value value = sample.getValue(headVar);
			if (value instanceof VectorVal) {
				values.add(((VectorVal)sample.getValue(headVar)).getArray());
			}
			else {
				throw new DialException ("value type is not allowed in " +
						"continuous distribution: " + value.getClass().getName());
			}
		}
		ProductKernelDensityFunction pkde = new ProductKernelDensityFunction(values);
		pkde.setAsBounded(true);
		return new MultivariateDistribution(headVar, pkde);
	}


	public static ContinuousProbDistribution createContinuousDistrib(
			Set<String> condVars, String headVar, Collection<Assignment> samples) throws DialException {
		
		ContinuousProbabilityTable distrib = new ContinuousProbabilityTable();
		
		Map<Assignment,SimpleEmpiricalDistribution> temp = new HashMap<Assignment,SimpleEmpiricalDistribution>();
		for (Assignment sample: samples) {
			Assignment condition = sample.getTrimmed(condVars);
			if (!temp.containsKey(condition)) {
				temp.put(condition, new SimpleEmpiricalDistribution());
			}
			temp.get(condition).addSample(sample.getTrimmed(headVar));
		}
		
		for (Assignment condition : temp.keySet()) {
			SimpleEmpiricalDistribution subdistrib = temp.get(condition);
			ContinuousProbDistribution continuousEquiv = subdistrib.toContinuous();
			distrib.addDistrib(condition, continuousEquiv);
		}
		
		return distrib;
	}




}
