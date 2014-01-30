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
 * @version $Date::                      $ *
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

