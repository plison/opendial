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

package opendial.bn.distribs;

import opendial.datastructs.Assignment;

/**
 * Generic interface for a utility function (also called value function), mapping
 * every assignment X1, ..., Xn to a scalar utility U(X1, ...., Xn).
 * 
 * <p>
 * Typically, at least one of these X1, ..., Xn variables consist of a decision
 * variable.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface UtilityFunction {

	/**
	 * Returns the utility associated with the specific assignment of values for the
	 * input nodes. If none exists, returns 0.0f.
	 * 
	 * @param input the value assignment for the input chance nodes
	 * @return the associated utility
	 */
	public double getUtil(Assignment input);

	/**
	 * Creates a copy of the utility distribution
	 * 
	 * @return the copy
	 */
	public UtilityFunction copy();

	/**
	 * Changes the variable label
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	public void modifyVariableId(String oldId, String newId);

}
