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

import java.util.logging.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.bn.values.ValueFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * (Univariate) uniform density function, with a minimum and maximum.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class UniformDensityFunction implements DensityFunction {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// minimum threshold
	final double minimum;

	// maximum threshold
	final double maximum;

	// sampler
	static final Random sampler = new Random();

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
	}

	/**
	 * Returns the density at the given point
	 *
	 * @param x the point
	 * @return the density at the point
	 */
	@Override
	public double getDensity(double... x) {
		if (x[0] >= minimum && x[0] <= maximum) {
			return 1.0f / (maximum - minimum);
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
	public double[] sample() {
		double length = maximum - minimum;
		return new double[] { sampler.nextFloat() * length + minimum };
	}

	/**
	 * Returns a set of discrete values for the distribution
	 * 
	 * @param nbBuckets the number of buckets to employ
	 * @return the discretised values and their probability mass.
	 */
	@Override
	public Map<double[], Double> discretise(int nbBuckets) {
		Map<double[], Double> values = new HashMap<double[], Double>(nbBuckets);
		double step = (maximum - minimum) / nbBuckets;
		for (int i = 0; i < nbBuckets; i++) {
			double value = minimum + i * step + step / 2.0f;
			values.put(new double[] { value }, 1.0 / nbBuckets);
		}
		return values;
	}

	/**
	 * Returns the cumulative probability up to the given point
	 *
	 * @param x the point
	 * @return the cumulative probability greater than 1
	 */
	@Override
	public double getCDF(double... x) {
		if (x.length != 1) {
			throw new RuntimeException(
					"Uniform distribution currently only accepts a dimensionality == 1");
		}

		if (x[0] < minimum) {
			return 0.0;
		}
		else if (x[0] > maximum) {
			return 1.0;
		}
		else {
			return (x[0] - minimum) / (maximum - minimum);
		}
	}

	/**
	 * Returns a copy of the density function
	 * 
	 * @return the copy
	 */
	@Override
	public UniformDensityFunction copy() {
		return new UniformDensityFunction(minimum, maximum);
	}

	/**
	 * Returns a pretty print for the density function
	 * 
	 * @return the pretty print for the density
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
	@Override
	public int hashCode() {
		return (new Double(maximum)).hashCode() - (new Double(minimum)).hashCode();
	}

	/**
	 * Returns the mean of the distribution
	 * 
	 * @return the mean
	 */
	@Override
	public double[] getMean() {
		return new double[] { (minimum + maximum) / 2.0 };
	}

	/**
	 * Returns the variance of the distribution
	 * 
	 * @return the variance
	 */
	@Override
	public double[] getVariance() {
		return new double[] { Math.pow(maximum - minimum, 2) / 12.0 };
	}

	/**
	 * Returns the dimensionality (constrained here to 1).
	 * 
	 * @return 1.
	 */
	@Override
	public int getDimensions() {
		return 1;
	}

	@Override
	public List<Element> generateXML(Document doc) {
		Element distribElement = doc.createElement("distrib");

		Attr id = doc.createAttribute("type");
		id.setValue("uniform");
		distribElement.setAttributeNode(id);
		Element minEl = doc.createElement("min");
		minEl.setTextContent("" + ValueFactory.create(minimum));
		distribElement.appendChild(minEl);
		Element maxEl = doc.createElement("max");
		maxEl.setTextContent("" + ValueFactory.create(maximum));
		distribElement.appendChild(maxEl);

		return Arrays.asList(distribElement);
	}

}
