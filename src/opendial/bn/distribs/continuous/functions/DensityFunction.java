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

import java.util.List;
import java.util.Map;

import opendial.arch.DialException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Density function for a continuous probability distribution. The density function can be either
 * univariate or multivariate.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */ 
public interface DensityFunction {

	/**
	 * Returns the density value of the function at a given point
	 * 
	 * @param x the (possibly multivariate) point
	 * @return the density value for the point
	 * @throws DialException 
	 */
	public double getDensity(Double ...x) throws DialException;
	

	/**
	 * Returns the mean of the density function.  The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return the density mean.
	 */
	public Double[] getMean();
	
	
	/**
	 * Returns the variance of the density function.  The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return
	 */
	public Double[] getVariance();
	
	
	/**
	 * Returns a sampled value given the point. The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return the sampled value.
	 */
	public Double[] sample();
	
	
	/**
	 * Returns the dimensionality of the density function.
	 * 
	 * @return the dimensionality.
	 */
	public int getDimensionality();


	/**
	 * Returns a discretised version of the density function.  The granularity of the 
	 * discretisation is defined by the number of discretisation buckets.
	 * 
	 * @param nbBuckets the number of discretisation buckets
	 * @return a discretised probability distribution, mapping a collection of points
	 *         to a probability value
	 */
	public Map<Double[], Double> discretise(int nbBuckets);

	
	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	public DensityFunction copy();
	


	/**
	 * Returns the cumulative probability up to the given point x.
	 * 
	 * @param x the (possibly multivariate) point x
	 * @return the cumulative probability from 0 to x
	 * @throws DialException if the CDF could not be extracted
	 */
	public Double getCDF(Double...x) throws DialException;


	
	/**
	 * Returns the XML representation (as a list of XML elements) of the density function
	 * 
	 * @param doc the XML document for the node
	 * @return the corresponding XML elements
	 */
	public List<Element> generateXML(Document doc) throws DialException;
	
}
