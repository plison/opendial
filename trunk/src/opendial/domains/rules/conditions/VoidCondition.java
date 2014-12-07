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

package opendial.domains.rules.conditions;

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;

/**
 * Representation of a void condition, which is always true.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VoidCondition implements Condition {

	// logger
	static Logger log = new Logger("VoidCondition", Logger.Level.NORMAL);

	static VoidCondition instance;
	
	/**
	 * Return an empty set 
	 * 
	 * @return an empty set
	 */
	@Override
	public Set<Template> getInputVariables() {
		return new HashSet<Template>();
	}


	/**
	 * Returns true (condition is always trivially satisfied)
	 *
	 * @param input the input assignment (ignored)
	 * @return true
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		return true;
	}
	
	/**
	 * Returns an empty set of groundings
	 */
	@Override
	public ValueRange getGroundings(Assignment input) {
		return new ValueRange();
	}
	
	
	
	/**
	 * Returns an empty list
	 * 
	 * @return an empty list
	 */
	@Override
	public Set<String> getSlots() {
		return new HashSet<String>();
	}
	
	
	/**
	 * Returns the string "true" indicating that the condition is
	 * always trivially true
	 *
	 * @return true
	 */
	@Override
	public String toString() {
		return "true";
	}
	
	/**
	 * Returns a constant representing the hashcode for the void condition
	 *
	 * @return 36
	 */
	@Override
	public int hashCode() {
		return 36;
	}
	
	/**
	 * Returns true if o is also a void condition
	 *
	 * @param o the object to compare
	 * @return true if o is also a void condition
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof VoidCondition);
	}
}
