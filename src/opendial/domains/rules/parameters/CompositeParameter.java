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

package opendial.domains.rules.parameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;

/**
 * Parameter represented as a linear combination of several continuous
 * parameters.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class CompositeParameter implements Parameter {

	// logger
	public static Logger log = new Logger("CompositionParameter", Logger.Level.NORMAL);
	
	
	// distributions for the composite parameter, indexed by their variable name
	Set<String> distribs;
	

	/**
	 * Creates a new composite parameter 
	 */
	public CompositeParameter() {
		this.distribs = new HashSet<String>();
	}

	
	/**
	 * Creates a new composite parameter with the given parameter distributions
	 * 
	 * @param distribs the collection of distributions
	 */
	public CompositeParameter(Collection<String> distribs) {
		this();
		this.distribs.addAll(distribs);
	}
	
	
	/**
	 * Adds a new parameter in the composite distribution
	 * 
	 * @param param the parameter to add
	 */
	public void addParameter(String param) {
		distribs.add(param);
	}
	
	/**
	 * Returns the parameter value for the composition, by extracting the values
	 * for each individual parameter in the input assignment, and then adding
	 * them in a linear combination. All parameters must have a specified value
	 * in the assignment, else an exception is thrown.
	 *
	 * @param input the input combination.
	 * @return the combined value for the parameters
	 * @throws DialException if the parameters are not all specified
	 */
	public double getParameterValue(Assignment input) throws DialException {
		double totalValue = 0.0;
		if (input.containsVars(distribs)) {
			for (String paramVar : distribs) {
				Value paramVal = input.getValue(paramVar);
				if (paramVal instanceof DoubleVal) {
					totalValue += ((DoubleVal)paramVal).getDouble();
				}
				else {
					throw new DialException ("value for parameter " + paramVar +
							" should be double, but is " + paramVal);
				}
			}
			return totalValue;
		}
		else {
			throw new DialException(" input " + input + " does not contain all " +
					"parameter variables " + distribs);
		}
	}
	
	/**
	 * Returns the collection of elementary distributions for the composite
	 * parameter
	 *
	 * @return the collection of distributions
	 */
	public Collection<String> getParameterIds() {
		return distribs;
	}
	
	/**
	 * Returns a string representation of the composite parameter
	 *
	 * @return the string representation
	 */
	public String toString() {
		String str = "";
		for (String distrib : distribs) {
			str += distrib;
		}
		return str.substring(0, str.length()-1);		
	}
	
	
	/**
	 * Returns a copy of the composite parameter
	 *
	 * @return the copy
	 */
	public CompositeParameter copy() {
		return new CompositeParameter(distribs);
	}
	
	/**
	 * Returns true if the composite parameter is identical to the given
	 * object, and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if identical, false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof CompositeParameter) {
			return distribs.equals(((CompositeParameter)o).getParameterIds());
		}
		return false;
	}
	
	
	/**
	 * Returns the hashcode for the parameter
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return distribs.hashCode();
	}
	
	
}
