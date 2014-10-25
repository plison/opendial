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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.effects.BasicEffect.EffectType;


/**
 * A complex effect, represented as a combination of elementary sub-effects connected
 * via an implicit AND relation.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Effect implements Value {

	// logger
	static Logger log = new Logger("Effect", Logger.Level.DEBUG);

	// the sub-effects included in the effect
	List<BasicEffect> subeffects;

	// ===================================
	//  EFFECT CONSTRUCTION
	// ===================================

	
	/**
	 * Creates a new, empty complex effect
	 */
	public Effect() {
		subeffects = new ArrayList<BasicEffect>();
	}
	
	/**
	 * Creates a new complex effect with a collection of existing effects
	 * 
	 * @param effects the effects to include
	 */
	public Effect(Collection<BasicEffect> effects) {
		this();
		addSubEffects(effects);
	}

	/**
	 * Adds a new sub-effect in the complex effect
	 * 
	 * @param effect the effect to add
	 */
	public void addSubEffect(BasicEffect effect) {
		if (!subeffects.contains(effect)) {
			subeffects.add(effect);
		}
	}


	public void addSubEffects(Collection<BasicEffect> effects) {
		for (BasicEffect effect : effects) {
			addSubEffect(effect);
		}
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
		for (BasicEffect e : subeffects) {
			if (!e.isFullyGrounded()) {
				return false;
			}
		}
		return true;
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
	public Effect getGrounded(Assignment grounding) {
		if (isFullyGrounded()) {
			return this;
		}
	
		Effect effect = new Effect();
		for (BasicEffect e : subeffects) {
			BasicEffect groundedE = e.ground(grounding);
			if (groundedE.isFullyGrounded()) {
				effect.addSubEffect(groundedE);
			}
		}
		return effect;
	}
	
	
	@Override
	public Value concatenate (Value v) throws DialException {
		if (v instanceof Effect) {
			Effect newEffect = copy();
			newEffect.addSubEffects(((Effect)v).getSubEffects());
			return newEffect;
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
		Set<String> variables = new HashSet<String>();
		for (BasicEffect e : subeffects) {
			variables.addAll(e.getAdditionalInputVariables());
		}
		return variables;
	}

	
	/**
	 * Returns the output variables for the complex effect
	 * (including all the output variables for the sub-effects)
	 * 
	 * @return the set of all output variables
	 */
	public Set<String> getOutputVariables() {
		Set<String> variables = new HashSet<String>();
		for (BasicEffect e : subeffects) {
			if (e.isFullyGrounded()) {
				variables.add(e.getVariable().getRawString());
			}
		}
		return variables;
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
			if (e.getVariable().getRawString().equals(variable) && e.getType() == type 
					&& !e.getValue().equals(ValueFactory.none())) {
				if (e.priority > highestPriority) {
					continue;
				}
				else if (e.priority < highestPriority) {
					result = new HashSet<Value>();
					highestPriority = e.priority;
				}
				result.add(e.getValue());
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
			if (e.getVariable().getRawString().equals(variable)) {
				effectTypes.add(e.getType());
			}
		}
		return effectTypes;
	}
	

	
	public Condition convertToCondition() {
		ComplexCondition condition = new ComplexCondition();
		for (BasicEffect subeffect : getSubEffects()) {
			condition.addCondition(subeffect.convertToCondition());
		}
		return condition;
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
		Effect effect = new Effect();
		for (BasicEffect e : subeffects) {
			effect.addSubEffect(e.copy());
		}
		return effect;
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
		Effect o = new Effect();
		if (str.contains(" ^ ")) {
			for (String split : str.split(" \\^ ")) {
				Effect subOutput = parseEffect (split);
				o.addSubEffects(subOutput.getSubEffects());
			}
		}
		else {
			if (str.contains("Void")) {
				return new Effect();
			}
			if (str.contains(":=") && str.contains("{}")) {
				String var = str.split(":=")[0];
				o.addSubEffect(new BasicEffect(new Template(var), new Template("None"), EffectType.SET));
			}
			else if (str.contains(":=")) {
				String var = str.split(":=")[0];
				String val = str.split(":=")[1];
				o.addSubEffect(new BasicEffect(new Template(var), new Template(val), EffectType.SET));
			}
			else if (str.contains("!=")) {
				String var = str.split("!=")[0];
				String val = str.split("!=")[1];
				o.addSubEffect(new BasicEffect(new Template(var), new Template(val), EffectType.DISCARD));
			}
			else if (str.contains("+=")) {
				String var = str.split("\\+=")[0];
				String val = str.split("\\+=")[1];
				o.addSubEffect(new BasicEffect(new Template(var), new Template(val), EffectType.ADD));
			}
		}	
		return o;
	}

	

}
