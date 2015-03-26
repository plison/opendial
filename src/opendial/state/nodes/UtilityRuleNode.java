// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.state.nodes;

import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.UtilityNode;
import opendial.datastructs.Assignment;
import opendial.state.AnchoredRule;
import opendial.state.distribs.RuleUtilDistribution;

/**
 * Representation of a utility node associated with a utility rule.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
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
