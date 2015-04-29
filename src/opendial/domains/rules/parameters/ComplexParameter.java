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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import opendial.arch.Logger;
import opendial.bn.values.DoubleVal;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;

/**
 * Representation of a complex parameter expression. The class uses the exp4j
 * package to dynamically evaluate the result of the expression.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ComplexParameter implements Parameter {

	// logger
	public static Logger log = new Logger("TemplateParameter",
			Logger.Level.DEBUG);

	// template expression for the parameter
	Template template;

	// mathematical expression
	Expression expression;

	// unknown parameter variables
	Set<String> unknowns;

	/**
	 * Constructs a new complex parameter with the given expression, and the
	 * list of unknown parameter variables whose values must be provided.
	 * 
	 * @param expression the expression
	 * @param unknowns the list of unknown parameter variables
	 */
	public ComplexParameter(String expression, Collection<String> unknowns) {
		template = new Template(expression);
		this.expression = new ExpressionBuilder(
				template.getStringWithoutBraces()).variables(
				template.getSlots()).build();
		this.unknowns = new HashSet<String>(unknowns);
	}

	/**
	 * Returns the parameter value corresponding to the expression and the
	 * assignment of values to the unknown parameters.
	 */
	@Override
	public double getParameterValue(Assignment input) {
		if (template.isFilledBy(input)) {
			Map<String, Double> valueMap = input
					.getEntrySet()
					.stream()
					.filter(e -> e.getValue() instanceof DoubleVal)
					.collect(
							Collectors.toMap(e -> e.getKey(),
									e -> ((DoubleVal) e.getValue()).getDouble()));
			return expression.setVariables(valueMap).evaluate();
		} else {
			log.warning("cannot evaluate parameter " + this + " given " + input);
			return 0.0;
		}
	}

	/**
	 * Grounds the parameter by assigning the values in the assignment to the
	 * unknown variables
	 * 
	 * @param input the grounding assignment
	 * @return the grounded parameter
	 */
	public Parameter ground(Assignment input) {
		if (template.isFilledBy(input)) {
			return new FixedParameter(getParameterValue(input));
		} else {
			return new ComplexParameter(template.fillSlots(input).toString(),
					unknowns);
		}
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
		return template.toString();
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
