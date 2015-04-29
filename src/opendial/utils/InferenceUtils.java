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
import java.util.stream.Collectors;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

/**
 * Utility functions for inference operations.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class InferenceUtils {

	// logger
	public static Logger log = new Logger("InferenceUtils", Logger.Level.DEBUG);

	static Random sampler = new Random();

	/**
	 * Normalise the given probability distribution (assuming no conditional
	 * variables).
	 * 
	 * @param distrib the distribution to normalise
	 * @param <T> the type of the elements in the distribution
	 * @return the normalised distribution
	 */
	public static <T> Map<T, Double> normalise(Map<T, Double> distrib) {
		double total = distrib.values().stream().mapToDouble(i -> i).sum();
		if (total == 0.0f) {
			log.warning("all assignments in the distribution have a zero "
					+ "probability, cannot be normalised");
			return distrib;
		}

		Map<T, Double> normalisedDistrib = distrib.keySet().stream()
				.collect(Collectors.toMap(a -> a, a -> distrib.get(a) / total));

		return normalisedDistrib;
	}

	/**
	 * Normalises the given distribution, assuming a set of conditional
	 * variables.
	 * 
	 * @param distrib the distribution to normalise
	 * @param condVars the conditional variables
	 * @return the normalised distribution
	 */
	public static Map<Assignment, Double> normalise(
			Map<Assignment, Double> distrib, Collection<String> condVars) {

		Map<Assignment, Double> totals = new HashMap<Assignment, Double>();
		for (Assignment a : distrib.keySet()) {
			Assignment condition = a.getTrimmed(condVars);
			if (!totals.containsKey(condition)) {
				totals.put(condition, 0.0);
			}
			totals.put(condition, totals.get(condition) + distrib.get(a));
		}

		Map<Assignment, Double> normalisedDistrib = new HashMap<Assignment, Double>();
		for (Assignment a : distrib.keySet()) {
			Assignment condition = a.getTrimmed(condVars);
			double total = totals.get(condition);
			if (total == 0) {
				log.warning("all assignments in the distribution have a zero "
						+ "probability, cannot be normalised");
				total = 1.0f;
			}
			normalisedDistrib.put(a, distrib.get(a) / total);
		}
		return normalisedDistrib;
	}

	/**
	 * Flattens a probability table, i.e. converts a double mapping into a
	 * single one, by creating every possible combination of assignments.
	 * 
	 * @param table the table to flatten
	 * @return the flattened table
	 */
	public static Map<Assignment, Double> flattenTable(
			Map<Assignment, Map<Assignment, Double>> table) {
		Map<Assignment, Double> flatTable = new HashMap<Assignment, Double>();
		for (Assignment condition : table.keySet()) {
			for (Assignment head : table.get(condition).keySet()) {
				flatTable.put(new Assignment(condition, head),
						table.get(condition).get(head));
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
	public static double[] normalise(double[] initProbs) {
		for (int i = 0; i < initProbs.length; i++) {
			if (initProbs[i] < 0) {
				initProbs[i] = 0.0;
			}
		}
		double sum = 0.0;
		for (double prob : initProbs) {
			sum += prob;
		}

		double[] result = new double[initProbs.length];

		if (sum > 0.001) {
			for (int i = 0; i < initProbs.length; i++) {
				result[i] = initProbs[i] / sum;
			}
		} else {
			for (int i = 0; i < initProbs.length; i++) {
				result[i] = 1.0 / initProbs.length;
			}
		}

		return result;
	}

	/**
	 * Returns a smaller version of the initial table that only retains the N
	 * elements with a highest value
	 * 
	 * @param initTable the full initial table
	 * @param nbest the number of elements to retain
	 * @param <T> the type of the elements in the table
	 * @return the resulting subset of the table
	 */
	public static <T> LinkedHashMap<T, Double> getNBest(
			Map<T, Double> initTable, int nbest) {
		if (nbest < 1) {
			log.warning("nbest should be >= 1, but is " + nbest);
			nbest = 1;
		}

		List<Map.Entry<T, Double>> entries = new ArrayList<Map.Entry<T, Double>>(
				initTable.entrySet());

		Collections.sort(entries, new EntryComparator<T>(0.0001));
		Collections.reverse(entries);

		LinkedHashMap<T, Double> newTable = new LinkedHashMap<T, Double>();
		int nb = 0;
		for (Map.Entry<T, Double> entry : entries) {
			if (nb < nbest) {
				newTable.put(entry.getKey(), entry.getValue());
				nb++;
			}
		}
		for (T key : new ArrayList<T>(newTable.keySet())) {
			if (key instanceof Assignment && ((Assignment) key).isDefault()
					|| key instanceof Value
					&& ((Value) key) == ValueFactory.none()) {
				double val = newTable.remove(key);
				newTable.put(key, val);
			}
		}
		return newTable;
	}

	/**
	 * Returns the ranking of the given assignment in the table, assuming an
	 * ordering of the table in descending order.
	 * 
	 * @param initTable the table
	 * @param assign the assignment to find
	 * @param <T> the type of the elements in the table
	 * @param minDifference the minimum difference between values
	 * @return the index in the ordered table, or -1 if the element is not in
	 *         the table
	 */
	public static <T> int getRanking(Map<T, Double> initTable, T assign,
			double minDifference) {

		List<Map.Entry<T, Double>> entries = new ArrayList<Map.Entry<T, Double>>(
				initTable.entrySet());
		EntryComparator<T> comp = new EntryComparator<T>(minDifference);
		Collections.sort(entries, comp);
		Collections.reverse(entries);

		for (int i = 0; i < entries.size(); i++) {
			Map.Entry<T, Double> entry = entries.get(i);
			if (entry.getKey().equals(assign)) {
				return entries.indexOf(entry);
			} else if (i < entries.size() - 1) {
				Map.Entry<T, Double> nextEntry = entries.get(i + 1);
				if (nextEntry.getKey().equals(assign)
						&& comp.compare(entry, nextEntry) == 0) {
					return entries.indexOf(entry);
				}
			}
		}
		return -1;
	}

	/**
	 * A comparator for the pair (assignment, double) that sorts the entries
	 * according to their double values.
	 * 
	 * @author Pierre Lison (plison@ifi.uio.no)
	 */
	static final class EntryComparator<T> implements
			Comparator<Map.Entry<T, Double>> {

		double minDifference;

		public EntryComparator(double minDifference) {
			this.minDifference = minDifference;
		}

		@Override
		public int compare(Entry<T, Double> arg0, Entry<T, Double> arg1) {
			double result = arg0.getValue() - arg1.getValue();
			if (Math.abs(result) < minDifference) {
				return 0;
			} else {
				return (int) (result * 10000000);
			}
		}

	}

}
