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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.Logger;

import opendial.domains.Domain;
import opendial.domains.rules.Case;
import opendial.domains.rules.CaseBasedRule;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.ComplexCondition.BinaryOperator;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.ClearEffect;
import opendial.domains.rules.effects.ComplexEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.rules.parameters.CompositeParameter;
import opendial.domains.rules.parameters.DirichletParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.utils.XMLUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLRuleReader {

	static Logger log = new Logger("XMLRuleReader", Logger.Level.DEBUG);
		
	Domain domain;
		
	public XMLRuleReader(Domain domain) {
		this.domain = domain;
	}
	

	/**
	 * 
	 * @param node
	 * @param domain
	 * @return
	 * @throws DialException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public <T extends CaseBasedRule> T getRule(Node topNode, Class<T> cls) throws DialException {
		
		try {
		T rule = cls.newInstance();
		
		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("id") != null) {
			String ruleId = topNode.getAttributes().getNamedItem("id").getNodeValue();
			rule.setRuleId(ruleId);
		}

		for (int i = 0 ; i < topNode.getChildNodes().getLength(); i++) {
			Node node = topNode.getChildNodes().item(i);

			if (node.getNodeName().equals("quantifier")) {
				String quantifier = getQuantifier(node);
				rule.addQuantifier(quantifier);
			}
			
			else if (node.getNodeName().equals("case")) {
				Case newCase = getCase(node, cls);
				rule.addCase(newCase);
			}
			else if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")){
				throw new DialException("Ill-formed rule: " + node.getNodeName() + " not accepted");
			}
		}

		return rule;
		}
		catch (IllegalAccessException e) {
			throw new DialException(e.toString());
		} catch (InstantiationException e) {
			throw new DialException(e.toString());
		}
	}



	private String getQuantifier(Node node) {
		if (node.getNodeName().equals("quantifier") && node.hasAttributes() &&
				node.getAttributes().getNamedItem("id") != null) {
			return node.getAttributes().getNamedItem("id").getNodeValue().replace("{", "").replace("}", "");
		}
		else {
			return "";
		}
	}


	/**
	 * 
	 * @param node
	 * @param domain
	 * @return
	 * @throws DialException 
	 */
	private <T extends CaseBasedRule> Case getCase(Node caseNode, Class<T> cls) throws DialException {

		Case newCase = new Case();

		for (int i = 0 ; i < caseNode.getChildNodes().getLength(); i++) {
			Node node = caseNode.getChildNodes().item(i);
			if (node.getNodeName().equals("condition")) {
				Condition condition = getFullCondition(node);
				if (newCase.getCondition() instanceof VoidCondition) {
					newCase.setCondition(condition); 
				}
				else {
					throw new DialException ("only one condition allowed by case, and two are defined: " 
							+ newCase.getCondition() + " and " + condition);
				}
			}
			else if (node.getNodeName().equals("effect")) {
				Effect effect = getFullEffect(node);
				Parameter prob = getParameter(node, cls);
				newCase.addEffect(effect, prob);
			}
			else if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")){
				throw new DialException("Ill-formed rule: " + node.getNodeName() + " not accepted");
			}
		}
		
		return newCase;
	}



	/**
	 * 
	 * @param node
	 * @param domain
	 * @return
	 * @throws DialException 
	 */
	private Condition getFullCondition(Node conditionNode) throws DialException {

		List<Condition> subconditions = new LinkedList<Condition>();

		for (int i = 0 ; i < conditionNode.getChildNodes().getLength(); i++) {
			Node node = conditionNode.getChildNodes().item(i);

			if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")) {
				Condition subcondition = getSubcondition(node);
				subconditions.add(subcondition);
			}
		}

		if (subconditions.isEmpty()) {
			return new VoidCondition();
		}
		else if (subconditions.size() == 1) {
			return subconditions.get(0);
		}
		else {
			BinaryOperator operator = getBinaryOperator(conditionNode);
			ComplexCondition condition = new ComplexCondition(subconditions);
			condition.setOperator(operator);
			return condition;
		}
	}



	/**
	 * 
	 * @param node
	 * @param domain
	 * @param rule
	 * @return
	 * @throws DialException 
	 */
	private Condition getSubcondition(Node node) throws DialException {

		Condition condition = new VoidCondition();

		if (node.getNodeName().equals("if") && node.hasAttributes() && 
				node.getAttributes().getNamedItem("var")!= null) {

			String variable = getStandardVariable(node, "var");

			if (node.getAttributes().getNamedItem("value") != null) {
				String valueStr = node.getAttributes().getNamedItem("value").getNodeValue();
				Relation relation = getRelation(node);			
				condition = new BasicCondition(variable, valueStr, relation);
			}
			
			else if (node.getAttributes().getNamedItem("var2") !=null) {
				String variable2 = getStandardVariable(node, "var2");
				Relation relation = getRelation(node);		
				condition = new BasicCondition(variable, "{"+variable2+"}", relation);
			}
			else {
				throw new DialException("unrecognized format for condition ");
			}
			
			for (int i = 0 ; i < node.getAttributes().getLength() ; i++) {
				Node attr = node.getAttributes().item(i);
				if (!attr.getNodeName().equals("var") && !attr.getNodeName().equals("var2")
						&& !attr.getNodeName().equals("value") && !attr.getNodeName().equals("relation")) {
					throw new DialException("unrecognized attribute: " + attr.getNodeName());
				}
			}
		}
		return condition;
	}


	/**
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private static Relation getRelation(Node node) throws DialException {
		Relation relation = Relation.EQUAL;
		if (node.hasAttributes() && node.getAttributes().getNamedItem("relation")!=null) {
			String relationStr = node.getAttributes().getNamedItem("relation").getNodeValue();
			if (relationStr.toLowerCase().trim().equals("=")) {
				relation = Relation.EQUAL;
			}
			else if (relationStr.toLowerCase().trim().equals("!=")) {
				relation = Relation.UNEQUAL;
			}
			else if (relationStr.toLowerCase().trim().equals("equal")) {
				relation = Relation.EQUAL;
			}
			else if (relationStr.toLowerCase().trim().equals("unequal")) {
				relation = Relation.UNEQUAL;
			}
			else if (relationStr.toLowerCase().trim().equals("contains")) {
				relation = Relation.CONTAINS;
			}
			else if (relationStr.toLowerCase().trim().equals("in")) {
				relation = Relation.CONTAINS;
			}
	/**		else if (relationStr.toLowerCase().trim().equals("length")) {
				relation = Relation.LENGTH;
			} */
			else if (relationStr.toLowerCase().trim().equals("notcontains")) {
				relation = Relation.NOT_CONTAINS;
			}
			else if (relationStr.toLowerCase().trim().equals("!in")) {
				relation = Relation.NOT_CONTAINS;
			}
			else if (relationStr.toLowerCase().trim().equals(">")) {
				relation = Relation.GREATER_THAN;
			}
			else if (relationStr.toLowerCase().trim().equals("<")) {
				relation = Relation.LOWER_THAN;
			}
			else {
				throw new DialException("unrecognized relation: " + relationStr);
			}
			
		}
		return relation;
	}


	private static BinaryOperator getBinaryOperator(Node conditionNode) {
		BinaryOperator operator = BinaryOperator.AND;
		if (conditionNode.hasAttributes() && conditionNode.getAttributes().getNamedItem("operator")!=null) {
			String operatorStr = conditionNode.getAttributes().getNamedItem("operator").getNodeValue();
			if (operatorStr.toLowerCase().trim().equals("and")) {
				operator = BinaryOperator.AND;
			}
			else if (operatorStr.toLowerCase().trim().equals("or")) {
				operator = BinaryOperator.OR;
			}
			else {
				try {
					throw new DialException("unrecognized relation: " + operatorStr);
				} catch (DialException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return operator;
	}

	
	
	private static String getStandardVariable(Node node, String varTag) throws DialException {
		
		if (node.hasAttributes() && node.getAttributes().getNamedItem(varTag)!=null) {
			return node.getAttributes().getNamedItem(varTag).getNodeValue();		
		}
		else {
			throw new DialException("attribute "  + varTag + " is mandatory");
		}
	}



	/**
	 * 
	 * @param node
	 * @param domain
	 * @return
	 * @throws DialException 
	 */
	private Effect getFullEffect(Node effectNode) throws DialException {

		List<Effect> effects = new LinkedList<Effect>();

		for (int i = 0 ; i < effectNode.getChildNodes().getLength(); i++) {
			Node node = effectNode.getChildNodes().item(i);

			if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment") && node.hasAttributes()) {
				Effect subeffect = getSubEffect(node);
				effects.add(subeffect);
			}
		}

		if (effects.isEmpty()) {
			return new VoidEffect();
		}
		else if (effects.size() == 1) {
			return effects.get(0);
		}
		else {
			ComplexEffect effect = new ComplexEffect(effects);
			return effect;
		}
	}


	/**
	 * 
	 * PS: we should have distinct relations: equal, unequal, include, substring
	 * 
	 * @param node
	 * @param domain
	 * @param rule
	 * @return
	 * @throws DialException 
	 */
	private Effect getSubEffect(Node node) throws DialException {

		Effect effect = new VoidEffect();

		if (node.getAttributes().getNamedItem("var")!=null) {

			String variable = getStandardVariable(node, "var");
			String valueStr = "";
			if (node.getAttributes().getNamedItem("value")!=null) {
				valueStr = node.getAttributes().getNamedItem("value").getNodeValue();
			}
		 	else if (node.getAttributes().getNamedItem("var2")!=null) {
		 		valueStr ="{" + node.getAttributes().getNamedItem("var2").getNodeValue() + "}";
			} 
			else if (node.getNodeName().equals("clear")) {
				effect = new ClearEffect(variable);
			}
			else {
				throw new DialException("effect must be either basic or equality");
			}
			
			if ((node.getNodeName().equalsIgnoreCase("set"))) {
				if (node.getAttributes().getNamedItem("relation")!=null && getRelation(node) == Relation.UNEQUAL) {
					effect = new BasicEffect(variable, valueStr, EffectType.DISCARD);
				}
				else {
					effect = new BasicEffect(variable, valueStr, EffectType.SET);
				}
			}
			else if ((node.getNodeName().equalsIgnoreCase("add"))) {
				effect = new BasicEffect(variable, valueStr, EffectType.ADD);
			}
			else if ((node.getNodeName().equalsIgnoreCase("remove"))) {
				effect = new BasicEffect(variable, valueStr, EffectType.DISCARD);
			}
			else if (!node.getNodeName().equals("clear") && !node.getNodeName().equals("#text") 
					&& ! node.getNodeName().equals("#comment")) {
				throw new DialException("unrecognized effect: " + node.getNodeName());
			}
		}
	
		else {
			throw new DialException("invalid format for effect: " + node.getNodeName() +
					" and var " + node.getAttributes().getNamedItem("var"));
		}
		
		for (int i = 0 ; i < node.getAttributes().getLength() ; i++) {
			Node attr = node.getAttributes().item(i);
			if (!attr.getNodeName().equals("var") && !attr.getNodeName().equals("var2")
					&& !attr.getNodeName().equals("value") && !attr.getNodeName().equals("relation")) {
				throw new DialException("unrecognized attribute: " + attr.getNodeName());
			}
		}
		
		return effect;
	}

	
	private <T> Parameter getParameter(Node node, Class<T> cls) throws DialException {
		
		if (cls.equals(UpdateRule.class) || cls.equals(PredictionRule.class)) {
			if (node.getAttributes().getNamedItem("prob")!= null) {
				String weightStr = node.getAttributes().getNamedItem("prob").getNodeValue();
				return getInnerParameter(weightStr);
			}
			else {
				return new FixedParameter(1.0);
			}
		}
		else if (cls.equals(DecisionRule.class)) {
			if (node.getAttributes().getNamedItem("util")!= null) {
				String utilStr = node.getAttributes().getNamedItem("util").getNodeValue();
				return getInnerParameter(utilStr);
			}
		}
		throw new DialException("rule class is not accepted: " + cls.getCanonicalName());
	}
	
	
	private Parameter getInnerParameter(String paramStr) throws DialException {
		try {
			double weight = Double.parseDouble(paramStr);
			return new FixedParameter(weight);
		}
		catch (NumberFormatException e) {
			if (paramStr.contains("+")) {
				String[] split = paramStr.split("\\+");
				CompositeParameter param = new CompositeParameter();
				for (int i = 0 ; i < split.length ; i++) {
					param.addParameter(split[i]);
				}
				return param;
			}
			else {
				Pattern p = Pattern.compile(".+(\\[[0-9]+\\])");
				Matcher m = p.matcher(paramStr);
				if (m.matches()) {
					int index = Integer.parseInt(m.group(1).replace("[", "").replace("]", ""));
					String paramId = paramStr.replace(m.group(1), "").trim();
					return new DirichletParameter(paramId, index);
				}
				else {
					return new SingleParameter(paramStr);					
				}
			}
		}
	}


}
