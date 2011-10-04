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

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.rules.Case;
import opendial.domains.rules.Rule;
import opendial.domains.rules.variables.Variable;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.AssignEffect;
import opendial.domains.rules.effects.ComplexEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.types.StandardType;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLRuleReader {

	static Logger log = new Logger("XMLRuleReader", Logger.Level.DEBUG);

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
	 * TODO: verify that the entity type matches the ones declared in the model
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private List<Variable> getVariables(Node node) throws DialException {

		NodeList varList = node.getChildNodes();
		List<Variable> vars = new LinkedList<Variable>();
		for (int j = 0 ; j < varList.getLength(); j++) {
			Node varNode = varList.item(j);
			if (varNode.getNodeName().equals("var") && varNode.hasAttributes()) {
				
				String typeStr;
				if (varNode.getAttributes().getNamedItem("type") != null) {
					typeStr = varNode.getAttributes().getNamedItem("type").getNodeValue();
				}
				else if (varNode.getAttributes().getNamedItem("label") != null) {
					typeStr = varNode.getAttributes().getNamedItem("label").getNodeValue();	
				}
				else {
					throw new DialException("type or label must be defined for variables");
				}

				Variable var;
				if (domain.hasType(typeStr)) {
					var = new Variable(typeStr,domain.getType(typeStr));
				}
				else {
					log.debug("entity type: " + typeStr);
					throw new DialException("entity type " + typeStr + " not declared in domain");
				}
				if (varNode.getAttributes().getNamedItem("denotation") != null) {
					String denotation = varNode.getAttributes().getNamedItem("denotation").getNodeValue();
					var.setDenotation(denotation);
				}
				vars.add(var);
			}
		}
		return vars;
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
			return complexCond;
		}
	}



	/**
	 * Returns the effect specified in the XML node
	 * 
	 * TODO: add add/remove effects to it
	 * 
	 * @param node
	 * @return
	 * @throws DialException
	 */
	public Effect getEffect(Node node) throws DialException {
		List<Effect> subeffects = new LinkedList<Effect>();
		NodeList effectList = node.getChildNodes();

		// get the probability of the effect
		float prob = 0.0f;
		if (node.hasAttributes() && node.getAttributes().getNamedItem("prob") != null) {
			try {
				prob = Float.parseFloat(node.getAttributes().getNamedItem("prob").getNodeValue());
			}
			catch (NumberFormatException e) {
				throw new DialException("probability " + 
						node.getAttributes().getNamedItem("prob").getNodeValue() + " not a valid probability");
			}
		}
		else {
			throw new DialException("effect must specify its probability");
		}

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
		}
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
}
