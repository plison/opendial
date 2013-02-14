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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.Logger;
import opendial.utils.DistanceUtils;

/**
 * Density function defined via a set of discrete points
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DiscreteDensityFunction implements UnivariateDensityFunction {

	// logger
	public static Logger log = new Logger("DiscreteDensityFunction", Logger.Level.DEBUG);
	
	// the set of points for the density function
	SortedMap<Double,Double> points;
	
	// the sampler
	Random sampler;

	// minimum distance between two points;
	double minDistance;
	
	
	/**
	 * Creates a new discrete density function, given the set of points
	 * 
	 * @param points a set of (value,prob) pairs
	 */
	public DiscreteDensityFunction(Map<Double,Double> points) {
		this.points = new TreeMap<Double,Double>();
		this.points.putAll(points);
		sampler = new Random();
		minDistance = DistanceUtils.getMinDistance(points.keySet());
	}
	
	
	/**
	 * Returns the density for a given point
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	public double getDensity(double x) {		
		double closest = Double.MAX_VALUE;
		for (double point : points.keySet()) {
			if (Math.abs(point-x) < Math.abs(closest-x)) {
				closest = point;
			}
		}
		if (Math.abs(closest-x) < minDistance/2) {
			return points.get(closest) / minDistance;
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
	public double sample() {
	        double sampled = sampler.nextFloat();
	        double sum = 0.0;
	        for (Double point : points.keySet()) {
	        	sum += points.get(point);
	        	if (sampled < sum) {
	        		return point;
	        	}
	        }
	        log.warning("discrete density function could not be sampled");
	        return 0.0;
	}
	
	
	/**
	 * Returns a set of discrete values (with a size of nbBuckets).  The values
	 * are the ones defined in the density function, repeated a number of times
	 * proportional to their probability.
	 *
	 * @param nbBuckets the number of buckets
	 * @return the set of discrete values
	 */
	@Override
	public List<Double> getDiscreteValues(int nbBuckets) {
		List<Double> values = new ArrayList<Double>();
		int nbLoops = 1;
		while (values.size() < nbBuckets && nbLoops < 5) {
		for (double val : points.keySet()) {
			double nbToPick = points.get(val) * nbBuckets * nbLoops;
				for (int i = 0 ; i <nbToPick && nbToPick >= 1.0 ; i++) {
					if (!values.contains(val)) {
						values.add(val);
					}
				}
		}
		nbLoops++;
		}
		return values;
	}
	
	   
	/**
	 * Returns the cumulative density value up to the given point
	 *
	 * @param x the point
	 * @return the resulting density value
	 */
	public double getCDF (double x) {
		double cdf = 0.0f;
		for (Double point : points.keySet()) {
			if (point <= x) {
				cdf += points.get(point);
			}
		}
		return cdf;
	}


	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public UnivariateDensityFunction copy() {
		return new DiscreteDensityFunction(points);
	}


	/**
	 * Returns a pretty print representation of the function
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		return points.toString();
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
	
	
	public double getMean() {
		double mean = 0;
		for (double point : points.keySet()) {
			mean += point * points.get(point);
		}
		return mean;
	}
	
	public double getVariance() {
		double variance = 0;
		double mean = getMean();
		for (double point : points.keySet()) {
			variance += Math.pow(point - mean, 2) * points.get(point);
		}
		return variance;
	}
	
}
