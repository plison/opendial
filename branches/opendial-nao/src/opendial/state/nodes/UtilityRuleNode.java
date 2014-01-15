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

package opendial.state.nodes;

import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.UtilityNode;
import opendial.datastructs.Assignment;
import opendial.state.anchoring.AnchoredRule;
import opendial.state.distribs.RuleUtilDistribution;

/**
 * Representation of a utility node associated with a utility rule.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UtilityRuleNode extends UtilityNode {

	// logger
	public static Logger log = new Logger("UtilityRuleNode", Logger.Level.DEBUG);

	// the anchored rule
	AnchoredRule rule;
	
	/**
	 * Creates an utility rule node for the anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not a utility rule
	 */
	public UtilityRuleNode(AnchoredRule rule) throws DialException {
		super(rule.getRule().getRuleId());
		this.rule = rule;	
		distrib = new RuleUtilDistribution(rule);
	}
	
	
	
	/**
	 * Copies the utility rule node
	 */
	@Override
	public UtilityRuleNode copy() throws DialException {
		UtilityRuleNode urn = new UtilityRuleNode(rule);
		urn.setId(nodeId);
		return urn;
	}
	
	
	/**
	 * Returns the possible input conditions for the utility rule node.
	 * 
	 * @return the possible input conditions.
	 */
	public Set<Assignment> getInputConditions() {
		return rule.getInputs().linearise();
	}



	/**
	 * Returns the anchored rule
	 * 
	 * @return the anchored rule
	 */
	public AnchoredRule getAnchor() {
		return rule;
	}

	
}
