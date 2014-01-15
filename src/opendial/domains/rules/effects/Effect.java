// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.domains.rules.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
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
	
	
	/**
	 * Grounds the effect with the given assignment.
	 * 
	 * @param grounding the assignment containing the filled values
	 * @return the resulting grounded effect
	 */
	public Effect ground(Assignment grounding) {
		Effect effect = new Effect();
		for (BasicEffect e : subeffects) {
			BasicEffect groundedE = e.ground(grounding);
			if (groundedE.isFullyGrounded()) {
				effect.addSubEffect(groundedE);
			}
		}
		return effect;
	}

	// ===================================
	//  GETTERS
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
	 * @param variable the variable
	 * @param type the effect type
	 * @return the values specified in the effect
	 */
	public Set<Value> getValues(String variable, EffectType type) {
		Set<Value> result = new HashSet<Value>();
		for (BasicEffect e : subeffects) {
			if (e.getVariable().getRawString().equals(variable) && e.getType() == type 
					&& !e.getValue().equals(ValueFactory.none())) {
				result.add(e.getValue());
			}
		}
		return result;
	}
	
	/**
	 * Returns the set of variables that must be cleared (according to the effect).
	 * 
	 * @return the set of variables to clear.
	 */
	public Set<String> getClearVariables() {
		Set<String> result = new HashSet<String>();
		for (BasicEffect e : subeffects) {
			if (e.getType() == EffectType.CLEAR) {
				result.add(e.getVariable().getRawString());
			}
		}
		return result;
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
				o.addSubEffect(new BasicEffect(new Template(var), null, EffectType.CLEAR));
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
