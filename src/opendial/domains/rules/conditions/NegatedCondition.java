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

import java.util.logging.*;

import java.util.Collection;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.domains.rules.RuleGrounding;
import opendial.templates.Template;

/**
 * Negated condition, which is satisfied when the included condition is not.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class NegatedCondition implements Condition {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// condition to negate
	final Condition initCondition;

	// ===================================
	// CONDITION CONSTRUCTIOn
	// ===================================

	/**
	 * Creates a new negated condition with the condition provided as argument
	 * 
	 * @param initCondition the condition to negate
	 */
	public NegatedCondition(Condition initCondition) {
		this.initCondition = initCondition;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the input variables for the condition (which are the same as the ones
	 * for the condition to negate)
	 * 
	 * @return the input variables
	 */
	@Override
	public Collection<Template> getInputVariables() {
		return initCondition.getInputVariables();
	}

	@Override
	public RuleGrounding getGroundings(Assignment input) {
		RuleGrounding g = initCondition.getGroundings(input);
		g = (g.isFailed()) ? new RuleGrounding() : g;
		return g;
	}

	/**
	 * Returns true if the condition to negate is *not* satisfied, and false if it is
	 * satisfied
	 * 
	 * @param input the input assignment to verify
	 * @return true if the included condition is false, and vice versa
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		return !initCondition.isSatisfiedBy(input);
	}

	/**
	 * Returns the condition to negate
	 * 
	 * @return the condition to negate
	 */
	public Condition getInitCondition() {
		return initCondition;
	}

	/**
	 * Returns the list of slots in the condition
	 * 
	 * @return the list of slots
	 */
	@Override
	public Set<String> getSlots() {
		return initCondition.getSlots();
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns the hashcode for the condition
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return -initCondition.hashCode();
	}

	/**
	 * Returns the string representation of the condition
	 */
	@Override
	public String toString() {
		return "!" + initCondition.toString();
	}

	/**
	 * Returns true if the current instance and the object are identical, and false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof NegatedCondition) {
			return ((NegatedCondition) o).getInitCondition().equals(initCondition);
		}
		return false;
	}
}
