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

import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;

/**
 * Generic interface for a condition used in a probability or utility rule. 
 * 
 * <p>A condition operates on a number of (possibly underspecified) input variables,
 * and can be applied to any input assignment to determine if it satisfies the condition
 * or not.  In addition, the condition can also produce some local output, for instance
 * based on slots filled via string matching.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Condition {
	
	/**
	 * Returns the input variables of the condition (as templates).
	 * 
	 * @return the input variable templates
	 */
	public Set<Template> getInputVariables();
		
	
	/**
	 * Returns true if the condition is satisfied for the given assignment,
	 * and false otherwise 
	 * 
	 * @param input the input assignment
	 * @return true if the condition is satisfied, false otherwise
	 */
	public boolean isSatisfiedBy(Assignment input);
	
	/**
	 * Returns the set of possible groundings derived from the condition,
	 * based on the given assignment.
	 * 
	 * @param input the assignment from which to calculate the groundings
	 * @return the groundings (as a value range).
	 */
	public ValueRange getGroundings(Assignment input); 
	
}

