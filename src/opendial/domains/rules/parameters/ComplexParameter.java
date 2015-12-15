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
import java.util.logging.Logger;

import opendial.datastructs.Assignment;
import opendial.datastructs.MathExpression;

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

	// mathematical expression
	final MathExpression expression;

	/**
	 * Constructs a new complex parameter with the given expression, assuming the
	 * list of parameters is provided as labels within the expression.
	 * 
	 * @param expression the expression
	 */
	public ComplexParameter(MathExpression expression) {
		this.expression = expression;
	}

	/**
	 * Returns the parameter value corresponding to the expression and the assignment
	 * of values to the unknown parameters.
	 */
	@Override
	public double getValue(Assignment input) {
		try {
			return expression.evaluate(input);
		}
		catch (IllegalArgumentException e) {
			log.warning("cannot evaluate " + this + " given " + input + ": "
					+ e.getMessage());
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
		if (input.containsVars(expression.getVariables())) {
			try {
				double result = expression.evaluate(input);
				return new FixedParameter(result);
			}
			catch (IllegalArgumentException e) {
				log.warning("cannot ground " + expression + " with " + input);
			}
		}

		String filled = expression.toString();
		for (String u : input.getVariables()) {
			filled.replaceAll(u, input.getValue(u).toString());
		}

		return new ComplexParameter(new MathExpression(filled));
	}

	/**
	 * Returns the list of unknown parameter variables
	 */
	@Override
	public Collection<String> getVariables() {
		return expression.getVariables();
	}

	/**
	 * Returns the mathematical expression representing the parameter
	 * 
	 * @return the expression
	 */
	@Override
	public MathExpression getExpression() {
		return expression;
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
