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
import java.util.Random;
import java.util.Set;
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
import opendial.domains.rules.conditions.NegatedCondition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.TemplateEffect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.utils.XMLUtils;

import org.w3c.dom.Node;

/**
 * Extraction of a probabilistic rule given an XML specification
 *
 * @author Pierre Lison (plison@ifi.uio.no)
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
		if (topNode.hasAttributes()
				&& topNode.getAttributes().getNamedItem("id") != null) {
			ruleId = topNode.getAttributes().getNamedItem("id").getNodeValue();
		} else {
			ruleId = "rule" + idCounter;
			idCounter++;
		}

		// creating the rule
		Rule rule = new Rule(ruleId, type);

		// extracting the rule cases
		for (int i = 0; i < topNode.getChildNodes().getLength(); i++) {
			Node node = topNode.getChildNodes().item(i);

			if (node.getNodeName().equals("case")) {
				RuleCase newCase = getCase(node, type);
				rule.addCase(newCase);

			} else if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				throw new DialException("Ill-formed rule: "
						+ node.getNodeName() + " not accepted");
			}
		}

		if (topNode.hasAttributes()
				&& topNode.getAttributes().getNamedItem("priority") != null) {
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
	private static RuleCase getCase(Node caseNode, RuleType type)
			throws DialException {

		RuleCase newCase = new RuleCase();

		for (int i = 0; i < caseNode.getChildNodes().getLength(); i++) {
			Node node = caseNode.getChildNodes().item(i);

			// extracting the condition
			if (node.getNodeName().equals("condition")) {
				Condition condition = getFullCondition(node);
				newCase = new RuleCase(condition);
			}
			// extracting an effect
			else if (node.getNodeName().equals("effect")) {
				Effect effect = getFullEffect(node);
				if (effect != null) {
					Parameter prob = getParameter(node, newCase.getCondition()
							.getSlots(), type);
					newCase.addEffect(effect, prob);
				}
			} else if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				throw new DialException("Ill-formed rule: "
						+ node.getNodeName() + " not accepted");
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
	private static Condition getFullCondition(Node conditionNode)
			throws DialException {

		List<Condition> subconditions = new LinkedList<Condition>();

		for (int i = 0; i < conditionNode.getChildNodes().getLength(); i++) {
			Node node = conditionNode.getChildNodes().item(i);

			if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				Condition subcondition = getSubcondition(node);
				subconditions.add(subcondition);
			}
		}

		if (subconditions.isEmpty()) {
			return new VoidCondition();
		} else {
			if (conditionNode.hasAttributes()
					&& conditionNode.getAttributes().getNamedItem("operator") != null) {
				String operatorStr = conditionNode.getAttributes()
						.getNamedItem("operator").getNodeValue();
				if (operatorStr.toLowerCase().trim().equals("and")) {
					return new ComplexCondition(subconditions,
							BinaryOperator.AND);
				} else if (operatorStr.toLowerCase().trim().equals("or")) {
					return new ComplexCondition(subconditions,
							BinaryOperator.OR);
				} else if (operatorStr.toLowerCase().trim().equals("neg")
						|| operatorStr.toLowerCase().trim().equals("not")) {
					Condition negated = (subconditions.size() == 1) ? subconditions
							.get(0) : new ComplexCondition(subconditions,
							BinaryOperator.AND);
					return new NegatedCondition(negated);
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
	 * @throws DialException if the condition is ill-defined
	 */
	private static Condition getSubcondition(Node node) throws DialException {

		// extracting a basic condition
		if (node.getNodeName().equals("if") && node.hasAttributes()
				&& node.getAttributes().getNamedItem("var") != null) {

			Condition condition;

			String variable = node.getAttributes().getNamedItem("var")
					.getNodeValue();
			Template tvar = new Template(variable);
			if (tvar.isUnderspecified()) {
				tvar = new Template(tvar.getRawString().replace("*",
						"{" + (new Random().nextInt(100)) + "}"));
			}

			if (node.getAttributes().getNamedItem("value") != null) {
				String valueStr = node.getAttributes().getNamedItem("value")
						.getNodeValue();
				Relation relation = getRelation(node);
				condition = new BasicCondition(variable, valueStr, relation);
			}

			else if (node.getAttributes().getNamedItem("var2") != null) {
				String variable2 = node.getAttributes().getNamedItem("var2")
						.getNodeValue();
				Relation relation = getRelation(node);
				condition = new BasicCondition(variable, "{" + variable2 + "}",
						relation);
			} else {
				throw new DialException("unrecognized format for condition ");
			}

			for (int i = 0; i < node.getAttributes().getLength(); i++) {
				Node attr = node.getAttributes().item(i);
				if (!attr.getNodeName().equals("var")
						&& !attr.getNodeName().equals("var2")
						&& !attr.getNodeName().equals("value")
						&& !attr.getNodeName().equals("relation")) {
					throw new DialException("unrecognized attribute: "
							+ attr.getNodeName());
				}
			}
			return condition;
		}

		// extracting a conjunction or disjunction
		else if (node.getNodeName().equals("or")
				|| node.getNodeName().equals("and")) {

			BinaryOperator operator = (node.getNodeName().equals("or")) ? BinaryOperator.OR
					: BinaryOperator.AND;

			List<Condition> conditions = new ArrayList<Condition>();
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				Node subNode = node.getChildNodes().item(i);
				if (!subNode.getNodeName().equals("#text")
						&& !subNode.getNodeName().equals("#comment")) {
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
				if (!subNode.getNodeName().equals("#text")
						&& !subNode.getNodeName().equals("#comment")) {
					conditions.add(getSubcondition(subNode));
				}
			}
			if (conditions.size() == 1) {
				return new NegatedCondition(conditions.get(0));
			} else if (conditions.size() > 1) {
				return new NegatedCondition(new ComplexCondition(conditions,
						BinaryOperator.AND));
			}
		}
		return new VoidCondition();
	}

	/**
	 * Extracts the relation specified in a condition
	 * 
	 * @param node the XML node containing the relation
	 * @return the corresponding relation
	 * @throws DialException if the relation is ill-defined
	 */
	private static Relation getRelation(Node node) throws DialException {
		Relation relation = Relation.EQUAL;
		if (node.hasAttributes()
				&& node.getAttributes().getNamedItem("relation") != null) {
			String relationStr = node.getAttributes().getNamedItem("relation")
					.getNodeValue();
			if (relationStr.toLowerCase().trim().equals("=")) {
				relation = Relation.EQUAL;
			} else if (relationStr.toLowerCase().trim().equals("!=")) {
				relation = Relation.UNEQUAL;
			} else if (relationStr.toLowerCase().trim().equals("equal")) {
				relation = Relation.EQUAL;
			} else if (relationStr.toLowerCase().trim().equals("unequal")) {
				relation = Relation.UNEQUAL;
			} else if (relationStr.toLowerCase().trim().equals("contains")) {
				relation = Relation.CONTAINS;
			} else if (relationStr.toLowerCase().trim().equals("in")) {
				relation = Relation.IN;
			} else if (relationStr.toLowerCase().trim().equals("length")) {
				relation = Relation.LENGTH;
			} else if (relationStr.toLowerCase().trim().equals("!contains")) {
				relation = Relation.NOT_CONTAINS;
			} else if (relationStr.toLowerCase().trim().equals("!in")) {
				relation = Relation.NOT_IN;
			} else if (relationStr.toLowerCase().trim().equals(">")) {
				relation = Relation.GREATER_THAN;
			} else if (relationStr.toLowerCase().trim().equals("<")) {
				relation = Relation.LOWER_THAN;
			} else {
				throw new DialException("unrecognized relation: " + relationStr);
			}

		}
		return relation;
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

		for (int i = 0; i < effectNode.getChildNodes().getLength(); i++) {
			Node node = effectNode.getChildNodes().item(i);

			if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")
					&& node.hasAttributes()) {
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

		if (node.getAttributes().getNamedItem("var") == null) {
			throw new DialException("invalid format for effect: "
					+ node.getNodeName() + " and var "
					+ node.getAttributes().getNamedItem("var"));
		}

		String var = node.getAttributes().getNamedItem("var").getNodeValue();

		String value = null;
		if (node.getAttributes().getNamedItem("value") != null) {
			value = node.getAttributes().getNamedItem("value").getNodeValue();
		} else if (node.getAttributes().getNamedItem("var2") != null) {
			value = "{"
					+ node.getAttributes().getNamedItem("var2").getNodeValue()
					+ "}";
		} else {
			value = "None";
		}
		value = value.replaceAll("\\s+", " ");

		boolean add = node.getNodeName().equalsIgnoreCase("add")
				|| (node.getAttributes().getNamedItem("add") != null && Boolean
						.parseBoolean(node.getAttributes().getNamedItem("add")
								.getNodeValue()));

		boolean negated = node.getAttributes().getNamedItem("relation") != null
				&& getRelation(node) == Relation.UNEQUAL;

		// "clear" effect is outdated
		if (node.getNodeName().equalsIgnoreCase("clear")) {
			value = "None";
		}

		// checking for other attributes
		for (int i = 0; i < node.getAttributes().getLength(); i++) {
			Node attr = node.getAttributes().item(i);
			if (!attr.getNodeName().equals("var")
					&& !attr.getNodeName().equals("var2")
					&& !attr.getNodeName().equals("value")
					&& !attr.getNodeName().equals("relation")
					&& !attr.getNodeName().equals("add")) {
				throw new DialException("unrecognized attribute: "
						+ attr.getNodeName());
			}
		}

		Template tvar = new Template(var);
		Template tval = new Template(value);
		if (tvar.isUnderspecified() || tval.isUnderspecified()) {
			return new TemplateEffect(tvar, tval, 1, add, negated);
		} else {
			return new BasicEffect(var, ValueFactory.create(tval.toString()),
					1, add, negated);
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
	private static Parameter getParameter(Node node, Set<String> caseSlots,
			RuleType type) throws DialException {

		if (type == RuleType.PROB) {
			if (node.getAttributes().getNamedItem("prob") != null) {
				String prob = node.getAttributes().getNamedItem("prob")
						.getNodeValue();
				return getInnerParameter(prob, caseSlots);
			} else {
				return new FixedParameter(1.0);
			}
		} else if (type == RuleType.UTIL) {
			if (node.getAttributes().getNamedItem("util") != null) {
				String util = node.getAttributes().getNamedItem("util")
						.getNodeValue();
				return getInnerParameter(util, caseSlots);
			}
		}
		throw new DialException("parameter is not accepted");
	}

	/**
	 * Returns the parameter described by the XML specification.
	 * 
	 * @param node the XML node
	 * @param caseSlots the underspecified slots for the case
	 * @return the parameter representation
	 * @throws DialException
	 */
	private static Parameter getInnerParameter(String paramStr,
			Set<String> caseSlots) throws DialException {

		// we first try to extract a fixed value
		try {
			double weight = Double.parseDouble(paramStr);
			return new FixedParameter(weight);
		}

		// if it fails, we extract an actual unknown parameter
		catch (NumberFormatException e) {
			// if we have a complex expression of parameters
			if (paramStr.contains("{")) {
				Template t = new Template(paramStr);
				Set<String> unknowns = t.getSlots();
				unknowns.removeAll(caseSlots);
				return new ComplexParameter(paramStr, unknowns);
			}

			// else, we extract a stochastic parameter
			else {
				Pattern p = Pattern.compile(".+(\\[[0-9]+\\])");
				Matcher m = p.matcher(paramStr);
				if (m.matches()) {
					int index = Integer.parseInt(m.group(1).replace("[", "")
							.replace("]", ""));
					String paramId = paramStr.replace(m.group(1), "").trim();
					return new SingleParameter(paramId, index);
				} else {
					return new SingleParameter(paramStr);
				}
			}
		}
	}

}
