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

package opendial.bn.distribs.continuous;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.continuous.functions.DensityFunction;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Representation of a continuous probability distribution, defined by an 
 * arbitrary density function over a single (univariate or multivariate) 
 * variable. The distribution does not take any conditional assignment.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ContinuousDistribution implements  IndependentProbDistribution {

	public static Logger log = new Logger("ContinuousDistribution", Logger.Level.DEBUG);

	// the variable for the distribution
	String variable;

	// density function for the distribution
	DensityFunction function;

	// discrete equivalent of the distribution
	CategoricalTable discreteCache;
		

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================


	/**
	 * Constructs a new distribution with a variable and a density function
	 * 
	 * @param variable the variable
	 * @param function the density function
	 */
	public ContinuousDistribution(String variable, DensityFunction function) {
		this.variable = variable;
		this.function = function;
	}

	
	/**
	 * Does nothing.
	 */
	@Override
	public void pruneValues(double frequencyThreshold) {
		return;
	}

	// ===================================
	//  GETTERS
	// ===================================



	/**
	 * Samples from the distribution (conditional assignment is ignored).
	 * 
	 * @return the sample (variable,value) pair.
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}
	
	
	/**
	 * Samples from the distribution.  
	 * 
	 * @return the sampled (variable, value) pair
	 */
	@Override
	public Assignment sample() {
		Value v = (function.getDimensionality() > 1)? 
				ValueFactory.create(function.sample()) 
				: ValueFactory.create(function.sample()[0]);
		return new Assignment (variable, v);
	}


	/**
	 * Returns the distribution
	 * 
	 * @return the distribution
	 */
	@Override
	public ContinuousDistribution toContinuous() {
		return this;
	}


	/**
	 * Returns a discretised version of the distribution. The number of discretisation
	 * buckets is defined in the configuration settings
	 * 
	 * @return the discretised version of the distribution
	 */
	@Override
	public CategoricalTable toDiscrete() {

		if (discreteCache == null) {
			Map<Double[],Double> discretisation = function.discretise(Settings.discretisationBuckets);
			discreteCache = new CategoricalTable();
			for (Double[] value : discretisation.keySet()) {
				Value val = (value.length > 1)? new ArrayVal(value) : ValueFactory.create(value[0]);
				discreteCache.addRow(new Assignment(variable, val), discretisation.get(value));
			}
		}
		return discreteCache;
	}



	/**
	 * Returns the probability density for the given head assignment
	 * 
	 * @param head the head assignment (must contain the distribution variable, and have a double value)
	 * @return the resulting density
	 */
	public double getProbDensity(Assignment head) {
		try {
			if (head.containsVar(variable)) {
				if (head.getValue(variable) instanceof ArrayVal) {
					return function.getDensity(((ArrayVal)head.getValue(variable)).getArray());
				}
				if (head.getValue(variable) instanceof DoubleVal) {
					return function.getDensity(((DoubleVal)head.getValue(variable)).getDouble());
				}
			}
			else {
				log.warning("head does not contain variable " + variable + ", or has a wrong-typed value: " + head);
			}
		}
		catch (DialException e) {
			log.warning("exception: " + e);
		}
		return 0.0;
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


	/**
	 * Returns the distribution.
	 */
	@Override
	public ContinuousDistribution getPosterior(Assignment condition) {
		return this;
	}
	

	/**
	 * Returns the distribution.
	 */
	@Override
	public ContinuousDistribution getPartialPosterior(Assignment condition) {
		return this;
	}


	/**
	 * Returns continuous
	 */
	@Override
	public DistribType getPreferredType() {
		return DistribType.CONTINUOUS;
	}

	
	/**
	 * Returns the cumulative probability from 0 up to a given point provided in the
	 * argument.
	 * 
	 * @param head the point up to which the cumulative probability must be estimated.
	 * @return the cumulative probability
	 */
	public double getCumulativeProb(Assignment head) {
		try {
		if (head.containsVar(variable)) {
			Value val = head.getValue(variable);
			if (val instanceof ArrayVal) {
				return function.getCDF(((ArrayVal)val).getArray());
			}
			else if (val instanceof DoubleVal) {
				return function.getCDF(new Double[]{((DoubleVal)val).getDouble()});
			}
		}
		else {
			log.warning("head does not contain variable " + variable + ", or has a wrong-typed value: " + head);
		}
		}
		catch (DialException e) {
			log.warning("exception: " + e);
		}
		return 0.0;
	} 
	
	
	/**
	 * 
	 * Discretises the distribution and returns a set of possible values for it.
	 * 
	 * @param range the input values (is ignored)
	 * @return the set of discretised values for the variable
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		return getPossibleValues();
	}
	
	
	/**
	 * Discretises the distribution and returns a set of possible values for it.
	 * 
	 * @return the set of discretised values for the variable
	 */
	@Override
	public Set<Assignment> getPossibleValues() {
		return toDiscrete().getPossibleValues();
	}
	
	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	/**
	 * Returns true if the distribution is well-formed -- more specifically,
	 * if the cumulative density function sums up to 1.0 as it should.
	 * 
	 * @return true if well-formed, false otherwise.
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
	public ContinuousDistribution copy() {
		return new ContinuousDistribution(variable, function.copy());
	}



	/**
	 * Returns a pretty print of the distribution
	 *
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		return "PDF(" + variable+")=" + function.toString();
	}



	/**
	 * Modifies the variable label
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
		if (discreteCache != null) {
			discreteCache.modifyVariableId(oldId, newId);
		}
	}
	
	
	/**
	 * Returns the XML representation of the distribution
	 * 
	 * @param doc the document to which the XML node belongs
	 * @return the corresponding node
	 * @throws DialException 
	 */
	@Override
	public Node generateXML(Document doc) throws DialException {
		
		Element var = doc.createElement("variable");

		Attr id = doc.createAttribute("id");
		id.setValue(variable);
		var.setAttributeNode(id);
		for (Node node : function.generateXML(doc)) {
			var.appendChild(node);
		}
				
		return var;
	}



}
