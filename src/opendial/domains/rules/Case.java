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

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.rules.parameters.DirichletParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.state.rules.Rule.RuleType;


/**
 * Representation of a rule case, containing a condition and a list of alternative 
 * effects if the condition holds.  Each alternative effect has a distinct 
 * probability or utility parameter.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-01-03 16:02:01 #$
 *
 */
public class Case {

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
	public Case() {
		condition = new VoidCondition();
		effects = new HashMap<Effect,Parameter>();
	}
	
	
	/**
	 * Creates a new case, with the given condition and an empty list
	 * of effects
	 * 
	 * @param condition the condition for the case
	 */
	public Case(Condition condition) {
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
	public void addEffect(Effect effect, float param) {
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
	

	
	// ===================================
	//  GETTERS
	// ===================================
	
	

	public OutputTable getEffectOutputs(Assignment input) {
		
		OutputTable outputs = new OutputTable();
			
		// add up the local condition output and the remaining input 
		Assignment localOutput = getCondition().getLocalOutput(input);
		Assignment totalInput = new Assignment(input, localOutput);
				
		// fill up the mapping with the outputs
		for (Effect effect : effects.keySet()) {
			
			Output output = effect.createOutput(totalInput);
			outputs.addOutput(output, effects.get(effect));
		}
		
		return outputs;
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
	 * Returns the list of alternative effects for the case
	 * 
	 * @return the list of alternative effects
	 */
	private List<Effect> getEffects() {
		List<Effect> effectList = new ArrayList<Effect>(effects.keySet());
		
		Collections.sort(effectList, new Comparator<Effect>() 
				{ public int compare(Effect e1, Effect e2) 
					{ return e1.hashCode() - e2.hashCode(); } } );
		
		return effectList;
	}
	
	
	
	/**
	 * Returns the probability for a given effect in the case.  
	 * If the effect is not specified in the case, 0.0 is returned.
	 * 
	 * @param effect the effect
	 * @return the probability
	 */
	private Parameter getParameter(Effect effect) {
		if (effects.containsKey(effect)) {
			return effects.get(effect);
		}
		log.warning("no parameter associated with " + effect);
		return new FixedParameter(0.0);
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
	public Set<Template> getOutputVariables() {
		Set<Template> outputVariables = new HashSet<Template>();
		for (Effect effect : effects.keySet()) {
			outputVariables.addAll(effect.getOutputVariables());
		}
		return outputVariables;
	}
	

	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	
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
			if (effects.get(e) instanceof FixedParameter) {
				str += " [" + effects.get(e) + "]";
			}
			else if (effects.get(e) instanceof SingleParameter) {
				str += " [" + effects.get(e) + "]";
			}
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


	
}
