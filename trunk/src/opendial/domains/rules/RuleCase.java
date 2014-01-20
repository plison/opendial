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

package opendial.domains.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;


/**
 * Representation of a rule case, containing a condition and a list of alternative 
 * effects if the condition holds.  Each alternative effect has a distinct 
 * probability or utility parameter.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleCase {

	// logger
	static Logger log = new Logger("Case", Logger.Level.DEBUG);

	// the condition for the case
	Condition condition;
	
	// the list of alternative effects, together with their probability/utility
	Map<Effect,Parameter> effects;
	
	
	// ===================================
	//  CASE CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new case, with a void condition and an empty list of
	 * effects
	 */
	public RuleCase() {
		condition = new VoidCondition();
		effects = new HashMap<Effect,Parameter>();
	}
	
	
	/**
	 * Creates a new case, with the given condition and an empty list
	 * of effects
	 * 
	 * @param condition the condition for the case
	 */
	public RuleCase(Condition condition) {
		this.condition = condition;
		effects = new HashMap<Effect,Parameter>();
	}
	
	/**
	 * Sets the condition for the case (if a condition is already specified,
	 * it is erased)
	 * 
	 * @param condition the condition
	 */
	public void setCondition(Condition condition) {
		this.condition = condition;
	}
	
	/**
	 * Adds an new effect and its associated probability/utility to the case
	 * 
	 * @param effect the effect
	 * @param param the effect's probability or utility
	 * @throws DialException 
	 */
	public void addEffect(Effect effect, double param) {
		addEffect(effect, new FixedParameter(param));
	}
	
	
	
	/**
	 * Adds a new effect and its associated parameter to the case
	 * 
	 * @param effect the effect
	 * @param param the parameter for the effect's probability or utility
	 */
	public void addEffect(Effect effect, Parameter param) {
		effects.put(effect, param);
	}

	
	/**
	 * Returns a grounded version of the rule case, based on the grounding
	 * assignment.
	 * 
	 * @param grounding the grounding associated with the filled values
	 * @return the grounded copy of the case.
	 */
	public RuleCase ground(Assignment grounding) {
		RuleCase groundCase = new RuleCase();
		for (Effect e : effects.keySet()) {
			Effect groundedEffect = e.ground(grounding);
			if (!groundedEffect.getSubEffects().isEmpty() || e.getSubEffects().isEmpty()) {
				groundCase.addEffect(groundedEffect, effects.get(e));
			}
		}
		return groundCase;
	}


	
	// ===================================
	//  GETTERS
	// ===================================

	
	
	/**
	 * Returns all the effects specified in the case.
	 * 
	 * @return the set of effects
	 */
	public Set<Effect> getEffects() {
		return effects.keySet();
	}
	
	
	/**
	 * Returns the parameter associated with the effect.  If the effect is not part of
	 * the case, returns null.
	 * 
	 * @param e the effect
	 * @return the parameter associated with the effect.
	 */
	public Parameter getParameter(Effect e) {
		return effects.get(e);
	}
	
	
	
	/**
	 * Returns the condition for the case
	 * 
	 * @return the condition
	 */
	public Condition getCondition() {
		return condition;
	}
	
	

	/**
	 * Returns the input variables for the case, composed of the input variables
	 * for the condition, plus the additional input variables for the effects.
	 * 
	 * @return the set of input variables for the case
	 */
	public Set<Template> getInputVariables() {
		Set<Template> inputVariables = new HashSet<Template>();
		inputVariables.addAll(condition.getInputVariables());
		for (Effect effect : effects.keySet()) {
			for (String inputVariable: effect.getAdditionalInputVariables()) {
				inputVariables.add(new Template(inputVariable));
			}
		}
		return inputVariables;
	}


	/**
	 * Returns the set of output variables for the case, as defined in the 
	 * effects associated with the case condition.
	 * 
	 * @return the set of output variables defined in the case's effects
	 */
	public Set<String> getOutputVariables() {
		Set<String> outputVariables = new HashSet<String>();
		for (Effect effect : effects.keySet()) {
			outputVariables.addAll(effect.getOutputVariables());
		}
		return outputVariables;
	}
	
	
	/**
	 * Returns the possible groundings for the case, based on the provided
	 * input assignment.
	 * 
	 * @param input the input assignment
	 * @return the set of possible groundings
	 */
	public ValueRange getGroundings(Assignment input) {
		return condition.getGroundings(input);
	}
	
	/**
	 * Returns the mapping between effects and parameters for the case.
	 * 
	 * @return the mapping effects -> parameters
	 */
	public Map<Effect,Parameter> getEffectMap() {
		return effects;
	}
	

	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns a string representation of the rule case.
	 */
	@Override
	public String toString() {
		String str = "";
		if (!(condition instanceof VoidCondition)) {
			str += "if (" + condition.toString() + ") then ";	
		}
		else {
			str += " ";
		}
		for (Effect e : effects.keySet()) {
			str += e.toString();
			str += " [" + effects.get(e) + "]";
			str += ",";
		}
		if (!effects.isEmpty()) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}


	/**
	 * Returns the hashcode for the case
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return condition.hashCode() - 2 * effects.hashCode();
	}


	
	/**
	 * Returns true if the object is a identical rule case, and
	 * false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof RuleCase 
				&& condition.equals(((RuleCase)o).condition) 
				&& effects.equals(((RuleCase)o).effects));
	}



	
}
