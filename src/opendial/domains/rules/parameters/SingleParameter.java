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

import java.util.logging.*;
import java.util.Arrays;
import java.util.Collection;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.MathExpression;

/**
 * Parameter represented by a single distribution over a continuous variable. If the
 * variable is multivariate, the parameter represents a specific dimension of the
 * multivariate distribution.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class SingleParameter implements Parameter {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// the parameter identifier
	final String paramId;

	// the selected dimension for the parameter. If the parameter is univariate,
	// the default value is -1.
	final int dimension;

	/**
	 * Creates a new stochastic parameter for a univariate distribution.
	 * 
	 * @param paramId the parameter identifier
	 */
	public SingleParameter(String paramId) {
		this.paramId = paramId;
		this.dimension = -1;
	}

	/**
	 * Creates a new stochastic parameter for a particular dimension of a
	 * multivariate distribution.
	 * 
	 * @param paramId the parameter identifier
	 * @param dimension the dimension for the multivariate variable
	 */
	public SingleParameter(String paramId, int dimension) {
		this.paramId = paramId;
		this.dimension = dimension;
	}

	/**
	 * Returns a singleton with the parameter label
	 *
	 * @return a collection with one element: the parameter distribution
	 */
	@Override
	public Collection<String> getVariables() {
		return Arrays.asList(paramId);
	}

	/**
	 * Returns the actual value for the parameter, as given in the input assignment
	 * (as a DoubleVal or ArrayVal). If the value is not given, throws an exception.
	 *
	 * @param input the input assignment
	 * @return the actual value for the parameter
	 */
	@Override
	public double getValue(Assignment input) {
		Value value = input.getValue(paramId);
		if (input.containsVar(paramId) && value instanceof DoubleVal) {
			return ((DoubleVal) input.getValue(paramId)).getDouble();
		}
		else if (input.containsVar(paramId) && value instanceof ArrayVal
				&& ((ArrayVal) value).getArray().length > dimension) {
			return ((ArrayVal) value).getArray()[dimension];
		}

		else {
			log.warning("input " + input + " does not contain " + paramId);
			return 0.0;
		}
	}

	/**
	 * Returns the mathematical expression representing the parameter
	 * 
	 * @return the expression
	 */
	@Override
	public MathExpression getExpression() {
		return new MathExpression(toString());
	}

	/**
	 * Returns a string representation of the stochastic parameter
	 */
	@Override
	public String toString() {
		if (dimension != -1) {
			return paramId.toString() + "[" + dimension + "]";
		}
		else {
			return paramId.toString();
		}
	}

	/**
	 * Returns the hashcode for the parameter
	 */
	@Override
	public int hashCode() {
		return -paramId.hashCode() + dimension;
	}

}
