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

package opendial.domains.rules.effects;

import java.util.logging.*;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.Condition;

/**
 * Representation of a basic effect of a rule. A basic effect is formally defined as
 * a triple with:
 * <ol>
 * <li>a variable label;
 * <li>one of four basic operations on the variable SET, DISCARD, ADD;
 * <li>a variable value;
 * </ol>
 * 
 * This class represented a usual, fully grounded effect. For effects including
 * underspecified entities, use TemplateEffect.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class BasicEffect {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** Variable label */
	final String variableLabel;

	/** Variable value */
	final Value variableValue;

	/**
	 * Whether the value is mutually exclusive with other values for the variable
	 * (default case) or not. If not, distinct values are added together in a list.
	 */
	final boolean exclusive;

	/** Whether the effect includes a negation (default is false). */
	final boolean negated;

	/** Priority level (default is 1) */
	int priority;

	/** effect weight (default is 1) */
	double weight = 1;

	// ===================================
	// EFFECT CONSTRUCTION
	// ===================================

	/**
	 * Constructs a new basic effect, with a variable label and value.
	 * 
	 * @param variable variable label (raw string)
	 * @param value variable value (raw string)
	 */
	public BasicEffect(String variable, String value) {
		this(variable, ValueFactory.create(value), 1, true, false);
	}

	/**
	 * Constructs a new basic effect, with a variable label, value and other
	 * arguments. The argument "add" specifies whether the effect is mutually
	 * exclusive with other effects. The argument "negated" specifies whether the
	 * effect includes a negation.
	 * 
	 * 
	 * @param variable variable label (raw string)
	 * @param value variable value
	 * @param priority the priority level (default is 1)
	 * @param exclusive whether distinct values are mutually exclusive or not
	 * @param negated whether to negate the effect or not.
	 */
	public BasicEffect(String variable, Value value, int priority, boolean exclusive,
			boolean negated) {
		this.variableLabel = variable;
		this.variableValue = value;
		this.priority = priority;
		this.exclusive = exclusive;
		this.negated = negated;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the variable label for the basic effect
	 * 
	 * @return the variable label
	 */
	public String getVariable() {
		return variableLabel;
	}

	/**
	 * Returns the variable value for the basic effect. If the variable value is
	 * underspecified, returns the value None.
	 * 
	 * @return the variable value
	 */
	public Value getValue() {
		return variableValue;
	}

	/**
	 * Converts the effect into an equivalent condition
	 * 
	 * @return the corresponding condition
	 */
	public Condition convertToCondition() {
		Relation r = (negated) ? Relation.UNEQUAL : Relation.EQUAL;
		return new BasicCondition(variableLabel + "'", variableValue, r);
	}

	/**
	 * Returns false.
	 * 
	 * @return false.
	 * 
	 */
	public boolean containsSlots() {
		return false;
	}

	/**
	 * Returns itself.
	 * 
	 * @param grounding the grounding assignment
	 * @return itself
	 */
	public BasicEffect ground(Assignment grounding) {
		return this;
	}

	/**
	 * Returns the rule priority
	 * 
	 * @return the priority level
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Returns the effect weight
	 * 
	 * @return the weight
	 */
	public double getWeight() {
		return weight;
	}

	/**
	 * Returns true if the effect allows only one distinct value for the variable
	 * (default case) and false otherwise
	 * 
	 * @return true if the effect allows only one distinct value, else false.
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Returns true is the effect is negated and false otherwise.
	 * 
	 * @return whether the effect is negated.
	 */
	public boolean isNegated() {
		return negated;
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns the string representation of the basic effect
	 */
	@Override
	public String toString() {
		String str = variableLabel;
		if (negated) {
			str += "!=";
		}
		else if (!exclusive) {
			str += "+=";
		}
		else {
			str += ":=";
		}
		str += variableValue;
		return str;
	}

	/**
	 * Returns the hashcode for the basic effect
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		int hashcode = ((negated) ? -2 : 1) * variableLabel.hashCode()
				^ (new Boolean(exclusive)).hashCode() ^ priority
				^ variableValue.hashCode();
		return hashcode;
	}

	/**
	 * Returns true if the object o is a basic effect that is identical to the
	 * current instance, and false otherwise.
	 *
	 * @param o the object to compare
	 * @return true if the objects are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof BasicEffect) {
			if (!((BasicEffect) o).getVariable().equals(variableLabel)) {
				return false;
			}
			else if (!((BasicEffect) o).getValue().equals(getValue())) {
				return false;
			}
			else if (((BasicEffect) o).exclusive != exclusive) {
				return false;
			}
			else if (((BasicEffect) o).isNegated() != negated) {
				return false;
			}
			else if (((BasicEffect) o).priority != priority) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns a copy of the effect.
	 * 
	 * @return the copy.
	 */
	public BasicEffect copy() {
		BasicEffect copy = new BasicEffect(variableLabel, variableValue, priority,
				exclusive, negated);
		return copy;
	}

	/**
	 * Changes the priority of the basic effects
	 * 
	 * @param priority the new priority
	 */
	public void changePriority(int priority) {
		this.priority = priority;
	}

}
