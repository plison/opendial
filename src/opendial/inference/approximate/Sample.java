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

package opendial.inference.approximate;

import java.util.logging.*;

import opendial.datastructs.Assignment;

/**
 * Representation of a (possibly weighted) sample, which consists of an assignment of
 * values together with a weight (here in logarithmic form) and utility.
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Sample extends Assignment implements Comparable<Sample> {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// logarithmic weight (+- equiv. of probability)
	double logWeight = 0.0f;

	// the utility
	double utility = 0.0f;

	/**
	 * Creates a new, empty weighted sample
	 */
	public Sample() {
		super();
	}

	/**
	 * Creates a new sample
	 * 
	 * @param a the existing assignment
	 */
	public Sample(Assignment a) {
		super(a);
	}

	/**
	 * Creates a new sample with an existing weight and utility
	 * 
	 * @param a the assignment
	 * @param logWeight the logarithmic weight
	 * @param utility the utility
	 */
	public Sample(Assignment a, double logWeight, double utility) {
		super(a);
		this.logWeight = logWeight;
		this.utility = utility;
	}

	/**
	 * Adds a logarithmic weight to the current one
	 * 
	 * @param addLogWeight the weight to add
	 */
	public void addLogWeight(double addLogWeight) {
		logWeight += addLogWeight;
	}

	/**
	 * Sets the logarithmic weight to a given value
	 * 
	 * @param weight the value for the weight
	 */
	public void setWeight(double weight) {
		this.logWeight = Math.log(weight);
	}

	/**
	 * Returns the sample weight (exponentiated value, not the logarithmic one!)
	 * 
	 * @return the (exponentiated) weight for the sample
	 */
	public double getWeight() {
		return Math.exp(logWeight);
	}

	/**
	 * Adds a utility to the sample
	 * 
	 * @param newUtil the utility to add
	 */
	public void addUtility(double newUtil) {
		utility += newUtil;
	}

	/**
	 * Returns the utility of the sample
	 * 
	 * @return the utility
	 */
	public double getUtility() {
		return utility;
	}

	/**
	 * Returns a string representation of the weighted sample
	 */
	@Override
	public String toString() {
		return super.toString() + " (w=" + getWeight() + ", util=" + utility + ")";
	}

	@Override
	public int compareTo(Sample arg0) {
		return (int) ((utility - arg0.utility) * 1000);
	}

}
