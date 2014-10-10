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

package opendial.bn.distribs.continuous.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.utils.DistanceUtils;
import opendial.utils.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Density function represented as a Gaussian kernel of data points. The distribution
 * is more exactly a Product KDE (a multivariate extension of classical KDE).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-05-07 14:40:41 #$
 *
 */
public class KernelDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("KernelDensityFunction", Logger.Level.DEBUG);

	// bandwidth for the kernel
	Double[] bandwidths;

	// the kernel function
	GaussianDensityFunction kernel = new GaussianDensityFunction(0.0, 1.0);

	// the points
	List<Double[]> points;

	// the sampler
	Random sampler;

	// whether the data points are bounded (if the sum of their values over the 
	// dimensions must amount o 1.0).
	boolean isBounded = false;


	/**
	 * Creates a new kernel density function with the given points
	 * 
	 * @param points the points
	 */
	public KernelDensityFunction(Collection<Double[]> points) {
		this.points = new ArrayList<Double[]>(points);		
		isBounded = shouldBeBounded();

		sampler = new Random();
		estimateBandwidths();
	}



	/**
	 * Returns the bandwidth defined for the KDE
	 * 
	 * @return the bandwidth
	 */
	public Double[] getBandwidth() {
		return bandwidths;
	}

	/**
	 * Returns the density for the given point
	 * 
	 * @param x the point
	 * @return its density
	 * @throws DialException 
	 */
	@Override
	public double getDensity(Double... x) throws DialException {
		double density = 0.0;
		int dim = (isBounded)? bandwidths.length -1 : bandwidths.length;
		for (Double[] point : points) {
			double logsum = 0.0;
			for (int i = 0 ; i < dim; i++) {
				logsum += Math.log((kernel.getDensity((x[i] - point[i]) / bandwidths[i])));
				logsum -= Math.log(bandwidths[i]);
			}
			density += Math.exp(logsum);
		}
		density /= points.size() ;
		
		// bounded support (cf. Jones 1993)
		if (isBounded) {
			Double[] l = new Double[bandwidths.length];
			Double[] u = new Double[bandwidths.length];
			for (int i = 0 ; i < bandwidths.length ; i++) {
				l[i] = (0 - x[i]) / bandwidths[i];
				u[i] = (1 - x[i]) / bandwidths[i];
			}
			double factor = 1/(kernel.getCDF(u) - kernel.getCDF(l));
			density = factor * density;
		}
		
		return density;
	}


	/**
	 * Samples from the kernel density function, first picking one of the point,
	 * and then deviating from it according to a Gaussian centered around it
	 * 
	 * @return the sampled point
	 */
	@Override
	public Double[] sample() {
		Double[] point = points.get(sampler.nextInt(points.size()));
		Double[] newPoint = new Double[point.length];
		
		for (int i = 0 ; i < newPoint.length ; i++) {
			newPoint[i] = new GaussianDensityFunction(point[i], 
					bandwidths[i] /Math.pow(bandwidths.length,3)).sample()[0];
		}
		
		if (isBounded) {
			double total = 0.0, negShift = 0.0;
			for (int i = 0 ; i < newPoint.length ; i++) {
				total += newPoint[i];
				if (newPoint[i] < negShift) {
					negShift = newPoint[i];
				}
			}
			for (int i = 0 ; i < newPoint.length ; i++) {
				newPoint[i] = (newPoint[i] - negShift) / (total - negShift*newPoint.length);
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
	public Map<Double[], Double> discretise(int nbBuckets) {
		Collections.shuffle(points);
		Map<List<Double>,Double> picked = new HashMap<List<Double>,Double>();
		int nbToPick = Math.min(nbBuckets, points.size());
		for (int i = 0 ; i < nbToPick ; i++) {
			Double[] point = points.get(i);
			if (!picked.containsKey(Arrays.asList(point))) {
				picked.put(Arrays.asList(point), 1.0/nbToPick);
			}
			else {
				picked.put(Arrays.asList(point), picked.get(Arrays.asList(point)) + 1.0/nbToPick);
			}
		}
		
		// convert to keys with double arrays
		Map<Double[], Double> vals = new HashMap<Double[], Double>();
		for (List<Double> point : picked.keySet()) {
			vals.put(point.toArray(new Double[point.size()]), picked.get(point));
		}
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
		Double[] means = getMean();
		for (double mean : means) {
			s += StringUtils.getShortForm(mean) +", ";
		}
		s = s.substring(0, s.length()-2) + "]),std=";
		double avgstd = 0.0;
		for (double std : getStandardDeviations()) {
			avgstd += std;
		} 
		s += StringUtils.getShortForm((avgstd / means.length));
		s += ") with " + points.size() + " kernels ";
		return s;
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
	 * Returns the dimensionality of the KDE.
	 * 
	 * @return the dimensionality
	 */
	@Override
	public int getDimensionality() {
		return bandwidths.length;
	}


	/**
	 * Returns the mean of the KDE.
	 * 
	 * @return the mean
	 */
	@Override
	public Double[] getMean() {
		Double[] mean = new Double[points.get(0).length];
		for (int i = 0 ; i < mean.length ; i++) {
			mean[i] = 0.0;
		}
		for (Double[] point : points) {
			for (int i = 0 ; i < mean.length ; i++) {
				mean[i] += point[i];
			}
		}
		for (int i = 0 ; i < mean.length ; i++) {
			mean[i] = mean[i] / points.size();
		}
		return mean;
	}


	/**
	 * Returns the variance of the KDE.
	 */
	@Override
	public Double[] getVariance() {
		Double[] mean = getMean();
		Double[] variance = new Double[points.get(0).length];
		for (int i = 0 ; i < variance.length ; i++) {
			variance[i] = 0.0;
		}
		for (Double[] point : points) {
			for (int i = 0 ; i < variance.length ; i++) {
				variance[i] += Math.pow(point[i] - mean[i], 2);
			}
		}
		for (int i = 0 ; i < variance.length ; i++) {
			variance[i] = variance[i] / points.size();
		}
		return variance;
	}

	

	/**
	 * Returns the cumulative probability distribution for the KDE.
	 * 
	 * @return the cumulative probability from 0 to x.
	 */
	@Override
	public Double getCDF(Double... x) throws DialException {
		if (x.length != getDimensionality()) {
			throw new DialException("Illegal dimensionality: " + x.length + "!=" + getDimensionality());
		}
		double nbOfLowerPoints = 0.0;
		for (Double[] point : points) {
			if (DistanceUtils.isLower(point,x)) {
				nbOfLowerPoints++;
			}
		}
		return nbOfLowerPoints / points.size();
	}


	

	/**
	 * Returns the standard deviation.
	 * 
	 * @return the standard deviation
	 */
	private Double[] getStandardDeviations() {
		Double[] variance = getVariance();
		Double[] std = new Double[points.get(0).length];
		for (int i = 0 ; i < variance.length ; i++) {
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
		for (int j = 0 ; j < this.points.get(0).length ; j++) {
			total += this.points.get(0)[j];
		}
		if (total > 0.99 && total < 1.01 && this.points.get(0).length > 1) {
			return true;
		}
		return false;
	}


	/**
	 * Estimate the bandwidths according to Silverman's rule of thumb.
	 */
	private void estimateBandwidths() {
		Double[] stds = getStandardDeviations();
		bandwidths = new Double[stds.length];
		for (int i = 0 ; i < bandwidths.length ;i++) {
			bandwidths[i] = 1.06 * stds[i] * Math.pow(points.size(), -1/(4.0+getDimensionality()));
			if (bandwidths[i] == 0.0) {
				bandwidths[i] = 0.05;
			}
		}
	}



	/**
	 * Converts the distribution to a Gaussian distribution and returns its XML
	 * representation.
	 * 
	 * @throws DialException 
	 * 
	 */
	@Override
	public List<Element> generateXML(Document doc) throws DialException {
		GaussianDensityFunction gaussian = new GaussianDensityFunction(points);
		return gaussian.generateXML(doc);
	}



	/**
	 * Multiplies the bandwidth of the KDE by a specific factor
	 * 
	 * @param factor the factor
	 */
	public void multiplyBandwidth(double factor) {
		for (int i = 0 ; i < bandwidths.length ; i++) {
			bandwidths[i] = bandwidths[i] * factor;
		}
	}
	

	

}
