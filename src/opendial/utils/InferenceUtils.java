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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;

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


	public static double[] normalise(double[] initProbs) {
		for (int i = 0 ; i < initProbs.length; i++) {
			if (initProbs[i] < 0) {
				initProbs[i] = 0;
			}
		}
		double sum = 0.0;
		for (double prob: initProbs) {
			sum += prob;
		}
		
		double[] result = new double[initProbs.length];
		
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

	
	
	
}
