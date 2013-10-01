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

package opendial.domains.rules.parameters;

import java.util.Arrays;
import java.util.Collection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.domains.rules.parameters.CompositeParameter.Operator;

/**
 * Parameter represented by a single distribution over a continuous variable.  If
 * the variable is multivariate, the parameter represents a specific dimension of 
 * the multivariate distribution.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StochasticParameter implements Parameter {

	// logger
	public static Logger log = new Logger("StochasticParameter", Logger.Level.NORMAL);
	
	// the parameter identifier
	String paramId;
	
	// the selected dimension for the parameter. If the parameter is univariate,
	// the default value is -1.
	int dimension = -1;
	
	/**
	 * Creates a new stochastic parameter for a univariate distribution.
	 * 
	 * @param paramId the parameter identifier
	 */
	public StochasticParameter(String paramId) {
		this.paramId = paramId;
	}
	
	/**
	 * Creates a new stochastic parameter for a particular dimension of 
	 * a multivariate distribution.
	 * 
	 * @param paramId the parameter identifier
	 * @param dimension the dimension for the multivariate variable
	 */
	public StochasticParameter(String paramId, int dimension) {
		this(paramId);
		this.dimension = dimension;
	}
	

	/**
	 * Creates a composite parameter with the addition of the two parameters
	 */
	@Override
	public Parameter sumParameter(Parameter otherParam) {
		return new CompositeParameter(Arrays.asList(this, otherParam), Operator.ADD);
	}
	
	

	
	/**
	 * Creates a composite parameter  with the multiplication of the two parameters
	 */
	@Override
	public Parameter multiplyParameter(Parameter otherParam) {
		return new CompositeParameter(Arrays.asList(this, otherParam), Operator.MULTIPLY);
	}
	
	
	/**
	 * Returns a singleton with the parameter label
	 *
	 * @return a collection with one element: the parameter distribution
	 */
	public Collection<String> getParameterIds() {
		return Arrays.asList(paramId);
	}

	
	/**
	 * Returns the actual value for the parameter, as given in the input assignment (as a
	 * DoubleVal or ArrayVal).  If the value is not given, throws an exception.
	 *
	 * @param input the input assignment
	 * @return the actual value for the parameter
	 * @throws DialException if the value is not specified in the input assignment
	 */
	public double getParameterValue(Assignment input) throws DialException {
		Value value = input.getValue(paramId);
		if (input.containsVar(paramId) && value instanceof DoubleVal) {
			return ((DoubleVal)input.getValue(paramId)).getDouble();
		}
		else if (input.containsVar(paramId) && value instanceof ArrayVal 
				&& ((ArrayVal)value).getArray().length > dimension) {
			return ((ArrayVal)value).getArray()[dimension];
		}
		else {
			throw new DialException("input " + input + " does not contain " + paramId);
		}
	}
	
	

	/**
	 * Returns a string representation of the stochastic parameter
	 */
	public String toString() {
		if (dimension != -1) {
			return paramId.toString()+"["+dimension+"]";
		}
		else {
			return paramId.toString();
		}
	}
	
	/**
	 * Returns the hashcode for the parameter
	 */
	public int hashCode() {
		return -paramId.hashCode() + dimension;
	}
	
}
