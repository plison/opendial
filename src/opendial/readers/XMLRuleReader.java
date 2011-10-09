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
import opendial.domains.rules.*;
import opendial.domains.rules.variables.*;
import opendial.domains.rules.conditions.*;
import opendial.domains.rules.effects.*;
import opendial.domains.types.*;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLRuleReader {

	static Logger log = new Logger("XMLRuleReader", Logger.Level.NORMAL);

	// ===================================
	//  RULE CONSTRUCTION METHODS
	// ===================================

	Rule rule;

	Domain domain;

	public Rule getRule(Node topNode, Domain domain) throws DialException {

		this.domain = domain;

		rule = new Rule();

		NodeList topList = topNode.getChildNodes();
		for (int i = 0 ; i < topList.getLength(); i++) {
			Node node = topList.item(i);
			if (node.getNodeName().equals("input")) {
				List<Variable> input = getVariables(node);
				rule.addInputVariables(input);
			}
			if (node.getNodeName().equals("output")) {
				List<Variable> output = getVariables(node);
				rule.addOutputVariables(output);
			}	
			if (node.getNodeName().equals("case")) {
				Case case1 = getCase(node);
				rule.addCase(case1);
			}			
		}
		return rule;
	}


	/**
	 * Get the variables declared in the input/output of the rule
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private List<Variable> getVariables(Node node) throws DialException {

		NodeList varList = node.getChildNodes();
		Map<String,Variable> vars = new HashMap<String,Variable>();
		for (int j = 0 ; j < varList.getLength(); j++) {
			Node varNode = varList.item(j);
			
			if (varNode.getNodeName().equals("var") && varNode.hasAttributes()) {
			
				Variable var = getVariable(varNode, vars);		
				vars.put(var.getDenotation(), var);
			}
		}
		return new LinkedList<Variable>(vars.values());
	}

	
	/**
	 * 
	 * @param varNode
	 * @return
	 * @throws DialException 
	 */
	private Variable getVariable(Node varNode, Map<String,Variable> previousVars) throws DialException {
		 
		if (varNode.getAttributes().getNamedItem("type") != null && 
				varNode.getAttributes().getNamedItem("denotation")!=null) {
			String typeStr = varNode.getAttributes().getNamedItem("type").getNodeValue();
			String denotation = varNode.getAttributes().getNamedItem("denotation").getNodeValue();
			if (domain.hasType(typeStr)) {
				return new Variable(denotation, domain.getType(typeStr));
			}
			else {
				log.debug("type: " + typeStr);
				throw new DialException("type " + typeStr + " not declared as entity in domain");
			}
		}
		else if (varNode.getAttributes().getNamedItem("label") != null) {
			String typeStr = varNode.getAttributes().getNamedItem("label").getNodeValue();	
			if (domain.hasType(typeStr)) {
				return new Variable(domain.getType(typeStr));
			}
			else {
				log.debug("type: " + typeStr);
				throw new DialException("type " + typeStr + " not declared in domain");
			}
		}
		else if (varNode.getAttributes().getNamedItem("pointer") != null) {
			String pointer = varNode.getAttributes().getNamedItem("pointer").getNodeValue();
			if (rule.hasInputVariable(pointer)) {
				Variable previousVar = rule.getInputVariable(pointer);
				return new PointerVariable(previousVar);
			}
			else {
				throw new DialException("sameAs variable not pointing to an existing variable");
			}
		}
		
		else if (varNode.getAttributes().getNamedItem("feature")!=null && 
				varNode.getAttributes().getNamedItem("base") != null && 
				varNode.getAttributes().getNamedItem("denotation")!=null) {
			
			String feat = varNode.getAttributes().getNamedItem("feature").getNodeValue();	
			String base = varNode.getAttributes().getNamedItem("base").getNodeValue();
			String denotation = varNode.getAttributes().getNamedItem("denotation").getNodeValue();

			if (previousVars.containsKey(base)) {
				Variable baseVar = previousVars.get(base);
				GenericType baseType = baseVar.getType();
				if (baseType.hasFeature(feat)) {
					FeatureType featType = baseType.getFeature(feat);
					return new FeatureVariable(denotation, featType, baseVar);
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
		else {
			throw new DialException("ill-formatted variable declaration");
		}
	}


	public Case getCase(Node node) throws DialException {
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
				case1.addEffect(effect);
			}
		}
		return case1;
	}


	public Condition getCondition(Node node) throws DialException {

		List<Condition> subconditions = new LinkedList<Condition>();
		NodeList condList = node.getChildNodes();
		for (int i = 0 ; i < condList.getLength(); i++) {
			Node subnode = condList.item(i);

			if (subnode.getNodeName().equals("if") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var")!=null && 
					subnode.getAttributes().getNamedItem("value") != null) {

				String denotation = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasInputVariable(denotation)) {
					Variable var = rule.getInputVariable(denotation);
					String value = subnode.getAttributes().getNamedItem("value").getNodeValue();
					BasicCondition basicCond = new BasicCondition(var,value);
					subconditions.add(basicCond);
				}

				else {
					throw new DialException("Variable " + denotation + " not included in rule input");
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



	/**
	 * Returns the effect specified in the XML node
	 * 
	 * @param node
	 * @return
	 * @throws DialException
	 */
	public Effect getEffect(Node node) throws DialException {
		
		// get the probability of the effect
		float prob = getEffectProbability(node);
		
		List<Effect> subeffects = getSubeffects(node, prob);
		if (subeffects.isEmpty()) {
			return new VoidEffect(prob);
		}
		else if (subeffects.size() == 1) {
			return subeffects.get(0);
		}
		else {
			ComplexEffect complexEffect = new ComplexEffect(prob);
			complexEffect.addSubeffects(subeffects);
			log.debug("number of (sub)effects: " + subeffects.size());
			return complexEffect;
		}
	}
	
	
	public List<Effect> getSubeffects(Node topNode, float prob) throws DialException {

		List<Effect> subeffects = new LinkedList<Effect>();
		
		NodeList effectList = topNode.getChildNodes();

		for (int i = 0 ; i < effectList.getLength(); i++) {
			Node subnode = effectList.item(i);

			if (subnode.getNodeName().equals("set") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var")!=null && 
					subnode.getAttributes().getNamedItem("value") != null) {

				String denotation = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(denotation)) {
					Variable var = rule.getOutputVariable(denotation);
					String value = subnode.getAttributes().getNamedItem("value").getNodeValue();
					AssignEffect assEffect = new AssignEffect(var,value,prob);
					subeffects.add(assEffect);
				}

				else {
					throw new DialException("Variable " + denotation + " not included in rule output");
				}
			}
			
			else if (subnode.getNodeName().equals("add") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var") != null) {
				String denotation = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(denotation)) {
					Variable var = rule.getOutputVariable(denotation);
					AddEntityEffect assEffect = new AddEntityEffect(var,prob);
					subeffects.add(assEffect);
				}
			}
			else if (subnode.getNodeName().equals("remove") && subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("var") != null) {
				String denotation = subnode.getAttributes().getNamedItem("var").getNodeValue();
				if (rule.hasOutputVariable(denotation)) {
					Variable var = rule.getOutputVariable(denotation);
					RemoveEntityEffect assEffect = new RemoveEntityEffect(var,prob);
					subeffects.add(assEffect);
				}
			}
		}
		return subeffects;
	}
	
	
	public float getEffectProbability (Node topNode) throws DialException {
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
