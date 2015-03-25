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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.ComplexCondition.BinaryOperator;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect.EffectType;


/**
 * A complex effect, represented as a combination of elementary sub-effects connected
 * via an implicit AND relation.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class Effect implements Value {

	// logger
	static Logger log = new Logger("Effect", Logger.Level.DEBUG);

	// the sub-effects included in the effect
	final Set<BasicEffect> subeffects;

	// ===================================
	//  EFFECT CONSTRUCTION
	// ===================================

	

	/**
	 * Creates a new complex effect with no effect
	 * 
	 * @param effect the effect to include
	 */
	public Effect() {
		subeffects = new HashSet<BasicEffect>();
	}
	

	/**
	 * Creates a new complex effect with a single effect
	 * 
	 * @param effect the effect to include
	 */
	public Effect(BasicEffect effect) {
		subeffects = new HashSet<BasicEffect>(Arrays.asList(effect));
	}
	
	/**
	 * Creates a new complex effect with a collection of existing effects
	 * 
	 * @param effects the effects to include
	 */
	public Effect(Collection<BasicEffect> effects) {
		subeffects = new HashSet<BasicEffect>(effects);
	}

	
	
	// ===================================
	//  GETTERS
	// ===================================

	
	
	/**
	 * Returns true if the effect is fully grounded, and false otherwise
	 * 
	 * @return true if fully grounded, false otherwise
	 */
	public boolean isFullyGrounded() {
		return subeffects.stream().allMatch(e -> !e.containsSlots());
	}
	
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
		if (isFullyGrounded()) {
			return this;
		}
		List<BasicEffect> grounded = subeffects.stream()
				.map(e -> e.ground(grounding))
				.filter(e -> !e.containsSlots())
				.collect(Collectors.toList());
		return new Effect(grounded);
	}
	
	
	@Override
	public Value concatenate (Value v) throws DialException {
		if (v instanceof Effect) {
			Collection<BasicEffect> effects = new ArrayList<BasicEffect>(subeffects);
			effects.addAll(((Effect)v).getSubEffects());
			return new Effect(effects);
		}
		else {
			throw new DialException("cannot concatenate " + this + " and " + v);
		}
	}

	

	/**
	 * Returns the additional input variables for the complex effect
	 * 
	 * @return the set of labels for the additional input variables
	 */
	public Set<String> getAdditionalInputVariables() {
		return subeffects.stream()
				.filter(e -> e.containsSlots())
				.flatMap(e -> e.getSlots().stream())
				.collect(Collectors.toSet());
	}

	
	/**
	 * Returns the output variables for the complex effect
	 * (including all the output variables for the sub-effects)
	 * 
	 * @return the set of all output variables
	 */
	public Set<String> getOutputVariables() {
		return subeffects.stream()
				.map(e -> e.getVariable())
				.collect(Collectors.toSet());
	}

	
	/**
	 * Returns the underspecified slots in the effect
	 * 
	 * @return the labels for the underspecified slots
	 */
	public Set<String> getSlots() {
		Set<String> slots = new HashSet<String>();
		for (BasicEffect e : subeffects) {
			if (e instanceof TemplateEffect) {
				slots.addAll(((TemplateEffect)e).getSlots());
			}
		}
		return slots;
	}
	
	/**
	 * Returns the set of values specified in the effect for the given variable and
	 * effect type.  The method accepts the effect types SET, DISCARD and ADD (the
	 * CLEAR effect does not return any value).
	 * 
	 * If several effects are defined with distinct priorities, only the effect
	 * with highest priority is retained.
	 * 
	 * @param variable the variable
	 * @param type the effect type
	 * @return the values specified in the effect
	 */
	public Set<Value> getValues(String variable, EffectType type) {
		Set<Value> result = new HashSet<Value>();
		int highestPriority = Integer.MAX_VALUE;
		for (BasicEffect e : subeffects) {
			if (e.getVariable().equals(variable) && e.getType() == type) {
				if (e.priority > highestPriority) {
					continue;
				}
				else if (e.priority < highestPriority) {
					result = new HashSet<Value>();
					highestPriority = e.priority;
				}
				if (!e.getValue().equals(ValueFactory.none())) {
					result.add(e.getValue());
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the effect types associated with the given variable name
	 * 
	 * @param variable the variable name
	 * @return the set of effect types used on the variable
	 */
	public Set<EffectType> getEffectTypes(String variable) {
		Set<EffectType> effectTypes = new HashSet<EffectType>();
		for (BasicEffect e : subeffects) {
			if (e.getVariable().equals(variable)) {
				effectTypes.add(e.getType());
			}
		}
		return effectTypes;
	}
	

	
	public Condition convertToCondition() {
		List<Condition> conditions = new ArrayList<Condition>();
		for (BasicEffect subeffect : getSubEffects()) {
			conditions.add(subeffect.convertToCondition());
		}
		if (conditions.isEmpty()) {
			return new VoidCondition();
		}
		else if (conditions.size() == 1) {
			return conditions.get(0);
		}
		else {
			return new ComplexCondition(conditions, (this.getOutputVariables().size() == 1)? 
					BinaryOperator.OR : BinaryOperator.AND);
		}
	}
	
	
	// ===================================
	//  UTILITY FUNCTIONS
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
		for (BasicEffect e: subeffects) {
			str += e.toString() + " ^ ";
		}
		return (!subeffects.isEmpty())? str.substring(0, str.length()-3) : "Void";
	}

	
	/**
	 * Returns a copy of the effect
	 * 
	 * @return the copy
	 */
	@Override
	public Effect copy() {
		return new Effect(subeffects.stream().map(e -> e.copy()).collect(Collectors.toList()));
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
	 * Parses the string representing the effect, and returns the corresponding effect.
	 * 
	 * @param str the string representing the effect
	 * @return the corresponding effect
	 */
	public static Effect parseEffect(String str) {
		if (str.contains(" ^ ")) {
			List<BasicEffect> effects = new ArrayList<BasicEffect>();
			for (String split : str.split(" \\^ ")) {
				Effect subOutput = parseEffect (split);
				effects.addAll(subOutput.getSubEffects());
			}
			return new Effect(effects);
		}
		else {
			if (str.contains("Void")) {
				return new Effect(new ArrayList<BasicEffect>());
			}
			
			EffectType type = EffectType.SET;
			String var = "";
			String val = "";
			if (str.contains(":=")) {
				var = str.split(":=")[0];
				val = str.split(":=")[1];
				val = (val.contains("{}"))? "None": val;
				type = EffectType.SET;
			}
			else if (str.contains("!=")) {
				var = str.split("!=")[0];
				val = str.split("!=")[1];
				type = EffectType.DISCARD;
			}
			else if (str.contains("+=")) {
				var = str.split("\\+=")[0];
				val = str.split("\\+=")[1];
				type = EffectType.ADD;
			}
			Template tvar = new Template(var);
			Template tval = new Template(val);
			if (tvar.isUnderspecified() || tval.isUnderspecified()) {
				return new Effect(new TemplateEffect(tvar, tval, type));
			}
			else {
				return new Effect(new BasicEffect(var, val, type));
			}
		}	
	}


}
