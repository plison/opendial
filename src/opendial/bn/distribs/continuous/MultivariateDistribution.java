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

package opendial.bn.distribs.continuous;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.Settings;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.MultivariateDensityFunction;
import opendial.bn.distribs.continuous.functions.UnivariateDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.VectorVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;
import opendial.utils.InferenceUtils;
import opendial.utils.DistanceUtils;


/**
 * Representation of a continuous probability distribution, defined by an 
 * arbitrary density function over a single variable.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class MultivariateDistribution implements ContinuousProbDistribution {

	public static Logger log = new Logger("MultivariateDistribution", Logger.Level.DEBUG);

	// the variable for the distribution
	String variable;
		
	// density function for the distribution
	MultivariateDensityFunction function;
	

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================

	
	/**
	 * Constructs a new distribution with a variable and a density function
	 * 
	 * @param variable the variable
	 * @param function the density function
	 */
	public MultivariateDistribution(String variable, MultivariateDensityFunction function) {
		this.variable = variable;
		this.function = function;
	}
	
	

	// ===================================
	//  GETTERS
	// ===================================

	
	/**
	 * Samples from the distribution.  The input assignment is ignored (the 
	 * distribution assumes no input conditions)
	 * 
	 * @param condition the input condition (ignored)
	 * @return the sampled (variable, value) pair
	 */
	@Override
	public Assignment sample(Assignment input) {
		return sample();
	}

	
	/**
	 * Samples from the distribution.  
	 * 
	 * @return the sampled (variable, value) pair
	 */
	public Assignment sample() {
		return new Assignment (variable, ValueFactory.create(function.sample()));
	}
	
	
	/**
	 * Returns the distribution
	 * 
	 * @return the distribution
	 */
	@Override
	public ContinuousProbDistribution toContinuous() {
		return this;
	}

	
	/**
	 * Returns a discretised version of the distribution. The number of discretisation
	 * buckets is defined in the configuration settings
	 * 
	 * @return the discretised version of the distribution
	 */
	@Override
	public SimpleTable toDiscrete() {
	
		int nbBuckets = Settings.getInstance().nbDiscretisationBuckets;
				
		List<double[]> values = function.getDiscreteValueArrays(nbBuckets);
		
		Map<Assignment, Double> distrib = new HashMap<Assignment, Double>();
		double minDistance = DistanceUtils.getMaxManhattanDistance(values)/2.0;
		for (int i = 0 ; i < Settings.getInstance().nbSamples ; i++) {
			
			double[] sample = function.sample();
			double[] closest = findClosest(values, sample, minDistance);
			
			if (closest != null) {
			Assignment a = new Assignment(variable,new VectorVal(closest));
			if (!distrib.containsKey(a)) {
				distrib.put(a, 0.0);
			}
			distrib.put(a, distrib.get(a) + 1.0);
			}
		}
		
		distrib = InferenceUtils.normalise(distrib);
		SimpleTable discreteVersion = new SimpleTable();
		discreteVersion.addRows(distrib);
		return discreteVersion;
	}
	
	
	private static double[] findClosest (List<double[]> values, double[] value, double minDistance) {
		
		double closestDist = Double.MAX_VALUE;
		double[] closestValue = null;
		
		for (double[] possibleVal : values) {
			double distance = 0;
			for (int i = 0 ; i < value.length ; i++) {
				distance += Math.abs(possibleVal[i] - value[i]);
			}
			
			if (distance < closestDist && distance < minDistance) {
				closestDist = distance;
				closestValue = possibleVal;
			}
		}
		return closestValue;
	}


	/**
	 * Returns the probability density for the given head assignment
	 * 
	 * @param condition the conditional assignment (ignored)
	 * @param head the head assignment (must contain the distribution variable, and have a double value)
	 * @return the resulting density
	 */
	@Override
	public double getProbDensity(Assignment condition, Assignment head) {
		return getProbDensity(head);
	}

	
	/**
	 * Returns the probability density for the given head assignment
	 * 
	 * @param head the head assignment (must contain the distribution variable, and have a double value)
	 * @return the resulting density
	 */
	public double getProbDensity(Assignment head) {
		if (head.containsVar(variable)) {
			if (head.getValue(variable) instanceof VectorVal) {
			return getProbDensity((VectorVal)head.getValue(variable));
			}
		}
		else {
			log.warning("head does not contain variable " + variable + ", or has a wrong-typed value: " + head);
		}
		return 0.0;
	}
	
	/**
	 * Returns the probability density for the given double value
	 * 
	 * @param value the double value
	 * @return the resulting density
	 */
	public double getProbDensity(VectorVal value) {
		return function.getDensity(value.getArray());
	}
	
	
	
	
	/**
	 * Returns the density function
	 * 
	 * @return the density function
	 */
	public MultivariateDensityFunction getFunction() {
		return function;
	}
	
	
	/**
	 * Returns the variable label
	 * 
	 * @return the variable label
	 */
	public String getVariable() {
		return variable;
	}


	/**
	 * Returns a singleton set with the variable label
	 * 
	 * @return a singleton set with the variable label
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>(Arrays.asList(variable));
		return headVars;
	}
	
	public int getDimensionality() {
		return function.getDimensionality();
	}
	
	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================

	
	/**
	 * Returns true if the distribution is well-formed -- more specifically,
	 * if the cumulative density function sums up to 1.0 as it should
	 * @return
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	
	/**
	 * Returns a copy of the probability distribution
	 * 
	 * @return the copy
	 */
	@Override
	public ProbDistribution copy() {
		return new MultivariateDistribution(variable, function.copy());
	}

	
	/**
	 * Returns a pretty print of the distribution
	 *
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		return "PDF(" + variable+")=" + function.prettyPrint();
	}
	
	
	/**
	 * Returns a pretty print of the distribution
	 *
	 * @return the pretty print
	 */
	public String toString() {
		return prettyPrint();
	}

	
	
	/**
	 * Modifies the variable label
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
	}



	@Override
	public double getCumulativeProb(Assignment condition, Assignment head) {
		log.debug("CDF not implemented!");
		return 0;
	}



	public double[] getMean() {
		return function.getMean();
	}
	
	
	public double[] getVariance() {
		return function.getVariance();
	}


}
