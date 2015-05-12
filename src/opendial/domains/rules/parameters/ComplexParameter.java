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

package opendial.domains.rules.parameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.utils.StringUtils;

/**
 * Representation of a complex parameter expression. The class uses the exp4j package
 * to dynamically evaluate the result of the expression.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ComplexParameter implements Parameter {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the expression (as a string)
	final String expressionStr;

	// mathematical expression
	final Expression expression;

	// unknown parameter variables
	final Set<String> unknowns;

	/**
	 * Constructs a new complex parameter with the given expression, and the list of
	 * unknown parameter variables whose values must be provided.
	 * 
	 * @param expression the expression
	 * @param unknowns the list of unknown parameter variables
	 */
	public ComplexParameter(String expression, Set<String> unknowns) {
		Set<String> allSlots = new HashSet<String>(unknowns);
		if (expression.contains("{")) {
			allSlots.addAll(StringUtils.getSlots(expression));
			expression = StringUtils.removeBraces(expression);
		}
		this.expressionStr = expression;
		ExpressionBuilder builder = new ExpressionBuilder(expression);
		this.expression = builder.variables(allSlots).build();
		this.unknowns = new HashSet<String>(unknowns);
	}

	/**
	 * Returns the parameter value corresponding to the expression and the assignment
	 * of values to the unknown parameters.
	 */
	@Override
	public double getValue(Assignment input) {
		Map<String, Double> valueMap = new HashMap<String, Double>();
		for (String var : input.getVariables()) {
			Value v = input.getValue(var);
			if (v instanceof DoubleVal) {
				valueMap.put(var, ((DoubleVal) v).getDouble());
			}
			else {
				valueMap.put(var, 0.0);
			}
		}
		try {
			return expression.setVariables(valueMap).evaluate();
		}
		catch (IllegalArgumentException e) {
			log.warning("cannot evaluate parameter " + this + " given " + input);
			return 0.0;
		}
	}

	/**
	 * Grounds the parameter by assigning the values in the assignment to the unknown
	 * variables
	 * 
	 * @param input the grounding assignment
	 * @return the grounded parameter
	 */
	public Parameter ground(Assignment input) {
		if (input.getVariables().containsAll(unknowns)) {
			return new FixedParameter(getValue(input));
		}
		else {
			String filled = expressionStr;
			for (String u : input.getVariables()) {
				filled.replaceAll("\\{" + u + "\\}", input.getValue(u).toString());
			}
			unknowns.removeAll(input.getVariables());
			return new ComplexParameter(filled, unknowns);
		}
	}

	/**
	 * Sums the two parameters and returns the corresponding parameter
	 * 
	 * @param p2 the other parameter
	 * @return the parameter describing the sum of the two
	 */
	@Override
	public Parameter sum(Parameter p2) {
		Set<String> unknowns = new HashSet<String>(p2.getVariables());
		unknowns.addAll(getVariables());
		return new ComplexParameter(toString() + "+" + p2.toString(), unknowns);
	}

	/**
	 * Multiplies the two parameters and returns the corresponding parameter
	 * 
	 * @param p2 the other parameter
	 * @return the parameter describing the product of the two
	 */
	@Override
	public Parameter multiply(Parameter p2) {
		Set<String> unknowns = new HashSet<String>(p2.getVariables());
		unknowns.addAll(getVariables());
		return new ComplexParameter(toString() + "*" + p2.toString(), unknowns);
	}

	/**
	 * Returns the list of unknown parameter variables
	 */
	@Override
	public Collection<String> getVariables() {
		return unknowns;
	}

	/**
	 * Returns the parameter template as a string
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return expression.toString();
	}

	/**
	 * Returns the hashcode for the parameter
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return -3 * expression.hashCode();
	}

}
