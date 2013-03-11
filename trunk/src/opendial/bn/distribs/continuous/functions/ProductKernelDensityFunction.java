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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;
import opendial.utils.DistanceUtils;
import opendial.utils.InferenceUtils;

/**
 * Density function represented as a kernel of points
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProductKernelDensityFunction implements MultivariateDensityFunction {

	// logger
	public static Logger log = new Logger("ProductKernelDensityFunction", Logger.Level.DEBUG);

	// bandwidth for the kernel
	double[] bandwidths;

	// the kernel function
	GaussianDensityFunction kernel = new GaussianDensityFunction(0.0, 1.0);

	// the points
	List<double[]> points;

	// the sampler
	Random sampler;

	boolean isBounded = false;


	/**
	 * Creates a new kernel density function with the given points
	 * 
	 * @param points the points
	 */
	public ProductKernelDensityFunction(Collection<double[]> points) {
		this.points = new ArrayList<double[]>(points);		
		sampler = new Random();
		estimateBandwidths();
	}


	public void setAsBounded(boolean isBounded) {
		this.isBounded = isBounded;
	}

	// Silverman's rule of thumb
	private void estimateBandwidths() {
		double[] stds = getStandardDeviations();
		bandwidths = new double[stds.length];
		for (int i = 0 ; i < bandwidths.length ;i++) {
			bandwidths[i] = 1.06 * stds[i] * Math.pow(points.size(), -1/(4.0+getDimensionality()));
			if (bandwidths[i] == 0.0) {
				bandwidths[i] = 0.05;
			}
		}
	}





	/**
	 * Returns the bandwidth defined for the KDE
	 * 
	 * @return the bandwidth
	 */
	public double[] getBandwidth() {
		return bandwidths;
	}

	/**
	 * Returns the density for the given point
	 * 
	 * @param x the point
	 * @return its density
	 */
	@Override
	public double getDensity(double[] x) {
		double density = 0.0;
		int nbKernels = (isBounded)? bandwidths.length - 1 : bandwidths.length;
		for (double[] point : points) {
			double logsum = 0.0;
			for (int i = 0 ; i < nbKernels; i++) {
				logsum += Math.log((kernel.getDensity((x[i] - point[i]) / bandwidths[i])));
				logsum -= Math.log(bandwidths[i]);
			}
			density += Math.exp(logsum);
		}
		density /= points.size() ;

		return density;
	}


	/**
	 * Samples from the kernel density function, first picking one of the point,
	 * and then deviating from it according to a Gaussian centered around it
	 * 
	 * @return the sampled point
	 */
	@Override
	public double[] sample() {
		double[] point = points.get(sampler.nextInt(points.size()));

		double[] newPoint = new double[point.length];

		if (isBounded) {
			int indexToChange = selectIndexToChange(point);
			for (int i = 0 ; i < point.length ; i++) {
				if (i != indexToChange) {
					newPoint[i] = point[i];
				}
				else {
					newPoint[i] = new GaussianDensityFunction(point[i], bandwidths[i]).sample();
				}
			}
			newPoint = InferenceUtils.normalise(newPoint);

		}
		else {
			for (int i = 0 ; i < newPoint.length ; i++) {
				newPoint[i] = new GaussianDensityFunction(point[i], bandwidths[i]).sample();
			}
		}

		return newPoint;

	}


	private int selectIndexToChange(double[] point) {
		try {
			Map<Integer,Double> map = new HashMap<Integer,Double>();
			for (int i = 0 ; i < point.length ; i++) {
				map.put(i, point[i]);
			}
			int indexToChange = (new Intervals<Integer>(map)).sample().intValue();
			return indexToChange;
		}
		catch (DialException e) {
			log.warning("could not select index to change, taking random index: " + e.toString());
			return (new Random()).nextInt(point.length);
		}
	}
	

	/**
	 * Returns a set of discrete values for the density function.  This set
	 * of values is of size nbBuckets
	 *
	 * @param nbBuckets the number of values to extract
	 * @return the discretised values
	 */
	@Override
	public List<double[]> getDiscreteValueArrays(int nbBuckets) {
		List<double[]> values = new ArrayList<double[]>();
		double nbToPick = nbBuckets / points.size();
		if (nbToPick > 0) {
			for (double[] val : points) {
				for (int i = 0 ; i <nbToPick && nbToPick >= 1.0 ; i++) {
					values.add(val);
				}
			}
		}
		else {
			while (values.size() < nbBuckets) {
				double[] val = sample();
				if (!values.contains(val)) {
					values.add(val);
				}
			}
			//		Collections.sort(values);
		}
		return values;
	}

	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public ProductKernelDensityFunction copy() {
		ProductKernelDensityFunction copy = new ProductKernelDensityFunction(points);
		copy.setAsBounded(isBounded);
		return copy;
	}

	/**
	 * Return a pretty print for the kernel density
	 * 
	 * @return
	 */
	@Override
	public String prettyPrint() {
		String s = "MKDE(mean=[";
		double[] means = getMean();
		for (double mean : means) {
			s += DistanceUtils.shorten(mean) +", ";
		}
		s = s.substring(0, s.length()-2) + "]) , avg. std=";
		double avgstd = 0.0;
		for (double std : getStandardDeviations()) {
			avgstd += std;
		} 
		s += DistanceUtils.shorten((avgstd / means.length));
		s += ") with " + points.size() + " kernels ";
		return s;
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
	public int getDimensionality() {
		return bandwidths.length;
	}


	@Override
	public double[] getMean() {
		double[] mean = new double[points.get(0).length];
		for (double[] point : points) {
			for (int i = 0 ; i < mean.length ; i++) {
				mean[i] += point[i];
			}
		}
		for (int i = 0 ; i < mean.length ; i++) {
			mean[i] = mean[i] / points.size();
		}
		return mean;
	}


	public double[] getVariance() {
		double[] mean = getMean();
		double[] variance = new double[points.get(0).length];
		for (double[] point : points) {
			for (int i = 0 ; i < variance.length ; i++) {
				variance[i] += Math.pow(point[i] - mean[i], 2);
			}
		}
		for (int i = 0 ; i < variance.length ; i++) {
			variance[i] = variance[i] / points.size();
		}
		return variance;
	}


	private double[] getStandardDeviations() {
		double[] variance = getVariance();
		double[] std = new double[points.get(0).length];
		for (int i = 0 ; i < variance.length ; i++) {
			std[i] = Math.sqrt(variance[i]);
		}
		return std;
	}

}
