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

import java.util.Set;

import opendial.datastructs.Assignment;

/**
 * Representation of a multivariate probability distribution P(X1,...Xn), where
 * X1,...Xn are random variables.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface MultivariateDistribution {

	/**
	 * Returns the names of the random variables in the distribution
	 * 
	 * @return the set of variable names.
	 */
	public Set<String> getVariables();

	/**
	 * Returns the set of possible assignments for the random variables.
	 * 
	 * @return the set of possible assignment
	 */
	public Set<Assignment> getValues();

	/**
	 * Returns the probability of a particular assignment of values.
	 * 
	 * @param values the assignment of values to X1,...Xn.
	 * @return the corresponding probability
	 */
	public double getProb(Assignment values);

	/**
	 * Returns a sample assignment for X1,...Xn.
	 * 
	 * @return the sampled assignment
	 */
	public Assignment sample();

	/**
	 * Returns the marginal probability distribution P(Xi) for a random variable Xi
	 * in X1,...Xn.
	 * 
	 * @param variable the random variable Xi
	 * @return the marginal distribution P(Xi)
	 */
	public IndependentDistribution getMarginal(String variable);

	/**
	 * Modifies the variable identifier in the distribution
	 * 
	 * @param oldId the old identifier
	 * @param newId the new identifier
	 */
	public void modifyVariableId(String oldId, String newId);

	/**
	 * Returns a representation of the distribution as a multivariate table.
	 * 
	 * @return the multivariate table.
	 */
	public MultivariateTable toDiscrete();

	/**
	 * Returns a copy of the distribution.
	 * 
	 * @return the copy.
	 */
	public MultivariateDistribution copy();

	/**
	 * Prunes all values assignment whose probability falls below the threshold.
	 * 
	 * @param threshold the threshold to apply
	 * @return true if at least one value has been removed, false otherwise
	 */
	public boolean pruneValues(double threshold);

	/**
	 * Returns the value with maximum probability.
	 * 
	 * @return the value with maximum probability
	 */
	public Assignment getBest();

}
