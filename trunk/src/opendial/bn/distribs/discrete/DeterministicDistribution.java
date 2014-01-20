// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
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
	 * @param the head assignment
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
	 * @param the conditional assignment
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

