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

package opendial.bn.nodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.statechange.AnchoredRule;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.discrete.RuleBasedDistribution;
import opendial.bn.distribs.utility.RuleBasedUtilDistribution;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.parameters.Parameter;
import opendial.utils.CombinatoricsUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UtilityRuleNode extends UtilityNode {

	// logger
	public static Logger log = new Logger("RuleNode", Logger.Level.NORMAL);

	AnchoredRule rule;

	public UtilityRuleNode(AnchoredRule rule) throws DialException {
		super(rule.getId());
		this.rule = rule;
		distrib = new RuleBasedUtilDistribution(rule);
		for (ChanceNode inputNode : rule.getInputNodes()) {
			addInputNode(inputNode);
		}
	}

	
}
