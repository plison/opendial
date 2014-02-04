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

package opendial.bn.distribs.discrete.functions;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Represents a deterministic function of its input.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public interface DeterministicFunction {

	/**
	 * Returns the unique value corresponding to the input assignment
	 * 
	 * @param input the input assignment
	 * @return the corresponding output
	 */
	public Value getValue(Assignment input);
	
	/**
	 * Returns a copy of the function
	 * 
	 * @return the copy
	 */
	public DeterministicFunction copy();

	/**
	 * Modify all occurrences of the old variable identifier oldId 
	 * by newId
	 * 
	 * @param oldId the old identifier
	 * @param newId the new identifier
	 */
	public void modifyVarId(String oldId, String newId);
}

