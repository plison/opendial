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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.domains.rules.effects.Effect;
import opendial.state.anchoring.AnchoredRule;
import opendial.state.distribs.RuleDistribution;


/**
 * Representation of a chance node describing the probability distribution over
 * the possible effects of a probability rule depending on its inputs.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ProbabilityRuleNode extends ChanceNode {

	// logger
	public static Logger log = new Logger("RuleNode", Logger.Level.DEBUG);

	// the anchored rule for the node
	AnchoredRule rule;		

	
	/**
	 * Creates the probability rule nod
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the anchored rule does not specify a probability rule
	 */
	public ProbabilityRuleNode(AnchoredRule rule) throws DialException {
		super(rule.getRule().getRuleId());
		this.rule = rule;
		distrib = new RuleDistribution(rule);
		
	}
	
	
	/**
	 * Returns the possible effects for the node
	 * 
	 * @return the possible effects
	 */
	public Set<Value> getValues() {
		if (cachedValues == null) {
			cachedValues = new HashSet<Value>();
			for (Effect e : rule.getEffects()) {
				cachedValues.add(e);
			}
		}
		return cachedValues;
	}


	/**
	 * Copies the rule node
	 */
	public ProbabilityRuleNode copy() throws DialException {
		ProbabilityRuleNode rn = new ProbabilityRuleNode(rule);
		rn.setId(nodeId);
		return rn;
	}

	/**
	 * Returns the anchored rule
	 * 
	 * @return the anchor
	 */
	public AnchoredRule getAnchor() {
		return rule;
	}



}
