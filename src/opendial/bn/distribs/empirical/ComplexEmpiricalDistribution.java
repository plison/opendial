// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.bn.distribs.empirical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;
import opendial.inference.datastructs.WeightedSample;
import opendial.utils.DistanceUtils;
import opendial.utils.InferenceUtils;

/**
 * Distribution defined "empirically" in terms of a set of samples on the relevant 
 * variables.  This distribution can then be explicitly converted into a table 
 * or a continuous distribution (depending on the variable type).
 * 
 * This distribution has a set of conditional variables X1,...Xn.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexEmpiricalDistribution implements EmpiricalDistribution {

	// logger
	public static Logger log = new Logger("CompositeEmpiricalDistribution", Logger.Level.DEBUG);

	// list of "flat" samples for the empirical distribution
	protected SimpleEmpiricalDistribution distrib;

	// cache for the discrete and continuous distributions
	DiscreteProbDistribution discreteCache;
	ContinuousProbDistribution continuousCache;

	// the head variables
	Set<String> headVars;

	// the conditionalVars variables
	Set<String> condVars;

	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================

	/**
	 * Constructs a new empirical distribution, initially with a empty set of
	 * samples
	 */
	public ComplexEmpiricalDistribution(Collection<String> headVars, 
			Collection<String> condVars, SimpleEmpiricalDistribution distrib) {
		
		this.headVars = new HashSet<String>(headVars);
		this.condVars = new HashSet<String>(condVars);
		this.distrib = distrib;
	}



	/**
	 * Adds a new sample to the distribution
	 * 
	 * @param sample the sample to add
	 */
	public synchronized void addSample(Assignment sample) {
		log.debug("method should not be used directly!!");
		distrib.addSample(sample);
	}


	// ===================================
	//  GETTERS
	// ===================================




	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment (ignored here)
	 * @return the selected sample
	 * @throws DialException 
	 */
	public Assignment sample() throws DialException {
		return distrib.sample();
	}
	

	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment 
	 * @return the selected sample
	 * @throws DialException 
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}



	@Override
	public boolean isWellFormed() {
		return distrib.isWellFormed();
	}


	@Override
	public Collection<String> getHeadVariables() {
		return headVars;
	}


	// ===================================
	//  CONVERSION METHODS
	// ===================================

	@Override
	public DiscreteProbDistribution toDiscrete() {
		if (discreteCache == null) {
			computeDiscreteCache();
		}
		return discreteCache;
	}


	/**
	 * Converts the distribution into a DiscreteProbabilityTable, by counting the number of 
	 * occurrences for each distinct value of a variable.
	 * 
	 * @return the resulting discrete distribution
	 */
	public void computeDiscreteCache() {

		if (condVars.isEmpty()) {
			this.discreteCache = InferenceUtils.createTable(headVars, distrib.getSamples());
		}
		
		else {
			if (condVars.toString().contains("theta")) {
				log.debug("===> HORROR!! discretising a table with parameters!!");
			}
			this.discreteCache = InferenceUtils.createTable(condVars, headVars, distrib.getSamples());
		}
	}


	@Override
	public ContinuousProbDistribution toContinuous() throws DialException {
		if (continuousCache == null) {
			computeContinuousCache();
		}
		return continuousCache;
	}

	/**
	 * Converts the distribution into a continuous probability table.
	 * 
	 * @return the converted continuous distribution
	 * @throws DialException if the above requirements are not met
	 */
	public synchronized void computeContinuousCache() throws DialException {

		if (condVars.isEmpty() && headVars.size() == 1) {
			continuousCache = InferenceUtils.createContinuousDistrib(headVars.iterator().next(), distrib.getSamples());
		}
		else if (headVars.size() == 1) {
			continuousCache = InferenceUtils.createContinuousDistrib(condVars, headVars.iterator().next(), distrib.getSamples());
		}
		else {
			log.warning("could not convert to a continuous distribution, headVars = " + headVars);
		}
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Returns a default assignment with default values
	 * 
	 * @return the default assignment
	 */
	private Assignment getDefaultAssignment() {
		Assignment defaultA = new Assignment();
		for (String headVar : headVars) {
			defaultA.addPair(headVar, ValueFactory.none());
		}
		return defaultA;
	}

	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns a copy of the distribution
	 * 
	 * @return the copy
	 */
	@Override
	public ComplexEmpiricalDistribution copy() {
		ComplexEmpiricalDistribution copy = new ComplexEmpiricalDistribution(headVars, condVars, distrib);
		return copy;
	}

	/**
	 * Returns a pretty print representation of the distribution: here, 
	 * tries to convert it to a discrete distribution, and displays its content.
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		return "dependent empirical distribution P(" + headVars + "|" + condVars + ")";
	}


	/**
	 * Replace a variable label by a new one
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public synchronized void modifyVarId(String oldId, String newId) {

		if (headVars.contains(oldId)) {
			headVars.remove(oldId);
			headVars.add(newId);
		}
		if (condVars.contains(oldId)) {
			condVars.remove(oldId);
			condVars.add(newId);
		}
		
		distrib.modifyVarId(oldId, newId);

		// change the structured samples
		/** 
		Map<Assignment, SimpleEmpiricalDistribution> newCondSamples = 
				new HashMap<Assignment, SimpleEmpiricalDistribution>();
		for (Assignment a: conditionedSamples.keySet()) {
			SimpleEmpiricalDistribution condSample = conditionedSamples.get(a);
			condSample.modifyVarId(oldId, newId);
			Assignment b = new Assignment();
			for (String var : a.getVariables()) {
				String newVar = (var.equals(oldId))? newId : var;
				b.addPair(newVar, a.getValue(var));
			}
			newCondSamples.put(b, condSample);
		}
		conditionedSamples = newCondSamples; */
	}


	public String toString() {
		return prettyPrint();
	}

	@Override
	public Collection<Assignment> getSamples() {
		return distrib.getSamples();
	}

}
