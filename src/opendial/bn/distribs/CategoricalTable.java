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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.densityfunctions.DiscreteDensityFunction;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Intervals;
import opendial.utils.InferenceUtils;
import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Representation of a categorical probability table P(X), where X is a random
 * variable.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class CategoricalTable implements IndependentProbDistribution {

	// logger
	public static Logger log = new Logger("CategoricalTable",
			Logger.Level.DEBUG);

	// the variable name
	String variable;

	// the probability table
	Map<Value, Double> table;

	// probability intervals (used for binary search in sampling)
	Intervals<Value> intervals;

	// sampler
	static Random sampler = new Random();

	// whether to automatically add a default value to fill the remaining
	// probability mass
	boolean addDefaultValue = true;

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new probability table, with no values
	 * 
	 * @param variable the name of the random variable
	 */
	public CategoricalTable(String variable) {
		table = new HashMap<Value, Double>(5);
		table = Collections.synchronizedMap(table);
		this.variable = variable;
	}

	/**
	 * Constructs a new probability table, with no values
	 * 
	 * @param variable the name of the random variable
	 * @param addDefaultValue whether to automatically add a default value to
	 *            fill the remaining probability mass
	 */
	public CategoricalTable(String variable, boolean addDefaultValue) {
		this(variable);
		this.addDefaultValue = addDefaultValue;
	}

	/**
	 * Constructs a new probability table with a mapping between head variable
	 * assignments and probability values. The construction assumes that the
	 * distribution does not have any conditional variables.
	 * 
	 * @param variable the name of the random variable
	 * @param headTable the mapping to fill the table
	 */
	public CategoricalTable(String variable, Map<Value, Double> headTable) {
		this(variable);
		double totalProb = 0.0;
		for (Value a : headTable.keySet()) {
			addRow(a, headTable.get(a));
			totalProb += headTable.get(a);
		}
		if (addDefaultValue && totalProb < 0.99999) {
			incrementRow(ValueFactory.none(), 1.0 - totalProb);
		}
	}

	/**
	 * Create a categorical table with a unique value with probability 1.0.
	 * 
	 * @param variable the name of the random variable
	 * @param uniqueValue the unique value for the table
	 */
	public CategoricalTable(String variable, Value uniqueValue) {
		this(variable);
		addRow(uniqueValue, 1.0);
	}

	/**
	 * Create a categorical table with a unique value with probability 1.0.
	 * 
	 * @param variable the name of the random variable
	 * @param uniqueValue the unique value for the table (as a string)
	 */
	public CategoricalTable(String variable, String uniqueValue) {
		this(variable);
		addRow(uniqueValue, 1.0);
	}

	/**
	 * Adds a new row to the probability table. If the table already contains a
	 * probability, it is erased.
	 * 
	 * @param value the value to add
	 * @param prob the associated probability
	 */
	public void addRow(Value value, double prob) {

		if (prob < 0.0f || prob > 1.02f) {
			return;
		}

		table.put(value, prob);

		if (addDefaultValue) {
			double totalProb = countTotalProb();
			if (totalProb < 0.98) {
				table.put(ValueFactory.none(), 1.0 - totalProb);
			} else {
				table.remove(ValueFactory.none());
			}
		}
		intervals = null;
	}

	/**
	 * Adds a new row to the probability table. If the table already contains a
	 * probability, it is erased.
	 * 
	 * @param value the value to add (as a string)
	 * @param prob the associated probability
	 */
	public void addRow(String value, double prob) {
		addRow(ValueFactory.create(value), prob);
	}

	/**
	 * Adds a new row to the probability table. If the table already contains a
	 * probability, it is erased.
	 * 
	 * @param value the value to add (as a double)
	 * @param prob the associated probability
	 */
	public void addRow(double value, double prob) {
		addRow(ValueFactory.create(value), prob);
	}

	/**
	 * Adds a new row to the probability table. If the table already contains a
	 * probability, it is erased.
	 * 
	 * @param value the value to add (as a boolean)
	 * @param prob the associated probability
	 */
	public void addRow(boolean value, double prob) {
		addRow(ValueFactory.create(value), prob);
	}

	/**
	 * Adds a new row to the probability table. If the table already contains a
	 * probability, it is erased.
	 * 
	 * @param value the value to add (as a double array)
	 * @param prob the associated probability
	 */
	public void addRow(double[] value, double prob) {
		addRow(ValueFactory.create(value), prob);
	}

	/**
	 * Increments the probability specified in the table for the given head
	 * assignment. If none exists, simply assign the probability.
	 * 
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Value head, double prob) {
		if (table.containsKey(head)) {
			if (head.equals(ValueFactory.none())) {
				return;
			}
			addRow(head, table.get(head) + prob);
		} else {
			addRow(head, prob);
		}
	}

	/**
	 * Add a new set of rows to the probability table.
	 * 
	 * @param heads the mappings (head assignment, probability value)
	 */
	public void addRows(Map<Value, Double> heads) {
		for (Value head : heads.keySet()) {
			addRow(head, heads.get(head));
		}
	}

	/**
	 * Removes a row from the table.
	 * 
	 * @param head head assignment
	 */
	public void removeRow(Value head) {

		table.remove(head);

		double totalProb = countTotalProb();
		if (addDefaultValue && totalProb < 0.99999
				&& head != ValueFactory.none()) {
			table.put(ValueFactory.none(), 1.0 - totalProb);
		}
		intervals = null;
	}

	/**
	 * Concatenate the values for the two tables (assuming the two tables share
	 * the same variable).
	 * 
	 * @param other the table to concatenate
	 * @return the table resulting from the concatenation
	 */
	public CategoricalTable concatenate(CategoricalTable other) {

		if (!variable.equals(other.getVariable())) {
			log.warning("can only concatenate tables with same variable");
		}

		CategoricalTable newtable = new CategoricalTable(variable,
				addDefaultValue);
		for (Value thisA : new HashSet<Value>(getValues())) {
			for (Value otherA : other.getValues()) {
				try {
					Value concat = thisA.concatenate(otherA);
					newtable.addRow(concat,
							getProb(thisA) * other.getProb(otherA));
				} catch (DialException e) {
					log.warning("could not concatenated the tables " + this
							+ " and " + other);
					return this.copy();
				}
			}
		}
		return newtable;
	}

	/**
	 * Modifies the distribution table by replace the old variable identifier by
	 * the new one
	 * 
	 * @param oldVarId the old identifier
	 * @param newVarId the new identifier
	 */
	@Override
	public void modifyVariableId(String oldVarId, String newVarId) {
		if (this.variable.equals(oldVarId)) {
			this.variable = newVarId;
		}
	}

	/**
	 * Prunes all table values that have a probability lower than the threshold.
	 * 
	 * @param threshold the threshold
	 * @return true if at least one value has been pruned, false otherwise
	 */
	@Override
	public boolean pruneValues(double threshold) {
		Map<Value, Double> newTable = new HashMap<Value, Double>();
		boolean changed = false;
		for (Value row : table.keySet()) {
			double prob = table.get(row);
			if (prob >= threshold) {
				newTable.put(row, prob);
			} else {
				changed = true;
			}
		}
		table = InferenceUtils.normalise(newTable);
		intervals = null;
		return changed;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the probability P(val).
	 * 
	 * @param val the value
	 * @return the associated probability, if one exists.
	 */
	@Override
	public double getProb(Value val) {

		if (table.containsKey(val)) {
			return table.get(val);
		}

		// if the distribution has continuous values, search for the closest
		// element
		else if (val instanceof DoubleVal && isContinuous()) {
			double toFind = ((DoubleVal) val).getDouble();
			Value closest = table
					.keySet()
					.stream()
					.filter(v -> v instanceof DoubleVal)
					.min((v1, v2) -> Double.compare(
							Math.abs(((DoubleVal) v1).getDouble() - toFind),
							Math.abs(((DoubleVal) v2).getDouble() - toFind)))
					.get();
			return getProb(closest);
		}

		else if (val instanceof ArrayVal && isContinuous()) {
			double[] toFind = ((ArrayVal) val).getArray();
			Value closest = table
					.keySet()
					.stream()
					.filter(v -> v instanceof ArrayVal)
					.min((v1, v2) -> Double.compare(MathUtils.getDistance(
							((ArrayVal) v1).getArray(), toFind), MathUtils
							.getDistance(((ArrayVal) v2).getArray(), toFind)))
					.get();
			return getProb(closest);
		}
		return 0.0f;
	}

	/**
	 * returns true if the table contains a probability for the given assignment
	 * 
	 * @param head the assignment
	 * @return true if the table contains a row for the assignment, false
	 *         otherwise
	 */
	public boolean hasProb(Value head) {
		return table.containsKey(head);
	}

	/**
	 * Sample a value from the distribution. If no assignment can be sampled
	 * (due to e.g. an ill-formed distribution), returns a none value.
	 * 
	 * @return the sampled assignment
	 * @throws DialException if no assignment could be sampled
	 */
	@Override
	public Value sample() throws DialException {

		if (intervals == null) {
			if (table.isEmpty()) {
				log.warning("creating intervals for an empty table");
			}
			intervals = new Intervals<Value>(table);
		}
		if (intervals.isEmpty()) {
			log.warning("interval is empty, table: " + table);
			return ValueFactory.none();
		}

		return intervals.sample();
	}

	/**
	 * Returns the continuous probability distribution equivalent to the current
	 * table
	 * 
	 * @return the continuous equivalent for the distribution
	 * @throws DialException if the distribution could not be converted
	 */
	@Override
	public ContinuousDistribution toContinuous() throws DialException {

		if (isContinuous()) {
			Map<double[], Double> points = new HashMap<double[], Double>();
			for (Value v : getValues()) {
				if (v instanceof ArrayVal) {
					points.put(((ArrayVal) v).getArray(), getProb(v));
				} else if (v instanceof DoubleVal) {
					points.put(new double[] { ((DoubleVal) v).getDouble() },
							getProb(v));
				}
			}
			DiscreteDensityFunction fun = new DiscreteDensityFunction(points);
			return new ContinuousDistribution(variable, fun);
		}

		throw new DialException("Distribution could not be converted to a "
				+ "continuous distribution: " + variable);
	}

	/**
	 * Returns itself.
	 */
	@Override
	public CategoricalTable toDiscrete() {
		return this;
	}

	/**
	 * Returns the set of variable labels used in the table
	 * 
	 * @return the variable labels in the table
	 */
	@Override
	public String getVariable() {
		return this.variable;
	}

	/**
	 * Returns true if the table is empty (or contains only a default
	 * assignment), false otherwise
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		if (table.isEmpty()) {
			return true;
		} else
			return (table.size() == 1 && table.keySet().stream()
					.anyMatch(v -> v.equals(ValueFactory.none())));
	}

	/**
	 * Returns a subset of the N values in the table with the highest
	 * probability.
	 * 
	 * @param nbest the number of values to select
	 * @return the distribution with the subset of values
	 */
	public CategoricalTable getNBest(int nbest) {

		Map<Value, Double> filteredTable = InferenceUtils
				.getNBest(table, nbest);
		return new CategoricalTable(variable, filteredTable);
	}

	/**
	 * Returns the most likely assignment of values in the table. If none could
	 * be found, returns an empty assignment.
	 * 
	 * @return the assignment with highest probability
	 */
	@Override
	public Value getBest() {
		if (table.size() > 0) {
			CategoricalTable nbest = getNBest(1);
			if (nbest.getValues().size() > 1) {
				nbest.removeRow(ValueFactory.none());
			}
			return nbest.getValues().iterator().next();
		} else {
			log.warning("table is empty, cannot extract best value");
			return ValueFactory.none();
		}
	}

	/**
	 * Returns the rows of the table.
	 * 
	 * @return the table rows
	 */
	@Override
	public Set<Value> getValues() {
		return table.keySet();
	}

	// ===================================
	// UTILITIES
	// ===================================

	/**
	 * Returns the hashcode for the table.
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}

	/**
	 * Returns true if the probability table is well-formed. The method checks
	 * that all possible assignments for the condition and head parts are
	 * covered in the table, and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		// checks that the total probability is roughly equal to 1.0f
		double totalProb = countTotalProb() + getProb(ValueFactory.none());
		if (totalProb < 0.9f || totalProb > 1.1f) {
			log.debug("total probability is " + totalProb);
			return false;
		}

		return true;
	}

	/**
	 * Returns a string representation of the probability table
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {

		Map<Value, Double> sortedTable = InferenceUtils.getNBest(table,
				Math.max(table.size(), 1));

		String str = "";
		for (Entry<Value, Double> entry : sortedTable.entrySet()) {
			String prob = StringUtils.getShortForm(entry.getValue());
			str += "P(" + variable + "=" + entry.getKey() + "):=" + prob + "\n";
		}

		return (str.length() > 0) ? str.substring(0, str.length() - 1) : str;
	}

	/**
	 * Returns true if the object o is a categorical table with the same content
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof CategoricalTable) {
			Set<Value> otherVals = ((CategoricalTable) o).getValues();
			if (!getValues().equals(otherVals)) {
				return false;
			}
			for (Value v : getValues()) {
				if (Math.abs(((CategoricalTable) o).getProb(v) - getProb(v)) > 0.01) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public CategoricalTable copy() {
		CategoricalTable tableCopy = new CategoricalTable(variable);
		for (Value head : table.keySet()) {
			tableCopy.addRow(head.copy(), table.get(head));
		}
		tableCopy.intervals = intervals;
		tableCopy.addDefaultValue = addDefaultValue;
		return tableCopy;
	}

	/**
	 * Generates the XML representation for the table, for the document doc.
	 * 
	 * @param doc the XML document for which to generate the XML.
	 */
	@Override
	public Node generateXML(Document doc) {

		Element var = doc.createElement("variable");

		Attr id = doc.createAttribute("id");
		id.setValue(variable.replace("'", ""));
		var.setAttributeNode(id);

		for (Value v : InferenceUtils.getNBest(table, table.size()).keySet()) {
			Element valueNode = doc.createElement("value");
			if (table.get(v) < 0.99) {
				Attr prob = doc.createAttribute("prob");
				prob.setValue("" + StringUtils.getShortForm(table.get(v)));
				valueNode.setAttributeNode(prob);
			}
			valueNode.setTextContent("" + v);
			var.appendChild(valueNode);
		}
		return var;
	}

	// ===================================
	// PRIVATE METHODS
	// ===================================

	/**
	 * Returns the total accumulated probability for the distribution
	 * 
	 * @return the total probability
	 */
	private double countTotalProb() {
		return table.keySet().stream()
				.filter(v -> !v.equals(ValueFactory.none()))
				.mapToDouble(v -> table.get(v)).sum();
	}

	/**
	 * Returns true if the table can be converted to a continuous distribution,
	 * and false otherwise.
	 * 
	 * @return true if convertible to continuous, false otherwise.
	 */
	private boolean isContinuous() {
		if (!table.keySet().isEmpty()) {
			for (Value v : getValues()) {
				if (!(v instanceof ArrayVal) && !(v instanceof DoubleVal)
						&& !(v instanceof NoneVal)) {
					return false;
				}
			}
			if (getValues().size() > 1) {
				return true;
			}
		}
		return false;
	}

}
