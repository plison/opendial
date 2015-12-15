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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.domains.rules.RuleGrounding;
import opendial.templates.Template;

/**
 * Complex condition made up of a collection of sub-conditions connected with a
 * logical operator (AND, OR).
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class ComplexCondition implements Condition {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the collection of subconditions
	final Collection<Condition> subconditions;

	// the enumeration of possible binary operators
	public static enum BinaryOperator {
		AND, OR
	}

	// the binary operator for the complex condition (default is AND)
	final BinaryOperator operator;

	// ===================================
	// CONDITION CONSTRUCTION
	// ===================================

	/**
	 * Creates a new complex condition with a list of subconditions
	 * 
	 * @param subconditions the subconditions
	 * @param operator the binary operator to employ between the conditions
	 */
	public ComplexCondition(List<Condition> subconditions, BinaryOperator operator) {
		this.subconditions = subconditions;
		this.operator = operator;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the logical operator for the complex condition
	 * 
	 * @return the operator
	 */
	public BinaryOperator getOperator() {
		return operator;
	}

	/**
	 * Returns the set of input variables for the complex condition
	 * 
	 * @return the set of input variables
	 */
	@Override
	public Collection<Template> getInputVariables() {
		List<Template> variables = new ArrayList<Template>();
		for (Condition cond : subconditions) {
			variables.addAll(cond.getInputVariables());
		}
		return variables;
	}

	/**
	 * Returns the subconditions in the complex condition.
	 * 
	 * @return the subconditions.
	 */
	public Collection<Condition> getConditions() {
		return subconditions;
	}

	/**
	 * Returns the list of all slots used in the conditions
	 * 
	 * @return the list of all slots
	 */
	@Override
	public Set<String> getSlots() {
		Set<String> slots = new HashSet<String>();
		for (Condition cond : subconditions) {
			slots.addAll(cond.getSlots());
		}
		return slots;
	}

	/**
	 * Returns true if the complex condition is satisfied by the input assignment,
	 * and false otherwise.
	 * 
	 * <p>
	 * If the logical operator is AND, all the subconditions must be satisfied. If
	 * the operator is OR, at least one must be satisfied.
	 * 
	 * @param input the input assignment
	 * @return true if the conditions are satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		for (Condition cond : subconditions) {
			if (operator == BinaryOperator.AND && !cond.isSatisfiedBy(input)) {
				return false;
			}
			else if (operator == BinaryOperator.OR && cond.isSatisfiedBy(input)) {
				return true;
			}
		}
		return (operator == BinaryOperator.AND);
	}

	/**
	 * Returns the groundings for the complex condition (which is the union of the
	 * groundings for all basic conditions).
	 * 
	 * @return the full set of groundings
	 */
	@Override
	public RuleGrounding getGroundings(Assignment input) {

		RuleGrounding groundings = new RuleGrounding();

		if (operator == BinaryOperator.AND) {
			for (Condition cond : subconditions) {

				RuleGrounding newGrounding = new RuleGrounding();
				boolean foundGrounding = false;
				for (Assignment g : groundings.getAlternatives()) {
					Assignment g2 = (g.isEmpty()) ? input : new Assignment(input, g);
					RuleGrounding ground = cond.getGroundings(g2);
					foundGrounding = foundGrounding || !ground.isFailed();
					ground.extend(g);
					newGrounding.add(ground);
				}
				if (!foundGrounding) {
					newGrounding.setAsFailed();
					return newGrounding;
				}
				groundings = newGrounding;
			}
		}
		else if (operator == BinaryOperator.OR) {

			List<RuleGrounding> alternatives = new ArrayList<RuleGrounding>();
			for (Condition cond : subconditions) {
				RuleGrounding newGround = cond.getGroundings(input);
				alternatives.add(newGround);
			}
			groundings.add(alternatives);

		}

		return groundings;
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns a string representation of the complex condition
	 */
	@Override
	public String toString() {
		String str = "";
		for (Condition cond : subconditions) {
			str += cond.toString();
			switch (operator) {
			case AND:
				str += " ^ ";
				break;
			case OR:
				str += " v ";
				break;
			}
		}
		return str.substring(0, str.length() - 3);
	}

	/**
	 * Returns the hashcode for the condition
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return subconditions.hashCode() - operator.hashCode();
	}

	/**
	 * Returns true if the complex conditions are equal, false otherwise
	 *
	 * @param o the object to compare with current instance
	 * @return true if the conditions are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return this.hashCode() == o.hashCode();
	}

}
