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

package opendial.inference.approximate;

import java.util.logging.*;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Representation of a collection of intervals, each of which is associated with a
 * content object, and start and end values. The difference between the start and end
 * values of an interval can for instance represent the object probability.
 * 
 * <p>
 * The intervals can then be used for sampling a content object according to the
 * defined intervals.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Intervals<T> {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the intervals
	final Interval<T>[] intervals;

	// sampler for the interval collection
	final static Random sampler = new Random();

	// total probability for the table
	final double totalProb;

	/**
	 * Creates a new interval collection with a set of (content,probability) pairs
	 * 
	 * @param table the tables from which to create the intervals could not be
	 *            created
	 */
	@SuppressWarnings("unchecked")
	public Intervals(Map<T, Double> table) {

		intervals = new Interval[table.size()];
		int i = 0;
		double total = 0.0f;

		for (T a : table.keySet()) {
			double prob = table.get(a);
			if (prob == Double.NaN) {
				throw new RuntimeException("probability is NaN: " + table);
			}
			intervals[i++] = new Interval<T>(a, total, total + prob);
			total += prob;
		}

		if (total < 0.0001) {
			throw new RuntimeException("total prob is null: " + table);
		}
		totalProb = total;
	}

	/**
	 * Creates a new interval collection with a collection of values and a function
	 * specifying the probability of each value
	 * 
	 * @param content the collection of content objects
	 * @param probs the function associating a weight to each object intervals could
	 *            not be created
	 */
	@SuppressWarnings("unchecked")
	public Intervals(Collection<T> content, Function<T, Double> probs) {

		intervals = new Interval[content.size()];
		int i = 0;
		double total = 0.0f;

		for (T a : content) {
			double prob = probs.apply(a);
			if (prob == Double.NaN) {
				throw new RuntimeException("probability is NaN: " + a);
			}
			intervals[i++] = new Interval<T>(a, total, total + prob);
			total += prob;
		}

		if (total < 0.0001) {
			throw new RuntimeException("total prob is null: " + content);
		}
		totalProb = total;
	}

	/**
	 * Samples an object from the interval collection, using a simple binary search
	 * procedure.
	 * 
	 * @return the sampled object
	 */
	public T sample() {

		if (intervals.length == 0) {
			throw new RuntimeException("could not sample: empty interval");
		}

		double rand = sampler.nextDouble() * totalProb;

		int min = 0;
		int max = intervals.length;
		while (min <= max) {
			int mid = min + (max - min) / 2;
			switch (intervals[mid].compareTo(rand)) {
			case +1:
				max = mid - 1;
				break;
			case 0:
				return intervals[mid].getObject();
			case -1:
				min = mid + 1;
				break;
			}
		}

		throw new RuntimeException(
				"could not sample given the intervals: " + toString());

	}

	/**
	 * Returns a string representation of the intervals
	 */
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < intervals.length; i++) {
			s += intervals[i].toString() + "\n";
		}
		return s;
	}

	/**
	 * Returns true is the interval is empty (no elements), false otherwise
	 * 
	 * @return whether the interval is empty
	 */
	public boolean isEmpty() {
		return (intervals.length == 0);
	}

}

/**
 * Representation of a single interval, made of an object, a start value, and an end
 * value
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 * @param <T>
 */
final class Interval<T> {

	// the interval content
	final T a;

	// the start value
	final double start;

	// the end value
	final double end;

	/**
	 * Creates a new interval
	 * 
	 * @param a the interval content
	 * @param start the start value for the interval
	 * @param end the end value for the interval
	 */
	public Interval(T a, double start, double end) {
		this.a = a;
		this.start = start;
		this.end = end;
	}

	/**
	 * Returns the object associated with the interval
	 * 
	 * @return the object
	 */
	public T getObject() {
		return a;
	}

	/**
	 * Returns the position of the double value in comparison with the start and end
	 * values of the interval
	 * 
	 * @param value the value to compare
	 * @return +1 if value < start, 0 if start <= value <= end, -1 if value > end
	 */
	public int compareTo(double value) {
		if (value >= start) {
			if (value < end) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			return +1;
		}

	}

	/**
	 * Returns a string representation for the interval
	 */
	@Override
	public String toString() {
		return a + "[" + start + "," + end + "]";
	}
}
