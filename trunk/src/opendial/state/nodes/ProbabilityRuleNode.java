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

import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;
import opendial.state.AnchoredRule;
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
	 * Creates the probability rule node
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
	@Override
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
	@Override
	public ProbabilityRuleNode copy() throws DialException {
		ProbabilityRuleNode rn = new ProbabilityRuleNode(rule);
		rn.setId(nodeId);
		rn.distrib = distrib.copy();
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


	/**
	 * Returns true if the rule contains at least one effect with add/discard operations on the
	 * output variable.  Else, returns false.
	 * 
	 * @param outputVar the output variable to consider (without primes)
	 * @return true if add/discard effects are defined for the output variable, else false
	 */
	public boolean hasAddRemoveEffects(String outputVar) {
		for (Value v : getValues()) {
			Set<EffectType> types = ((Effect) v).getEffectTypes(outputVar);
			if (types.contains(EffectType.ADD) || types.contains(EffectType.DISCARD)) {
				return true;
			}
		}
		return false;
	}




}
