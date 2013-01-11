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

import java.util.Set;

import opendial.arch.Logger;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.rules.parameters.Parameter;

/**
 * Class representing a decision rule, mapping conditions to assignment
 * of utility values for particular actions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DecisionRule extends CaseBasedRule implements Rule {

	// logger
	public static Logger log = new Logger("DecisionRule", Logger.Level.NORMAL);
	
	/**
	 * Returns the default case for a decision rule, which is a void effect
	 * with utility 0.0
	 *
	 * @return the default case
	 */
	@Override
	protected Case getDefaultCase() {	
			Case defaultCase = new Case();
			defaultCase.setCondition(new VoidCondition());
			defaultCase.addEffect(new VoidEffect(), 0.0f);
			return defaultCase;
	}
	
	/**
	 * Returns a string representation of the decision rule
	 *
	 * @return the string representation 
	 */
	@Override
	public String toString() {
		return "decision rule " + super.toString();
	}


	/**
	 * Returns the rule type (here, a utility rule).
	 * 
	 * @return RuleType.UTIL
	 */
	@Override
	public RuleType getRuleType() {
		return RuleType.UTIL;
	}

}
