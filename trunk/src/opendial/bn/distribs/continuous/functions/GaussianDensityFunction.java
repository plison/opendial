// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import opendial.arch.Logger;

/**
 * Density function represented by a Gaussian
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GaussianDensityFunction implements UnivariateDensityFunction {

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
	 */
	public double getDensity(double x) {
		double spread = 1.0/( stdDev * Math.sqrt(2*Math.PI));
		double exp = Math.exp(-Math.pow(x-mean,2) / (2*variance));
		double result = spread*exp;
		return result;
	}


	/**
	 * Samples values from the Gaussian
	 *
	 * @return a sample value
	 */
	@Override
	public double sample() {
		if (spareready) {
			spareready = false;
			return spare * stdDev + mean;
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
			return mean + stdDev * u * Math.sqrt(-2.0 * Math.log(s) / s);
		}
	}


	/**
	 * Returns a set of discrete values (of a size of nbBuckets) extracted
	 * from the Gaussian
	 *
	 * @param nbBuckets the number of values to extract
	 * @return the set of extracted values
	 */
	@Override
	public List<Double> getDiscreteValues(int nbBuckets) {

		List<Double> values = new ArrayList<Double>(nbBuckets);

		double minimum = mean - 4*stdDev;
		double maximum = mean + 4*stdDev;

		double step = (maximum-minimum)/nbBuckets;
		for (int i = 0 ; i < nbBuckets ; i++) {
			double value = minimum  + i*step + step/2.0f;
			values.add(value);
		}
		return values;
	}


	/**
	 * Returns the cumulative probability up to the point x
	 *
	 * @param x the point
	 * @return the cumulative density function up to the point
	 */
	public double getCDF (double x) {
		double z = (x-mean) /stdDev;
		if (z < -8.0) return 0.0;
		if (z >  8.0) return 1.0;
		double sum = 0.0, term = z;
		for (int i = 3; sum + term != sum; i += 2) {
			sum  = sum + term;
			term = term * z * z / i;
		}
		return 0.5 + sum * density_normalised(z);
	}


	/**
	 * Normalise the density function
	 * 
	 * @param x the initial point
	 * @return its normalised equivalent
	 */
	private static double density_normalised(double x) {
		return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
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
	public String prettyPrint() {
		return "N("+mean+"," + variance+")";
	}


	/**
	 * Returns a pretty print representation of the function
	 * 
	 * @return the pretty print
	 */
	public String toString() {
		return prettyPrint();
	}


	/**
	 * Returns the hashcode for the density function
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return (new Double(mean)).hashCode() + (new Double(variance)).hashCode();
	}
	
	
	public double getMean() {
		return mean;
	}
	
	public double getVariance() {
		return variance;
	}
}
