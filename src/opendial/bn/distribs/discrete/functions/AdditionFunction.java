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

import opendial.arch.Logger;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

/**
 * Deterministic function that outputs the sum of 
 * all double values in the input assignment.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$ *
 */
public class AdditionFunction implements DeterministicFunction {

	// logger
	public static Logger log = new Logger("AdditionFunction", Logger.Level.NORMAL);

	/**
	 * Returns the sum of all double values in the input
	 * 
	 * @return the sum of all values.
	 */
	@Override
	public Value getValue(Assignment input) {
		double total = 0.0;
		for (Value val : input.getValues()) {
			if (val instanceof DoubleVal) {
				total += ((DoubleVal)val).getDouble();
			}
		}
		return ValueFactory.create(total);
	}

	
	/**
	 * Returns "addition function".
	 * 
	 */
	@Override
	public String toString() {
		return "addition function";
	}

	
	/**
	 * Returns a copy of the function.
	 */
	@Override
	public DeterministicFunction copy() {
		return new AdditionFunction();
	}

	
	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		return;
	}
	

	/**
	 * Returns a constant.
	 * 
	 * @return 456.
	 */
	public int hashcode() {
		return 456;
	}
}

