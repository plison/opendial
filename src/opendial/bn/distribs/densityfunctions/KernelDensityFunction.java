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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Density function represented as a Gaussian kernel of data points. The distribution
 * is more exactly a Product KDE (a multivariate extension of classical KDE).
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class KernelDensityFunction implements DensityFunction {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// bandwidth for the kernel
	final double[] bandwidths;
	// shorter bandwidth (for multivariate sampling)
	final double[] samplingDeviation;

	// the kernel function
	static final GaussianDensityFunction kernel =
			new GaussianDensityFunction(0.0, 1.0);

	// the points
	final double[][] points;

	// the sampler
	static final Random sampler = new Random(Calendar.getInstance().getTimeInMillis()
			+ Thread.currentThread().getId());

	// whether the data points are bounded (if the sum of their values over the
	// dimensions must amount o 1.0).
	final boolean isBounded;

	/**
	 * Creates a new kernel density function with the given points
	 * 
	 * @param points the points
	 */
	public KernelDensityFunction(double[][] points) {
		this.points = points;
		if (points.length == 0) {
			throw new RuntimeException("KDE must contain at least one point");
		}
		isBounded = shouldBeBounded();
		bandwidths = estimateBandwidths();
		samplingDeviation = Arrays.stream(bandwidths)
				.map(b -> b / Math.pow(bandwidths.length, 2)).toArray();
	}

	/**
	 * Creates a new kernel density function with the given points
	 * 
	 * @param points the points
	 */
	public KernelDensityFunction(Collection<double[]> points) {
		this(points.toArray(new double[points.size()][]));
	}

	/**
	 * Returns the bandwidth defined for the KDE
	 * 
	 * @return the bandwidth
	 */
	public double[] getBandwidth() {
		return bandwidths;
	}

	/**
	 * Returns the density for the given point
	 * 
	 * @param x the point
	 * @return its density
	 */
	@Override
	public double getDensity(double... x) {
		int dim = (isBounded) ? bandwidths.length - 1 : bandwidths.length;

		// Density of x for a single point in the KDE
		Function<double[], Double> pointDensity = p -> IntStream.range(0, dim)
				.mapToDouble(d -> Math
						.log(kernel.getDensity((x[d] - p[d]) / bandwidths[d])
								/ bandwidths[d]))
				.sum();

		double density = Arrays.stream(points)
				.mapToDouble(p -> Math.exp(pointDensity.apply(p))).sum()
				/ points.length;

		// bounded support (cf. Jones 1993)
		if (isBounded) {
			double[] l = new double[bandwidths.length];
			double[] u = new double[bandwidths.length];
			for (int i = 0; i < bandwidths.length; i++) {
				l[i] = (0 - x[i]) / bandwidths[i];
				u[i] = (1 - x[i]) / bandwidths[i];
			}
			double factor = 1 / (kernel.getCDF(u) - kernel.getCDF(l));
			density = factor * density;
		}

		return density;
	}

	/**
	 * Samples from the kernel density function, first picking one of the point, and
	 * then deviating from it according to a Gaussian centered around it
	 * 
	 * @return the sampled point
	 */
	@Override
	public double[] sample() {

		// step 1 : selecting one point from the available points
		double[] centre = points[sampler.nextInt(points.length)];

		// step 2: sampling a point in its vicinity (following a Gaussian)
		double[] newPoint = new double[bandwidths.length];
		double total = 0.0;
		double shift = 0.0;
		for (int i = 0; i < centre.length; i++) {
			newPoint[i] =
					(sampler.nextGaussian() * samplingDeviation[i]) + centre[i];
			total += newPoint[i];
			if (newPoint[i] < shift) {
				shift = newPoint[i];
			}
		}

		// step 3: if the density must be bounded, ensure the sum is = 1
		if (isBounded) {
			for (int i = 0; i < centre.length; i++) {
				newPoint[i] =
						(newPoint[i] - shift) / (total - shift * centre.length);
			}
		}
		return newPoint;
	}

	/**
	 * Returns a set of discrete values for the density function.
	 *
	 * @param nbBuckets the number of values to extract
	 * @return the discretised values
	 */
	@Override
	public Map<double[], Double> discretise(int nbBuckets) {
		int nbToPick = Math.min(nbBuckets, points.length);
		Map<double[], Double> vals = Arrays.stream(points).distinct().limit(nbToPick)
				.collect(Collectors.toMap(p -> p, p -> 1.0 / nbToPick));
		return vals;
	}

	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public KernelDensityFunction copy() {
		KernelDensityFunction copy = new KernelDensityFunction(points);
		return copy;
	}

	/**
	 * Return a pretty print for the kernel density
	 * 
	 * @return the KDE string representation
	 */
	@Override
	public String toString() {
		String s = "KDE(mean=[";
		double[] means = getMean();
		for (double mean : means) {
			s += StringUtils.getShortForm(mean) + ", ";
		}
		s = s.substring(0, s.length() - 2) + "]),std=";
		double avgstd = 0.0;
		for (double std : getStandardDeviations()) {
			avgstd += std;
		}
		s += StringUtils.getShortForm((avgstd / means.length));
		s += ") with " + points.length + " kernels ";
		return s;
	}

	/**
	 * Returns the hashcode for the function
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return Arrays.asList(points).hashCode();
	}

	/**
	 * Returns the dimensionality of the KDE.
	 * 
	 * @return the dimensionality
	 */
	@Override
	public int getDimensions() {
		return bandwidths.length;
	}

	/**
	 * Returns the mean of the KDE.
	 * 
	 * @return the mean
	 */
	@Override
	public double[] getMean() {
		double[] mean = new double[points[0].length];
		for (int i = 0; i < mean.length; i++) {
			mean[i] = 0.0;
		}
		for (double[] point : points) {
			for (int i = 0; i < mean.length; i++) {
				mean[i] += point[i];
			}
		}
		for (int i = 0; i < mean.length; i++) {
			mean[i] = mean[i] / points.length;
		}
		return mean;
	}

	/**
	 * Returns the variance of the KDE.
	 */
	@Override
	public double[] getVariance() {
		double[] mean = getMean();
		double[] variance = new double[points[0].length];
		for (int i = 0; i < variance.length; i++) {
			variance[i] = 0.0;
		}
		for (double[] point : points) {
			for (int i = 0; i < variance.length; i++) {
				variance[i] += Math.pow(point[i] - mean[i], 2);
			}
		}
		for (int i = 0; i < variance.length; i++) {
			variance[i] = variance[i] / points.length;
		}
		return variance;
	}

	/**
	 * Returns the cumulative probability distribution for the KDE.
	 * 
	 * @return the cumulative probability from 0 to x.
	 */
	@Override
	public double getCDF(double... x) {
		if (x.length != getDimensions()) {
			throw new RuntimeException(
					"Illegal dimensionality: " + x.length + "!=" + getDimensions());
		}
		double nbOfLowerPoints =
				Arrays.stream(points).filter(v -> MathUtils.isLower(v, x)).count();
		return nbOfLowerPoints / points.length;
	}

	/**
	 * Returns the standard deviation.
	 * 
	 * @return the standard deviation
	 */
	private double[] getStandardDeviations() {
		double[] variance = getVariance();
		double[] std = new double[points[0].length];
		for (int i = 0; i < variance.length; i++) {
			std[i] = Math.sqrt(variance[i]);
		}
		return std;
	}

	/**
	 * Returns true is the distribution is bounded to a sum == 1, and false
	 * otherwise.
	 * 
	 * @return true if each point should be bounded, and false otherwise
	 */
	private boolean shouldBeBounded() {
		double total = 0;
		for (int j = 0; j < points[0].length; j++) {
			total += points[0][j];
		}
		if (total > 0.99 && total < 1.01 && points[0].length > 1) {
			return true;
		}
		return false;
	}

	/**
	 * Estimate the bandwidths according to Silverman's rule of thumb.
	 */
	private double[] estimateBandwidths() {
		double[] stds = getStandardDeviations();
		double[] silverman = new double[stds.length];
		for (int i = 0; i < silverman.length; i++) {
			silverman[i] = 1.06 * stds[i]
					* Math.pow(points.length, -1 / (4.0 + silverman.length));
			if (silverman[i] == 0.0) {
				silverman[i] = 0.05;
			}
		}
		return silverman;
	}

	/**
	 * Converts the distribution to a Gaussian distribution and returns its XML
	 * representation.
	 * 
	 * @param doc the XML document
	 * 
	 */
	@Override
	public List<Element> generateXML(Document doc) {
		GaussianDensityFunction gaussian = new GaussianDensityFunction(points);
		return gaussian.generateXML(doc);
	}

}
