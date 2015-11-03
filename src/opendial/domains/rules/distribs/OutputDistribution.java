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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.utils.InferenceUtils;

/**
 * Representation of an output distribution (see Pierre Lison's PhD thesis, page 70
 * for details), which is a reflection of the combination of effects specified in the
 * parent rules.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class OutputDistribution implements ProbDistribution {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// output variables
	String baseVar;

	// primes attached to the variable label
	String primes;

	// incoming anchored rules
	List<AnchoredRule> inputRules;

	/**
	 * Creates the output distribution for the output variable label
	 * 
	 * @param var the variable name
	 */
	public OutputDistribution(String var) {
		this.baseVar = var.replace("'", "");
		this.primes = var.replace(baseVar, "");
		inputRules = new ArrayList<AnchoredRule>();
	}

	/**
	 * Adds an incoming anchored rule to the output distribution.
	 * 
	 * @param rule the incoming rule
	 */
	public void addAnchoredRule(AnchoredRule rule) {
		inputRules.add(rule);
	}

	/**
	 * Modifies the label of the output variable.
	 * 
	 * @param oldId the old label
	 * @param newId the new label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if ((baseVar + primes).equals(oldId)) {
			this.baseVar = newId.replace("'", "");
			this.primes = newId.replace(baseVar, "");
		}
	}

	/**
	 * Samples a particular value for the output variable.
	 * 
	 * @param condition the values of the parent (rule) nodes
	 * @return an assignment with the output value
	 */
	@Override
	public Value sample(Assignment condition) {
		IndependentDistribution result = getProbDistrib(condition);
		return result.sample();
	}

	/**
	 * Does nothing.
	 */
	@Override
	public boolean pruneValues(double threshold) {
		return false;
	}

	/**
	 * Returns the probability associated with the given conditional and head
	 * assignments.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Value head) {
		IndependentDistribution result = getProbDistrib(condition);
		return result.getProb(head);
	}

	/**
	 * Fills the cache with the resulting table for the given condition
	 * 
	 * @param condition the condition for which to fill the cache
	 */
	@Override
	public IndependentDistribution getProbDistrib(Assignment condition) {

		// creating the table
		CategoricalTable.Builder builder =
				new CategoricalTable.Builder(baseVar + primes);

		// combining all effects
		List<BasicEffect> fullEffects = new ArrayList<BasicEffect>();
		for (Value inputVal : condition.getValues()) {
			if (inputVal instanceof Effect) {
				fullEffects.addAll(((Effect) inputVal).getSubEffects());
			}
		}
		Effect fullEffect = new Effect(fullEffects);
		Map<Value, Double> values = fullEffect.getValues(baseVar);
		// case 1: add effects
		if (fullEffect.isNonExclusive(baseVar)) {
			SetVal addVal = ValueFactory.create(values.keySet());
			builder.addRow(addVal, 1.0);
		}
		// case 2 (most common): classical set operations
		else if (!values.isEmpty()) {
			double total = values.values().stream().mapToDouble(d -> d).sum();
			for (Value v : values.keySet()) {
				builder.addRow(v, values.get(v) / total);
			}
		}
		// case 3: set to none value
		else {
			builder.addRow(ValueFactory.none(), 1.0);
		}
		return builder.build();
	}

	/**
	 * Returns the probability table associated with the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the resulting probability table
	 */
	@Override
	public MarginalDistribution getPosterior(Assignment condition) {
		return new MarginalDistribution(this, condition);
	}

	/**
	 * Returns the possible outputs values given the input range in the parent nodes
	 * (probability rule nodes)
	 * 
	 * @return the possible values for the output
	 */
	@Override
	public Set<Value> getValues() {
		Set<Value> values = new HashSet<Value>();

		for (AnchoredRule rule : inputRules) {
			for (Effect e : rule.getEffects()) {
				if (e.isNonExclusive(baseVar)) {
					return getValues_linearise();
				}
				Set<Value> setValues = e.getValues(baseVar).keySet();
				values.addAll(setValues);
				if (setValues.isEmpty()) {
					values.add(ValueFactory.none());
				}
			}
		}

		if (values.isEmpty()) {
			values.add(ValueFactory.none());
		}
		return values;
	}

	/**
	 * Returns a singleton set with the label of the output
	 * 
	 * @return the singleton set with the output label
	 */
	@Override
	public String getVariable() {
		return baseVar + primes;
	}

	/**
	 * Returns the set of identifiers for all incoming rule nodes.
	 */
	@Override
	public Set<String> getInputVariables() {
		return inputRules.stream().map(r -> r.getVariable())
				.collect(Collectors.toSet());
	}

	/**
	 * Returns a copy of the distribution
	 */
	@Override
	public OutputDistribution copy() {
		OutputDistribution copy = new OutputDistribution(baseVar + primes);
		for (AnchoredRule rule : inputRules) {
			copy.addAnchoredRule(rule);
		}
		return copy;
	}

	/**
	 * Returns "(output)".
	 */
	@Override
	public String toString() {
		return "(output)";
	}

	/**
	 * Calculates the possible values for the output distribution via linearisation
	 * (more costly operation, but necessary in case of add effects).
	 * 
	 * @return the set of possible output values
	 */
	private Set<Value> getValues_linearise() {

		Map<String, Set<Value>> range = new HashMap<String, Set<Value>>();
		for (int i = 0; i < inputRules.size(); i++) {
			range.put("" + i, new HashSet<Value>(inputRules.get(i).getEffects()));
		}
		Set<Assignment> combinations = InferenceUtils.getAllCombinations(range);
		Set<Value> values = combinations.stream()
				.flatMap(cond -> getProbDistrib(cond).getValues().stream())
				.collect(Collectors.toSet());
		if (values.isEmpty()) {
			values.add(ValueFactory.none());
		}
		return values;
	}

}
