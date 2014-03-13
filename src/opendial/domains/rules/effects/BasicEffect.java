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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;

/**
 * Representation of a basic effect of a rule.  A basic effect is formally
 * defined as a triple with: <ol>
 * <li> a (possibly underspecified) variable label;
 * <li> one of four basic operations on the variable SET, DISCARD, ADD, REMOVE;
 * <li> a (possibly underspecified) variable value;
 * </ol>
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicEffect {

	static Logger log = new Logger("BasicEffect", Logger.Level.DEBUG);
	
	// variable label for the basic effect
	Template variableLabel;
	
	// variable value for the basic effect
	Template variableValue;

	// enumeration of the four possible effect operations
	public static enum EffectType {
		SET, 		// for variable := value
		DISCARD, 	// for variable != value
		ADD, 		// for variable += value (add the value to the set)
		CLEAR,  	// clearing the variable (NB: variableValue is then ignored)
	}
	
	// effect type for the basic effect
	EffectType type;
	

	// ===================================
	//  EFFECT CONSTRUCTION
	// ===================================
	
	
	/**
	 * Constructs a new basic effect, with a variable label, value, and type
	 * 
	 * @param variable variable label (raw string, possibly with slots)
	 * @param value variable value (raw string, possibly with slots)
	 * @param type type of effect
	 */
	public BasicEffect(Template variable, Template value, EffectType type){
		this.variableLabel = variable;
		this.variableValue = (value != null)? value : new Template("");
		this.type = type;
	}


	/**
	 * Ground the slots in the effect variable and value (given the assignment)
	 * and returns the resulting effect.
	 * 
	 * @param grounding the grounding
	 * @return the grounded effect
	 */
	public BasicEffect ground(Assignment grounding) {
		if (!variableLabel.isUnderspecified() && !variableValue.isUnderspecified()) {
			return this;
		}
		Template newT = variableLabel.fillSlots(grounding);
		Template newV = variableValue.fillSlots(grounding);
		return new BasicEffect(newT, newV, type);
	}
	
	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns the set of additional input variables for the effect (from slots 
	 * in the variable label and value).
	 *
	 * @return sets of input variables
	 */
	public Set<String> getAdditionalInputVariables() {
		Set<String> additionalInputVariables = new HashSet<String>();
		additionalInputVariables.addAll(this.variableLabel.getSlots());
		additionalInputVariables.addAll(this.variableValue.getSlots());
		return additionalInputVariables;
		}


	/**
	 * Returns the variable label for the basic effect
	 * 
	 * @return the variable label
	 */
	public Template getVariable() {
		return variableLabel;
	}
	
	
	/**
	 * Returns the variable value for the basic effect.  If the variable value is 
	 * underspecified, returns the value None.
	 * 
	 * @return the variable value
	 */
	public Value getValue() {
		return (!variableValue.isUnderspecified())? 
				ValueFactory.create(variableValue.getRawString()) : ValueFactory.none();
	}
	
	
	/**
	 * Returns the variable value as a template.
	 * 
	 * @return the variable value (as a template).
	 */
	public Template getTemplateValue() {
		return variableValue;
	}
	

	/**
	 * Returns the effect type
	 * 
	 * @return the effect type
	 */
	public EffectType getType() {
		return type;
	}
	

	/**
	 * Returns true if the effect is fully grounded (i.e. does not include any
	 * underspecified slots in the variable or value), and false otherwise.
	 * 
	 * @return true if the effect is fully grounded, and false otherwise.
	 */
	public boolean isFullyGrounded() {
		return (variableLabel.getSlots().isEmpty() && variableValue.getSlots().isEmpty());
	}

	
	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns the string representation of the basic effect
	 */
	@Override
	public String toString() {
		String str = variableLabel.toString();
		switch (type) {
		case SET: str += ":="; break;
		case DISCARD: str += "!="; break;
		case ADD: str += "+="; break;
		case CLEAR : return "clear " + variableLabel.toString();
		}
		str += variableValue.toString();
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
			else if (!((BasicEffect)o).getTemplateValue().equals(getTemplateValue())) {
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
		return new BasicEffect(variableLabel, variableValue, type);
	}

	
}
