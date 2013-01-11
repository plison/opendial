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

import java.util.Map;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.rules.parameters.Parameter;

/**
 * Representation of a prediction rule, mapping particular conditions to 
 * probabilistic predictions on the future values of specific variables
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PredictionRule extends CaseBasedRule implements Rule {

	// logger
	public static Logger log = new Logger("PredictionRule", Logger.Level.NORMAL);
	
	/**
	 * Returns the default case for a prediction rule, which is a void effect
	 * with a probability 1.0
	 *
	 * @return the default case
	 */
	@Override
	protected Case getDefaultCase() {	
			Case defaultCase = new Case();
			defaultCase.setCondition(new VoidCondition());
			defaultCase.addEffect(new VoidEffect(), 1.0f);
			return defaultCase;
	}
	
	/**
	 * Returns the string representation of the prediction rule
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "prediction rule " + super.toString();
	}
	
	/**
	 * Returns the parametrised effects for the rule, and add the ^p ending
	 * to the output variables to indicate that they are a prediction
	 * 
	 * @input the input assignment
	 * @return the parametrised effects
	 */
	@Override
	public Map<Output,Parameter> getEffectOutputs (Assignment input) {
		Map<Output,Parameter> outputs = super.getEffectOutputs(input);
		for (Output o : outputs.keySet()) {
			o.addEndingToVariables("^p");
		}
		return outputs;
	}
	
	/**
	 * Returns the rule type (here, a probability rule).
	 * 
	 * @return RuleType.PROB
	 */
	@Override
	public RuleType getRuleType() {
		return RuleType.PROB;
	}
}
