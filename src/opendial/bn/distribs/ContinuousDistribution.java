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
import java.util.Map;
import java.util.Set;

import opendial.Settings;
import opendial.bn.distribs.densityfunctions.DensityFunction;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Representation of a continuous probability distribution, defined by an arbitrary
 * density function over a single (univariate or multivariate) variable. The
 * distribution does not take any conditional assignment.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ContinuousDistribution implements IndependentDistribution {

	public final static Logger log = Logger.getLogger("OpenDial");

	// the variable for the distribution
	String variable;

	// density function for the distribution
	DensityFunction function;

	// discrete equivalent of the distribution
	CategoricalTable discreteCache;

	// ===================================
	// DISTRIBUTION CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new distribution with a variable and a density function
	 * 
	 * @param variable the variable
	 * @param function the density function
	 */
	public ContinuousDistribution(String variable, DensityFunction function) {
		this.variable = variable;
		this.function = function;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public boolean pruneValues(double frequencyThreshold) {
		return false;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Samples from the distribution.
	 * 
	 * @return the sampled (variable, value) pair
	 */
	@Override
	public Value sample() {
		Value v = (function.getDimensions() > 1)
				? ValueFactory.create(function.sample())
				: ValueFactory.create(function.sample()[0]);
		return v;
	}

	/**
	 * Returns the probability of the particular value, based on a discretised
	 * representation of the continuous distribution.
	 * 
	 * @return the probability value for the discretised table.
	 * 
	 */
	@Override
	public double getProb(Value value) {
		return toDiscrete().getProb(value);
	}

	/**
	 * Returns the mean value of the distribution
	 */
	@Override
	public Value getBest() {
		return ValueFactory.create(function.getMean());
	}

	/**
	 * Returns a discretised version of the distribution. The number of
	 * discretisation buckets is defined in the configuration settings
	 * 
	 * @return the discretised version of the distribution
	 */
	@Override
	public CategoricalTable toDiscrete() {

		if (discreteCache == null) {
			Map<double[], Double> discretisation =
					function.discretise(Settings.discretisationBuckets);
			CategoricalTable.Builder builder =
					new CategoricalTable.Builder(variable);
			for (double[] value : discretisation.keySet()) {
				Value val = (value.length > 1) ? new ArrayVal(value)
						: ValueFactory.create(value[0]);
				builder.addRow(val, discretisation.get(value));
			}
			discreteCache = builder.build().toDiscrete();
		}
		return discreteCache;
	}

	/**
	 * Returns itself.
	 */
	@Override
	public ContinuousDistribution toContinuous() {
		return this;
	}

	/**
	 * Returns the probability density for the given value
	 * 
	 * @param val the value (must be a DoubleVal or ArrayVal)
	 * @return the resulting density
	 */
	public double getProbDensity(Value val) {
		if (val instanceof ArrayVal) {
			return function.getDensity(((ArrayVal) val).getArray());
		}
		if (val instanceof DoubleVal) {
			return function.getDensity(((DoubleVal) val).getDouble());
		}
		return 0.0;
	}

	/**
	 * Returns the probability density for the given value
	 * 
	 * @param val (as a Double array)
	 * @return the resulting density
	 */
	public double getProbDensity(double val) {
		return function.getDensity(val);
	}

	/**
	 * Returns the probability density for the given value
	 * 
	 * @param val (as a Double array)
	 * @return the resulting density
	 */
	public double getProbDensity(double[] val) {
		return function.getDensity(val);
	}

	/**
	 * Returns the density function
	 * 
	 * @return the density function
	 */
	public DensityFunction getFunction() {
		return function;
	}

	/**
	 * Returns the variable label
	 * 
	 * @return the variable label
	 */
	@Override
	public String getVariable() {
		return variable;
	}

	/**
	 * Returns the cumulative probability from 0 up to a given point provided in the
	 * argument.
	 * 
	 * @param val the value up to which the cumulative probability must be estimated.
	 * @return the cumulative probability
	 */
	public double getCumulativeProb(Value val) {
		try {
			if (val instanceof ArrayVal) {
				return function.getCDF(((ArrayVal) val).getArray());
			}
			else if (val instanceof DoubleVal) {
				return function
						.getCDF(new double[] { ((DoubleVal) val).getDouble() });
			}
		}
		catch (RuntimeException e) {
			log.warning("exception: " + e);
		}
		return 0.0;
	}

	/**
	 * Returns the cumulative probability from 0 up to a given point provided in the
	 * argument.
	 * 
	 * @param val value up to which the cumulative probability must be estimated (as
	 *            a double)
	 * @return the cumulative probability
	 */
	public double getCumulativeProb(double val) {
		try {
			return function.getCDF(val);
		}
		catch (RuntimeException e) {
			log.warning("exception: " + e);
		}
		return 0.0;
	}

	/**
	 * Returns the cumulative probability from 0 up to a given point provided in the
	 * argument.
	 * 
	 * @param val value up to which the cumulative probability must be estimated (as
	 *            an array of Doubles)
	 * @return the cumulative probability
	 */
	public double getCumulativeProb(double[] val) {
		try {
			return function.getCDF(val);
		}
		catch (RuntimeException e) {
			log.warning("exception: " + e);
		}
		return 0.0;
	}

	/**
	 * Discretises the distribution and returns a set of possible values for it.
	 * 
	 * @return the set of discretised values for the variable
	 */
	@Override
	public Set<Value> getValues() {
		return toDiscrete().getValues();
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns a copy of the probability distribution
	 * 
	 * @return the copy
	 */
	@Override
	public ContinuousDistribution copy() {
		return new ContinuousDistribution(variable, function.copy());
	}

	/**
	 * Returns a pretty print of the distribution
	 *
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		return "PDF(" + variable + ")=" + function.toString();
	}

	/**
	 * Modifies the variable label
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
		if (discreteCache != null) {
			discreteCache.modifyVariableId(oldId, newId);
		}
	}

	/**
	 * Returns the XML representation of the distribution
	 * 
	 * @param doc the document to which the XML node belongs
	 * @return the corresponding node generated.
	 */
	@Override
	public Node generateXML(Document doc) {

		Element var = doc.createElement("variable");

		Attr id = doc.createAttribute("id");
		id.setValue(variable);
		var.setAttributeNode(id);
		for (Node node : function.generateXML(doc)) {
			var.appendChild(node);
		}

		return var;
	}

}
