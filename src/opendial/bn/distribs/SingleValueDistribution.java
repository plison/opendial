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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Representation of a distribution with a single value associated with a probability
 * of 1.0. Although this can also be represented in a categorical table, this
 * representation is much faster than operating on a full table.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class SingleValueDistribution implements IndependentDistribution {

	/** the variable label for the distribution */
	String variable;

	/** the variable value */
	final Value value;

	/**
	 * Creates a new single-value distribution
	 * 
	 * @param variable the variable label
	 * @param value the value
	 */
	public SingleValueDistribution(String variable, Value value) {
		this.variable = variable;
		this.value = value;
	}

	/**
	 * Creates a new single-value distribution
	 * 
	 * @param variable the variable label
	 * @param value the value (as a string)
	 */
	public SingleValueDistribution(String variable, String value) {
		this.variable = variable;
		this.value = ValueFactory.create(value);
	}

	/**
	 * Returns the variable label
	 */
	@Override
	public String getVariable() {
		return variable;
	}

	/**
	 * Does nothing
	 */
	@Override
	public boolean pruneValues(double threshold) {
		return false;
	}

	/**
	 * Modifies the variable label
	 * 
	 * @param oldId the old identifier to replace
	 * @param newId the new identifier
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
	}

	/**
	 * Returns 1.0 if the value is equal to the one in the distribution, and 0.0
	 * otherwise.
	 */
	@Override
	public double getProb(Value value) {
		return (value.equals(this.value)) ? 1.0 : 0.0;
	}

	/**
	 * Returns the value.
	 */
	@Override
	public Value sample() {
		return this.value;
	}

	/**
	 * Returns a singleton set with the value.
	 */
	@Override
	public Set<Value> getValues() {
		return Collections.singleton(this.value);
	}

	/**
	 * Returns a continuous representation of the distribution (if possible)
	 */
	@Override
	public ContinuousDistribution toContinuous() {
		return toDiscrete().toContinuous();
	}

	/**
	 * Returns the categorical table corresponding to the distribution.
	 */
	@Override
	public CategoricalTable toDiscrete() {
		Map<Value, Double> map = new HashMap<Value, Double>();
		map.put(value, 1.0);
		return new CategoricalTable(variable, map);
	}

	/**
	 * Returns the value
	 */
	@Override
	public Value getBest() {
		return value;
	}

	/**
	 * Returns the XML representation (as a categorical table)
	 */
	@Override
	public Node generateXML(Document document) {
		return toDiscrete().generateXML(document);
	}

	/**
	 * Copies the distribution
	 */
	@Override
	public IndependentDistribution copy() {
		return new SingleValueDistribution(variable, value);
	}

	/**
	 * Returns the distribution's hashcode
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return variable.hashCode() - value.hashCode();
	}

	/**
	 * Returns the string representation for this distribution
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return "P(" + variable + "=" + value.toString() + ")=1";
	}

	/**
	 * Returns true if the assignment is identical to the one in this distribution,
	 * otherwise false
	 * 
	 * @param o the object to compare
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof SingleValueDistribution) {
			return ((SingleValueDistribution) o).value.equals(value)
					&& ((SingleValueDistribution) o).variable.equals(variable);
		}
		return false;
	}

}
