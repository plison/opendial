// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.TemplateString;

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
public class BasicEffect implements Effect {

	static Logger log = new Logger("BasicEffect", Logger.Level.DEBUG);
	
	// variable label for the basic effect
	TemplateString variableLabel;
	
	// variable value for the basic effect
	TemplateString variableValue;

	// enumeration of the four possible effect operations
	public static enum EffectType {
		SET, 		// for variable := value
		DISCARD, 	// for variable != value
		ADD, 		// for variable += value (add the value to the set)
	}
	
	// effect type for the basic effect
	EffectType type;
	
	// input variables for the effect (from slots in the variable label and/or value)
	Set<String> additionalInputVariables;
	

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
	public BasicEffect(String variable, String value, EffectType type){
		this.variableLabel = new TemplateString(variable);
		this.variableValue = new TemplateString(value);
		additionalInputVariables = new HashSet<String>();
		additionalInputVariables.addAll(this.variableLabel.getSlots());
		additionalInputVariables.addAll(this.variableValue.getSlots());
		this.type = type;
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
	@Override
	public Set<String> getAdditionalInputVariables() {
		return new HashSet<String>(additionalInputVariables);
	}


	/**
	 * Return the set of output variables for the effect (here, the variable label)
	 * 
	 * @return a singleton set with the variable label
	 */
	@Override
	public Set<TemplateString> getOutputVariables() {
		Set<TemplateString> variables = new HashSet<TemplateString>();
		variables.add(variableLabel);
		return variables;
	}


	/**
	 * Returns the output of the effect, given the additional input as argument.
	 * 
	 * @param additionalInput the additional input to fill slots 
	 *        in the variable or value
	 * @return the output created by the effect
	 */
	@Override
	public Output createOutput(Assignment additionalInput) {
		
		Output output = new Output();
		
		TemplateString filledVariable = variableLabel.fillSlotsPartial(additionalInput);
		TemplateString filledValue = variableValue.fillSlotsPartial(additionalInput);
		
		// check if all the slots are filled
		if (filledVariable.getSlots().isEmpty() && filledValue.getSlots().isEmpty()) {
			
			String variable = filledVariable.getRawString();
			Value value = ValueFactory.create(filledValue.getRawString());
			switch (type) {
			case SET: output.setValueForVariable(variable,value); break;
			case DISCARD: output.discardValueForVariable(variable,value); break;
			case ADD: output.addValueForVariable(variable,value); break;
			}
			
		}
		return output;
	}
	

	/**
	 * Returns the variable label for the basic effect
	 * 
	 * @return the variable label
	 */
	public TemplateString getVariable() {
		return variableLabel;
	}
	
	
	/**
	 * Returns the variable value for the basic effect
	 * 
	 * @return the variable value
	 */
	public TemplateString getValue() {
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
	
	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns the string representation of the basic effect
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = variableLabel.toString();
		switch (type) {
		case SET: str += ":="; break;
		case DISCARD: str += "!="; break;
		case ADD: str += "+="; break;
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
			else if (!((BasicEffect)o).getValue().equals(variableValue)) {
				return false;
			}
			else if (!((BasicEffect)o).getType().equals(type)) {
				return false;
			}
			return true;
		}
		return false;
	}

	
}
