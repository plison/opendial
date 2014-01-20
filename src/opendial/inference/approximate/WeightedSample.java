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

package opendial.inference.approximate;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;


/**
 * Representation of a weighted sample, which consists of an assignment
 * of values together with a weight (here in logarithmic form) and utility.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class WeightedSample extends Assignment implements Comparable<WeightedSample> {

	// logger
	public static Logger log = new Logger("WeightedSample", Logger.Level.NORMAL);

	// logarithmic weight (+- equiv. of probability)
	double logWeight = 0.0f;

	// the utility
	double utility = 0.0f;

	/**
	 * Creates a new, empty weighted sample
	 */
	public WeightedSample() {
		super();
	}
	
	/**
	 * Creates a new sample
	 */
	public WeightedSample(Assignment a) {
		super(a);
	}
	
	public WeightedSample(Assignment a, double logWeight, double utility) {
		super(a);
		this.logWeight = logWeight;
		this.utility = utility;
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
		return Math.exp(logWeight);
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
	 */
	@Override
	public String toString() {
		return super.toString() + " (w=" + getWeight() + ", util=" + utility+")";
	}

	
	@Override
	public int compareTo(WeightedSample arg0) {
		return (int)((utility - arg0.getUtility())*1000);
	}

}
