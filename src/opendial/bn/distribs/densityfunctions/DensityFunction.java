// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.bn.distribs.densityfunctions;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Density function for a continuous probability distribution. The density function
 * can be either univariate or multivariate.
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface DensityFunction {

	/**
	 * Returns the density value of the function at a given point
	 * 
	 * @param x the (possibly multivariate) point
	 * @return the density value for the point calculated.
	 */
	public double getDensity(double... x);

	/**
	 * Returns the mean of the density function. The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return the density mean.
	 */
	public double[] getMean();

	/**
	 * Returns the variance of the density function. The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return the density variance
	 */
	public double[] getVariance();

	/**
	 * Returns a sampled value given the point. The size of the double array
	 * corresponds to the dimensionality of the function.
	 * 
	 * @return the sampled value.
	 */
	public double[] sample();

	/**
	 * Returns the dimensionality of the density function.
	 * 
	 * @return the dimensionality.
	 */
	public int getDimensions();

	/**
	 * Returns a discretised version of the density function. The granularity of the
	 * discretisation is defined by the number of discretisation buckets.
	 * 
	 * @param nbBuckets the number of discretisation buckets
	 * @return a discretised probability distribution, mapping a collection of points
	 *         to a probability value
	 */
	public Map<double[], Double> discretise(int nbBuckets);

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
	 * @return the cumulative probability from 0 to x extracted
	 */
	public double getCDF(double... x);

	/**
	 * Returns the XML representation (as a list of XML elements) of the density
	 * function
	 * 
	 * @param doc the XML document for the node
	 * @return the corresponding XML elements
	 */
	public List<Element> generateXML(Document doc);

}
