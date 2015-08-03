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

package opendial.bn.distribs;

import java.util.logging.*;
import java.util.Collections;
import java.util.Set;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Representation of an unconditional probability distribution P(X), where X is a
 * random variable.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface IndependentDistribution extends ProbDistribution {

	public final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Returns an empty set
	 */
	@Override
	public default Set<String> getInputVariables() {
		return Collections.emptySet();
	}

	/**
	 * Returns the probability P(value), if any is specified. Else, returns 0.0f.
	 * 
	 * @param value the value for the random variable
	 * @return the associated probability, if one exists.
	 */
	public double getProb(Value value);

	/**
	 * Returns the probability P(value), if any is specified. Else, returns 0.0f.
	 * 
	 * @param value the value for the random variable (as a string)
	 * @return the associated probability, if one exists.
	 */
	public default double getProb(String value) {
		return getProb(ValueFactory.create(value));
	}

	/**
	 * Returns the probability P(value), if any is specified. Else, returns 0.0f.
	 * 
	 * @param value the value for the random variable (as a boolean)
	 * @return the associated probability, if one exists.
	 */
	public default double getProb(boolean value) {
		return getProb(ValueFactory.create(value));
	}

	/**
	 * Returns the probability P(value), if any is specified. Else, returns 0.0f.
	 * 
	 * @param value the value for the random variable (as a double)
	 * @return the associated probability, if one exists.
	 */
	public default double getProb(double value) {
		return getProb(ValueFactory.create(value));
	}

	/**
	 * Returns the probability P(value), if any is specified. Else, returns 0.0f.
	 * 
	 * @param value the value for the random variable (as a double array)
	 * @return the associated probability, if one exists.
	 */
	public default double getProb(double[] value) {
		return getProb(ValueFactory.create(value));
	}

	/**
	 * Returns a sampled value for the distribution.
	 * 
	 * @return the sampled value
	 */
	public Value sample();

	/**
	 * Returns a set of possible values for the distribution. If the distribution is
	 * continuous, assumes a discretised representation of the distribution.
	 * 
	 * @return the possible values for the distribution
	 */
	@Override
	public abstract Set<Value> getValues();

	/**
	 * Returns a continuous representation of the distribution.
	 * 
	 * @return the distribution in a continuous form be converted to a continuous
	 *         form
	 */
	public ContinuousDistribution toContinuous();

	/**
	 * Returns a discrete representation of the distribution
	 * 
	 * @return the distribution in a discrete form.
	 */
	public CategoricalTable toDiscrete();

	/**
	 * Returns the value with maximum probability (discrete case) or the mean value
	 * of the distribution (continuous case)
	 * 
	 * @return the maximum-probability value (discrete) or the mean value
	 *         (continuous)
	 */
	public Value getBest();

	/**
	 * Generates a XML node that represents the distribution.
	 * 
	 * @param document the XML node to which the node will be attached
	 * @return the corresponding XML node
	 */
	public Node generateXML(Document document);

	/**
	 * Returns a copy of the distribution.
	 * 
	 * @return the copied distribution
	 */
	@Override
	public IndependentDistribution copy();

	/**
	 * Returns itself.
	 * 
	 * @return the distribution itself.
	 */
	@Override
	public default IndependentDistribution getPosterior(Assignment condition) {
		return this;
	}

	/**
	 * Returns the probability for the head assignment, if the value can be found in
	 * the distribution. Else, returns 0.0. The conditional assignment is ignored.
	 * 
	 * @param condition the conditional assignment
	 * @param head the value for the head variable
	 * @return the resulting probability
	 */
	@Override
	public default double getProb(Assignment condition, Value head) {
		return getProb(head);
	}

	/**
	 * Returns a sample from the distribution (the condition is ignored).
	 */
	@Override
	public default Value sample(Assignment condition) {
		return sample();
	}

	/**
	 * Returns itself.
	 */
	@Override
	public default IndependentDistribution getProbDistrib(Assignment condition) {
		return this;
	}

}
