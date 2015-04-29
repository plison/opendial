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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.Template.MatchResult;
import opendial.domains.rules.RuleGrounding;
import opendial.domains.rules.conditions.BasicCondition.Relation;

/**
 * Basic condition between a variable and a value
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class TemplateCondition implements Condition {

	static Logger log = new Logger("TemplateCondition", Logger.Level.DEBUG);

	// variable label (can include slots to fill)
	final Template variable;

	// expected variable value (can include slots to fill)
	final Template expectedValue;

	// the relation which needs to hold between the variable and the value
	// (default is EQUAL)
	final Relation relation;

	// ===================================
	// CONDITION CONSTRUCTION
	// ===================================

	/**
	 * Creates a new basic condition, given a variable label, an expected value,
	 * and a relation to hold between the variable and its value
	 * 
	 * @param variable the variable
	 * @param value the value
	 * @param relation the relation to hold
	 */
	public TemplateCondition(Template variable, Template value,
			Relation relation) {
		this.variable = variable;
		this.expectedValue = value;
		this.relation = relation;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the relation in place for the condition
	 * 
	 * @return the relation
	 */
	public Relation getRelation() {
		return relation;
	}

	/**
	 * Returns the variable label for the basic condition
	 * 
	 * @return the variable label
	 */
	public Template getVariable() {
		return variable;
	}

	/**
	 * Returns the expected variable value for the basic condition
	 * 
	 * @return the expected variable value
	 */
	public Template getValue() {
		return expectedValue;
	}

	/**
	 * Returns the input variables for the condition (the main variable itself,
	 * plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Set<Template> getInputVariables() {
		Set<Template> inputVariables = new HashSet<Template>();
		inputVariables.add(variable);
		return inputVariables;
	}

	/**
	 * Returns the slots in the variable and value template
	 */
	@Override
	public Set<String> getSlots() {
		Set<String> slots = new HashSet<String>();
		slots.addAll(variable.getSlots());
		slots.addAll(expectedValue.getSlots());
		return slots;
	}

	/**
	 * Returns true if the condition is satisfied by the value assignment
	 * provided as argument, and false otherwise
	 * 
	 * <p>
	 * This method uses an external ConditionCheck object to ease the process.
	 *
	 * @param input the actual assignment of values
	 * @return true if the condition is satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		if (!variable.isFilledBy(input) || !expectedValue.isFilledBy(input)) {
			return false;
		}

		Template expectedValue2 = expectedValue.fillSlots(input);
		String filledVar = variable.fillSlots(input).getRawString();
		Value actualValue = input.getValue(filledVar);

		// regular expressions in the expected value
		if (expectedValue2.isUnderspecified()) {
			switch (relation) {
			case EQUAL:
				return expectedValue2.match(actualValue.toString())
						.isMatching();
			case UNEQUAL:
				return !expectedValue2.match(actualValue.toString())
						.isMatching();
			case CONTAINS:
				return expectedValue2.partialmatch(actualValue.toString())
						.isMatching();
			case NOT_CONTAINS:
				return !expectedValue2.partialmatch(actualValue.toString())
						.isMatching();
			case LENGTH:
				return expectedValue2.match("" + actualValue.length())
						.isMatching();
			case IN:
				return new Template(actualValue.toString()).partialmatch(
						expectedValue2.toString()).isMatching();
			case NOT_IN:
				return !new Template(actualValue.toString()).partialmatch(
						expectedValue2.toString()).isMatching();
			default:
				return false;
			}
		} else {
			Value filledValue = ValueFactory.create(expectedValue2
					.getRawString());
			switch (relation) {
			case EQUAL:
				return actualValue.equals(filledValue);
			case UNEQUAL:
				return !actualValue.equals(filledValue);
			case GREATER_THAN:
				return (actualValue.compareTo(filledValue) > 0);
			case LOWER_THAN:
				return (actualValue.compareTo(filledValue) < 0);
			case CONTAINS:
				return actualValue.contains(filledValue);
			case NOT_CONTAINS:
				return !actualValue.contains(filledValue);
			case LENGTH:
				return actualValue.length() == filledValue.length();
			case IN:
				return filledValue.contains(actualValue);
			case NOT_IN:
				return !filledValue.contains(actualValue);
			}
			return false;
		}
	}

	/**
	 * Returns the set of possible groundings for the given input assignment
	 * 
	 * @param input the input assignment
	 * @return the set of possible (alternative) groundings for the condition
	 */
	@Override
	public RuleGrounding getGroundings(Assignment input) {

		RuleGrounding groundings = new RuleGrounding();
		if (!variable.getSlots().isEmpty() && !variable.isRawSlot()
				&& !variable.fillSlots(input).getSlots().isEmpty()) {
			for (String inputVar : input.getVariables()) {
				MatchResult m = variable.match(inputVar);
				if (m.isMatching()) {
					Assignment newInput = new Assignment(input,
							m.getFilledSlots());
					RuleGrounding specGrounds = getGroundings(newInput);
					specGrounds.extend(m.getFilledSlots());
					groundings.add(specGrounds);
				}
			}
			return groundings;
		}

		Template expectedValue2 = expectedValue.fillSlots(input);
		if (expectedValue2.isUnderspecified()) {

			String filledVar = variable.fillSlots(input).getRawString();
			Value actualValue = input.getValue(filledVar);

			if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
				MatchResult m = expectedValue2.match(actualValue.toString());
				if (m.isMatching()) {
					Assignment possGrounding = m.getFilledSlots();
					possGrounding.removeAll(input.getVariables());
					possGrounding.removeValues(ValueFactory.none());
					return new RuleGrounding(possGrounding);
				} else if (relation == Relation.EQUAL) {
					return new RuleGrounding.Failed();
				}
			} else if (relation == Relation.CONTAINS
					&& actualValue instanceof SetVal) {
				List<Assignment> alternatives = new ArrayList<Assignment>();
				for (Value subval : ((SetVal) actualValue).getSet()) {
					MatchResult m2 = expectedValue2.match(subval.toString());
					if (m2.isMatching()) {
						Assignment possGrounding = m2.getFilledSlots();
						possGrounding.removeAll(input.getVariables());
						possGrounding.removeValues(ValueFactory.none());
						alternatives.add(possGrounding);
					}
				}
				return (!alternatives.isEmpty()) ? new RuleGrounding(
						alternatives) : new RuleGrounding.Failed();
			} else if (relation == Relation.CONTAINS) {
				List<MatchResult> m2 = expectedValue2.find(
						actualValue.toString(), 100);
				List<Assignment> alternatives = new ArrayList<Assignment>();
				for (MatchResult match : m2) {
					Assignment filledSlot = match.getFilledSlots();
					filledSlot.removeAll(input.getVariables());
					filledSlot.removeValues(ValueFactory.none());
					alternatives.add(filledSlot);
				}
				return (!alternatives.isEmpty()) ? new RuleGrounding(
						alternatives) : new RuleGrounding.Failed();
			}
		}
		return groundings;
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns a string representation of the condition
	 */
	@Override
	public String toString() {
		switch (relation) {
		case EQUAL:
			return variable + "=" + expectedValue;
		case UNEQUAL:
			return variable + "!=" + expectedValue;
		case GREATER_THAN:
			return variable + ">" + expectedValue;
		case LOWER_THAN:
			return variable + "<" + expectedValue;
		case CONTAINS:
			return variable + " contains " + expectedValue;
		case NOT_CONTAINS:
			return variable + " does not contains " + expectedValue;
		case LENGTH:
			return "length(" + variable + ")=" + expectedValue;
		case IN:
			return variable + " in " + expectedValue;
		case NOT_IN:
			return variable + " not in " + expectedValue;
		default:
			return "";
		}
	}

	/**
	 * Returns the hashcode for the condition
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return variable.hashCode() + expectedValue.hashCode() - 3
				* relation.hashCode();
	}

	/**
	 * Returns true if the given object is a condition identical to the current
	 * instance, and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if the condition are equals, and false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof TemplateCondition) {
			return (((TemplateCondition) o).getVariable().equals(variable)
					&& ((TemplateCondition) o).getValue().equals(expectedValue) && relation == ((TemplateCondition) o)
						.getRelation());
		}
		return false;
	}

}
