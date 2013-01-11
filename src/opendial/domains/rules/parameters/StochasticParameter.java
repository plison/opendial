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

import java.util.Arrays;
import java.util.Collection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;

/**
 * Parameter represented by a single distribution over a continuous
 * variable.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StochasticParameter implements Parameter {

	// logger
	public static Logger log = new Logger("StochasticParameter", Logger.Level.NORMAL);
	
	ChanceNode distrib;
	
	public StochasticParameter(ChanceNode distrib) {
		this.distrib = distrib;
	}
	
	public String toString() {
		return distrib.toString();
	}
	
	public int hashCode() {
		return -distrib.hashCode();
	}
	
	
	public StochasticParameter copy() {
		return new StochasticParameter(distrib);
	}

	/**
	 * Returns the distribution for the parameter
	 * 
	 * @return the distribution
	 */
	public ChanceNode getDistrib() {
		return distrib;
	}
	
	
	/**
	 * Returns the actual value for the parameter, as given in the input assignment (as a
	 * DoubleVal).  If the value is not given, throws an exception.
	 *
	 * @param input the input assignment
	 * @return the actual value for the parameter
	 * @throws DialException if the value is not specified in the input assignment
	 */
	public double getParameterValue(Assignment input) throws DialException {
		String variable = distrib.getId();
		if (input.containsVar(variable) && input.getValue(variable) instanceof DoubleVal) {
			return ((DoubleVal)input.getValue(variable)).getDouble();
		}
		else {
			throw new DialException("input " + input + " does not contain " + variable);
		}
	}
	
	
	/**
	 * Returns a singleton with the distribution for the parameter
	 *
	 * @return a collection with one element: the parameter distribution
	 */
	public Collection<ChanceNode> getDistributions() {
		return Arrays.asList(distrib);
	}
}
