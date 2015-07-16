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

package opendial.bn.distribs.densityfunctions;

import java.util.logging.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.bn.values.ValueFactory;
import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Density function defined via a set of discrete points. The density at a given
 * point x is defined as the probability mass for the closest point y in the
 * distribution, divided by a constant volume (used for normalisation).
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class DiscreteDensityFunction implements DensityFunction {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// the set of points for the density function
	Map<double[], Double> points;

	// the sampler
	Random sampler;

	// minimum distance between points
	double minDistance;

	// the volume employed for the normalisation
	double volume;

	/**
	 * Creates a new discrete density function, given the set of points
	 * 
	 * @param points a set of (value,prob) pairs
	 */
	public DiscreteDensityFunction(Map<double[], Double> points) {
		this.points = new HashMap<double[], Double>();
		this.points.putAll(points);
		sampler = new Random();

		// calculate the minimum distance between points
		this.minDistance = MathUtils.getMinEuclidianDistance(points.keySet());

		// and define the volume with a radius that is half this distance
		this.volume = MathUtils.getVolume(minDistance / 2, getDimensions());
	}

	/**
	 * Returns the density for a given point. The density is derived in two steps:
	 * <br>
	 * <ol>
	 * 
	 * <li>locating the point in the distribution that is closest to x (according to
	 * Euclidian distance)
	 * 
	 * <li>dividing the probability mass for the point by the n-dimensional volume
	 * around this point. The radius of the ball is the half of the minimum distance
	 * between the points of the distribution.
	 * </ol>
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	@Override
	public double getDensity(double... x) {
		double[] closest = null;
		double closestDist = -Double.MAX_VALUE;
		for (double[] point : points.keySet()) {
			double curDist = MathUtils.getDistance(point, x);
			if (closest == null || curDist < closestDist) {
				closest = point;
				closestDist = curDist;
			}
		}
		if (closestDist < minDistance / 2) {
			return points.get(closest)
					/ MathUtils.getVolume(minDistance / 2, getDimensions());
		}
		else {
			return 0;
		}
	}

	/**
	 * Samples according to the density function
	 *
	 * @return the resulting sample
	 */
	@Override
	public double[] sample() {
		double sampled = sampler.nextFloat();
		double sum = 0.0;
		for (double[] point : points.keySet()) {
			sum += points.get(point);
			if (sampled < sum) {
				return point;
			}
		}
		log.warning("discrete density function could not be sampled");
		return new double[0];
	}

	/**
	 * Returns the points for this distribution.
	 * 
	 */
	@Override
	public Map<double[], Double> discretise(int nbBuckets) {
		return points;
	}

	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public DiscreteDensityFunction copy() {
		return new DiscreteDensityFunction(points);
	}

	/**
	 * Returns a pretty print representation of the function
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		String s = "Discrete(";
		for (double[] point : points.keySet()) {
			s += "(";
			for (int i = 0; i < point.length; i++) {
				s += StringUtils.getShortForm(point[i]) + ",";
			}
			s = s.substring(0, s.length() - 1) + "):="
					+ StringUtils.getShortForm(points.get(point));
		}
		return s + ")";
	}

	/**
	 * Returns the hashcode for the function
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return points.hashCode();
	}

	/**
	 * Returns the dimensionality of the distribution.
	 */
	@Override
	public int getDimensions() {
		return points.keySet().iterator().next().length;
	}

	/**
	 * Returns the means of the distribution (calculated like for a categorical
	 * distribution).
	 */
	@Override
	public double[] getMean() {
		double[] mean = new double[getDimensions()];
		for (int i = 0; i < getDimensions(); i++) {
			mean[i] = 0.0;
			for (double[] point : points.keySet()) {
				mean[i] += (point[i] * points.get(point));
			}
		}
		return mean;
	}

	/**
	 * Returns the variance of the distribution (calculated like for a categorical
	 * distribution)
	 * 
	 */
	@Override
	public double[] getVariance() {
		double[] variance = new double[getDimensions()];
		double[] mean = getMean();
		for (int i = 0; i < getDimensions(); i++) {
			variance[i] = 0.0;
			for (double[] point : points.keySet()) {
				variance[i] += Math.pow(point[i] - mean[i], 2) * points.get(point);
			}
		}
		return variance;
	}

	/**
	 * Returns the cumulative distribution for the distribution (by counting all the
	 * points with a value that is lower than x).
	 */
	@Override
	public double getCDF(double... x) {
		if (x.length != getDimensions()) {
			throw new RuntimeException(
					"Illegal dimensionality: " + x.length + "!=" + getDimensions());
		}

		double cdf = points.keySet().stream().filter(v -> MathUtils.isLower(v, x))
				.mapToDouble(v -> points.get(v)).sum();

		return cdf;
	}

	@Override
	public List<Element> generateXML(Document doc) {
		List<Element> elList = new ArrayList<Element>();

		for (double[] a : points.keySet()) {
			Element valueNode = doc.createElement("value");
			Attr prob = doc.createAttribute("prob");
			prob.setValue("" + StringUtils.getShortForm(points.get(a)));
			valueNode.setAttributeNode(prob);
			valueNode.setTextContent("" + ValueFactory.create(a));
			elList.add(valueNode);
		}
		return elList;
	}

}
