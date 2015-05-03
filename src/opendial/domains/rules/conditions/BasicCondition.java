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

import java.util.Arrays;
import java.util.Collection;
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

/**
 * Basic condition between a variable and a value
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class BasicCondition implements Condition {

	static Logger log = new Logger("BasicCondition", Logger.Level.DEBUG);

	// variable label (can include slots to fill)
	final Template variable;

	// expected variable value (can include slots to fill)
	final Template templateValue;
	Value groundValue;

	// possible relations used in a basic condition
	public static enum Relation {
		EQUAL, UNEQUAL, CONTAINS, NOT_CONTAINS, GREATER_THAN, LOWER_THAN, IN, NOT_IN, LENGTH
	}

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
	public BasicCondition(String variable, String value, Relation relation) {
		this.variable = new Template(variable);
		this.templateValue = new Template(value);
		if (!templateValue.isUnderspecified()) {
			this.groundValue = ValueFactory.create(value);
		}
		this.relation = relation;
	}

	/**
	 * Creates a new basic condition, given a variable label, an expected value,
	 * and a relation to hold between the variable and its value
	 * 
	 * @param variable the variable
	 * @param value the value
	 * @param relation the relation to hold
	 */
	public BasicCondition(String variable, Value value, Relation relation) {
		this.variable = new Template(variable);
		this.templateValue = new Template(value.toString());
		this.groundValue = value;
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
	 * Returns the input variables for the condition (the main variable itself,
	 * plus optional slots in the value to fill)
	 * 
	 * @return the input variables
	 */
	@Override
	public Collection<Template> getInputVariables() {
		return Arrays.asList(variable);
	}

	/**
	 * Returns the slots in the variable and value template
	 */
	@Override
	public Set<String> getSlots() {
		Set<String> slots = new HashSet<String>();
		slots.addAll(variable.getSlots());
		slots.addAll(templateValue.getSlots());
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

		if (!variable.isFilledBy(input) || !templateValue.isFilledBy(input)) {
			return false;
		}

		String filledVar = variable.fillSlots(input);
		Value actualValue = input.getValue(filledVar);

		Template filledTemplate = templateValue;
		Value expectedValue = groundValue;
		if (!filledTemplate.getSlots().isEmpty()) {
			filledTemplate = new Template(templateValue.fillSlots(input));
			if (!filledTemplate.isUnderspecified()) {
				expectedValue = ValueFactory.create(filledTemplate.toString());
			}
		}

		if (expectedValue != null) {
			return isSatisfiedBy(actualValue, expectedValue);
		} else {
			return isSatisfiedBy(actualValue, filledTemplate);
		}
	}

	/**
	 * Returns true if the relation is satisfied between the actual and expected
	 * values.
	 * 
	 * @param actualValue the actual value
	 * @param expectedValue the expected value
	 * @return true if satisfied, false otherwise
	 */
	private boolean isSatisfiedBy(Value actualValue, Value expectedValue) {
		switch (relation) {
		case EQUAL:
			return actualValue.equals(expectedValue);
		case UNEQUAL:
			return !actualValue.equals(expectedValue);
		case GREATER_THAN:
			return (actualValue.compareTo(expectedValue) > 0);
		case LOWER_THAN:
			return (actualValue.compareTo(expectedValue) < 0);
		case CONTAINS:
			return actualValue.contains(expectedValue);
		case NOT_CONTAINS:
			return !actualValue.contains(expectedValue);
		case LENGTH:
			return actualValue.length() == expectedValue.length();
		case IN:
			return expectedValue.contains(actualValue);
		case NOT_IN:
			return !expectedValue.contains(actualValue);
		}
		return false;
	}

	/**
	 * Returns true if the relation is satisfied between the actual and
	 * (template) expected values.
	 * 
	 * @param actualValue the actual value
	 * @param templateValue the templated expected value
	 * @return true if satisfied, false otherwise
	 */
	private boolean isSatisfiedBy(Value actualValue, Template templateValue) {
		switch (relation) {
		case EQUAL:
			return templateValue.match(actualValue.toString()).isMatching();
		case UNEQUAL:
			return !templateValue.match(actualValue.toString()).isMatching();
		case CONTAINS:
			return templateValue.partialmatch(actualValue.toString())
					.isMatching();
		case NOT_CONTAINS:
			return !templateValue.partialmatch(actualValue.toString())
					.isMatching();
		case LENGTH:
			return templateValue.match("" + actualValue.length()).isMatching();
		default:
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

		// case 1: the variable label is underspecified
		if (!variable.getSlots().isEmpty()
				&& !new Template(variable.fillSlots(input)).getSlots()
						.isEmpty()) {

			RuleGrounding groundings = new RuleGrounding();
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

		String filledVar = variable.fillSlots(input);
		Value actualValue = input.getValue(filledVar);

		Template filledTemplate = templateValue;
		Value expectedValue = groundValue;
		if (!filledTemplate.getSlots().isEmpty()) {
			filledTemplate = new Template(templateValue.fillSlots(input));
			if (!filledTemplate.isUnderspecified()) {
				expectedValue = ValueFactory.create(filledTemplate.toString());
			}
		}

		// case 2 : the expected value contains unfilled slots
		if (!filledTemplate.getSlots().isEmpty()) {

			RuleGrounding grounding = getGroundings(actualValue, filledTemplate);
			grounding.removeVariables(input.getVariables());
			grounding.removeValue(ValueFactory.none());
			return grounding;
		}

		else {
			// case 3: the expected value is a set and the relation is IN
			if (relation == Relation.IN && expectedValue instanceof SetVal) {
				return new RuleGrounding(filledVar,
						((SetVal) expectedValue).getSet());
			}

			// case 4: none of this applies (usual case)
			else if (isSatisfiedBy(input)) {
				return new RuleGrounding();
			}

			else {
				return new RuleGrounding.Failed();
			}
		}
	}

	/**
	 * Tries to match the template with the actual value, and returns the
	 * associated groundings
	 * 
	 * @param actualValue the actual filled value
	 * @param templateValue the template to match
	 * @return the resulting groundings
	 */
	private RuleGrounding getGroundings(Value actualValue,
			Template templateValue) {

		RuleGrounding grounding = new RuleGrounding();
		if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
			MatchResult m = templateValue.match(actualValue.toString());
			if (m.isMatching()) {
				grounding.add(m.getFilledSlots());
			}
		} else if (relation == Relation.CONTAINS
				&& actualValue instanceof SetVal) {
			for (Value subval : ((SetVal) actualValue).getSet()) {
				MatchResult m2 = templateValue.match(subval.toString());
				if (m2.isMatching()) {
					grounding.add(m2.getFilledSlots());
				}
			}
		} else if (relation == Relation.CONTAINS) {
			List<MatchResult> m2 = templateValue.find(actualValue.toString(),
					100);
			for (MatchResult match : m2) {
				grounding.add(match.getFilledSlots());
			}

		}

		if (grounding.isEmpty() && relation != Relation.UNEQUAL) {
			return new RuleGrounding.Failed();
		}
		return grounding;
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
			return variable + "=" + templateValue;
		case UNEQUAL:
			return variable + "!=" + templateValue;
		case GREATER_THAN:
			return variable + ">" + templateValue;
		case LOWER_THAN:
			return variable + "<" + templateValue;
		case CONTAINS:
			return variable + " contains " + templateValue;
		case NOT_CONTAINS:
			return variable + " does not contains " + templateValue;
		case LENGTH:
			return "length(" + variable + ")=" + templateValue;
		case IN:
			return variable + " in " + templateValue;
		case NOT_IN:
			return variable + " not in " + templateValue;
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
		return variable.hashCode() + templateValue.hashCode() - 3
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
		if (o instanceof BasicCondition) {
			return (((BasicCondition) o).getVariable().equals(variable)
					&& ((BasicCondition) o).templateValue.equals(templateValue) && relation == ((BasicCondition) o)
						.getRelation());
		}
		return false;
	}

}
