// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
