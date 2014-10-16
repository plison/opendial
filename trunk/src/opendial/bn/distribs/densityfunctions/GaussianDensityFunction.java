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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.utils.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Gaussian density function.  In the multivariate case, the density function is currently
 * limited to Gaussian distribution with a diagonal covariance (which are equivalent to the
 * product of univariate distributions).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GaussianDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("GaussianDensityFunction", Logger.Level.DEBUG);

	// the mean of the Gaussian
	Double[] mean;

	// the variance of the Gaussian
	// NB: we assume a diagonal covariance
	Double[] variance;

	// the standard deviation of the Gaussian
	Double[] stdDev;

	// sampler object
	Random sampler;

	// internal objects for sampling the Gaussian
	private Double[] spare;


	/**
	 * Creates a new density function with the given mean and variance vector.
	 * Only diagonal coveriance are currently supported
	 * 
	 * @param mean the Gaussian mean vector
	 * @param variance the variances for each dimension
	 */
	public GaussianDensityFunction(Double[] mean, Double[] variance) {
		this.mean = mean;
		if (mean.length != variance.length) {
			log.warning("different lengths for mean and variance");
		}
		stdDev = new Double[variance.length];
		for (int i = 0 ; i < variance.length ; i++) {
			if (variance[i] < 0) {
				log.warning("variance should not be negative, but is : " + variance);
			}
			stdDev[i] = Math.sqrt(variance[i]);
		}
		this.variance = variance;
		sampler = new Random();
	}

	/**
	 * Creates a new, univariate density function with a given mean and variance
	 *
	 * @param mean the Gaussian mean 
	 * @param variance the variance
	 */
	public GaussianDensityFunction(Double mean, Double variance) {
		this.mean = new Double[]{mean};
		this.variance = new Double[]{variance};
		stdDev = new Double[]{Math.sqrt(variance)};
		if (variance < 0) {
			log.warning("variance should not be negative, but is : " + variance);
		}
		sampler = new Random();
	}
	
	
	public GaussianDensityFunction(List<Double[]> samples) throws DialException {
		if (samples.isEmpty()) {
			throw new DialException("empty list of samples");
		}
		this.mean = new Double[samples.get(0).length];
		this.variance = new Double[samples.get(0).length];
		for (int i = 0 ; i < mean.length ; i++) {
			mean[i] = 0.0 ; variance[i] = 0.0;
		}
		for (Double[] sample : samples) {
			for (int i = 0 ; i < sample.length ; i++) {
				mean[i] += sample[i] / samples.size();
			}
		}
		for (Double[] sample : samples) {
			for (int i = 0 ; i < sample.length ; i++) {
				variance[i] += Math.pow(sample[i] - mean[i], 2) / samples.size();
			}
		}
		stdDev = new Double[variance.length];
		for (int i = 0 ; i < variance.length ; i++) {
			stdDev[i] = Math.sqrt(variance[i]);
		}
		sampler = new Random();
	}


	/**
	 * Returns the density at the given point
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	@Override
	public double getDensity(Double... x)  {
		double spread = 1.0/( Math.sqrt(2*Math.PI));
		double insideSum = 0;
		for (int i = 0 ; i < variance.length ; i++) {
			spread /= stdDev[i];
			insideSum -= Math.pow(x[i] - mean[i], 2) / (2*variance[i]);
		}
		return spread * Math.exp(insideSum);
	}


	/**
	 * Samples values from the Gaussian using Box-Muller's method.
	 *
	 * @return a sample value
	 */
	@Override
	public Double[] sample() {
		if (spare != null) {
			Double[] result = new Double[spare.length];
			for (int i = 0 ; i < spare.length ; i++) {
				result[i] = spare[i] * stdDev[i] + mean[i];
			}
			return result;
		}
		else {
			Double[] result = new Double[mean.length];
			Double[] spare = new Double[mean.length];
			for (int i = 0 ; i < mean.length ; i++) {			
				double u, v, s;
				do {
					u =  sampler.nextFloat() * 2 - 1;
					v = sampler.nextFloat() * 2 - 1;
					s = u * u + v * v;
				} while (s >= 1 || s == 0);
				spare[i] = v * Math.sqrt(-2.0 * Math.log(s) / s);
				result[i] = mean[i] + stdDev[i] * u * Math.sqrt(-2.0 * Math.log(s) / s);
			}
			return result;
		}
	}


	/**
	 * Returns a set of discrete values (of a size of nbBuckets) extracted
	 * from the Gaussian.  The number of values is derived from 
	 * Settings.NB_DISCRETISATION_BUCKETS
	 *
	 * @param nbBuckets the number of buckets to employ
	 * @return the set of extracted values
	 */
	@Override
	public Map<Double[], Double> discretise(int nbBuckets) {

		double[] minima = new double[mean.length];
		double[] step = new double[mean.length];
		for (int i = 0 ; i < mean.length ; i++) {
			minima[i] = mean[i] - 4*stdDev[i];
			step[i] = (8*stdDev[i])/nbBuckets;
		}

		Map<Double[], Double> values = new HashMap<Double[], Double>(nbBuckets);

		double prevCdf = 0;
		for (int i = 0 ; i < nbBuckets ; i++) {

			Double[] newVal = new Double[mean.length];
			for (int j = 0 ; j < mean.length ; j++) {
				newVal[j] = minima[j]  + i*step[j] + step[j]/2.0f;
			}
				Double curCdf = getCDF(newVal);
				values.put(newVal, curCdf - prevCdf);
				prevCdf = curCdf;
		}

		return values;
	}


	/**
	 * Returns the cumulative probability up to the point x
	 *
	 * @param x the point
	 * @return the cumulative density function up to the point
	 */
	@Override
	public Double getCDF (Double... x)  {
		double product = 1;
		for (int i = 0 ; i < mean.length ; i++) {
			double z = (x[i]-mean[i]) /stdDev[i];
			if (z < -8.0) return 0.0;
			if (z >  8.0) continue;
			double sum = 0.0, term = z;
			for (int j = 3; sum + term != sum; j += 2) {
				sum  = sum + term;
				term = term * z * z / j;
			}
			product *= 0.5 + sum * Math.exp(-z*z / 2) / Math.sqrt(2 * Math.PI);
		}
		return product;
	}


	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public GaussianDensityFunction copy() {
		return new GaussianDensityFunction(mean,variance);
	}


	/**
	 * Returns a pretty print representation of the function
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		return "N("+ValueFactory.create(mean)+"," + ValueFactory.create(variance)+")";
	}


	/**
	 * Returns the hashcode for the density function
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return mean.hashCode() + variance.hashCode();
	}


	/**
	 * Returns the mean of the Gaussian.
	 * 
	 */
	@Override
	public Double[] getMean() {
		return mean;
	}


	/**
	 * Returns the variance of the Gaussian.
	 * 
	 */
	@Override
	public Double[] getVariance() {
		return variance;
	}


	/**
	 * Returns the dimensionality of the Gaussian.
	 * 
	 */
	@Override
	public int getDimensionality() {
		return mean.length;
	}

	@Override
	public List<Element> generateXML(Document doc) {
		Element distribElement = doc.createElement("distrib");

		Attr id = doc.createAttribute("type");
		id.setValue("gaussian");
		distribElement.setAttributeNode(id);
		Element meanEl = doc.createElement("mean");
		meanEl.setTextContent((mean.length > 1)? 
				ValueFactory.create(mean).toString() 
				: "" + StringUtils.getShortForm(mean[0]));
		distribElement.appendChild(meanEl);
		Element varianceEl = doc.createElement("variance");
		varianceEl.setTextContent((variance.length > 1)? 
				ValueFactory.create(variance).toString() 
				: "" + StringUtils.getShortForm(variance[0]));
		distribElement.appendChild(varianceEl);
		
		return Arrays.asList(distribElement);
	}


}
