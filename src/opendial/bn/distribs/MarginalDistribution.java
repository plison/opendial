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
import java.util.HashSet;
import java.util.Set;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a probability distribution P(X|Y1,...Yn) by way of two
 * distributions:
 * <ul>
 * <li>a distribution P(X|Y1,...Yn, Z1,...Zm)
 * <li>a distribution P(Z1,...Zm)
 * </ul>
 * 
 * The probability P(X|Y1,...Yn) can be straightforwardly calculated by marginalising
 * out the variables Z1,...Zm.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class MarginalDistribution implements ProbDistribution {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// the conditional distribution P(X|Y1,...Yn, Z1,...Zm)
	ProbDistribution condDistrib;

	// the unconditional distribution P(Z1,...Zm)
	MultivariateDistribution uncondDistrib;

	/**
	 * Creates a new marginal distribution given the two component distributions.
	 * 
	 * @param condDistrib the distribution P(X|Y1,...Yn, Z1,...Zm)
	 * @param uncondDistrib the distributionP(Z1,...Zm)
	 */
	public MarginalDistribution(ProbDistribution condDistrib,
			MultivariateDistribution uncondDistrib) {
		this.condDistrib = condDistrib;
		this.uncondDistrib = uncondDistrib;
	}

	/**
	 * Creates a new marginal distribution given the first distributions and the
	 * value assignment (corresponding to a distribution P(Z1,...Zm) where the
	 * provided assignment has a probability 1.0).
	 * 
	 * @param condDistrib the distribution P(X|Y1,...Yn, Z1,...Zm)
	 * @param assign the assignment of values for Z1,...Zm
	 */
	public MarginalDistribution(ProbDistribution condDistrib, Assignment assign) {
		this.condDistrib = condDistrib;
		this.uncondDistrib = new MultivariateTable(assign);
	}

	/**
	 * Creates a new marginal distribution given the first distribution and the
	 * categorical table P(Z).
	 * 
	 * @param condDistrib the distribution P(X|Y1,...Yn, Z)
	 * @param uncondDistrib the distribution P(Z).
	 */
	public MarginalDistribution(ProbDistribution condDistrib,
			CategoricalTable uncondDistrib) {
		this.condDistrib = condDistrib;
		this.uncondDistrib = new MultivariateTable(uncondDistrib);
	}

	/**
	 * Returns the variable X.
	 */
	@Override
	public String getVariable() {
		return condDistrib.getVariable();
	}

	/**
	 * Returns the conditional variables Y1,...Yn for the distribution.
	 * 
	 * @return the set of conditional variables
	 */
	@Override
	public Set<String> getInputVariables() {
		Set<String> inputs = new HashSet<String>(condDistrib.getInputVariables());
		inputs.removeAll(uncondDistrib.getVariables());
		return inputs;
	}

	/**
	 * Returns the conditional distribution P(X|Y1,...Yn,Z1,...Zm)
	 * 
	 * @return the conditional distribution
	 */
	public ProbDistribution getConditionalDistrib() {
		return condDistrib;
	}

	/**
	 * Returns the probability P(X=head|condition)
	 * 
	 * @param condition the conditional assignment for Y1,...Yn
	 * @param head the value for the random variable X
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Value head) {
		double totalProb = 0.0;
		for (Assignment assign : uncondDistrib.getValues()) {
			Assignment augmentedCond = new Assignment(condition, assign);
			totalProb += condDistrib.getProb(augmentedCond, head);
		}
		return totalProb;
	}

	/**
	 * Returns a sample value for the variable X given the conditional assignment
	 * 
	 * @param condition the conditional assignment for Y1,...Yn
	 * @return the sampled value for X
	 */
	@Override
	public Value sample(Assignment condition) {
		Assignment augmentedCond = new Assignment(condition, uncondDistrib.sample());
		return condDistrib.sample(augmentedCond);
	}

	/**
	 * Returns the categorical table P(X) given the conditional assignment for the
	 * variables Y1,...Yn.
	 * 
	 * @param condition the conditional assignment for Y1,...Yn
	 * @return the categorical table for the random variable X
	 */
	@Override
	public IndependentDistribution getProbDistrib(Assignment condition) {
		CategoricalTable.Builder result =
				new CategoricalTable.Builder(condDistrib.getVariable());
		for (Assignment assign : uncondDistrib.getValues()) {
			double assignProb = uncondDistrib.getProb(assign);
			Assignment augmentedCond = new Assignment(condition, assign);
			CategoricalTable subtable =
					condDistrib.getProbDistrib(augmentedCond).toDiscrete();
			for (Value value : subtable.getValues()) {
				result.incrementRow(value, assignProb * subtable.getProb(value));
			}
		}
		return result.build();
	}

	/**
	 * Returns the possible values for X.
	 * 
	 * @return the set of possible values
	 */
	@Override
	public Set<Value> getValues() {
		return condDistrib.getValues();
	}

	/**
	 * Returns the posterior distribution given the conditional assignment.
	 * 
	 * @param condition a conditional assignment on a subset of variables from
	 *            Y1,...Yn
	 * @return the resulting posterior distribution.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition) {
		MultivariateTable extended = uncondDistrib.toDiscrete();
		extended.extendRows(condition);
		return new MarginalDistribution(condDistrib, extended);
	}

	/**
	 * Prune the values below a certain threshold.
	 * 
	 * @param threshold the threshold to apply
	 */
	@Override
	public synchronized boolean pruneValues(double threshold) {
		boolean changed = condDistrib.pruneValues(threshold);
		boolean changed2 = uncondDistrib.pruneValues(threshold);
		return changed || changed2;
	}

	/**
	 * Copies the marginal distribution.
	 */
	@Override
	public MarginalDistribution copy() {
		return new MarginalDistribution(condDistrib.copy(), uncondDistrib.copy());
	}

	/**
	 * Modifies the variable identifier in the two distributions.
	 */
	@Override
	public synchronized void modifyVariableId(String oldId, String newId) {
		condDistrib.modifyVariableId(oldId, newId);
		uncondDistrib.modifyVariableId(oldId, newId);
	}

	/**
	 * Returns a text representation of the marginal distribution.
	 */
	@Override
	public String toString() {
		return "Marginal distribution with " + condDistrib.toString() + " and "
				+ uncondDistrib.toString();
	}

}
