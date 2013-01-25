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

package opendial.state.rules;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
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
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.state.DialogueState;
import opendial.state.rules.Rule.RuleType;
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

	Set<String> inputVariables;
	
	Set<String> outputVariables;

	Map<String,ChanceNode> parameters;

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

		inputVariables = extractInputVariables();

		inputNodes = extractInputNodes(state);
	
		cache = new AnchoredRuleCache(this);
		
		outputVariables = extractOutputVariables();
	
		parameters = extractParameters(state);
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


	private Set<String> extractInputVariables() {
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
	

	public Collection<ChanceNode> getParameterNodes() {
		return parameters.values();
	}


	private Set<String> extractOutputVariables() {
		Set<String> outputVariables = new HashSet<String>();		
		for (Output output : cache.getOutputs().keySet()) {
			for (String outputVariable : output.getVariables()) {
				outputVariables.add(outputVariable);	
			}
		}
		return outputVariables;
	}
	

	private Map<String,ChanceNode> extractParameters(DialogueState state) {
		Map<String,ChanceNode> parameters = new HashMap<String,ChanceNode>();		
		for (Output output : cache.getOutputs().keySet()) {
			for (String param : cache.getOutputs().get(output).getParameterIds()) {
				if (state.getNetwork().hasChanceNode(param)) {
					parameters.put(param,state.getNetwork().getChanceNode(param));
				}
				else {
					log.warning("parameter " + param + " is not defined!");
				}
			}
		}
		return parameters;
	}
	
	
	public Set<String> getOutputVariables() {
		return outputVariables;
	}


	public boolean isRelevant() {
		if (cache.getOutputs().isEmpty()) {
			return false;
		}
		else if (cache.getOutputs().size() == 1) {
			Map.Entry<Output,Parameter> o = cache.getOutputs().entrySet().iterator().next();
			if (o.getKey().isVoid()) {
				if (rule.getRuleType() == RuleType.PROB) {
					return false;
				}
				else if (o.getValue() instanceof FixedParameter && 
				(((FixedParameter)o.getValue()).getParameterValue() == 0.0)) {
					return false;
				}
			}
		}
		return true;
	}

	public Set<Output> getValues() {
		return cache.getOutputs().keySet();
	}

	public Map<Output,Parameter> getEffectOutputs (Assignment input) {
		Assignment augmentedInput = new Assignment(input.removeSpecifiers(), anchor);
		return rule.getEffectOutputs(augmentedInput);
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
		for (String inputVar : inputVariables) {
			boolean isAttached = false;
			for (int i = 4 ; i >= 0 && !isAttached ; i--) {
				String specifiedVar = inputVar + StringUtils.createNbPrimes(i);
				if (state.getNetwork().hasChanceNode(specifiedVar) 
						&& !state.isVariableToProcess(specifiedVar)) {
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
