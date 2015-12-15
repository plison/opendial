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

package opendial.domains.rules.distribs;

import java.util.logging.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.templates.Template;

/**
 * Representation of an equivalence distribution (see dissertation p. 78 for details)
 * with two possible values: true or false. The distribution is essentially defined
 * as:
 * 
 * <p>
 * P(eq=true | X, X^p) = 1 when X = X^p and != None = NONE_PROB when X = None or X^p
 * = None = 0 otherwise.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class EquivalenceDistribution implements ProbDistribution {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// the variable label
	String baseVar;

	// sampler
	Random sampler;

	// probability of the equivalence variable when X or X^p have a None value.
	public static double NONE_PROB = 0.02;

	/**
	 * Create a new equivalence node for the given variable.
	 * 
	 * @param variable the variable label
	 */
	public EquivalenceDistribution(String variable) {
		this.baseVar = variable;
		sampler = new Random();
	}

	/**
	 * Does nothing
	 */
	@Override
	public boolean pruneValues(double threshold) {
		return false;
	}

	/**
	 * Copies the distribution
	 */
	@Override
	public EquivalenceDistribution copy() {
		return new EquivalenceDistribution(baseVar);
	}

	/**
	 * Returns a string representation of the distribution
	 */
	@Override
	public String toString() {
		String str = "Equivalence(" + baseVar + ", " + baseVar + "^p)";
		return str;
	}

	/**
	 * Replaces occurrences of the old variable identifier oldId with the new
	 * identifier newId.
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (baseVar.equals(oldId)) {
			baseVar = newId.replace("'", "");
		}
	}

	/**
	 * Generates a sample from the distribution given the conditional assignment.
	 */
	@Override
	public Value sample(Assignment condition) {
		double prob = getProb(condition);

		if (sampler.nextDouble() < prob) {
			return ValueFactory.create(true);
		}
		else {
			return ValueFactory.create(false);
		}
	}

	/**
	 * Returns the identifier for the equivalence distribution
	 * 
	 * @return a singleton set with the equality identifier
	 */
	@Override
	public String getVariable() {
		return "=_" + baseVar;
	}

	/**
	 * Returns the conditional variables of the equivalence distribution. (NB: not
	 * sure where this implementation works in all cases?)
	 */
	@Override
	public Set<String> getInputVariables() {
		Set<String> inputs = new HashSet<String>();
		inputs.add(baseVar + "^p");
		inputs.add(baseVar + "'");
		return inputs;
	}

	/**
	 * Returns the probability of P(head | condition).
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Value head) {

		try {
			double prob = getProb(condition);
			if (head instanceof BooleanVal) {
				boolean val = ((BooleanVal) head).getBoolean();
				if (val) {
					return prob;
				}
				else {
					return 1 - prob;
				}
			}
			log.warning("cannot extract prob for P(" + head + "|" + condition + ")");
		}
		catch (RuntimeException e) {
			log.warning(e.toString());
		}
		return 0.0;
	}

	/**
	 * Returns a new equivalence distribution with the conditional assignment as
	 * fixed input.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition) {
		return new MarginalDistribution(this, condition);
	}

	/**
	 * Returns the categorical table associated with the conditional assignment.
	 * 
	 * @param condition the conditional assignment
	 * @return the corresponding categorical table on the true and false values @ if
	 *         the table could not be extracted for the condition
	 */
	@Override
	public IndependentDistribution getProbDistrib(Assignment condition) {
		double positiveProb = getProb(condition);
		CategoricalTable.Builder builder =
				new CategoricalTable.Builder(getVariable());
		builder.addRow(true, positiveProb);
		builder.addRow(false, 1 - positiveProb);
		return builder.build();
	}

	/**
	 * Returns a set of two assignments: one with the value true, and one with the
	 * value false.
	 * 
	 * @return the set with the two possible assignments
	 */
	@Override
	public Set<Value> getValues() {
		Set<Value> vals = new HashSet<Value>();
		vals.add(ValueFactory.create(true));
		vals.add(ValueFactory.create(false));
		return vals;
	}

	/**
	 * Returns the probability of eq=true given the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the probability of eq=true
	 */
	private double getProb(Assignment condition) {

		Value predicted = null;
		Value actual = null;
		for (String inputVar : condition.getVariables()) {
			if (inputVar.equals(baseVar + "^p")) {
				predicted = condition.getValue(inputVar);
			}
			else if (inputVar.equals(baseVar + "'")) {
				actual = condition.getValue(inputVar);
			}
			else if (inputVar.equals(baseVar)) {
				actual = condition.getValue(inputVar);
			}
		}
		if (predicted == null || actual == null) {
			throw new RuntimeException("equivalence distribution with variable "
					+ baseVar + " cannot handle condition " + condition);
		}

		if (predicted.equals(ValueFactory.none())
				|| actual.equals(ValueFactory.none())) {
			return NONE_PROB;
		}
		else if (predicted.equals(actual)) {
			return 1.0;
		}
		else if (predicted instanceof StringVal && actual instanceof StringVal) {
			String str1 = ((StringVal) predicted).getString();
			String str2 = ((StringVal) actual).getString();
			if (Template.create(str1).match(str2).isMatching()
					|| Template.create(str2).match(str1).isMatching()) {
				return 1.0;
			}
			return 0.0;
		}
		else if (!predicted.getSubValues().isEmpty()
				&& !(actual.getSubValues().isEmpty())) {
			Collection<Value> vals0 = predicted.getSubValues();
			Collection<Value> vals1 = actual.getSubValues();
			Set<Value> intersect = new HashSet<Value>(vals0);
			intersect.retainAll(vals1);
			return ((double) intersect.size()) / vals0.size();
		}
		else {
			return 0.0;
		}

	}

}
