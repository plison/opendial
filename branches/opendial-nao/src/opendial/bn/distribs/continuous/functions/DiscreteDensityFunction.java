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
import opendial.utils.DistanceUtils;
import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

/**
 * Density function defined via a set of discrete points.  The density at a given point x
 * is defined as the probability mass for the closest point y in the distribution, divided
 * by a constant volume (used for normalisation).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DiscreteDensityFunction implements DensityFunction {

	// logger
	public static Logger log = new Logger("DiscreteDensityFunction", Logger.Level.DEBUG);

	// the set of points for the density function
	Map<Double[],Double> points;

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
	public DiscreteDensityFunction(Map<Double[],Double> points) {
		this.points = new HashMap<Double[],Double>();
		this.points.putAll(points);
		sampler = new Random();
		
		// calculate the minimum distance between points
		this.minDistance = DistanceUtils.getMinEuclidianDistance(points.keySet());
		
		// and define the volume with a radius that is half this distance
		this.volume = MathUtils.getVolume(minDistance/2, getDimensionality());
	}


	/**
	 * Returns the density for a given point.  The density is derived in two steps:<br><ol>
	 * 
	 * <li> locating the point in the distribution that is closest to x 
	 *    (according to Euclidian distance)
	 *    
	 * <li> dividing the probability mass for the point by the n-dimensional volume 
	 *    around this point.  The radius of the ball is the half of the minimum
	 *    distance between the points of the distribution.
	 * </ol>
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	@Override
	public double getDensity(Double... x) {		
		Double[] closest = null;
		double closestDist = - Double.MAX_VALUE;
		for (Double[] point : points.keySet()) {
			double curDist = DistanceUtils.getDistance(point, x);
			if (closest == null || curDist < closestDist) {
				closest = point;
				closestDist = curDist;
			}
		}
		if (closestDist < minDistance/2) {
			return points.get(closest) / MathUtils.getVolume(minDistance/2, getDimensionality());
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
	public Double[] sample() {
		double sampled = sampler.nextFloat();
		double sum = 0.0;
		for (Double[] point : points.keySet()) {
			sum += points.get(point);
			if (sampled < sum) {
				return point;
			}
		}
		log.warning("discrete density function could not be sampled");
		return new Double[0];
	}


	/**
	 * Returns the points for this distribution.
	 * 
	 */
	@Override
	public Map<Double[], Double> discretise(int nbBuckets) {
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
		for (Double[] point : points.keySet()) {
			s += "(";
			for (int i = 0 ; i < point.length; i++) {
				s += StringUtils.getShortForm(point[i]) + ",";
			}
			s = s.substring(0, s.length()-1)+"):=" + StringUtils.getShortForm(points.get(point));
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
	public int getDimensionality() {
		return points.keySet().iterator().next().length;
	}


	/**
	 * Returns the means of the distribution (calculated like for a categorical distribution).
	 */
	@Override
	public Double[] getMean() {
		Double[] mean = new Double[getDimensionality()];
		for (int i = 0 ; i < getDimensionality(); i++) {
			mean[i] = 0.0;
			for (Double[] point : points.keySet()) {
				mean[i] += (point[i] * points.get(point));
			}
		}
		return mean;
	}

	/**
	 * Returns the variance of the distribution (calculated like for a categorical distribution)
	 * 
	 */
	@Override
	public Double[] getVariance() {
		Double[] variance = new Double[getDimensionality()];
		Double[] mean = getMean();
		for (int i = 0 ; i < getDimensionality(); i++) {
			variance[i] = 0.0;
			for (Double[] point : points.keySet()) {
				variance[i] += Math.pow(point[i] - mean[i], 2) * points.get(point);
			}
		}
		return variance;
	}


	/**
	 * Returns the cumulative distribution for the distribution (by counting all the points
	 * with a value that is lower than x). 
	 */
	public Double getCDF(Double... x) throws DialException {
		if (x.length != getDimensionality()) {
			throw new DialException("Illegal dimensionality: " + x.length + "!=" + getDimensionality());
		}

		double cdf = 0.0f;
		for (Double[] point : points.keySet()) {
			if (DistanceUtils.isLower(point, x)) {
				cdf += points.get(point);
			}
		}
		return cdf;
	}

}
