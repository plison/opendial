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

package opendial.readers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Template;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.RuleCase;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.ComplexCondition.BinaryOperator;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.TemplateCondition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.TemplateEffect;
import opendial.domains.rules.parameters.CompositeParameter;
import opendial.domains.rules.parameters.CompositeParameter.Operator;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.StochasticParameter;
import opendial.utils.XMLUtils;

import org.w3c.dom.Node;

/**
 *  Extraction of a probabilistic rule given an XML specification
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLRuleReader {

	static Logger log = new Logger("XMLRuleReader", Logger.Level.DEBUG);


	static int idCounter = 1;

	/**
	 * Extracts the rule corresponding to the XML specification.
	 * 
	 * @param topNode the XML node
	 * @return the corresponding rule
	 * @throws DialException if the specification is ill-defined.
	 */
	public static Rule getRule(Node topNode) throws DialException {

		// extracting the rule type
		RuleType type = RuleType.PROB;
		if (XMLUtils.serialise(topNode).contains("util=")) {
			type = RuleType.UTIL;
		}

		// setting the rule identifier
		String ruleId;
		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("id") != null) {
			ruleId = topNode.getAttributes().getNamedItem("id").getNodeValue();
		}
		else {
			ruleId = "rule" + idCounter;
			idCounter++;
		}

		//creating the rule
		Rule rule = new Rule(ruleId, type);

		
		// extracting the rule cases
		for (int i = 0 ; i < topNode.getChildNodes().getLength(); i++) {
			Node node = topNode.getChildNodes().item(i);

			if (node.getNodeName().equals("case")) {
				RuleCase newCase = getCase(node, type);
				rule.addCase(newCase);
				
			}
			else if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")){
				throw new DialException("Ill-formed rule: " + node.getNodeName() + " not accepted");
			}
		}

		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("priority") != null) {
			int priority = Integer.parseInt(topNode.getAttributes()
					.getNamedItem("priority").getNodeValue());
			rule.setPriority(priority);
		}
		return rule;
	}



	/**
	 * Returns the case associated with the rule specification.
	 * 
	 * @param node the XML node
	 * @param type the rule type
	 * @return the associated rule case
	 * @throws DialException if the specification is ill-defined.
	 */
	private static RuleCase getCase(Node caseNode, RuleType type) throws DialException {

		RuleCase newCase = new RuleCase();

		for (int i = 0 ; i < caseNode.getChildNodes().getLength(); i++) {
			Node node = caseNode.getChildNodes().item(i);

			// extracting the condition
			if (node.getNodeName().equals("condition")) {
				Condition condition = getFullCondition(node);
					newCase  = new RuleCase(condition);
			}
			// extracting an effect
			else if (node.getNodeName().equals("effect")) {
				Effect effect = getFullEffect(node);
				if (effect != null) {
					Parameter prob = getParameter(node, type);
					newCase.addEffect(effect, prob);
				}
			}
			else if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")){
				throw new DialException("Ill-formed rule: " + node.getNodeName() + " not accepted");
			}
		}

		return newCase;
	}



	/**
	 * Extracting the condition associated with an XML specification.
	 * 
	 * @param node the XML node 
	 * @return the associated condition
	 * @throws DialException if the specification is ill-defined.
	 */
	private static Condition getFullCondition(Node conditionNode) throws DialException {

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
			ComplexCondition condition = new ComplexCondition(subconditions, operator);
			return condition;
		}
	}



	/**
	 * Extracting a partial condition from a rule specification
	 * 
	 * @param node the XML node
	 * @return the corresponding condition
	 * @throws DialException if the condition is ill-defined
	 */
	private static Condition getSubcondition(Node node) throws DialException {

		// extracting a basic condition
		if (node.getNodeName().equals("if") && node.hasAttributes() && 
				node.getAttributes().getNamedItem("var")!= null) {

			Condition condition;

			String variable = node.getAttributes().getNamedItem("var").getNodeValue();

			if (node.getAttributes().getNamedItem("value") != null) {
				String valueStr = node.getAttributes().getNamedItem("value").getNodeValue();
				Relation relation = getRelation(node);
				if ((new Template(variable)).isUnderspecified() || (new Template(valueStr)).isUnderspecified()) {
					condition = new TemplateCondition(new Template(variable), new Template(valueStr), relation);
				}
				else {
					condition = new BasicCondition(variable, ValueFactory.create(valueStr), relation);
				}
			}

			else if (node.getAttributes().getNamedItem("var2") !=null) {
				String variable2 = node.getAttributes().getNamedItem("var2").getNodeValue();
				Relation relation = getRelation(node);		
				condition = new TemplateCondition(new Template(variable), new Template("{"+variable2+"}"), relation);
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
			return condition;
		}

		// extracting a conjunction, disjunction, or negated conjunction
		else if (node.getNodeName().equals("or") || node.getNodeName().equals("and")) {
		
			BinaryOperator operator = (node.getNodeName().equals("or"))? BinaryOperator.OR : BinaryOperator.AND;

			List<Condition> conditions = new ArrayList<Condition>();
			for (int i = 0; i < node.getChildNodes().getLength() ; i++) {
				Node subNode = node.getChildNodes().item(i);
				if (!subNode.getNodeName().equals("#text") && !subNode.getNodeName().equals("#comment")) {
					conditions.add(getSubcondition(subNode));
				}
			}
			return new ComplexCondition(conditions, operator);
		}
		return new VoidCondition();
	}


	/**
	 * Extracts the relation specified in a condition
	 * @param node the XML node containing the relation
	 * @return the corresponding relation
	 * @throws DialException  if the relation is ill-defined
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
				relation = Relation.IN;
			}
			/**		else if (relationStr.toLowerCase().trim().equals("length")) {
				relation = Relation.LENGTH;
			} */
			else if (relationStr.toLowerCase().trim().equals("!contains")) {
				relation = Relation.NOT_CONTAINS;
			}
			else if (relationStr.toLowerCase().trim().equals("!in")) {
				relation = Relation.NOT_IN;
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


	/**
	 * Extracting the binary operator specified at the top of a condition (if any).
	 * 
	 * @param conditionNode the XML node
	 * @return the corresponding operator
	 */
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
					e.printStackTrace();
				}
			}
		}
		return operator;
	}



	/**
	 * Extracts a full effect from the XML specification.
	 * 
	 * @param node the XML node
	 * @return the corresponding effect
	 * @throws DialException 
	 */
	private static Effect getFullEffect(Node effectNode) throws DialException {

		List<BasicEffect> effects = new LinkedList<BasicEffect>();

		for (int i = 0 ; i < effectNode.getChildNodes().getLength(); i++) {
			Node node = effectNode.getChildNodes().item(i);

			if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment") && node.hasAttributes()) {
				BasicEffect subeffect = getSubEffect(node);
				effects.add(subeffect);
			}
		}
		return new Effect(effects);		
	}


	/**
	 * Extracts a basic effect from the XML specification.
	 * 
	 * @param node the XML node
	 * @return the corresponding basic effect
	 * @throws DialException if the effect is ill-defined
	 */
	private static BasicEffect getSubEffect(Node node) throws DialException {

		if (node.getAttributes().getNamedItem("var")==null) {
			throw new DialException("invalid format for effect: " + node.getNodeName() +
					" and var " + node.getAttributes().getNamedItem("var"));
		}

		String var = node.getAttributes().getNamedItem("var").getNodeValue();
		
		String value = null;
		if (node.getAttributes().getNamedItem("value")!=null) {
			value = node.getAttributes().getNamedItem("value").getNodeValue();
		}
		else if (node.getAttributes().getNamedItem("var2")!=null) {
			value = "{" + node.getAttributes().getNamedItem("var2").getNodeValue() + "}";
		}
		else {
			value = "None";
		}
		value = value.replaceAll("\\s+"," ");
		EffectType type = getEffectType(node);
		// "clear" effect is outdated
		if (node.getNodeName().equalsIgnoreCase("clear")) {
			value = "None";
		}
		
		// checking for other attributes
		for (int i = 0 ; i < node.getAttributes().getLength() ; i++) {
			Node attr = node.getAttributes().item(i);
			if (!attr.getNodeName().equals("var") && !attr.getNodeName().equals("var2")
					&& !attr.getNodeName().equals("value") && !attr.getNodeName().equals("relation")) {
				throw new DialException("unrecognized attribute: " + attr.getNodeName());
			}
		}
		
		Template tvar = new Template(var);
		Template tval = new Template(value);
		if (tvar.isUnderspecified() || tval.isUnderspecified()) {
			return new TemplateEffect(tvar, tval, type);
		}
		else {
			return new BasicEffect(var, value, type);
		}
		
	}

	
	/**
	 * Returns the effect type corresponding to an XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding type
	 * @throws DialException if the effect type is ill-defined
	 */
	private static EffectType getEffectType(Node node) throws DialException {
		
		if (node.getNodeName().equalsIgnoreCase("set") & node.getAttributes().getNamedItem("relation")!=null 
				&& getRelation(node) == Relation.UNEQUAL) {
				return EffectType.DISCARD;
		}
		else if (node.getNodeName().equalsIgnoreCase("set")) {
			return EffectType.SET;
		}
		else if ((node.getNodeName().equalsIgnoreCase("add"))) {
			return EffectType.ADD;
		}
		else if ((node.getNodeName().equalsIgnoreCase("remove"))) {
			return EffectType.DISCARD;
		}
		else if (node.getNodeName().equalsIgnoreCase("clear")) {
			return EffectType.SET;
		}
		else  {
			throw new DialException("unrecognized effect: " + node.getNodeName());
		}
	}

	
	/**
	 * Returns the parameter described by the XML specification.
	 * 
	 * @param node the XML node
	 * @param type the rule type
	 * @return the parameter representation
	 * @throws DialException
	 */
	private static Parameter getParameter(Node node, RuleType type) throws DialException {

		if (type == RuleType.PROB) {
			if (node.getAttributes().getNamedItem("prob")!= null) {
				String prob = node.getAttributes().getNamedItem("prob").getNodeValue();
				return getInnerParameter(prob);
			}
			else {
				return new FixedParameter(1.0);
			}
		}
		else if (type == RuleType.UTIL) {
			if (node.getAttributes().getNamedItem("util")!= null) {
				String util = node.getAttributes().getNamedItem("util").getNodeValue();
				return getInnerParameter(util);
			}
		}
		throw new DialException("parameter is not accepted");
	}

	
	/** Returns the parameter described by the XML specification.
	 * 
	 * @param node the XML node
	 * @return the parameter representation
	 * @throws DialException
	 */
	private static Parameter getInnerParameter(String paramStr) throws DialException {
		
		// we first try to extract a fixed value
		try {
			double weight = Double.parseDouble(paramStr);
			return new FixedParameter(weight);
		}
		
		// if it fails, we extract an actual unknown parameter
		catch (NumberFormatException e) {
			
			// if we have a linear model
			if (paramStr.contains("+")) {
				String[] split = paramStr.split("\\+");
				CompositeParameter param = new CompositeParameter(Operator.ADD);
				for (int i = 0 ; i < split.length ; i++) {
					param.addParameter(new StochasticParameter(split[i]));
				}
				return param;
			}
			
			// else, we extract a stochastic parameter
			else {
				Pattern p = Pattern.compile(".+(\\[[0-9]+\\])");
				Matcher m = p.matcher(paramStr);
				if (m.matches()) {
					int index = Integer.parseInt(m.group(1).replace("[", "").replace("]", ""));
					String paramId = paramStr.replace(m.group(1), "").trim();
					return new StochasticParameter(paramId, index);
				}
				else {
					return new StochasticParameter(paramStr);					
				}
			}
		}
	}


}
