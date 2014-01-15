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

package opendial.bn.distribs.continuous.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;

/**
 * Density function represented by a univariate Gaussian.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GaussianDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("GaussianDensityFunction", Logger.Level.DEBUG);

	// the mean of the Gaussian
	double mean;

	// the variance of the Gaussian
	double variance;

	// the standard deviation of the Gaussian
	double stdDev;

	// sampler object
	Random sampler;

	// internal objects for sampling the Gaussian
	private double spare;
	private boolean spareready = false;



	/**
	 * Creates a new density function with the given mean and variance
	 * 
	 * @param mean the Gaussian mean
	 * @param variance the Gaussian variance
	 */
	public GaussianDensityFunction(double mean, double variance) {
		this.mean = mean;
		if (variance < 0) {
			log.warning("variance should not be negative, but is : " + variance);
		}
		this.variance = variance;
		stdDev = Math.sqrt(variance);
		sampler = new Random();
	}


	/**
	 * Returns the density at the given point
	 *
	 * @param x the point
	 * @return the density at the point
	 * @throws DialException 
	 */
	@Override
	public double getDensity(Double... x)  {
		double spread = 1.0/( stdDev * Math.sqrt(2*Math.PI));
		double exp = Math.exp(-Math.pow(x[0]-mean,2) / (2*variance));
		double result = spread*exp;
		return result;
	}


	/**
	 * Samples values from the Gaussian using Box-Muller's method.
	 *
	 * @return a sample value
	 */
	@Override
	public Double[] sample() {
		if (spareready) {
			spareready = false;
			return new Double[]{spare * stdDev + mean};
		}
		else {
			double u, v, s;
			do {
				u =  sampler.nextFloat() * 2 - 1;
				v = sampler.nextFloat() * 2 - 1;
				s = u * u + v * v;
			} while (s >= 1 || s == 0);
			spare = v * Math.sqrt(-2.0 * Math.log(s) / s);
			spareready = true;
			return new Double[]{mean + stdDev * u * Math.sqrt(-2.0 * Math.log(s) / s)};
		}
	}


	/**
	 * Returns a set of discrete values (of a size of nbBuckets) extracted
	 * from the Gaussian.  The number of values is derived from 
	 * Settings.NB_DISCRETISATION_BUCKETS
	 *
	 * @return the set of extracted values
	 */
	@Override
	public Map<Double[], Double> discretise(int nbBuckets) {
		
		Map<Double[], Double> values = new HashMap<Double[], Double>(nbBuckets);

		double minimum = mean - 4*stdDev;
		double maximum = mean + 4*stdDev;

		double step = (maximum-minimum)/nbBuckets;
		double cdf = 0.0;
		for (int i = 0 ; i < nbBuckets ; i++) {
			double value = minimum  + i*step + step/2.0f;
			try {
				double curCdf = getCDF(new Double[]{value});
				double prob = curCdf - cdf;
				values.put(new Double[]{value}, prob);
				cdf = curCdf;
			}
			catch (DialException e) {
				log.warning(e.toString());
			}
		}
		return values;
	}


	/**
	 * Returns the cumulative probability up to the point x
	 *
	 * @param x the point
	 * @return the cumulative density function up to the point
	 * @throws DialException 
	 */
	@Override
	public Double getCDF (Double... x) throws DialException {
		if (x.length != 1) {
			throw new DialException("Gaussian distribution only accepts a dimensionality == 1");
		}
		double z = (x[0]-mean) /stdDev;
		if (z < -8.0) return 0.0;
		if (z >  8.0) return 1.0;
		double sum = 0.0, term = z;
		for (int i = 3; sum + term != sum; i += 2) {
			sum  = sum + term;
			term = term * z * z / i;
		}
		return 0.5 + sum * Math.exp(-z*z / 2) / Math.sqrt(2 * Math.PI);
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
		return "N("+mean+"," + variance+")";
	}


	/**
	 * Returns the hashcode for the density function
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return (new Double(mean)).hashCode() + (new Double(variance)).hashCode();
	}
	
	
	/**
	 * Returns the mean of the Gaussian.
	 * 
	 */
	public Double[] getMean() {
		return new Double[]{mean};
	}
	
	
	/**
	 * Returns the variance of the Gaussian.
	 * 
	 */
	public Double[] getVariance() {
		return new Double[]{variance};
	}


	/**
	 * Returns 1.
	 * 
	 */
	@Override
	public int getDimensionality() {
		return 1;
	}
	

}
