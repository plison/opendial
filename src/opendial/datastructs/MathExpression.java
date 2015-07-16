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

package opendial.datastructs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;
import net.objecthunter.exp4j.shuntingyard.ShuntingYard;
import net.objecthunter.exp4j.tokenizer.Token;

/**
 * Representation of a mathematical expression whose value can be evaluated. The
 * expression may contain unknown variables. In this case, one can evaluate the value
 * of the expression given a particular assignment of values.
 * 
 * The class builds on the exp4j package, see http://www.objecthunter.net/exp4j/.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class MathExpression {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// regular expression for brackets
	final static Pattern varlabelRegex = Pattern.compile("([a-zA-Z][\\w_]*)");

	// list of predefined functions in exp4j
	final static List<String> functions = Arrays.asList("abs", "acos", "asin",
			"atan", "cbrt", "ceil", "cos", "cosh", "exp", "floor", "log", "log10",
			"log2", "sin", "sinh", "sqrt", "tan", "tanh");

	/** The original string for the expression */
	final String expression;

	/** The tokens in the expression */
	final Token[] tokens;

	/** The unknown variable labels */
	final Set<String> variables;

	/**
	 * Creates a new mathematical expression from the string
	 * 
	 * @param expression the expression
	 */
	public MathExpression(String expression) {
		this.expression = expression;
		this.variables = getVariableLabels(expression);
		String local = expression.replaceAll("[\\[\\]\\{\\}]", "");
		tokens = ShuntingYard.convertToRPN(local, new HashMap<String, Function>(),
				new HashMap<String, Operator>(), getVariableLabels(local));
	}

	/**
	 * Creates a new mathematical expression that is a copy from another one
	 * 
	 * @param existing the expression to copy
	 */
	public MathExpression(MathExpression existing) {
		this.expression = existing.expression;
		this.variables = existing.variables;
		this.tokens = existing.tokens;
	}

	/**
	 * Returns the unknown variable labels in the expression
	 * 
	 * @return the variable labels
	 */
	public Set<String> getVariables() {
		return variables;
	}

	/**
	 * Evaluates the result of the expression
	 * 
	 * @return the result
	 */
	public double evaluate() {
		if (!variables.isEmpty()) {
			throw new RuntimeException("variables " + variables + " are not set");
		}
		Expression exp = new Expression(tokens);
		return exp.evaluate();
	}

	/**
	 * Evaluates the result of the expression, given an assignment of values to the
	 * unknown variables
	 * 
	 * @param input the assignment
	 * @return the result
	 */
	public double evaluate(Assignment input) {
		Expression exp = new Expression(tokens);
		exp.setVariables(getDoubles(input));
		return exp.evaluate();
	}

	/**
	 * Combines the current expression with one or more other expressions and a
	 * binary operator (such as +,* or -).
	 * 
	 * @param operator the operator between the expression
	 * @param elements the elements to add
	 * @return the expression corresponding to the combination
	 */
	public MathExpression combine(char operator, MathExpression... elements) {
		String newExpression = "(" + expression;
		for (int i = 0; i < elements.length; i++) {
			MathExpression element = elements[i];
			newExpression += operator + element.expression;
		}
		return new MathExpression(newExpression + ")");
	}

	/**
	 * Returns a set of possible variable labels in the given string.
	 * 
	 * @param str the string to analyse
	 * @return the extracted labels
	 */
	private static Set<String> getVariableLabels(String str) {
		Set<String> indexedVars = new HashSet<String>();
		Matcher m = varlabelRegex.matcher(str);
		while (m.find()) {
			String varlabel = m.group(1);
			if (!functions.contains(varlabel)) {
				indexedVars.add(varlabel);
			}
		}
		return indexedVars;
	}

	/**
	 * Returns a representation of the assignment limited to double values. For
	 * arrays, each variable is expanded into separate variables for each dimension.
	 * 
	 * @return the assignment of all double (and array) values
	 */
	private static Map<String, Double> getDoubles(Assignment assign) {
		Map<String, Double> doubles = new HashMap<String, Double>();
		for (String var : assign.getVariables()) {
			Value v = assign.getValue(var);
			if (v instanceof DoubleVal) {
				doubles.put(var, ((DoubleVal) v).getDouble());
			}
			else if (v instanceof ArrayVal) {
				double[] array = ((ArrayVal) v).getArray();
				for (int i = 0; i < array.length; i++) {
					doubles.put(var + i, array[i]);
				}
			}
		}
		return doubles;
	}

	/**
	 * Returns a string representation of the expression
	 */
	@Override
	public String toString() {
		return expression;
	}

	/**
	 * Returns true if the expressions are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof MathExpression)
				&& ((MathExpression) o).expression.equals(expression);
	}

	/**
	 * Returns the hashcode for the expression
	 */
	@Override
	public int hashCode() {
		return expression.hashCode();
	}

}
