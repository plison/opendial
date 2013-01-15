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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import opendial.arch.ConfigurationSettings;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.DensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;


/**
 * Representation of a continuous probability distribution, defined by an 
 * arbitrary density function over a single variable.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FunctionBasedDistribution implements ContinuousProbDistribution {

	public static Logger log = new Logger("FunctionBasedDistribution", Logger.Level.DEBUG);

	// the variable for the distribution
	String variable;
		
	// density function for the distribution
	DensityFunction function;
	

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================

	
	/**
	 * Constructs a new distribution with a variable and a density function
	 * 
	 * @param variable the variable
	 * @param function the density function
	 */
	public FunctionBasedDistribution(String variable, DensityFunction function) {
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
	
		int nbBuckets = ConfigurationSettings.getInstance().getNbDiscretisationBuckets();
		
		List<Double> values = function.getDiscreteValues(nbBuckets);
		SimpleTable discreteVersion = new SimpleTable();
		double prevCDF = -1.0;
		Iterator<Double> valuesIt1 = values.iterator();
		
		while (valuesIt1.hasNext()) {
			double curVal = valuesIt1.next();
			double curCDF = function.getCDF(curVal);
			
			double leftPart = (prevCDF > 0.0)? (curCDF-prevCDF) : curCDF;
			
		//	double rightPart = (valuesIt2.hasNext()) ? (function.getCDF(valuesIt2.next()) - curCDF)/2.0 : 1.0 - curCDF; 
			
			double prob = leftPart; // + rightPart;
			
			if (!discreteVersion.hasProb(new Assignment(variable, curVal))) {
				discreteVersion.addRow(new Assignment(variable, curVal), prob);
			}
			prevCDF = curCDF;
		}
		return discreteVersion;
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
			if (head.getValue(variable) instanceof DoubleVal) {
			return getProbDensity((DoubleVal)head.getValue(variable));
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
	public double getProbDensity(DoubleVal value) {
		return function.getDensity(value.getDouble());
	}
	
	
	/**
	 * Returns the cumulative probability for the given conditional and head 
	 * assignments
	 * 
	 * @param condition the conditional assignment (ignored in this case)
	 * @param head the head assignment (must contain the distribution variable and 
	 *        be associated with a double value)
	 * @return the resulting cumulative probability
	 */
	@Override
	public double getCumulativeProb(Assignment condition, Assignment head) {
		return getCumulativeProb(head);
	}
	
	

	/**
	 * Returns the cumulative probability for the given conditional and head 
	 * assignments
	 * 
	 * @param head the head assignment (must contain the distribution variable and 
	 *        be associated with a double value)
	 * @return the resulting cumulative probability
	 */
	public double getCumulativeProb(Assignment head) {
		if (head.containsVar(variable)) {
			if (head.getValue(variable) instanceof DoubleVal) {
			return function.getCDF(((DoubleVal)head.getValue(variable)).getDouble());
			}
		}
		else {
			log.warning("head does not contain variable " + variable + ", or has a wrong-typed value: " + head);
		}
		return 0.0;
	}
	
	
	/**
	 * Returns the cumulative probability for the given double value
	 * 
	 * @param value the double value
	 * @return the resulting cumulative probability
	 */
	public double getCumulativeProb(DoubleVal value) {
		return function.getCDF(value.getDouble());
	}

	
	/**
	 * Returns the density function
	 * 
	 * @return the density function
	 */
	public DensityFunction getFunction() {
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
		return function.getCDF(-Double.MAX_VALUE) >= 0.0
		&& function.getCDF(-Double.MAX_VALUE) < 0.01 
		&& function.getCDF(Double.MAX_VALUE) > 0.99 
		&& function.getCDF(Double.MAX_VALUE) < 1.01;
	}

	
	/**
	 * Returns a copy of the probability distribution
	 * 
	 * @return the copy
	 */
	@Override
	public ProbDistribution copy() {
		return new FunctionBasedDistribution(variable, function.copy());
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



}
