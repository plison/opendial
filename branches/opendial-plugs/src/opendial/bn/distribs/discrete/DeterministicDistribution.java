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

package opendial.bn.distribs.discrete;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.functions.DeterministicFunction;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;


/**
 * A deterministic distribution derived from the specification of a specific
 * function.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class DeterministicDistribution implements DiscreteDistribution {

	// logger
	public static Logger log = new Logger("DeterministicDistribution",
			Logger.Level.DEBUG);
	
	// the variable label associated with the distribution
	String variable;
	
	// the deterministic function
	DeterministicFunction function;
	
	
	/**
	 * Creates a new deterministic distribution with a particular variable label and function.
	 * 
	 * @param variable the variable label
	 * @param function the function
	 */
	public DeterministicDistribution (String variable, DeterministicFunction function) {
		this.variable = variable;
		this.function = function;
	}



	/**
	 * Copies the distribution.
	 * 
	 * @return the copy of the distribution
	 */
	@Override
	public DeterministicDistribution copy() {
		return new DeterministicDistribution(variable, function.copy());
	}


	/**
	 * Returns a string representation of the distribution
	 */
	@Override
	public String toString() {
		return "deterministic distribution P(" + variable + ") with function " + function.toString();
	}


	/**
	 * Replaces all occurrences of the old variable identifier oldId with the new identifier newId
	 * 
	 * @param oldId the old identifier
	 * @param newId the new identifier
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
		function.modifyVarId(oldId, newId);
	}

	
	/**
	 * Does nothing
	 */
	@Override
	public void pruneValues(double threshold) {
		return;
	}

	/**
	 * Samples from the distribution given the condition assignment (here,
	 * always returns the output of the function).
	 * 
	 * @return the sampled value
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return new Assignment(variable, function.getValue(condition));
	}


	/**
	 * Returns the single variable associated with the distribution
	 * 
	 * @return the variable label for the distribution
	 */
	@Override
	public Collection<String> getHeadVariables() {
		return Arrays.asList(variable);
	}


	/**
	 * Returns the probability P(head|condition) -- which in this case is either
	 * 1.0 if head is the function output for condition, or 0.0 otherwise.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {
		Value value = function.getValue(condition);
		if (head.containsVar(variable) && head.getValue(variable).equals(value)) {
			return 1.0;
		}
		return 0.0;
	}



	/**
	 * Returns a categorical table with one unique element (with probability 1.0), namely 
	 * the value associated with the conditional assignment for the deterministic function.
	 * 
	 * @param condition the conditional assignment
	 * @return a categorical table with one unique value
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition) throws DialException {
		Value value = function.getValue(condition);
		return new CategoricalTable(new Assignment(variable, value));
	}
	
	

	/**
	 * Returns the distribution.
	 * @throws DialException 
	 */
	@Override
	public CategoricalTable getPartialPosterior(Assignment condition) throws DialException {
		return getPosterior(condition);
	}
	
	/**
	 * Generates all possible input assignments for the input values (expensive operation!) 
	 * and calculates the corresponding output value for each.
	 * 
	 * @param range possible input values for the conditional variables
	 * @return the corresponding outputs
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		Set<Assignment> possibleInputAssigns = range.linearise();
		Set<Assignment> result = new HashSet<Assignment>();
		for (Assignment possibleInput : possibleInputAssigns) {
			result.add(new Assignment(variable, function.getValue(possibleInput)));
		}
		return result;
	}
	
	/**
	 * Returns the hashcode for the distribution
	 */
	@Override
	public int hashCode() {
		return variable.hashCode() - function.hashCode();
	}
	

	/**
	 * Returns DISCRETE.
	 */
	@Override
	public DistribType getPreferredType() {
		return DistribType.DISCRETE;
	}



	/**
	 * Returns true.
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}



	/**
	 * Returns itself
	 */
	@Override
	public DiscreteDistribution toDiscrete() throws DialException {
		return this;
	}



}

