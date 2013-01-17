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

import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;

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
public class DepEmpiricalDistribution implements EmpiricalDistribution {

	// logger
	public static Logger log = new Logger("DepEmpiricalDistribution", Logger.Level.DEBUG);
	
	// list of "flat" samples for the empirical distribution
	protected List<Assignment> samples;

	// structured samples according to the condition/head distinction
	Map<Assignment, SimpleEmpiricalDistribution> conditionedSamples;

	// the sampler
	Random sampler;
	
	// cache for the discrete and continuous distributions
	DiscreteProbDistribution discreteCache;
	ContinuousProbabilityTable continuousCache;
	
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
	public DepEmpiricalDistribution(Collection<String> headVars, Collection<String> condVars) {
		samples = new ArrayList<Assignment>(3000);
		sampler = new Random();
		this.headVars = new HashSet<String>(headVars);
		this.condVars = new HashSet<String>(condVars);
		conditionedSamples = new HashMap<Assignment, SimpleEmpiricalDistribution>();
	}
	
	/**
	 * Constructs a new empirical distribution with the provided set of samples
	 * 
	 * @param samples the samples for the distribution
	 */
	public DepEmpiricalDistribution(Collection<String> headVars, 
			Collection<String> condVars, List<Assignment> samples) {
		this(headVars, condVars);
		for (Assignment a : samples) {
			addSample(a);
		}
	}
	
	
	/**
	 * Adds a new sample to the distribution
	 * 
	 * @param sample the sample to add
	 */
	public void addSample(Assignment sample) {
		samples.add(sample);
		Assignment condition = sample.getTrimmed(condVars);
		if (!conditionedSamples.containsKey(condition)) {
			conditionedSamples.put(condition, new SimpleEmpiricalDistribution());
		}
		conditionedSamples.get(condition).addSample(sample.getTrimmed(headVars));
	}
	
	
	// ===================================
	//  GETTERS
	// ===================================
	

	 
	
	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment 
	 * @return the selected sample
	 */
	@Override
	public Assignment sample(Assignment condition) {
		if (conditionedSamples.containsKey(condition)) {
			SimpleEmpiricalDistribution headSamples = conditionedSamples.get(condition);
			return headSamples.sample(condition);
		}
				
		log.debug("cannot sample dependent empirical distribution for condition: " + condition);
		return getDefaultAssignment();
	}

	

	@Override
	public boolean isWellFormed() {
		return !conditionedSamples.isEmpty();
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
		
		DiscreteProbabilityTable discreteCache = new DiscreteProbabilityTable();
			
		for (Assignment condition : conditionedSamples.keySet()) {
			discreteCache.addRows(condition, (SimpleTable)(conditionedSamples.get(condition).toDiscrete()));
		}
		this.discreteCache = discreteCache;
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
		
		continuousCache = new ContinuousProbabilityTable();
		for (Assignment condition : conditionedSamples.keySet()) {
			SimpleEmpiricalDistribution subdistrib = conditionedSamples.get(condition);
			FunctionBasedDistribution continuousEquiv = subdistrib.toContinuous();
			continuousCache.addDistrib(condition, continuousEquiv);
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
	public DepEmpiricalDistribution copy() {
		DepEmpiricalDistribution copy = new DepEmpiricalDistribution(headVars, condVars, samples);
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
		return toDiscrete().prettyPrint();
	}


	/**
	 * Replace a variable label by a new one
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		
		// change the raw samples
		List<Assignment> newSamples = new ArrayList<Assignment>(samples.size());
		for (Assignment a : samples) {
			Assignment b = new Assignment();
			for (String var : a.getVariables()) {
				String newVar = (var.equals(oldId))? newId : var;
				b.addPair(newVar, a.getValue(var));
			}
			newSamples.add(b);
		}
		samples = newSamples; 
		
		// change the structured samples
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
		conditionedSamples = newCondSamples;
	}

}
