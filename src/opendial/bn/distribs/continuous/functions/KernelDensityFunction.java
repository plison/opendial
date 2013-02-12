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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import opendial.arch.Logger;
import opendial.bn.values.Value;

/**
 * Density function represented as a kernel of points
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class KernelDensityFunction implements UnivariateDensityFunction {

	// logger
	public static Logger log = new Logger("KernelDensityFunction", Logger.Level.DEBUG);

	// bandwidth for the kernel
	double bandwidth = 1.0;
	
	// the kernel function
	GaussianDensityFunction kernel = new GaussianDensityFunction(0.0, 1.0);
	
	// the points
	List<Double> points;
	
	// the sampler
	Random sampler;
	
	
	/**
	 * Creates a new kernel density function with the given points
	 * 
	 * @param points the points
	 */
	public KernelDensityFunction(Collection<Double> points) {
		this.points = new ArrayList<Double>();		
		sampler = new Random();
		this.points.addAll(points);
		Collections.sort(this.points);
		sampler = new Random();
		estimateBandwidth();
	}
	
	
	// Silverman's rule of thumb
	private void estimateBandwidth() {
		double std = Math.sqrt(getVariance());		
		bandwidth = 1.06 * std * Math.pow(points.size(), -1/5.0);
	}
	

	
	/**
	 * Changes the bandwidth for the KDE
	 * 
	 * @param bandwidth the bandwidth
	 */
	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	
	
	/**
	 * Returns the bandwidth defined for the KDE
	 * 
	 * @return the bandwidth
	 */
	public double getBandwidth() {
		return bandwidth;
	}
	
	/**
	 * Returns the density for the given point
	 * 
	 * @param x the point
	 * @return its density
	 */
	@Override
	public double getDensity(double x) {
		double sum = 0.0;
		for (Double point : points) {
			sum += kernel.getDensity((x - point) / bandwidth);
		}
		return sum / (points.size() * bandwidth) ;
	}

	
	/**
	 * Returns the cumulative probability up to the point
	 * 
	 * TODO: test this implementation!
	 * 
	 * @param x the point
	 * @return its cumulative density
	 */
	@Override
	public double getCDF(double x) {
		double index = Collections.binarySearch(points, x);
		if (index < 0 ) {
			index = -index-1;
		}
		else {
			index = points.lastIndexOf(x)+1;
		}
		return index/points.size();
	}

	
	/**
	 * Samples from the kernel density function, first picking one of the point,
	 * and then deviating from it according to a Gaussian centered around it
	 * 
	 * @return the sampled point
	 */
	@Override
	public double sample() {
		double point = points.get(sampler.nextInt(points.size()));
		return new GaussianDensityFunction(point, bandwidth).sample();
	}

	/**
	 * Returns a set of discrete values for the density function.  This set
	 * of values is of size nbBuckets
	 *
	 * @param nbBuckets the number of values to extract
	 * @return the discretised values
	 */
	@Override
	public List<Double> getDiscreteValues(int nbBuckets) {
		List<Double> values = new ArrayList<Double>();
		double nbToPick = nbBuckets / points.size();
		if (nbToPick > 0) {
		for (double val : points) {
				for (int i = 0 ; i <nbToPick && nbToPick >= 1.0 ; i++) {
					values.add(val);
				}
		}
		}
		else {
			while (values.size() < nbBuckets) {
				double val = sample();
				if (!values.contains(val)) {
					values.add(val);
				}
			}
			Collections.sort(values);
		}
		return values;
	}

	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public UnivariateDensityFunction copy() {
		KernelDensityFunction copy = new KernelDensityFunction(points);
		copy.setBandwidth(bandwidth);
		return copy;
	}

	/**
	 * Return a pretty print for the kernel density
	 * 
	 * @return
	 */
	@Override
	public String prettyPrint() {
		return "KDE("+points.toString()+")";
	}
	
	
	/**
	 * Returns the hashcode for the function
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return points.hashCode();
	}


	@Override
	public double getMean() {
		double mean = 0.0;
		for (double point :points) {
			mean += point;
		}
		mean = mean / points.size();
		return mean;
	}


	public double getVariance() {
		double mean = 0;
		for (Double point : points) {
			mean += point.doubleValue();
		}
		mean = mean / points.size();
		
		double variance = 0;
		
		for (Double point : points) {
			variance += Math.pow(point.doubleValue() - mean, 2);
		}
		variance = variance / points.size();
		
		return variance;
	}

}
