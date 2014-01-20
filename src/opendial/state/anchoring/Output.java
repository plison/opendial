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

package opendial.state.anchoring;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.RuleCase;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.Parameter;


/**
 * Representation of a particular output derived from the application of a probabilistic 
 * rule.  The output essentially contains a (parametrised) distribution over possible 
 * effects. If the rule contains multiple groundings, the output is a merge (joint probability
 * distribution for a probability rule, additive table for a utility rule) of the rule
 * case for every possible groundings.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class Output {

	// the rule effects
	Map<Effect, Parameter> effects;
	
	// the rule type
	RuleType type;

	/**
	 * Creates an empty output for a particular rule type.
	 * 
	 * @param type the rule type
	 */
	public Output(RuleType type) {
		effects = new HashMap<Effect, Parameter>();
		this.type = type;
	}

	/**
	 * Adds a rule case to the output.  The result is a joint probability distribution
	 * in the case of a probability rule, and an additive table in the case of a utility
	 * rule.
	 * 
	 * @param newCase the new rule case to add
	 */
	public void addCase(RuleCase newCase) {

		if (newCase.getEffects().isEmpty()) { return;	}
		else if (effects.hashCode() == newCase.getEffectMap().hashCode()) { return ; }
		else if (effects.isEmpty()) {
			effects.putAll(newCase.getEffectMap());
			return;
		}
		
		if (type == RuleType.PROB) {
			Map<Effect,Parameter> newOutput = new HashMap<Effect,Parameter>();
			for (Effect o : effects.keySet()) {
				for (Effect o2 : newCase.getEffects()) {
					Effect newEffect = new Effect(o.getSubEffects());
					newEffect.addSubEffects(o2.getSubEffects());
					Parameter mergeParam = effects.get(o).multiplyParameter(newCase.getParameter(o2));
					newOutput.put(newEffect, mergeParam);
				}
			}
			effects = newOutput;
		}
		else if (type == RuleType.UTIL){
			for (Effect o2: newCase.getEffects()) {
				if (effects.containsKey(o2)) {
					Parameter mergeParam = effects.get(o2).sumParameter(newCase.getParameter(o2));
					effects.put(o2, mergeParam);
				}
				else {
					effects.put(o2, newCase.getParameter(o2));
				}
			}
		}
	}
	
	
	/**
	 * Returns the effects in the output
	 * 
	 * @return the possible effects
	 */
	public Set<Effect> getEffects() {
		return effects.keySet();
	}

	/**
	 * Returns the parameters employed in the output for the effect
	 * 
	 * @param effect the effect
	 * @return the associated parameter
	 */
	public Parameter getParameter(Effect effect) {
		return effects.get(effect);
	}
	
	
	
	/**
	 * Returns the total probability mass specified by the output (possibly given
	 * an assignment of parameter values).
	 * 
	 * @param input input assignment (with parameters values)
	 * @return the corresponding mass
	 * @throws DialException
	 */
	public double getTotalMass(Assignment input) throws DialException {
		double mass = 0;
		for (Parameter param : effects.values()) {
			double paramValue = param.getParameterValue(input);
			if (paramValue > 0) {
				mass += param.getParameterValue(input);
			}
		}
		return mass;
	}
	


	/**
	 * Returns a string representation of the output
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
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
	 * Returns the hashcode for the output
	 */
	@Override
	public int hashCode() {
		return effects.hashCode();
	}


}