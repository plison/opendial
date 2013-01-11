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

import opendial.arch.Logger;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.VoidEffect;

/**
 * Representation of an update rule, mapping conditions to probabilistic effects
 * on the values of specific output variables
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UpdateRule extends CaseBasedRule implements Rule {

	// logger
	public static Logger log = new Logger("UpdateRule", Logger.Level.NORMAL);

	
	/**
	 * Returns the default case for the rule, which is a void effect with 
	 * probability 1.0
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
	 * Returns the string representation for the update rule
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "update rule " + super.toString();
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
