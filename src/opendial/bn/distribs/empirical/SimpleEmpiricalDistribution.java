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
import java.util.Iterator;
import java.util.Set;

import java.util.List;
import java.util.Map;
import java.util.Random;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.MultivariateDistribution;

import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.continuous.functions.ProductKernelDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;
import opendial.inference.datastructs.WeightedSample;
import opendial.utils.InferenceUtils;

/**
 * Distribution defined "empirically" in terms of a set of samples on the relevant 
 * variables.  This distribution can then be explicitly converted into a table 
 * or a continuous distribution (depending on the variable type)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SimpleEmpiricalDistribution implements EmpiricalDistribution {

	// logger
	public static Logger log = new Logger("EmpiricalDistribution",
			Logger.Level.DEBUG);

	// list of samples for the empirical distribution
	protected List<Assignment> samples;

	public static boolean USE_KDE = true;
	
	Random sampler;

	boolean cacheCreated = false;
	DiscreteProbDistribution discreteCache;
	ContinuousProbDistribution continuousCache;
	
	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================

	/**
	 * Constructs a new empirical distribution, initially with a empty set of
	 * samples
	 */
	public SimpleEmpiricalDistribution() {
		samples = new ArrayList<Assignment>();
		sampler = new Random();
	}

	/**
	 * Constructs a new empirical distribution with the provided set of samples
	 * 
	 * @param samples the samples for the distribution
	 */
	public SimpleEmpiricalDistribution(Collection<Assignment> samples) {
		this();
		this.samples.addAll(samples);
	}


	/**
	 * Adds a new sample to the distribution
	 * 
	 * @param sample the sample to add
	 */
	public void addSample(Assignment sample) {
		samples.add(sample);
		discreteCache = null;
		continuousCache = null;
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
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}


	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment (ignored here)
	 * @return the selected sample
	 * @throws DialException 
	 */
	public Assignment sample() throws DialException {
		
		if (USE_KDE) {
		if (!cacheCreated) {
			if (shouldUseParametricForm()) {
				try { computeContinuousCache(); } catch (DialException e) { }
			}
			cacheCreated = true;
		}
		
		if (continuousCache != null && continuousCache.getDimensionality() == 1) {
			return continuousCache.sample(new Assignment());
		}
		}
		
		if (!samples.isEmpty()) {
			int selection = sampler.nextInt(samples.size());
			Assignment selected = samples.get(selection);
			return selected;
		}
		else {
			log.warning("distribution has no samples");
			return new Assignment();
		}
	}
	
	

	private boolean shouldUseParametricForm() {
		int nbRealValues = 0;
		for (int i = 0 ; i < 20 ; i++) {
			Assignment a = samples.get(sampler.nextInt(samples.size()));
			for (String var : getHeadVariables()) {
				if (a.containsVar(var) && a.getValue(var) instanceof DoubleVal) {
					nbRealValues++;
					if (nbRealValues > 2) {
						return true;
					}
				}		
			}
			}
		return false;
	}


	/**
	 * Checks whether the distribution is well-formed or not.  The only requirement
	 * we have here is that the set of samples must be non-empty
	 * 
	 * @return true if well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		return !samples.isEmpty();
	}


	/**
	 * Returns the size of the set of samples defining the distribution
	 * 
	 * @return the set of samples
	 */
	public int getSize() {
		return samples.size();
	}


	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>();
		for (Assignment a : samples) {
			headVars.addAll(a.getVariables());
		}
		return headVars;
	}


	// ===================================
	//  CONVERSION METHODS
	// ===================================


	/**
	 * Converts the distribution into a SimpleTable, by counting the number of 
	 * occurrences for each distinct value of a variable.
	 * 
	 * @return the resulting discrete distribution
	 */
	@Override
	public DiscreteProbDistribution toDiscrete() {
		if (discreteCache == null) {
			computeDiscreteCache();
		}
		return discreteCache;
	}


	protected void computeDiscreteCache() {
		this.discreteCache = InferenceUtils.createTable(getHeadVariables(), samples);
	}


	/**
	 * Converts the distribution into a continuous distribution based on a kernel
	 * density function.  This conversion will only work if:<ol>
	 * <li> the samples only relate to a single random variable
	 * <li> the value space of this random variable is continuous (double)
	 * </ol>
	 * 
	 * @return the converted continuous distribution
	 * @throws DialException if the above requirements are not met
	 */
	@Override
	public ContinuousProbDistribution toContinuous()
			throws DialException {

		if (continuousCache == null) {
			computeContinuousCache();
		}
		return continuousCache;
	}


	protected void computeContinuousCache() throws DialException {
		
		if (getHeadVariables().size() == 1) {
			continuousCache = InferenceUtils.createContinuousDistrib(getHeadVariables().iterator().next(), samples);
		}
		else {
			log.warning("empirical distribution could not converted to a " +
					"continuous distribution, headVars = " + getHeadVariables());
		}
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
	public SimpleEmpiricalDistribution copy() {
		SimpleEmpiricalDistribution copy = new SimpleEmpiricalDistribution(samples);
		return copy;
	}

	/**
	 * Returns a pretty print representation of the distribution: here, 
	 * tries to convert it to a continuous or discrete distribution,
	 * and displays its content.
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		try {
			return toContinuous().prettyPrint();
		}
		catch (DialException e) {
			return toDiscrete().prettyPrint();
		}
	}

	/**
	 * Returns a pretty print representation of the distribution: here, 
	 * tries to convert it to a continuous or discrete distribution,
	 * and displays its content.
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		int nbEmptys = 0;
		for (Assignment s : samples) {
			if (s.isDefault()) {
				nbEmptys++;
			}
		}
		return prettyPrint() ; // + " [nb emptys: " + nbEmptys +"]";
	}


	/**
	 * Returns the hashcode for the distribution
	 * 
	 * @return the hashcode
	 */
	public int hashCode() {
		return samples.hashCode();
	}


	/**
	 * Replace a variable label by a new one
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
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
	}


	public ProbDistribution compile() {
		try {
			if (!samples.isEmpty() && samples.get(0).getVariables().size() == 1) {
				String headVar = samples.get(0).getVariables().iterator().next();
				if (samples.get(0).getValue(headVar) instanceof DoubleVal) {
					return toContinuous();
				}
			}
			return toDiscrete();
		}
		catch (DialException e) {
			return toDiscrete();
		}
	}

	@Override
	public Collection<Assignment> getSamples() {
		return samples;
	}

}
