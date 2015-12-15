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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import opendial.bn.values.ArrayVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

/**
 * Math utilities.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class MathUtils {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Returns true is all elements in the array a have a lower value than the
	 * corresponding elements in the array b
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return true is a is lower than b in all dimensions, and false otherwise
	 */
	public static boolean isLower(double[] a, double[] b) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] > b[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the Euclidian distance between two double arrays.
	 * 
	 * @param point1 the first double array
	 * @param point2 the second double array
	 * @return the distance beteeen the two points
	 */
	public static double getDistance(double[] point1, double[] point2) {
		if (point1 == null || point2 == null || point1.length != point2.length) {
			return Double.MAX_VALUE;
		}
		double dist = IntStream.range(0, point1.length)
				.mapToDouble(i -> Math.pow(point1[i] - point2[i], 2)).sum();
		return Math.sqrt(dist);
	}

	/**
	 * Returns the minimal Euclidian distance between any two pairs of points in the
	 * collection of points provided as argument.
	 * 
	 * @param points the collection of points
	 * @return the minimum distance between all possible pairs of points
	 */
	public static double getMinEuclidianDistance(Collection<double[]> points) {
		double minDistance = Double.MAX_VALUE;
		List<double[]> l = new ArrayList<double[]>(points);
		for (int i = 0; i < points.size() - 1; i++) {
			double[] first = l.get(i);
			for (int j = i + 1; j < points.size(); j++) {
				double[] second = l.get(j);
				double dist = getDistance(first, second);
				if (dist < minDistance) {
					minDistance = dist;
				}
			}
		}
		return minDistance;
	}

	/**
	 * Returns the log-gamma value using Lanczos approximation formula
	 * 
	 * Reference: http://introcs.cs.princeton.edu/java/91float/Gamma.java
	 * 
	 * @param x the point
	 * @return the log-gamma value for the point
	 */
	static double logGamma(double x) {
		double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
		double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
				+ 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
				+ 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
		return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	}

	/**
	 * Returns the value of the gamma function: Gamma(x) = integral( t^(x-1) e^(-t),
	 * t = 0 .. infinity)
	 * 
	 * @param x the point
	 * @return the gamma value fo the point
	 */
	public static double gamma(double x) {
		return Math.exp(logGamma(x));
	}

	/**
	 * Returns the volume of an N-ball of a certain radius.
	 * 
	 * @param radius the radius.
	 * @param dimension the number of dimensions to consider
	 * @return the resulting volume
	 */
	public static double getVolume(double radius, int dimension) {
		double numerator = Math.pow(Math.PI, dimension / 2.0);
		double denum = gamma((dimension / 2.0) + 1);
		double radius2 = Math.pow(radius, dimension);
		return radius2 * numerator / denum;
	}

	public final static class DotProduct implements Function<List<String>, Value> {

		@Override
		public Value apply(List<String> input) {
			if (input.size() != 2) {
				throw new RuntimeException("Dot product requires 2 arguments");
			}
			Value val1 = ValueFactory.create(input.get(0));
			Value val2 = ValueFactory.create(input.get(1));
			if (!(val1 instanceof ArrayVal) || !(val2 instanceof ArrayVal)) {
				throw new RuntimeException(
						val1 + " and " + val2 + " are not ArrayVal objects");
			}
			double[] array1 = ((ArrayVal) val1).getArray();
			double[] array2 = ((ArrayVal) val2).getArray();
			if (array1.length != array2.length) {
				throw new RuntimeException(
						val1 + " and " + val2 + " have different dimensions");
			}
			double result = 0.0;
			for (int i = 0; i < array1.length; i++) {
				result += (array1[i] * array2[i]);
			}
			return ValueFactory.create(result);
		}

	}

	public final static class LogisticFunction
			implements Function<List<String>, Value> {

		@Override
		public Value apply(List<String> input) {
			if (input.size() != 2) {
				throw new RuntimeException(
						"Logistic function requires 2 arguments, but is given: "
								+ input);
			}
			Value val1 = ValueFactory.create(input.get(0));
			Value val2 = ValueFactory.create(input.get(1));
			if (!(val1 instanceof ArrayVal) || !(val2 instanceof ArrayVal)) {
				throw new RuntimeException(
						val1 + " and " + val2 + " are not ArrayVal objects");
			}
			double[] array1 = ((ArrayVal) val1).getArray();
			double[] array2 = ((ArrayVal) val2).getArray();
			if (array1.length != array2.length) {
				throw new RuntimeException(
						val1 + " and " + val2 + " have different dimensions");
			}
			double dotproduct = 0.0;
			for (int i = 0; i < array1.length; i++) {
				dotproduct += (array1[i] * array2[i]);
			}
			double result = 1.0 / (1 + Math.exp(-dotproduct));
			return ValueFactory.create(result);
		}

	}

}
