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

package opendial.templates;

import java.util.Collection;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.MathExpression;
import opendial.utils.StringUtils;

/**
 * Template for an arithmetic template, such as "exp(({A}+{B})/{C})". When filling
 * the slots of the template, the function is evaluated.
 *
 */
class ArithmeticTemplate extends RegexTemplate {

	public ArithmeticTemplate(String rawString) {
		super(rawString);
	}

	/**
	 * Fills the slots of the template, and returns the result of the function
	 * evaluation. If the function is not a simple arithmetic expression,
	 */
	@Override
	public String fillSlots(Assignment fillers) {

		String filled = super.fillSlots(fillers);
		if (filled.contains("{")) {
			return filled;
		}

		if (isArithmeticExpression(filled)) {
			try {
				double result = new MathExpression(filled).evaluate();
				return StringUtils.getShortForm(result);
			}
			catch (Exception e) {
				log.warning("cannot evaluate " + filled);
				return filled;
			}
		}

		// handling expressions that manipulate sets
		// (using + and - to respectively add/remove elements)
		Value merge = ValueFactory.none();
		for (String split : filled.split("\\+")) {
			String[] negation = split.split("\\-");
			merge = merge.concatenate(ValueFactory.create(negation[0]));
			for (int i = 1; i < negation.length; i++) {
				Collection<Value> values = merge.getSubValues();
				values.remove(ValueFactory.create(negation[i]));
				merge = ValueFactory.create(values);
			}
		}
		return merge.toString();
	}

	/**
	 * Returns true if the string corresponds to an arithmetic expression, and false
	 * otherwise
	 * 
	 * @param exp the string to check
	 * @return true if the string is an arithmetic expression, false otherwise
	 */
	public static boolean isArithmeticExpression(String exp) {
		boolean mathOperators = false;
		StringBuilder curString = new StringBuilder();
		for (int i = 0; i < exp.length(); i++) {
			char c = exp.charAt(i);
			if (c == '+' || c == '-' || c == '/' || (c == '*' && exp.length() > 2)) {
				mathOperators = true;
			}
			else if (c == '?' || c == '|' || c == '[' || c == '_' || c == '\'') {
				return false;
			}
			else if (Character.isLetter(c)) {
				curString.append(c);
			}
			else if (StringUtils.isDelimiter(c)) {
				if (!MathExpression.fixedFunctions.contains(curString.toString())) {
					return false;
				}
				mathOperators = true;
				curString = new StringBuilder();
			}
		}
		return (mathOperators);
	}

}
