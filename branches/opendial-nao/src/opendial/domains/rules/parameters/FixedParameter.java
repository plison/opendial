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
import java.util.LinkedList;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.domains.rules.parameters.CompositeParameter.Operator;


/**
 * Representation of a parameter fixed to one single specific value.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FixedParameter implements Parameter {

	// logger
	public static Logger log = new Logger("FixedParameter", Logger.Level.NORMAL);
	
	// the parameter value
	double param;
	
	/**
	 * Constructs a fixed parameter with the given value.
	 * 
	 * @param param the parameter value
	 */
	public FixedParameter(double param) {
		this.param = param;
	}
	
	
	/**
	 * Sums the parameter with the other parameter.  If both values are fixed parameter, the
	 * returned parameter is also fixed.  Else, the method constructs a composite parameter
	 * with the two included parameters.
	 * 
	 * @param otherParam the other parameter
	 * @return the parameter resulting from the addition of the two parameters
	 */
	@Override
	public Parameter sumParameter(Parameter otherParam) {
		if (otherParam instanceof FixedParameter) {
			return new FixedParameter(param +((FixedParameter)otherParam).getParameterValue());
		}
		else {
			return new CompositeParameter (Arrays.asList(this, otherParam), Operator.ADD);
		}
	}
	
	
	/**
	 * Multiplies the parameter with the other parameter.  If both values are fixed parameter, the
	 * returned parameter is also fixed.  Else, the method constructs a composite parameter
	 * with the two included parameters.
	 * 
	 * @param otherParam the other parameter
	 * @return the parameter resulting from the multiplication of the two parameters
	 */
	@Override
	public Parameter multiplyParameter(Parameter otherParam) {
		if (otherParam instanceof FixedParameter) {
			return new FixedParameter(param *((FixedParameter)otherParam).getParameterValue());
		}
		else {
			return new CompositeParameter (Arrays.asList(this, otherParam), Operator.MULTIPLY);
		}
	}
	
	/**
	 * Returns the parameter value
	 * 
	 * @return the value for the parameter
	 */
	public double getParameterValue() {
		return param;
	}
	
	
	/**
	 * Returns the parameter value, ignoring the input
	 * 
	 * @return the value for the parameter
	 */
	@Override
	public double getParameterValue(Assignment input) {
		return getParameterValue();
	}
	
	
	/**
	 * Returns an empty set
	 *
	 * @return an empty set of distributions
	 */
	@Override
	public Collection<String> getParameterIds() {
		return new LinkedList<String>();
	}
	
	
	/**
	 * Returns the parameter value as a string
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return""+ param;
	}
	
	
	/**
	 * Returns the hashcode for the fixed parameter
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return 2* new Double(param).hashCode();
	}
	

}
