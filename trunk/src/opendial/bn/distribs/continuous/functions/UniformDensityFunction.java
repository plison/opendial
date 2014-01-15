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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;

/**
 * (Univariate) uniform density function, with a minimum and maximum.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UniformDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("UniformDensityFunction", Logger.Level.NORMAL);
	
	// minimum threshold
	double minimum;
	
	// maximum threshold
	double maximum;
	
	// sampler
	Random sampler;
	
	/**
	 * Creates a new uniform density function with the given minimum and maximum
	 * threshold
	 * 
	 * @param minimum the minimum threshold
	 * @param maximum the maximum threshold
	 */
	public UniformDensityFunction(double minimum, double maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
		sampler = new Random();
	}
	
	
	/**
	 * Returns the density at the given point
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	public double getDensity(Double... x) {
		if (x[0] >= minimum && x[0] <= maximum) {
			return 1.0f/(maximum-minimum);
		}
		else {
			return 0.0f;
		}
	}

	
	/**
	 * Samples the density function 
	 * 
	 * @return the sampled point
	 */
	@Override
	public Double[] sample() {
		double length = maximum - minimum;
		return new Double[]{sampler.nextFloat()*length + minimum};
	}

	
	/**
	 * Returns a set of discrete values for the distribution
	 * 
	 * @return the discretised values and their probability mass.
	 */
	public Map<Double[],Double> discretise(int nbBuckets) {
		Map<Double[], Double> values = new HashMap<Double[],Double>(nbBuckets);
		double step = (maximum-minimum)/nbBuckets;
		for (int i = 0 ; i < nbBuckets ; i++) {
			double value = minimum  + i*step + step/2.0f;
			values.put(new Double[]{value}, 1.0/nbBuckets);
		}
		return values;
	}
	
	
	/**
	 * Returns the cumulative probability up to the given point
	 *
	 * @param x the point
	 * @return the cumulative probability 
	 * @throws DialException 
	 */
	@Override
	public Double getCDF(Double... x) throws DialException {
		if (x.length != 1) {
			throw new DialException("Uniform distribution currently only accepts a dimensionality == 1");
		}
		
		if (x[0] < minimum) {
			return 0.0;
		}
		else if (x[0] > maximum) {
			return 1.0;
		}
		else {
			return (x[0]-minimum) / (maximum-minimum);
		}
	}

	
	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public UniformDensityFunction copy() {
		return new UniformDensityFunction(minimum,maximum);
	}

	
	/**
	 * Returns a pretty print for the density function
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		return "Uniform(+" + minimum + "," + maximum + ")";
	}

	
	/**
	 * Returns the hashcode for the function
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return (new Double(maximum)).hashCode() - (new Double(minimum)).hashCode();
	}


	/**
	 * Returns the mean of the distribution
	 * 
	 * @return the mean
	 */
	@Override
	public Double[] getMean() {
		return new Double[]{(minimum + maximum)/2.0};
	}

	/**
	 * Returns the variance of the distribution
	 * 
	 * @return the variance
	 */
	@Override
	public Double[] getVariance() {
		return new Double[]{Math.pow(maximum - minimum, 2) / 12.0};
	}



	/**
	 * Returns the dimensionality (constrained here to 1).
	 * 
	 * @return 1.
	 */
	@Override
	public int getDimensionality() {
		return 1;
	}
	
}

