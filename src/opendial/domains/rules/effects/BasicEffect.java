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


import java.util.Collection;
import java.util.HashSet;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.BasicCondition.Relation;

/**
 * Representation of a basic effect of a rule.  A basic effect is formally
 * defined as a triple with: <ol>
 * <li> a variable label;
 * <li> one of four basic operations on the variable SET, DISCARD, ADD;
 * <li> a variable value;
 * </ol>
 * 
 * This class represented a usual, fully grounded effect.  For effects including
 * underspecified entities, use TemplateEffect.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicEffect {

	String variableLabel;
	
	Value variableValue;
	
	// enumeration of the four possible effect operations
	public static enum EffectType {
		SET, 		// for variable := value
		DISCARD, 	// for variable != value
		ADD, 		// for variable += value (add the value to the set)
	}
		
	EffectType type;

	int priority;
	
	// ===================================
	//  EFFECT CONSTRUCTION
	// ===================================
	
	
	/**
	 * Constructs a new basic effect, with a variable label, value, and type
	 * 
	 * @param variable variable label (raw string)
	 * @param value variable value (raw string)
	 * @param type type of effect
	 */
	public BasicEffect(String variable, String value, EffectType type){
		this(variable, ValueFactory.create(value), type);
	}
	
	/**
	 * Constructs a new basic effect, with a variable label, value, and type
	 * 
	 * @param variable variable label (raw string)
	 * @param value variable value
	 * @param type type of effect
	 */
	public BasicEffect(String variable, Value value, EffectType type){
		this.variableLabel = variable;
		this.variableValue = value;
		this.type = type;
	}

	
	/**
	 * Sets the priority level of the basic effect
	 * 
	 * @param priority priority level (1 is highest)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
	
	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns the effect type
	 * 
	 * @return the type
	 */
	public EffectType getType() {
		return type;
	}


	/**
	 * Returns the variable label for the basic effect
	 * 
	 * @return the variable label
	 */
	public String getVariable() {
		return variableLabel;
	}
	
	
	/**
	 * Returns the variable value for the basic effect.  If the variable value is 
	 * underspecified, returns the value None.
	 * 
	 * @return the variable value
	 */
	public Value getValue() {
		return variableValue;
	}


	
	/**
	 * Converts the basic effect into an equivalent condition.
	 * 
	 * @return the equivalent (basic or template-based) condition
	 */
	public Condition convertToCondition() {
		Relation r = (type == EffectType.DISCARD)? Relation.UNEQUAL : Relation.EQUAL;
			return new BasicCondition(variableLabel, variableValue, r);
	}
	
	
	/**
	 * Returns false.
	 * 
	 */
	public boolean containsSlots() {
		return false;
	}

	
	/**
	 * Returns itself.
	 */
	public BasicEffect ground(Assignment grounding) {
		return this;
	}

	/**
	 * Returns an empty set.
	 * @return
	 */
	public Collection<String> getSlots() {
		return new HashSet<String>();
	}

	
	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns the string representation of the basic effect
	 */
	@Override
	public String toString() {
		String str = variableLabel;
		switch (type) {
		case SET: str += ":="; break;
		case DISCARD: str += "!="; break;
		case ADD: str += "+="; break;
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
		return variableLabel.hashCode() - variableValue.hashCode() + type.hashCode();
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
			if (!((BasicEffect)o).getVariable().equals(variableLabel)) {
				return false;
			}
			else if (!((BasicEffect)o).getValue().equals(getValue())) {
				return false;
			}
			else if (!((BasicEffect)o).getType().equals(type)) {
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
		BasicEffect copy = new BasicEffect(variableLabel, variableValue, type);
		copy.priority = this.priority;
		return copy;
	}


}
