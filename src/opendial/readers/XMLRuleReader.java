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

import java.util.logging.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.MathExpression;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.RuleOutput;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.ComplexCondition.BinaryOperator;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.NegatedCondition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.TemplateEffect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.templates.Template;
import opendial.utils.XMLUtils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Extraction of a probabilistic rule given an XML specification
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLRuleReader {

	final static Logger log = Logger.getLogger("OpenDial");

	static int idCounter = 1;

	/**
	 * Extracts the rule corresponding to the XML specification.
	 * 
	 * @param topNode the XML node
	 * @return the corresponding rule
	 */
	public static Rule getRule(Node topNode) {

		// extracting the rule type
		RuleType type = RuleType.PROB;
		if (XMLUtils.serialise(topNode).contains("util=")) {
			type = RuleType.UTIL;
		}

		// setting the rule identifier
		String ruleId;
		if (topNode.hasAttributes()
				&& topNode.getAttributes().getNamedItem("id") != null) {
			ruleId = topNode.getAttributes().getNamedItem("id").getNodeValue();
		}
		else {
			ruleId = "rule" + idCounter;
			idCounter++;
		}

		// creating the rule
		Rule rule = new Rule(ruleId, type);

		int priority = 1;
		if (topNode.hasAttributes()
				&& topNode.getAttributes().getNamedItem("priority") != null) {
			priority = Integer.parseInt(
					topNode.getAttributes().getNamedItem("priority").getNodeValue());
		}

		// extracting the rule cases
		for (int i = 0; i < topNode.getChildNodes().getLength(); i++) {
			Node node = topNode.getChildNodes().item(i);

			if (node.getNodeName().equals("case")) {
				Condition cond = getCondition(node, type);
				RuleOutput output = getOutput(node, type, priority);
				rule.addCase(cond, output);

			}
			else if (XMLUtils.hasContent(node)) {
				if (node.getNodeName().equals("#text")) {
					throw new RuntimeException("cannot insert free text in <rule>");
				}
				throw new RuntimeException(
						"Invalid tag in <rule>: " + node.getNodeName());
			}
		}

		return rule;
	}

	/**
	 * Returns the condition associated with the rule specification.
	 * 
	 * @param node the XML node
	 * @param type the rule type
	 * @return the associated rule condition
	 */
	private static Condition getCondition(Node caseNode, RuleType type) {

		for (int i = 0; i < caseNode.getChildNodes().getLength(); i++) {
			Node node = caseNode.getChildNodes().item(i);

			// extracting the condition
			if (node.getNodeName().equals("condition")) {
				return getFullCondition(node);
			}
		}

		return new VoidCondition();
	}

	/**
	 * Returns the output associated with the rule specification.
	 * 
	 * @param node the XML node
	 * @param type the rule type
	 * @param condition the associated condition
	 * @param priority the rule priority
	 */
	private static RuleOutput getOutput(Node caseNode, RuleType type, int priority) {

		RuleOutput output = new RuleOutput(type);

		for (int i = 0; i < caseNode.getChildNodes().getLength(); i++) {
			Node node = caseNode.getChildNodes().item(i);

			// extracting an effect
			if (node.getNodeName().equals("effect")) {
				Effect effect = getFullEffect(node, priority);
				if (effect != null) {
					Parameter prob = getParameter(node, type);
					output.addEffect(effect, prob);
				}
			}
		}

		return output;
	}

	/**
	 * Extracting the condition associated with an XML specification.
	 * 
	 * @param node the XML node
	 * @return the associated condition
	 */
	private static Condition getFullCondition(Node conditionNode) {

		List<Condition> subconditions = new LinkedList<Condition>();

		for (int i = 0; i < conditionNode.getChildNodes().getLength(); i++) {
			Node node = conditionNode.getChildNodes().item(i);

			if (XMLUtils.hasContent(node)) {
				Condition subcondition = getSubcondition(node);
				subconditions.add(subcondition);
			}
		}

		if (subconditions.isEmpty()) {
			return new VoidCondition();
		}
		else {
			if (conditionNode.hasAttributes() && conditionNode.getAttributes()
					.getNamedItem("operator") != null) {
				String operatorStr = conditionNode.getAttributes()
						.getNamedItem("operator").getNodeValue();
				if (operatorStr.toLowerCase().trim().equals("and")) {
					return new ComplexCondition(subconditions, BinaryOperator.AND);
				}
				else if (operatorStr.toLowerCase().trim().equals("or")) {
					return new ComplexCondition(subconditions, BinaryOperator.OR);
				}
				else if (operatorStr.toLowerCase().trim().equals("neg")
						|| operatorStr.toLowerCase().trim().equals("not")) {
					Condition negated =
							(subconditions.size() == 1) ? subconditions.get(0)
									: new ComplexCondition(subconditions,
											BinaryOperator.AND);
					return new NegatedCondition(negated);
				}
				else {
					throw new RuntimeException("Invalid operator: " + operatorStr);
				}
			}
			return (subconditions.size() == 1) ? subconditions.get(0)
					: new ComplexCondition(subconditions, BinaryOperator.AND);
		}
	}

	/**
	 * Extracting a partial condition from a rule specification
	 * 
	 * @param node the XML node
	 * @return the corresponding condition
	 */
	private static Condition getSubcondition(Node node) {

		// extracting a basic condition
		if (node.getNodeName().equals("if")) {

			if (!node.hasAttributes()
					|| node.getAttributes().getNamedItem("var") == null) {
				throw new RuntimeException("<if> without attribute 'var'");
			}

			Condition condition;

			String variable =
					node.getAttributes().getNamedItem("var").getNodeValue();
			Template tvar = Template.create(variable);
			if (tvar.isUnderspecified()) {
				tvar = Template.create(tvar.toString().replace("*",
						"{" + (new Random().nextInt(100)) + "}"));
			}

			if (node.getAttributes().getNamedItem("value") != null) {
				String valueStr =
						node.getAttributes().getNamedItem("value").getNodeValue();
				Relation relation = getRelation(node);
				condition = new BasicCondition(variable, valueStr, relation);
			}

			else if (node.getAttributes().getNamedItem("var2") != null) {
				String variable2 =
						node.getAttributes().getNamedItem("var2").getNodeValue();
				Relation relation = getRelation(node);
				condition = new BasicCondition(variable, "{" + variable2 + "}",
						relation);
			}
			else {
				throw new RuntimeException("unrecognized format for condition ");
			}

			for (int i = 0; i < node.getAttributes().getLength(); i++) {
				Node attr = node.getAttributes().item(i);
				if (!attr.getNodeName().equals("var")
						&& !attr.getNodeName().equals("var2")
						&& !attr.getNodeName().equals("value")
						&& !attr.getNodeName().equals("relation")) {
					throw new RuntimeException(
							"unrecognized attribute: " + attr.getNodeName());
				}
			}
			return condition;
		}

		// extracting a conjunction or disjunction
		else if (node.getNodeName().equals("or")
				|| node.getNodeName().equals("and")) {

			BinaryOperator operator = (node.getNodeName().equals("or"))
					? BinaryOperator.OR : BinaryOperator.AND;

			List<Condition> conditions = new ArrayList<Condition>();
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				Node subNode = node.getChildNodes().item(i);
				if (XMLUtils.hasContent(subNode)) {
					conditions.add(getSubcondition(subNode));
				}
			}
			return new ComplexCondition(conditions, operator);
		}
		// extracting a negated conjunction
		else if (node.getNodeName().equals("neg")
				|| node.getNodeName().equals("not")) {

			List<Condition> conditions = new ArrayList<Condition>();
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				Node subNode = node.getChildNodes().item(i);
				if (XMLUtils.hasContent(subNode)) {
					conditions.add(getSubcondition(subNode));
				}
			}
			if (conditions.size() == 1) {
				return new NegatedCondition(conditions.get(0));
			}
			else if (conditions.size() > 1) {
				return new NegatedCondition(
						new ComplexCondition(conditions, BinaryOperator.AND));
			}
		}
		else if (XMLUtils.hasContent(node)) {
			throw new RuntimeException("Invalid condition: " + node.getNodeName());
		}
		return new VoidCondition();
	}

	/**
	 * Extracts the relation specified in a condition
	 * 
	 * @param node the XML node containing the relation
	 * @return the corresponding relation
	 */
	private static Relation getRelation(Node node) {
		Relation relation = Relation.EQUAL;
		if (node.hasAttributes()
				&& node.getAttributes().getNamedItem("relation") != null) {
			String relationStr =
					node.getAttributes().getNamedItem("relation").getNodeValue();
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
			else if (relationStr.toLowerCase().trim().equals("length")) {
				relation = Relation.LENGTH;
			}
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
				throw new RuntimeException("invalid relation: " + relationStr);
			}

		}
		return relation;
	}

	/**
	 * Extracts a full effect from the XML specification.
	 * 
	 * @param node the XML node
	 * @param priority the rule priority
	 * @return the corresponding effect @
	 */
	private static Effect getFullEffect(Node effectNode, int priority) {

		List<BasicEffect> effects = new LinkedList<BasicEffect>();

		for (int i = 0; i < effectNode.getChildNodes().getLength(); i++) {
			Node node = effectNode.getChildNodes().item(i);

			if (XMLUtils.hasContent(node) && node.hasAttributes()) {
				BasicEffect subeffect = getSubEffect(node, priority);
				effects.add(subeffect);
			}
		}
		return new Effect(effects);
	}

	/**
	 * Extracts a basic effect from the XML specification.
	 * 
	 * @param node the XML node
	 * @param priority the rule priority
	 * @return the corresponding basic effect
	 */
	private static BasicEffect getSubEffect(Node node, int priority) {

		NamedNodeMap attrs = node.getAttributes();
		if (attrs.getNamedItem("var") == null) {
			throw new RuntimeException("Effect without attribute 'var'");
		}

		String var = attrs.getNamedItem("var").getNodeValue();

		String value = null;
		if (attrs.getNamedItem("value") != null) {
			value = attrs.getNamedItem("value").getNodeValue();
		}
		else if (attrs.getNamedItem("var2") != null) {
			value = "{" + attrs.getNamedItem("var2").getNodeValue() + "}";
		}
		else {
			value = "None";
		}
		value = value.replaceAll("\\s+", " ");

		boolean exclusive = true;
		if (attrs.getNamedItem("exclusive") != null
				&& attrs.getNamedItem("exclusive").getNodeValue()
						.equalsIgnoreCase("false")) {
			exclusive = false;
		}

		boolean negated = attrs.getNamedItem("relation") != null
				&& getRelation(node) == Relation.UNEQUAL;

		// "clear" effect is outdated
		if (node.getNodeName().equalsIgnoreCase("clear")) {
			value = "None";
		}

		// checking for other attributes
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (!attr.getNodeName().equals("var")
					&& !attr.getNodeName().equals("var2")
					&& !attr.getNodeName().equals("value")
					&& !attr.getNodeName().equals("relation")
					&& !attr.getNodeName().equals("exclusive")) {
				throw new RuntimeException(
						"unrecognized attribute: " + attr.getNodeName());
			}
		}

		Template tvar = Template.create(var);
		Template tval = Template.create(value);
		if (tvar.isUnderspecified() || tval.isUnderspecified()) {
			return new TemplateEffect(tvar, tval, priority, exclusive, negated);
		}
		else {
			return new BasicEffect(var, ValueFactory.create(tval.toString()),
					priority, exclusive, negated);
		}

	}

	/**
	 * Returns the parameter described by the XML specification.
	 * 
	 * @param node the XML node
	 * @param type the rule type
	 * @return the parameter representation @
	 */
	private static Parameter getParameter(Node node, RuleType type) {

		if (type == RuleType.PROB) {
			if (node.getAttributes().getNamedItem("prob") != null) {
				String prob =
						node.getAttributes().getNamedItem("prob").getNodeValue();
				return getInnerParameter(prob);
			}
			else {
				return new FixedParameter(1.0);
			}
		}
		else if (type == RuleType.UTIL) {
			if (node.getAttributes().getNamedItem("util") != null) {
				String util =
						node.getAttributes().getNamedItem("util").getNodeValue();
				return getInnerParameter(util);
			}
		}
		throw new RuntimeException("parameter is not accepted");
	}

	/**
	 * Returns the parameter described by the XML specification.
	 * 
	 * @param node the XML node
	 * @return the parameter representation @
	 */
	private static Parameter getInnerParameter(String paramStr) {

		// we first try to extract a fixed value
		try {
			double weight = Double.parseDouble(paramStr);
			return new FixedParameter(weight);
		}

		// if it fails, we extract an actual unknown parameter
		catch (NumberFormatException e) {
			// if we have a complex expression of parameters
			if (paramStr.contains("{") || paramStr.contains("+")
					|| paramStr.contains("*") || paramStr.contains("-")) {
				return new ComplexParameter(new MathExpression(paramStr));
			}

			// else, we extract a stochastic parameter
			else {
				Pattern p = Pattern.compile(".+(\\[[0-9]+\\])");
				Matcher m = p.matcher(paramStr);
				if (m.matches()) {
					int index = Integer
							.parseInt(m.group(1).replace("[", "").replace("]", ""));
					String paramId = paramStr.replace(m.group(1), "").trim();
					return new SingleParameter(paramId, index);
				}
				else {
					return new SingleParameter(paramStr);
				}
			}
		}
	}

}
