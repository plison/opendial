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

package opendial.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;

/**
 * Utility functions for inference operations.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
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


	/**
	 * Normalises the double array (ensuring that the sum is equal to 1.0).
	 * 
	 * @param initProbs the unnormalised values
	 * @return the normalised values
	 */
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


	/**
	 * Returns a smaller version of the initial table that only retains the N elements with a
	 * highest value
	 * 
	 * @param initTable the full initial table
	 * @param nbest the number of elements to retain
	 * @return the resulting subset of the table
	 */
	public static Map<Assignment,Double> getNBest (Map<Assignment,Double> initTable, int nbest) {
		if (nbest < 1) {
			log.warning("nbest should be >= 1");
			nbest = 1;
		}

		List<Map.Entry<Assignment,Double>> entries = 
				new ArrayList<Map.Entry<Assignment,Double>>(initTable.entrySet());

		Collections.sort(entries, new AssignComparator());
		Collections.reverse(entries);

		Map<Assignment,Double> newTable = new LinkedHashMap<Assignment,Double>();
		int nb = 0;
		for (Map.Entry<Assignment,Double> entry : entries) {
			if (nb < nbest) {
				newTable.put(entry.getKey(), entry.getValue());
				nb++;
			}
		}
		return newTable;
	}
	
	
	/**
	 * Returns the ranking of the given assignment in the table, assuming an ordering of the
	 * table in descending order.
	 * 
	 * @param initTable the table 
	 * @param assign the assignment to find
	 * @return the index in the ordered table, or -1 if the element is not in the table
	 */
	public static int getRanking(Map<Assignment,Double> initTable, Assignment assign) {
	
		List<Map.Entry<Assignment,Double>> entries = 
				new ArrayList<Map.Entry<Assignment,Double>>(initTable.entrySet());

		Collections.sort(entries, new AssignComparator());
		Collections.reverse(entries);

		for (Map.Entry<Assignment,Double> entry : entries) {
			if (entry.getKey().equals(assign)) {
				return entries.indexOf(entry);
			}
		}
		return -1;
	}


	/**
	 * A comparator for the pair (assignment, double) that sorts the entries according
	 * to their double values.
	 * 
	 * @author  Pierre Lison (plison@ifi.uio.no)
	 * @version $Date::                      $
	 */
	static final class AssignComparator implements Comparator<Map.Entry<Assignment,Double>> {

		@Override
		public int compare(Entry<Assignment, Double> arg0, Entry<Assignment, Double> arg1) {
			return (int)((arg0.getValue() - arg1.getValue())*1000);
		}

	}


}
