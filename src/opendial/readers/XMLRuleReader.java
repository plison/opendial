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

package opendial.readers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialConstants.BinaryOperator;
import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Type;
import opendial.domains.rules.*;
import opendial.domains.rules.variables.*;
import opendial.domains.rules.conditions.*;
import opendial.domains.rules.effects.*;
import opendial.utils.Logger;

/**
 * XML reader for a rule specification
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLRuleReader {

	// logger
	static Logger log = new Logger("XMLRuleReader", Logger.Level.NORMAL);

	// the rule which is being extracted
	Rule rule;

	// the dialogue domain (assumes that the declared types are already extracted)
	Domain domain;

	
	
	// ===================================
	//  RULES
	// ===================================

	
	/**
	 * Returns the rule specified in the XML node, given the (partly extracted) domain.
	 * The method assumes that the type declarations are already extracted.
	 * 
	 * @param topNode the XML node
	 * @param domain the domain
	 * @return the corresponding rule
	 * @throws DialException if the node is ill-formatted
	 */
	public Rule getRule(Node topNode, Domain domain) throws DialException {

		this.domain = domain;

		rule = new Rule();

		NodeList topList = topNode.getChildNodes();
		for (int i = 0 ; i < topList.getLength(); i++) {
			Node node = topList.item(i);
			
			// rule input
			if (node.getNodeName().equals("input")) {
				List<StandardVariable> input = getVariables(node);
				rule.addInputVariables(input);
			}
			
			// rule output
			if (node.getNodeName().equals("output")) {
				List<StandardVariable> output = getVariables(node);
				rule.addOutputVariables(output);
			}	
			
			// rule case
			if (node.getNodeName().equals("case")) {
				Case case1 = getCase(node);
				rule.addCase(case1);
			}			
		}
		return rule;
	}


	// ===================================
	//  VARIABLES
	// ===================================

	
	/**
	 * Get the variables declared in the input/output of the rule
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private List<StandardVariable> getVariables(Node node) throws DialException {

		NodeList varList = node.getChildNodes();
		Map<String,StandardVariable> vars = new HashMap<String,StandardVariable>();
		for (int j = 0 ; j < varList.getLength(); j++) {
			Node varNode = varList.item(j);
			
			if (varNode.getNodeName().equals("var") && varNode.hasAttributes()) {
			
				StandardVariable var = getVariable(varNode, vars);		
				vars.put(var.getIdentifier(), var);
			}
		}
		return new LinkedList<StandardVariable>(vars.values());
	}

	
	/**
	 * Returns the variable specified in the input/output of a rule, given the XML node
	 * 
	 * @param varNode the XML node
	 * @return the variable
	 * @throws DialException if node is ill-formatted 
	 */
	private StandardVariable getVariable(Node varNode, Map<String,StandardVariable> previousVars) throws DialException {
		 
		if (varNode.getAttributes().getNamedItem("type") != null && 
				varNode.getAttributes().getNamedItem("id")!=null) {
			String typeStr = varNode.getAttributes().getNamedItem("type").getNodeValue();
			String id = varNode.getAttributes().getNamedItem("id").getNodeValue();
			if (domain.hasType(typeStr)) {
				return new StandardVariable(id, domain.getType(typeStr));
			}
			else {
				throw new DialException("type " + typeStr + " not declared as entity in domain");
			}
		}
		else if (varNode.getAttributes().getNamedItem("label") != null) {
			String typeStr = varNode.getAttributes().getNamedItem("label").getNodeValue();	
			if (domain.hasType(typeStr)) {
				return new StandardVariable(domain.getType(typeStr));
			}
			else {
				throw new DialException("type " + typeStr + " not declared in domain");
			}
		}
		else if (varNode.getAttributes().getNamedItem("pointer") != null) {
			String pointer = varNode.getAttributes().getNamedItem("pointer").getNodeValue();
			if (rule.hasInputVariable(pointer)) {
				return new PointerVariable(rule.getInputVariable(pointer));
			}
			else {
				throw new DialException("sameAs variable not pointing to an existing variable");
			}
		}
		
		else if (varNode.getAttributes().getNamedItem("feature")!=null && 
				varNode.getAttributes().getNamedItem("base") != null && 
				varNode.getAttributes().getNamedItem("id")!=null) {
			
			return getFeatureVariable(varNode, previousVars);	
		}
		else {
			throw new DialException("ill-formatted variable declaration");
		}
	}

	
	/**
	 * Returns the feature variable specified in the XML node
	 * 
	 * @param varNode the XML node
	 * @param previousVars the previously defined variables
	 * @return the feature variable
	 * @throws DialException if XML node is ill-formatted
	 */
	private FeatureVariable getFeatureVariable(Node varNode, Map<String,StandardVariable> previousVars) throws DialException {
		String feat = varNode.getAttributes().getNamedItem("feature").getNodeValue();	
		String base = varNode.getAttributes().getNamedItem("base").getNodeValue();
		String id = varNode.getAttributes().getNamedItem("id").getNodeValue();

		if (previousVars.containsKey(base)) {
			StandardVariable baseVar = previousVars.get(base);
			Type baseType = baseVar.getType();
			if (baseType.hasFeature(feat)) {
				Type featType = baseType.getFeature(feat);
				return new FeatureVariable(id, featType, baseVar);
			}
			else {
				log.debug("base variable: " + base + ", existing features: " + baseType.getFeatures());
				throw new DialException("base variable " + base + " has no declared feature " + feat);
			}
		}
		else {
			throw new DialException("base variable " + base + " has not been set");
		}
	}

	
	// ===================================
	//  CASES
	// ===================================

	
	/**
	 * Returns the case specified in the XML node
	 * 
	 * @param node the XML node
	 * @return the case
	 * @throws DialException if node is ill-formatted
	 */
	private Case getCase(Node node) throws DialException {
		Case case1 = new Case();
		NodeList caseContent = node.getChildNodes();
		for (int j = 0 ; j < caseContent.getLength(); j++) {
			Node subnode = caseContent.item(j);
			if (subnode.getNodeName().equals("condition")) {
				Condition condition = getCondition(subnode);
				case1.setCondition(condition);
			}
			else if (subnode.getNodeName().equals("effect")) {
				Effect effect = getEffect(subnode);
				
				// get the probability of the effect
				float prob = getEffectProbability(subnode);
	
				case1.addEffect(effect, prob);
			}
		}
		return case1;
	}

	
	// ===================================
	//  CONDITIONS
	// ===================================


	/**
	 * Returns the condition specified in the XML node
	 * 
	 * @param node the XML node
	 * @return the condition
	 * @throws DialException if node is ill-formatted
	 */
	private Condition getCondition(Node node) throws DialException {

		List<Condition> subconditions = new LinkedList<Condition>();
		NodeList condList = node.getChildNodes();
		for (int i = 0 ; i < condList.getLength(); i++) {
			Node subnode = condList.item(i);

			if (subnode.getNodeName().equals("if") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var")!=null && 
					subnode.getAttributes().getNamedItem("value") != null) {

				String id = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasInputVariable(id)) {
					StandardVariable var = rule.getInputVariable(id);
					String value = subnode.getAttributes().getNamedItem("value").getNodeValue();
					BasicCondition<String> basicCond = new BasicCondition<String>(var,value);
					subconditions.add(basicCond);
				}

				else {
					throw new DialException("Variable " + id + " not included in rule input");
				}						
			}
		}
		if (subconditions.isEmpty()) {
			return new VoidCondition();
		}
		else if (subconditions.size() == 1) {
			return subconditions.get(0);
		}
		else {
			ComplexCondition complexCond = new ComplexCondition();
			complexCond.addSubconditions(subconditions);
			log.debug("number of (sub)conditions: " + subconditions.size());
			
			if (node.hasAttributes() && node.getAttributes().getNamedItem("operator") != null) {
				String operator = node.getAttributes().getNamedItem("operator").getNodeValue();
				if (operator.equals("or")) {
					complexCond.setOperator(BinaryOperator.OR);
				}
				else if (operator.equals("and")) {
					complexCond.setOperator(BinaryOperator.AND);
				}
			}
			return complexCond;
		}
	}

	
	// ===================================
	//  EFFECTS
	// ===================================

	

	/**
	 * Returns the effect specified in the XML node
	 * 
	 * @param node the node
	 * @return the effect
	 * @throws DialException
	 */
	private Effect getEffect(Node node) throws DialException {
			
		List<Effect> subeffects = getSubeffects(node);
		if (subeffects.isEmpty()) {
			return new VoidEffect();
		}
		else if (subeffects.size() == 1) {
			return subeffects.get(0);
		}
		else {
			ComplexEffect complexEffect = new ComplexEffect();
			complexEffect.addSubeffects(subeffects);
			log.debug("number of (sub)effects: " + subeffects.size());
			return complexEffect;
		}
	}
	
	
	
	/**
	 * Returns the list of sub-effects specified in the XML node
	 * 
	 * @param topNode the node
	 * @return the effect
	 * @throws DialException
	 */
	private List<Effect> getSubeffects(Node topNode) throws DialException {

		List<Effect> subeffects = new LinkedList<Effect>();
		
		NodeList effectList = topNode.getChildNodes();

		for (int i = 0 ; i < effectList.getLength(); i++) {
			Node subnode = effectList.item(i);

			if (subnode.getNodeName().equals("set") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var")!=null && 
					subnode.getAttributes().getNamedItem("value") != null) {

				String id = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(id)) {
					StandardVariable var = rule.getOutputVariable(id);
					String value = subnode.getAttributes().getNamedItem("value").getNodeValue();
					AssignEffect<String> assEffect = new AssignEffect<String>(var,value);
					subeffects.add(assEffect);
				}

				else {
					throw new DialException("Variable " + id + " not included in rule output");
				}
			}
			
			else if (subnode.getNodeName().equals("add") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var") != null) {
				String id = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(id)) {
					StandardVariable var = rule.getOutputVariable(id);
					AddEntityEffect assEffect = new AddEntityEffect(var);
					subeffects.add(assEffect);
				}
			}
			else if (subnode.getNodeName().equals("remove") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var") != null) {
				String id = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(id)) {
					StandardVariable var = rule.getOutputVariable(id);
					RemoveEntityEffect assEffect = new RemoveEntityEffect(var);
					subeffects.add(assEffect);
				}
			}
		}
		return subeffects;
	}
	
	
	/**
	 * Returns the effect probability as specified in the XML node
	 * 
	 * @param topNode the node
	 * @return the probability
	 * @throws DialException
	 */
	private float getEffectProbability (Node topNode) throws DialException {
		float prob = 1.0f;
		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("prob") != null) {
			try {
				prob = Float.parseFloat(topNode.getAttributes().getNamedItem("prob").getNodeValue());
			}
			catch (NumberFormatException e) {
				throw new DialException("probability " + 
						topNode.getAttributes().getNamedItem("prob").getNodeValue() + " not a valid probability");
			}
		}
		return prob;
	}
}
