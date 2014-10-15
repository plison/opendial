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
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;

/**
 * Utilities to calculate distance (between doubles and arrays).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class DistanceUtils {

	// logger
	public static Logger log = new Logger("DistanceUtils", Logger.Level.DEBUG);

	public static final double MIN_PROXIMITY_DISTANCE = 0.1;


	/**
	 * Returns true is all elements in the array a have a lower value than
	 * the corresponding elements in the array b
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return true is a is lower than b in all dimensions, and false otherwise
	 */
	public static boolean isLower(Double[] a, Double[] b) {
		for (int i = 0 ; i < a.length  ; i++) {
			if (a[i] > b[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Find the assignment whose values are closest to the value toFind, assuming the values
	 * contained in the assignment are composed of only DoubleVal or ArrayVal.
	 * 
	 * @param rows the values in which to search for the closest element
	 * @param toFind the reference value
	 * @return the closest value, if any is found.
	 * @throws DialException if no closest assignment could be found
	 */
	public static Value getClosestElement(Collection<Value> rows, 
			Value toFind) throws DialException {
			
		Value closest = null;
		double minDistance = Double.MAX_VALUE;
		for (Value v : rows) {
			double totalDistance = 0;
			Double[] val1 = convertToDouble(toFind);
			Double[] val2 = convertToDouble(v);
			totalDistance += getDistance(val1, val2);
			
			if (totalDistance < minDistance) {
				minDistance = totalDistance;
				closest = v;
			}
		}	
		if (closest == null) {
		throw new DialException("could not find closest element");
		}
		return closest;
	}
	

	/**
	 * Returns the Euclidian distance between two double arrays.
	 * 
	 * @param point1 the first double array
	 * @param point2 the second double array
	 * @return the distance beteeen the two points
	 */
	public static double getDistance(Double[] point1, Double[] point2) {
		if (point1 == null || point2 == null || point1.length != point2.length) {
			return Double.MAX_VALUE;
		}
		double dist = 0;
		for (int i = 0 ; i < point1.length ; i++) {
			dist += Math.pow(point1[i]-point2[i], 2);
		}
		return Math.sqrt(dist);
	}
	
	
	
	/**
	 * Returns the distance between the two points on each dimension.
	 * 
	 * @param point1 the first point
	 * @param point2 the second point
	 * @return the array with the distance on each dimension
	 */
	public static Double[] getFullDistance(Double[] point1, Double[] point2) {
		Double[] dist = new Double[point1.length];
		if (point1 == null || point2 == null || point1.length != point2.length) {
			return dist;
		}
		for (int i = 0 ; i < point1.length ; i++) {
			dist[i] = Math.abs(point1[i] - point2[i]);
		}
		return dist;
	}
	
	
	/**
	 * Returns the minimal Euclidian distance between any two pairs of points
	 * in the collection of points provided as argument.
	 * 
	 * @param points the collection of points
	 * @return the minimum distance between all possible pairs of points
	 */
	public static double getMinEuclidianDistance(Collection<Double[]> points) {
		double minDistance = Double.MAX_VALUE;
		List<Double[]> l = new ArrayList<Double[]>(points);
		for (int i = 0 ; i < points.size()-1 ; i++) {
			Double[] first = l.get(i);
			for (int j = i+1 ; j < points.size() ; j++) {
				Double[] second = l.get(j);
				double dist = getDistance(first, second);
				if (dist < minDistance) {
					minDistance = dist;
				}
			}
		}
		return minDistance;
	}
	


	/**
	 * Converts the value to a double array, if possible.  Else, returns null
	 * 
	 * @param val the value to convert
	 * @return the converted value or null.
	 */
	private static Double[] convertToDouble(Value val) {
		Double[] a = null;
		if (val instanceof ArrayVal) {
			a = ((ArrayVal)val).getArray();
		}
		else if (val instanceof DoubleVal) {
			a = new Double[]{((DoubleVal)val).getDouble()};
		}
		return a;
	}

	
	

}


	
