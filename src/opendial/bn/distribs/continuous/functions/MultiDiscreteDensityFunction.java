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
import java.util.Collections;
import java.util.HashMap;
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
public class MultiDiscreteDensityFunction implements MultivariateDensityFunction {

	// logger
	public static Logger log = new Logger("MultiDiscreteDensityFunction", Logger.Level.DEBUG);
	
	// the set of points for the density function
	Map<double[],Double> points;
	
	// the sampler
	Random sampler;

	// minimum distance between two points;
	double avgDistance;
	
	
	/**
	 * Creates a new discrete density function, given the set of points
	 * 
	 * @param points a set of (value,prob) pairs
	 */
	public MultiDiscreteDensityFunction(Map<double[],Double> points) {
		this.points = new HashMap<double[],Double>();
		this.points.putAll(points);
		sampler = new Random();
		avgDistance = DistanceUtils.getAverageDistance(points.keySet());
	}
	
	
	/**
	 * Returns the density for a given point
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	public double getDensity(double[] x) {		
		double[] closest = null;
		double closestDist = - Double.MAX_VALUE;
		for (double[] point : points.keySet()) {
			double curDist = DistanceUtils.getDistance(point, x);
			if (closest == null || curDist < closestDist) {
				closest = point;
				closestDist = curDist;
			}
		}
		if (closestDist < avgDistance/2) {
			return points.get(closest) / avgDistance;
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
	 * Returns a set of discrete values (with a size of nbBuckets).  The values
	 * are the ones defined in the density function, repeated a number of times
	 * proportional to their probability.
	 *
	 * @param nbBuckets the number of buckets
	 * @return the set of discrete values
	 */
	@Override
	public List<double[]> getDiscreteValueArrays(int nbBuckets) {
		List<double[]> values = new ArrayList<double[]>();
		int nbLoops = 1;
		while (values.size() < nbBuckets && nbLoops < 5) {
		for (double[] val : points.keySet()) {
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
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public MultiDiscreteDensityFunction copy() {
		return new MultiDiscreteDensityFunction(points);
	}


	/**
	 * Returns a pretty print representation of the function
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		String s = "MKDE(";
		for (double[] point : points.keySet()) {
			s += "(";
			for (int i = 0 ; i < point.length; i++) {
				s += DistanceUtils.shorten(point[i]) + ",";
			}
			s = s.substring(0, s.length()-1)+"):=" + DistanceUtils.shorten(points.get(point));
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


	@Override
	public int getDimensionality() {
		return points.keySet().iterator().next().length;
	}


	@Override
	public double[] getMean() {
		double[] mean = new double[getDimensionality()];
		for (int i = 0 ; i < getDimensionality(); i++) {
			for (double[] point : points.keySet()) {
				mean[i] += (point[i] * points.get(point));
			}
		}
		return mean;
	}

	// verify correctness
	@Override
	public double[] getVariance() {
		double[] variance = new double[getDimensionality()];
		double[] mean = getMean();
		for (int i = 0 ; i < getDimensionality(); i++) {
			for (double[] point : points.keySet()) {
				variance[i] += Math.pow(point[i] - mean[i], 2) * points.get(point);
			}
		}
		return variance;
	}
	

	
}
