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


import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.values.ArrayVal;
import opendial.utils.MathUtils;

/**
 * Density function for a Dirichlet distribution.  The distribution is defined through an array
 * of alpha hyper-parameters. 
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $ *
 */
public class DirichletDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("DirichletDensityFunction",
			Logger.Level.DEBUG);

	// hyper-parameters
	Double[] alphas;

	// normalisation factor
	double C;
	
	// random number generator
	Random rng = new Random(Calendar.getInstance().getTimeInMillis() + Thread.currentThread().getId());

	/**
	 * Create a new Dirichlet density function with the provided alpha parameters
	 * 
	 * @param alphas the hyper-parameters for the density function
	 */
	public DirichletDensityFunction(Double[] alphas) {
		this.alphas = alphas;
		if (alphas.length < 2) {
			log.warning("must have at least 2 alphas");
		}
		for (int i = 0 ; i < alphas.length ; i++) {
			if (alphas[i] <= 0) {
				log.warning("alphas of the Dirichlet distribution are not well formed");
			}
		}
		C = calculateC();
	}

	/**
	 * Returns the density for a given point x.  The dimensionality of x must correspond to
	 * the dimensionality of the density function.
	 * 
	 * @param x a given point
	 * @return the density for the point
	 */
	@Override
	public double getDensity(Double... x) {
		if (x.length == alphas.length) {

			double sum = 0;
			for (int i = 0; i < x.length ; i++) {
				if (x[i] <0 || x[i] > 1) {
					log.warning(new ArrayVal(x) + " does not satisfy the constraints >= 0 and <= 1");
				}
				sum += x[i];
			}
			if (sum < 0.98 || sum > 1.02) {
				log.warning(new ArrayVal(x) + " does not sum to 1.0");
			}

			double result = C;
			for (int i = 0; i < x.length ; i++) {
				result *= Math.pow(x[i], alphas[i]-1);
			}
			return result;
		}
		log.warning("incompatible sizes: " + x.length + "!=" + alphas.length);
		return 0.0;
	}


	/**
	 * Returns the dimensionality of the density function
	 * 
	 * @return the dimensionality
	 */
	public int getDimensionality() {
		return alphas.length;
	}

	
	/**
	 * Returns a sampled value for the density function.
	 * 
	 * @return the sampled point.
	 */
	@Override
	public Double[] sample() {

		double sum = 0;
		Double[] sample = new Double[alphas.length];
		for (int i = 0 ; i < alphas.length ; i++) {
			sample[i] = sampleFromGamma(alphas[i], 1);
			sum += sample[i];
		}
		for (int i = 0 ; i < alphas.length ; i++) {
			sample[i] = sample[i] / sum;
		}
		return sample;		
	}

	
	/**
	 * Copies the density function (keeping the same alpha-values).
	 * 
	 * @return the copied function
	 */
	@Override
	public DirichletDensityFunction copy() {
		return new DirichletDensityFunction(alphas);
	}

	/**
	 * Returns the name and hyper-parameters of the distribution
	 * 
	 * @return the string for the density function
	 */
	@Override
	public String toString() {
		return "Dirichlet(" + Arrays.asList(alphas) + ")";
	}

	/**
	 * Returns the normalisation factor for the distribution.
	 * 
	 * @return the normalisation factor.
	 */
	private double calculateC() {
		double alphaSum = 0;
		double denominator = 1;
		for (int i = 0 ; i < alphas.length ;i++) {
			alphaSum += alphas[i];
			denominator *= MathUtils.gamma(alphas[i]);
		}
		double numerator = MathUtils.gamma(alphaSum);
		if (denominator != 0.0) {
		return numerator / denominator;
		}
		else {
			return Double.MAX_VALUE;
		}
	}
	
	   
	/**
	 * Samples a value from a gamma distribution with parameters k and theta. Reference: 
	 * Non-Uniform Random Variate Generation, Devroye.
	 * (URL: http://cgm.cs.mcgill.ca/~luc/rnbookindex.html).
	 * 
	 * @param k the parameter k
	 * @param theta the parameter theta
	 * @return the sample distribution
	 */
	private double sampleFromGamma(double k, double theta) {
		 boolean accept = false;
		    if (k < 1) {
		 // Weibull algorithm
		 double c = (1 / k);
		 double d = ((1 - k) * Math.pow(k, (k / (1 - k))));
		 double u, v, z, e, x;
		 do {
		  u = rng.nextDouble();
		  v = rng.nextDouble();
		  z = -Math.log(u);
		  e = -Math.log(v);
		  x = Math.pow(z, c);
		  if ((z + e) >= (d + x)) {
		   accept = true;
		  }
		 } while (!accept);
		 return (x * theta);
		    } else {
		 // Cheng's algorithm
		 double b = (k - Math.log(4));
		 double c = (k + Math.sqrt(2 * k - 1));
		 double lam = Math.sqrt(2 * k - 1);
		 double cheng = (1 + Math.log(4.5));
		 double u, v, x, y, z, r;
		 do {
		  u = rng.nextDouble();
		  v = rng.nextDouble();
		  y = ((1 / lam) * Math.log(v / (1 - v)));
		  x = (k * Math.exp(y));
		  z = (u * v * v);
		  r = (b + (c * y) - x);
		  if ((r >= ((4.5 * z) - cheng)) ||
		                    (r >= Math.log(z))) {
		   accept = true;
		  }
		 } while (!accept);
		 return (x * theta);
		    }
		  }

	
	/**
	 * Returns a discretised version of the Dirichlet.  The discretised table
	 * is simply a list of X sampled values from the Dirichlet, each value having
	 * a probability 1/X. 
	 * 
	 * @return the discretised version of the density function.
	 */
	@Override
	public Map<Double[], Double> discretise(int nbBuckets) {
		Map<Double[], Double> table = new HashMap<Double[], Double>();
		for (int i = 0 ; i < nbBuckets ; i++) {
			table.put(sample(), 1.0/nbBuckets);
		}
		return table;
	}
	
	/**
	 * Returns the mean of the Dirichlet.
	 * 
	 * @return the mean value.
	 */
	@Override
	public Double[] getMean() {
		Double[] mean = new Double[alphas.length];
		for (int i = 0 ; i < alphas.length ; i++) {
			mean[i] = alphas[i] / getAlphaSum();
		}
		return mean;
	}
	
	
	/**
	 * Returns the variance of the Dirichlet. 
	 * 
	 * @return the variance.
	 */
	@Override
	public Double[] getVariance() {
		Double[] variance = new Double[alphas.length];
		double denominator = Math.pow(getAlphaSum(), 2) * (getAlphaSum() + 1);
		for (int j = 0 ; j < alphas.length ; j++) {
			double numerator = alphas[j]*(getAlphaSum() - alphas[j]);
			variance[j] = numerator / denominator;
		}
		return variance;
	}
	
	
	/**
	 * Throws an exception (calculating the CDF of a Dirichlet is quite hard and not
	 * currently implemented).
	 * 
	 */
	@Override
	public Double getCDF(Double... x) throws DialException {
		throw new DialException("currently not implemented (CDF of Dirichlet has apparently no closed-form solution)");
	}



	
	/**
	 * Returns the hashcode for the distribution.
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return -32 + alphas.hashCode();
	}
	
	
	private double getAlphaSum() {
		double sum = 0;
		for (int j = 0 ; j < alphas.length ; j++) {
			sum += alphas[j];
		}
		return sum;
	}

	
	
}

