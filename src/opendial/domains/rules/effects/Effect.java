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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.ComplexCondition.BinaryOperator;
import opendial.templates.Template;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;

/**
 * A complex effect, represented as a combination of elementary sub-effects connected
 * via an implicit AND relation.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class Effect implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the sub-effects included in the effect
	final List<BasicEffect> subeffects;

	// the table of (grounded) values for the effect
	final Map<String, Map<Value, Double>> valueTable;

	// "equivalent" condition (inverse view)
	Condition equivalentCondition;

	// whether the effect is fully grounded or not
	final boolean fullyGrounded;

	// underspecified slots of the form "{randomX}"
	final Set<String> randomsToGenerate;
	// ===================================
	// EFFECT CONSTRUCTION
	// ===================================

	/**
	 * Creates a new complex effect with no effect
	 * 
	 */
	public Effect() {
		subeffects = new ArrayList<BasicEffect>();
		fullyGrounded = true;
		randomsToGenerate = new HashSet<String>();
		valueTable = new HashMap<String, Map<Value, Double>>();
	}

	/**
	 * Creates a new complex effect with a single effect
	 * 
	 * @param effect the effect to include
	 */
	public Effect(BasicEffect effect) {
		subeffects = Arrays.asList(effect);
		fullyGrounded = !effect.containsSlots();
		valueTable = new HashMap<String, Map<Value, Double>>();
		randomsToGenerate = new HashSet<String>();
		if (effect instanceof TemplateEffect) {
			((TemplateEffect) effect).getAllSlots().stream()
					.filter(s -> s.startsWith("random"))
					.forEach(s -> randomsToGenerate.add(s));
		}
	}

	/**
	 * Creates a new complex effect with a collection of existing effects
	 * 
	 * @param effects the effects to include
	 */
	public Effect(Collection<BasicEffect> effects) {

		subeffects = new ArrayList<BasicEffect>(effects);
		fullyGrounded = subeffects.stream().allMatch(e -> !e.containsSlots());
		valueTable = new HashMap<String, Map<Value, Double>>();

		randomsToGenerate = new HashSet<String>();
		for (BasicEffect effect : effects) {
			if (effect instanceof TemplateEffect) {
				((TemplateEffect) effect).getAllSlots().stream()
						.filter(s -> s.startsWith("random"))
						.forEach(s -> randomsToGenerate.add(s));
			}
		}
	}

	/**
	 * Creates a new complex effect with a collection of existing effects
	 * 
	 * @param effects the effects to include
	 */
	public Effect(Effect... effects) {
		this(Arrays.stream(effects).flatMap(e -> e.getSubEffects().stream())
				.collect(Collectors.toList()));
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns all the sub-effect included in the complex effect
	 * 
	 * @return the collection of sub-effects
	 */
	public Collection<BasicEffect> getSubEffects() {
		return subeffects;
	}

	/**
	 * Grounds the effect with the given assignment.
	 * 
	 * @param grounding the assignment containing the filled values
	 * @return the resulting grounded effect
	 */
	public Effect ground(Assignment grounding) {
		if (fullyGrounded) {
			return this;
		}
		List<BasicEffect> grounded = subeffects.stream()
				.map(e -> e.ground(grounding)).filter(e -> !e.containsSlots())
				.collect(Collectors.toList());
		return new Effect(grounded);
	}

	@Override
	public Value concatenate(Value v) {
		if (v instanceof Effect) {
			Collection<BasicEffect> effects = new ArrayList<BasicEffect>(subeffects);
			effects.addAll(((Effect) v).getSubEffects());
			return new Effect(effects);
		}
		else {
			throw new RuntimeException("cannot concatenate " + this + " and " + v);
		}
	}

	/**
	 * Returns the additional input variables for the complex effect
	 * 
	 * @return the set of labels for the additional input variables
	 */
	public Set<String> getValueSlots() {
		return subeffects.stream().filter(e -> e instanceof TemplateEffect)
				.map(e -> ((TemplateEffect) e).getValueTemplate())
				.flatMap(t -> t.getSlots().stream()).collect(Collectors.toSet());
	}

	/**
	 * Returns the output variables for the complex effect (including all the output
	 * variables for the sub-effects)
	 * 
	 * @return the set of all output variables
	 */
	public Set<String> getOutputVariables() {
		return subeffects.stream().map(e -> e.getVariable())
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the set of values specified in the effect for the given variable.
	 * 
	 * If several effects are defined with distinct priorities, only the effect with
	 * highest priority is retained.
	 * 
	 * @param variable the variable
	 * @return the values specified in the effect
	 */
	public Map<Value, Double> getValues(String variable) {
		if (!valueTable.containsKey(variable)) {
			valueTable.put(variable, createTable(variable));
		}
		return valueTable.get(variable);
	}

	/**
	 * Returns true if all of the included effects for the variable are not mutually
	 * exclusive (allowing multiple values).
	 * 
	 * @param variable the variable to check
	 * @return true if the effects are not mutually exclusive, else false
	 */
	public boolean isNonExclusive(String variable) {
		boolean nonExclusive = false;
		for (BasicEffect e : subeffects) {
			if (e.getVariable().equals(variable)) {
				if (!e.isExclusive()) {
					nonExclusive = true;
				}
				else if (e.getValue().length() > 0 && !e.isNegated()) {
					return false;
				}
			}
		}
		return nonExclusive;
	}

	/**
	 * Converts the effect into a condition.
	 * 
	 * @return the corresponding condition
	 */
	public Condition convertToCondition() {
		if (equivalentCondition == null) {
			List<Condition> conditions = new ArrayList<Condition>();
			for (BasicEffect subeffect : getSubEffects()) {
				conditions.add(subeffect.convertToCondition());
			}
			if (conditions.isEmpty()) {
				equivalentCondition = new VoidCondition();
			}
			else if (conditions.size() == 1) {
				equivalentCondition = conditions.get(0);
			}
			else if (getOutputVariables().size() == 1) {
				equivalentCondition =
						new ComplexCondition(conditions, BinaryOperator.OR);
			}
			else {
				equivalentCondition =
						new ComplexCondition(conditions, BinaryOperator.AND);
			}
		}
		return equivalentCondition;
	}

	/**
	 * Returns the number of basic effects
	 * 
	 * @return the number of effects
	 */
	@Override
	public int length() {
		return subeffects.size();
	}

	/**
	 * Returns the effect as an assignment of values. The variable labels are ended
	 * by a prime character.
	 * 
	 * @return the assignment of new values to the variables
	 */
	public Assignment getAssignment() {
		Assignment a = new Assignment();
		for (BasicEffect e : subeffects) {
			if (!e.negated) {
				a.addPair(e.getVariable() + "'", e.getValue());
			}
			else {
				a.addPair(e.getVariable() + "'", ValueFactory.none());
			}
		}
		return a;
	}

	/**
	 * Returns the slots of the form {randomX}, which are to be replaced by random
	 * integers.
	 * 
	 * @return the set of slots to replace by random integers
	 */
	public Set<String> getRandomsToGenerate() {
		return randomsToGenerate;
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Returns the hashcode for the complex effect
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return subeffects.hashCode();
	}

	/**
	 * Returns true if the object is a complex effect with an identical content
	 *
	 * @param o the object to compare
	 * @return true if the objects are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return o.hashCode() == hashCode();
	}

	/**
	 * Returns a string representation for the effect
	 */
	@Override
	public String toString() {
		String str = "";
		for (BasicEffect e : subeffects) {
			str += e.toString() + " ^ ";
		}
		return (!subeffects.isEmpty()) ? str.substring(0, str.length() - 3) : "Void";
	}

	/**
	 * Returns a copy of the effect
	 * 
	 * @return the copy
	 */
	@Override
	public Effect copy() {
		return new Effect(
				subeffects.stream().map(e -> e.copy()).collect(Collectors.toList()));
	}

	/**
	 * Returns false.
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}

	/**
	 * Compares the effect with another value (based on their hashcode).
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	/**
	 * Returns an empty list
	 */
	@Override
	public Collection<Value> getSubValues() {
		return new ArrayList<Value>();
	}

	/**
	 * Parses the string representing the effect, and returns the corresponding
	 * effect.
	 * 
	 * @param str the string representing the effect
	 * @return the corresponding effect
	 */
	public static Effect parseEffect(String str) {
		if (str.contains(" ^ ")) {
			List<BasicEffect> effects = new ArrayList<BasicEffect>();
			for (String split : str.split(" \\^ ")) {
				Effect subOutput = parseEffect(split);
				effects.addAll(subOutput.getSubEffects());
			}
			return new Effect(effects);
		}
		else {
			if (str.contains("Void")) {
				return new Effect(new ArrayList<BasicEffect>());
			}

			String var = "";
			String val = "";
			boolean exclusive = true;
			boolean negated = false;

			if (str.contains(":=")) {
				var = str.split(":=")[0];
				val = str.split(":=")[1];
				val = (val.contains("{}")) ? "None" : val;
			}
			else if (str.contains("!=")) {
				var = str.split("!=")[0];
				val = str.split("!=")[1];
				negated = true;
			}
			else if (str.contains("+=")) {
				var = str.split("\\+=")[0];
				val = str.split("\\+=")[1];
				exclusive = false;
			}
			Template tvar = Template.create(var);
			Template tval = Template.create(val);
			if (tvar.isUnderspecified() || tval.isUnderspecified()) {
				return new Effect(
						new TemplateEffect(tvar, tval, 1, exclusive, negated));
			}
			else {
				return new Effect(new BasicEffect(var, ValueFactory.create(val), 1,
						exclusive, negated));
			}
		}
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	/**
	 * Extracts the values (along with their weight) specified in the effect.
	 * 
	 * @param variable the variable
	 * @return the corresponding values
	 */
	private Map<Value, Double> createTable(String variable) {

		Map<Value, Double> values = new HashMap<Value, Double>();
		// first pass
		int maxPriority = Integer.MAX_VALUE;
		Set<Value> toRemove = new HashSet<Value>();
		toRemove.add(ValueFactory.none());
		for (BasicEffect e : subeffects) {
			if (e.getVariable().equals(variable)) {
				if (e.priority < maxPriority) {
					maxPriority = e.priority;
				}
				if (e.negated) {
					toRemove.add(e.getValue());
				}
			}
		}
		// second pass
		for (BasicEffect e : subeffects) {
			String var = e.getVariable();
			if (var.equals(variable)) {
				Value value = e.getValue();
				if (e.priority > maxPriority || e.negated
						|| toRemove.contains(value)) {
					continue;
				}
				if (toRemove.size() > 1 && value instanceof SetVal) {
					Collection<Value> subvalues = ((SetVal) value).getSubValues();
					subvalues.removeAll(toRemove);
					value = ValueFactory.create(subvalues);
				}
				values.merge(value, e.weight, (w1, w2) -> w1 + w2);
			}
		}
		return values;
	}

}
