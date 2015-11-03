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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opendial.bn.distribs.densityfunctions.DiscreteDensityFunction;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.inference.approximate.Intervals;
import opendial.utils.InferenceUtils;
import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Representation of a categorical probability table P(X), where X is a random
 * variable. Constructing a categorical table should be done via the Builder:
 * <p>
 * builder = new CategoricalTable.Builder("variable name"); builder.addRow(...);
 * CategoricalTable table = builder.build();
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class CategoricalTable implements IndependentDistribution {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the variable name
	String variable;

	// the probability table
	Map<Value, Double> table;

	// probability intervals (used for binary search in sampling)
	Intervals<Value> intervals;

	// ===================================
	// TABLE CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new probability table with a mapping between head variable
	 * assignments and probability values. The construction assumes that the
	 * distribution does not have any conditional variables.
	 * 
	 * @param variable the name of the random variable
	 * @param headTable the mapping to fill the table
	 */
	protected CategoricalTable(String variable, Map<Value, Double> headTable) {
		this.variable = variable;
		this.table = headTable;
	}

	/**
	 * Concatenate the values for the two tables (assuming the two tables share the
	 * same variable).
	 * 
	 * @param other the table to concatenate
	 * @return the table resulting from the concatenation
	 */
	public IndependentDistribution concatenate(CategoricalTable other) {

		if (!variable.equals(other.getVariable())) {
			log.warning("can only concatenate tables with same variable");
		}

		CategoricalTable.Builder builder = new CategoricalTable.Builder(variable);
		for (Value thisA : new HashSet<Value>(getValues())) {
			for (Value otherA : other.getValues()) {
				try {
					Value concat = thisA.concatenate(otherA);
					builder.addRow(concat, getProb(thisA) * other.getProb(otherA));
				}
				catch (RuntimeException e) {
					log.warning("could not concatenated the tables " + this + " and "
							+ other);
					return this.copy();
				}
			}
		}
		return builder.build();
	}

	/**
	 * Modifies the distribution table by replace the old variable identifier by the
	 * new one
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
			}
			else {
				changed = true;
			}
		}

		if (changed) {
			table = InferenceUtils.normalise(newTable);
		}
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
			Value closest = table.keySet().stream()
					.filter(v -> v instanceof DoubleVal)
					.min((v1, v2) -> Double.compare(
							Math.abs(((DoubleVal) v1).getDouble() - toFind),
							Math.abs(((DoubleVal) v2).getDouble() - toFind)))
					.get();
			return getProb(closest);
		}

		else if (val instanceof ArrayVal && isContinuous()) {
			double[] toFind = ((ArrayVal) val).getArray();
			Value closest =
					table.keySet().stream().filter(v -> v instanceof ArrayVal)
							.min((v1, v2) -> Double.compare(
									MathUtils.getDistance(((ArrayVal) v1).getArray(),
											toFind),
							MathUtils.getDistance(((ArrayVal) v2).getArray(),
									toFind)))
							.get();
			return getProb(closest);
		}
		return 0.0f;
	}

	/**
	 * returns true if the table contains a probability for the given assignment
	 * 
	 * @param head the assignment
	 * @return true if the table contains a row for the assignment, false otherwise
	 */
	public boolean hasProb(Value head) {
		return table.containsKey(head);
	}

	/**
	 * Sample a value from the distribution. If no assignment can be sampled (due to
	 * e.g. an ill-formed distribution), returns a none value.
	 * 
	 * @return the sampled assignment
	 */
	@Override
	public Value sample() {
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

		Value sample = intervals.sample();
		return sample;
	}

	/**
	 * Returns the continuous probability distribution equivalent to the current
	 * table
	 * 
	 * @return the continuous equivalent for the distribution could not be converted
	 */
	@Override
	public ContinuousDistribution toContinuous() {

		if (isContinuous()) {
			Map<double[], Double> points = new HashMap<double[], Double>();
			for (Value v : getValues()) {
				if (v instanceof ArrayVal) {
					points.put(((ArrayVal) v).getArray(), getProb(v));
				}
				else if (v instanceof DoubleVal) {
					points.put(new double[] { ((DoubleVal) v).getDouble() },
							getProb(v));
				}
			}
			DiscreteDensityFunction fun = new DiscreteDensityFunction(points);
			return new ContinuousDistribution(variable, fun);
		}

		throw new RuntimeException("Distribution could not be converted to a "
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
	 * Returns true if the table is empty (or contains only a default assignment),
	 * false otherwise
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		if (table.isEmpty()) {
			return true;
		}
		else
			return (table.size() == 1
					&& table.keySet().iterator().next().equals(ValueFactory.none()));
	}

	/**
	 * Returns a subset of the N values in the table with the highest probability.
	 * 
	 * @param nbest the number of values to select
	 * @return the distribution with the subset of values
	 */
	public CategoricalTable getNBest(int nbest) {
		Map<Value, Double> ntable = InferenceUtils.getNBest(table, nbest);
		Builder builder = new Builder(variable);
		for (Value v : ntable.keySet()) {
			builder.addRow(v, ntable.get(v));
		}
		return builder.build().toDiscrete();
	}

	/**
	 * Returns the most likely assignment of values in the table. If none could be
	 * found, returns an empty assignment.
	 * 
	 * @return the assignment with highest probability
	 */
	@Override
	public Value getBest() {
		if (table.size() > 0) {
			double maxprob = -10;
			Value maxVal = ValueFactory.none();
			for (Value v : table.keySet()) {
				double prob = table.get(v);
				if (prob > maxprob) {
					maxprob = prob;
					maxVal = v;
				}
			}
			return maxVal;
		}
		else {
			log.warning("table is empty, cannot extract best value");
			return ValueFactory.none();
		}
	}

	/**
	 * Returns the size of the table
	 * 
	 * @return the size of the table
	 */
	public int size() {
		return table.size();
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
	 * Returns a string representation of the probability table
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {

		Map<Value, Double> sortedTable =
				InferenceUtils.getNBest(table, Math.max(table.size(), 1));

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
				if (Math.abs(
						((CategoricalTable) o).getProb(v) - getProb(v)) > 0.01) {
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
		Map<Value, Double> newTable = new HashMap<Value, Double>();
		for (Value v : table.keySet()) {
			newTable.put(v, table.get(v));
		}
		return new CategoricalTable(variable, newTable);
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
			if (!v.equals(ValueFactory.none())) {
				Element valueNode = doc.createElement("value");
				if (table.get(v) < 0.99) {
					Attr prob = doc.createAttribute("prob");
					prob.setValue("" + StringUtils.getShortForm(table.get(v)));
					valueNode.setAttributeNode(prob);
				}
				valueNode.setTextContent("" + v);
				var.appendChild(valueNode);
			}
		}
		return var;
	}

	/**
	 * Returns the table of values with their probability.
	 * 
	 * @return the table
	 */
	public Map<Value, Double> getTable() {
		return table;
	}

	// ===================================
	// PRIVATE METHODS
	// ===================================

	/**
	 * Returns true if the table can be converted to a continuous distribution, and
	 * false otherwise.
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

	// ===================================
	// BUILDER CLASS
	// ===================================

	/**
	 * Builder class used to construct a categorical table row-by-row. When all raws
	 * have been added, one can simply call build() to generate the corresponding
	 * distribution. When the probabilities do not sum up to 1, a default value
	 * "None" is added to cover the remaining probability mass.
	 * 
	 *
	 */
	public static class Builder {

		// the variable name
		String variable;

		// the probability table
		Map<Value, Double> table;

		// ===================================
		// TABLE CONSTRUCTION
		// ===================================

		/**
		 * Constructs a new probability table, with no values
		 * 
		 * @param variable the name of the random variable
		 */
		public Builder(String variable) {
			table = new HashMap<Value, Double>(5);
			this.variable = variable;
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
			addRow(head, table.getOrDefault(head, 0.0) + prob);
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
		}

		public void normalise() {
			table = InferenceUtils.normalise(table);
		}

		/**
		 * Builds the categorical table based on the provided rows. If the total
		 * probability mass is less than 1.0, adds a default value None. If the total
		 * mass is higher than 1.0, normalise the table. Finally, if one single value
		 * is present, creates a SingleValueDistribution instead.
		 * 
		 * @return the distribution (CategoricalTable or SingleValueDistribution).
		 */
		public IndependentDistribution build() {
			double totalProb = table.values().stream().mapToDouble(d -> d).sum();
			if (totalProb < 0.99) {
				incrementRow(ValueFactory.none(), 1.0 - totalProb);
			}
			else if (totalProb > 1.01) {
				table = InferenceUtils.normalise(table);
			}
			if (table.size() == 1) {
				Value singleValue = table.keySet().iterator().next();
				return new SingleValueDistribution(variable, singleValue);
			}
			else {
				return new CategoricalTable(variable, table);
			}
		}

		/**
		 * Returns true if the probability table is well-formed. The method checks
		 * that all possible assignments for the condition and head parts are covered
		 * in the table, and that the probabilities add up to 1.0f.
		 * 
		 * @return true if the table is well-formed, false otherwise
		 */
		public boolean isWellFormed() {
			// checks that the total probability is roughly equal to 1.0f
			double totalProb =
					table.keySet().stream().mapToDouble(v -> table.get(v)).sum();
			if (totalProb < 0.9f || totalProb > 1.1f) {
				log.fine("total probability is " + totalProb);
				return false;
			}

			return true;
		}

		/**
		 * Returns whether the current table is empty or not
		 * 
		 * @return true if empty, false otherwise
		 */
		public boolean isEmpty() {
			return table.isEmpty();
		}

		/**
		 * Returns the total probability in the table
		 * 
		 * @return the total probability
		 */
		public double getTotalProb() {
			return table.values().stream().mapToDouble(d -> d).sum();
		}

		/**
		 * Returns the values included so far in the builder.
		 * 
		 * @return the values
		 */
		public List<Value> getValues() {
			return new ArrayList<Value>(table.keySet());
		}

		/**
		 * Clears the builder.
		 */
		public void clear() {
			table.clear();
		}
	}

}
