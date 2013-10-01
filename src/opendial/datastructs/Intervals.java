// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.datastructs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.inference.approximate.WeightedSample;


/**
 * Representation of a collection of intervals, each of which is associated with
 * a content object, and start & end values. The difference between the start & 
 * end values of an interval can for instance represent the object probability.
 * 
 * <p>The intervals can then be used for sampling a content object according to 
 * the defined intervals.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Intervals<T> {

	// logger
	public static Logger log = new Logger("Intervals", Logger.Level.DEBUG);

	// the intervals
	Interval<T>[] intervals;

	// sampler for the interval collection
	Random sampler ;

	// total probability for the table
	double totalProb;


	/**
	 * Creates a new interval collection with a set of (content,probability) pairs
	 * 
	 * @param table the tables from which to create the intervals
	 * @throws DialException 
	 */
	@SuppressWarnings("unchecked")
	public Intervals(Map<T,Double> table) throws DialException {

		intervals = new Interval[table.size()];
		int i = 0;
		totalProb = 0.0f;

		for (T a : table.keySet()) {
			double prob = table.get(a);
			if (prob == Double.NaN) {
				throw new DialException("probability is NaN: " + table);
			}
			intervals[i++] = new Interval<T>(a,totalProb, totalProb+prob);
			totalProb += prob;
		}

		if (totalProb < 0.0001) {
			throw new DialException("total prob is null: " + table);
		}
		sampler = new Random();
	}
	
	
	/**
	 * Creates an interval with an array of elements and their associated probability
	 * 
	 * @param elements the elements
	 * @param probs their probability
	 * @throws DialException 
	 */
	@SuppressWarnings("unchecked")
	public Intervals(T[] elements, double[] probs) throws DialException {
		
		if (elements.length != probs.length) {
			throw new DialException("array lenghts are not identical");
		}
		
		intervals = new Interval[elements.length];
		totalProb = 0.0f;

		for (int i = 0 ; i < elements.length ; i++) {
			double prob = probs[i];
			if (prob == Double.NaN) {
				throw new DialException("probability is NaN: " +  prob);
			}
			intervals[i] = new Interval<T>(elements[i],totalProb, totalProb+prob);
			totalProb += prob;
		}

		if (totalProb < 0.0001) {
			throw new DialException("total prob is null: ");
		}
		sampler = new Random();

	}
	
	/**
	 * Samples an object from the interval collection, using a simple
	 * binary search procedure.
	 * 
	 * @return the sampled object
	 * @throws DialException if the sampling could not be performed
	 */
	public T sample() throws DialException {

		if (intervals.length == 0) {
			throw new DialException("could not sample: empty interval");	
		}

		if (sampler == null) {
			log.debug("sampler is null");
			sampler = new Random();
		}
		
		double rand = sampler.nextDouble()*totalProb;

		int min = 0;
		int max = intervals.length ;
		while (min <= max) {
			int mid = min + (max - min) / 2;
			if (mid < 0 || mid > intervals.length -1) {
				throw new DialException("could not sample: (min=" + min + ", max=" + max +
						", length=" + intervals.length+") -- intervals = " + Arrays.asList(intervals));
			}
			switch (intervals[mid].compareTo(rand)) {
			case +1: max = mid - 1; break;
			case 0: 
				T result = intervals[mid].getObject(); 
				if (result == null) {
					log.warning("result of sampling with intervals is null (mid=" + mid+")");
					log.warning("intervals: " + this);
				}
				return result;
			case -1 : min = mid + 1; break;
			}
		}

		throw new DialException("could not sample given the intervals: " + toString());

	}


	/**
	 * Returns a string representation of the intervals
	 */
	@Override
	public String toString() {
		String s =  "";
		for (int i=0; i< intervals.length ; i++) {
			s += intervals[i].toString()  + "\n";
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
 * Representation of a single interval, made of an object, a start value,
 * and an end value
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 * @param <T>
 */
final class Interval<T> {

	// the interval content
	T a ;

	// the start value
	double start;

	// the end value
	double end;

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
	 * Returns the position of the double value in comparison with 
	 * the start and end values of the interval
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
	public String toString() {
		return a + "[" + start + "," + end + "]";
	}
}
