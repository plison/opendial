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

package opendial.inference.datastructs;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.AssignmentVal;
import opendial.bn.values.Value;


/**
 * Representation of a weighted sample, which consists of an assignment
 * of values together with a weight (here in logarithmic form) and utility.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class WeightedSample {

	// logger
	public static Logger log = new Logger("WeightedSample", Logger.Level.NORMAL);

	// the sample
	Assignment sample;

	// logarithmic weight (+- equiv. of probability)
	double logWeight = 0.0f;

	// the utility
	double utility = 0.0f;

	/**
	 * Creates a new, empty weighted sample
	 */
	public WeightedSample() {
		sample = new Assignment();
	}
	
	/**
	 * Creates a new sample
	 */
	public WeightedSample(Assignment a) {
		sample = new Assignment(a);
	}
	
	/**
	 * Adds a new assignment to the sample
	 * 
	 * @param variable the variable label
	 * @param value the value
	 */
	public void addPoint(String variable, Value value) {
		if (value instanceof AssignmentVal) {
			for (String var : ((AssignmentVal)value).getAssignment().getVariables()) {
				sample.addPair(var, ((AssignmentVal)value).getAssignment().getValue(var));
			}
		}
		else {
			sample.addPair(variable, value);
		}
	}
	
	public void addPoints(Assignment assign) {
		sample.addAssignment(assign);
	}

	/**
	 * Adds a logarithmic weight to the current one
	 * 
	 * @param addLogWeight the weight to add
	 */
	public void addLogWeight(double addLogWeight) {
		logWeight += addLogWeight;
	}

	/**
	 * Sets the logarithmic weight to a given value
	 * 
	 * @param weight the value for the weight
	 */
	public void setWeight(double weight) {
		this.logWeight = Math.log(weight);
	}

	/**
	 * Returns the sample weight (exponentiated value, not the logarithmic one!)
	 * 
	 * @return the (exponentiated) weight for the sample
	 */
	public double getWeight() {
		return (double)Math.exp(logWeight);
	}

	/**
	 * Returns the sample
	 * 
	 * @return the sample
	 */
	public Assignment getSample() {
		return sample;
	}

	/**
	 * Adds a utility to the sample
	 * 
	 * @param newUtil the utility to add
	 */
	public void addUtility(double newUtil) {
		utility += newUtil;
	}

	/**
	 * Returns the utility of the sample
	 * 
	 * @return the utility
	 */
	public double getUtility() {
		return utility;
	}

	/**
	 * Returns a string representation of the weighted sample
	 *
	 * @return the string representation
	 */
	public String toString() {
		return sample + "(w=" + getWeight() + ", util=" + utility+")";
	}

	public void addAssign(Assignment newVals) {
		sample.addAssignment(newVals);
	}
}
