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
import java.util.Set;

import opendial.arch.Logger;

/**
 * Density function for a continuous probability distribution
 * 
 * TODO: extend this to handle e.g. Dirichlets
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface UnivariateDensityFunction{

	
	/**
	 * Returns the density value of the function at a given point
	 * 
	 * @param x the point
	 * @return the density value for the point
	 */
	public abstract double getDensity(double x);
	
	/**
	 * Returns the cumulative probability up to the given point
	 * 
	 * @param x the point
	 * @return the cumulative density up to the point
	 */
	public abstract double getCDF(double x);
	
	/**
	 * Returns a sampled value given the point
	 * 
	 * @return
	 */
	public abstract double sample();
	
	/**
	 * Returns a set of discrete values representing the function
	 * 
	 * @param nbBuckets the number of buckets for the discretisation
	 * @return the resulting points
	 */
	public abstract List<Double> getDiscreteValues(int nbBuckets);
	
	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	public UnivariateDensityFunction copy();
	
	
	/**
	 * Returns a pretty print for the function
	 * 
	 * @return the pretty print representation
	 */
	public String prettyPrint();
	
	
	/**
	 * Calculates the mean of the density function
	 * 
	 * @return the mean
	 */
	public double getMean();
	
	
	/**
	 * Calculates the variance of the density function
	 * 
	 * @return the variance
	 */
	public double getVariance();
	
}
