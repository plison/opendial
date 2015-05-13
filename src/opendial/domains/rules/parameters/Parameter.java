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

import java.util.Collection;

import opendial.datastructs.Assignment;
import opendial.datastructs.MathExpression;

/**
 * Interface for a parameter associated with an effect
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface Parameter {

	/**
	 * Returns the actual parameter value given the inputs provided as arguments. If
	 * the actual value cannot be retrieved (missing information), throws an
	 * exception.
	 * 
	 * @param input the input assignment
	 * @return the actual parameter value
	 */
	public double getValue(Assignment input);

	/**
	 * Returns the (possibly empty) set of parameter identifiers used in the
	 * parameter object.
	 * 
	 * @return the collection of parameter labels
	 */
	public Collection<String> getVariables();

	/**
	 * Returns the mathematical expression representing the parameter
	 * 
	 * @return the expression
	 */
	public MathExpression getExpression();

}
