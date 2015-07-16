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

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Utility functions for inference operations.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class InferenceUtils {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

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

		Map<T, Double> normalisedDistrib = distrib.entrySet().stream().collect(
				Collectors.toMap(a -> a.getKey(), a -> a.getValue() / total));

		return normalisedDistrib;
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
		}
		else {
			for (int i = 0; i < initProbs.length; i++) {
				result[i] = 1.0 / initProbs.length;
			}
		}

		return result;
	}

	/**
	 * Generates all possible assignment combinations from the set of values provided
	 * as parameters -- each variable being associated with a set of alternative
	 * values.
	 * 
	 * <p>
	 * NB: use with caution, computational complexity is exponential!
	 * 
	 * @param valuesMatrix the set of values to combine
	 * @return the list of all possible combinations
	 */
	public static Set<Assignment> getAllCombinations(
			Map<String, Set<Value>> valuesMatrix) {

		try {
			// start with a single, empty assignment
			Set<Assignment> assignments = new HashSet<Assignment>();
			assignments.add(new Assignment());

			// at each iterator, we expand each assignment with a new
			// combination
			for (String label : valuesMatrix.keySet()) {
				Set<Value> values = valuesMatrix.get(label);
				assignments = assignments.stream()
						.flatMap(a -> values.stream()
								.map(v -> new Assignment(a, label, v)).sequential())
						.collect(Collectors.toSet());
			}
			return assignments;
		}
		catch (OutOfMemoryError e) {
			log.fine("out of memory error, initial matrix: " + valuesMatrix);
			e.printStackTrace();
			return new HashSet<Assignment>();
		}
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
	public static <T> LinkedHashMap<T, Double> getNBest(Map<T, Double> initTable,
			int nbest) {
		if (nbest < 1) {
			log.warning("nbest should be >= 1, but is " + nbest);
			nbest = 1;
		}

		List<Map.Entry<T, Double>> entries =
				new ArrayList<Map.Entry<T, Double>>(initTable.entrySet());

		Collections.sort(entries, (a, b) -> {
			double result = a.getValue() - b.getValue();
			return (Math.abs(result) < 0.0001) ? 0 : (int) (result * 10000000);
		});

		Collections.reverse(entries);

		LinkedHashMap<T, Double> newTable = new LinkedHashMap<T, Double>();
		int nb = 0;
		for (Map.Entry<T, Double> entry : entries) {
			if (nb < nbest) {
				newTable.put(entry.getKey(), entry.getValue());
				nb++;
			}
		}

		return newTable;
	}

	/**
	 * Returns the ranking of the given assignment in the table, assuming an ordering
	 * of the table in descending order.
	 * 
	 * @param initTable the table
	 * @param assign the assignment to find
	 * @param <T> the type of the elements in the table
	 * @param minDifference the minimum difference between values
	 * @return the index in the ordered table, or -1 if the element is not in the
	 *         table
	 */
	public static <T> int getRanking(Map<T, Double> initTable, T assign,
			double minDifference) {

		List<Map.Entry<T, Double>> entries =
				new ArrayList<Map.Entry<T, Double>>(initTable.entrySet());

		Comparator<Map.Entry<T, Double>> comp = (a, b) -> {
			double result = a.getValue() - b.getValue();
			return (Math.abs(result) < minDifference) ? 0
					: (int) (result * 10000000);
		};

		Collections.sort(entries, comp);
		Collections.reverse(entries);

		// find the minimum rank
		for (int i = 0; i < entries.size(); i++) {
			Map.Entry<T, Double> entry = entries.get(i);
			if (entry.getKey().equals(assign)) {
				return i;
			}
			for (int j = i + 1; j < entries.size(); j++) {
				Map.Entry<T, Double> nextEntry = entries.get(j);
				if (comp.compare(entry, nextEntry) != 0) {
					break;
				}
				if (nextEntry.getKey().equals(assign)) {
					return i;
				}
			}
		}
		return -1;
	}

}
