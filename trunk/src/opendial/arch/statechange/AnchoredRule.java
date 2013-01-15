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

package opendial.arch.statechange;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.CaseBasedRule;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.StochasticParameter;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.StringUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AnchoredRule {

	// logger
	public static Logger log = new Logger("AnchoredRule", Logger.Level.DEBUG);

	Rule rule;
	Assignment anchor;

	Map<String, ChanceNode> inputNodes;	

	AnchoredRuleCache cache;

	String id;

	public AnchoredRule(Rule rule, DialogueState state, Assignment anchor) {
		this.rule = rule;
		this.anchor = anchor;
		if (rule.getInputVariables().contains(new TemplateString("random"))) {
			anchor.addPair("random", ValueFactory.create((new Random()).nextInt(10000)));
		}

		String base = ((anchor.isEmpty())? rule.getRuleId() :
			rule.getRuleId() + "-" + anchor.toString());
		this.id = state.getNetwork().getUniqueNodeId(base);

		inputNodes = extractInputNodes(state);

		cache = new AnchoredRuleCache(this);
	}




	public Rule getRule() {
		return rule;
	}



	public String getId() {
		return id;
	}

	public void renewCache() {
		cache = new AnchoredRuleCache(this);
	}


	public Set<String> getInputVariables() {
		Set<TemplateString> templatedVariables = rule.getInputVariables();
		Set<String> filledVariables = new HashSet<String>();
		for (TemplateString templatedVar : templatedVariables) {
			try {
				String filledVar = templatedVar.fillSlots(anchor);
				filledVariables.add(filledVar);
			}
			catch (DialException e) {
				log.debug("could not fill input variable: " + e.toString());
			}
		}
		return filledVariables;
	}


	public Collection<ChanceNode> getParameters() {
		return cache.getParameters();
	}


	public Set<String> getOutputVariables() {
		Set<String> outputVariables = new HashSet<String>();		
		for (Output output : cache.getOutputs()) {
			for (String outputVariable : output.getVariables()) {
				outputVariables.add(outputVariable);	
			}
		}
		return outputVariables;
	}


	public boolean isRelevant() {
		if (cache.getOutputs().isEmpty()) {
			return false;
		}
		else if (cache.getOutputs().size() == 1 && cache.getOutputs().iterator().next().isVoid()) {
			return false;
		}
		else {
			return true;
		}
	}

	public Set<Output> getValues() {
		return cache.getOutputs();
	}

	public Map<Output,Parameter> getEffectOutputs (Assignment input) {
		return rule.getEffectOutputs(new Assignment(input.removeSpecifiers(), anchor));
	}


	public Set<Assignment> getPossibleConditions() {
		return cache.getPossibleConditions();
	}


	public String toString() {
		String str = rule.toString();
		if (!anchor.isEmpty()) {
			str += " (with " + anchor + ")"; 
		}
		return str;
	}


	public int hashCode() {
		return rule.hashCode() - anchor.hashCode() + 2* inputNodes.hashCode();
	}



	private Map<String, ChanceNode> extractInputNodes(DialogueState state) {
		Map<String, ChanceNode> tempInputNodes = new HashMap<String, ChanceNode>();
		for (String inputVar : getInputVariables()) {	
			boolean isAttached = false;
			for (int i = 4 ; i >= 0 && !isAttached ; i--) {
				String specifiedVar = inputVar + StringUtils.createNbPrimes(i);
				if (state.getNetwork().hasChanceNode(specifiedVar) 
						&& !state.getController().hasNewVariable(specifiedVar)) {
					tempInputNodes.put(specifiedVar, state.getNetwork().getChanceNode(specifiedVar));
					isAttached = true;
				}
			}
		}
		return tempInputNodes;
	}



	public Collection<ChanceNode> getInputNodes() {
		return inputNodes.values();
	}


}
