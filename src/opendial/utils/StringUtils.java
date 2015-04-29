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

package opendial.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import opendial.arch.Logger;

/**
 * Various utilities for manipulating strings
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class StringUtils {

	// logger
	public static Logger log = new Logger("StringUtils", Logger.Level.DEBUG);


	// regular expression to detect algebraic expressions
	final static Pattern mathExpression = Pattern
			.compile("[0-9|\\-\\.\\s]+[+\\-*/][0-9|\\-\\.\\s]+");

	// regular expression with * characters
	final static Pattern simpleRegex = Pattern.compile("\\*");

	// regular expression with alternative or optional elements
	final static Pattern complexRegex = Pattern
			.compile("\\\\\\((.+?)\\\\\\)(\\\\\\?)?");


	/**
	 * Returns the string version of the double up to a certain decimal point.
	 * 
	 * @param value the double
	 * @return the string
	 */
	public static String getShortForm(double value) {
		String rounded = "" + Math.round(value * 10000.0) / 10000.0;
		if (rounded.endsWith(".0")) {
			rounded = rounded.substring(0, rounded.length() - 2);
		}
		return rounded;
	}

	/**
	 * Returns a HTML-compatible rendering of the raw string provided as
	 * argument
	 * 
	 * @param str the raw string
	 * @return the formatted string
	 */
	public static String getHtmlRendering(String str) {
		str = str.replace("phi", "&phi;");
		str = str.replace("theta", "&theta;");
		str = str.replace("psi", "&psi;");
		Matcher matcher = Pattern.compile("_\\{(\\p{Alnum}*?)\\}").matcher(str);
		while (matcher.find()) {
			String subscript = matcher.group(1);
			str = str.replace("_{" + subscript + "}", "<sub>" + subscript
					+ "</sub>");
		}
		Matcher matcher2 = Pattern.compile("_(\\p{Alnum}*)").matcher(str);
		while (matcher2.find()) {
			String subscript = matcher2.group(1);
			str = str.replace("_" + subscript, "<sub>" + subscript + "</sub>");
		}
		Matcher matcher3 = Pattern.compile("\\^\\{(\\p{Alnum}*?)\\}").matcher(
				str);
		while (matcher3.find()) {
			String subscript = matcher3.group(1);
			str = str.replace("^{" + subscript + "}", "<sup>" + subscript
					+ "</sup>");
		}
		Matcher matcher4 = Pattern.compile("\\^([\\w\\-\\^]+)").matcher(str);
		while (matcher4.find()) {
			String subscript = matcher4.group(1);
			str = str.replace("^" + subscript, "<sup>" + subscript + "</sup>");
		}
		return str;
	}

	/**
	 * Returns the total number of occurrences of the character in the string.
	 * 
	 * @param s the string
	 * @param c the character to search for
	 * @return the number of occurrences
	 */
	public static long countNbOccurrences(String s, char c) {
		return s.chars().filter(sc -> sc == c).count();
	}

	/**
	 * Checks the form of the string to ensure that all parentheses, braces and
	 * brackets are balanced. Logs warning messages if problems are detected.
	 * 
	 * @param str the string
	 */
	public static void checkForm(String str) {

		if (countNbOccurrences(str, '(') != countNbOccurrences(str, ')')) {
			log.warning("Unequal number of parenthesis in string: " + str
					+ ", Problems ahead!");
		}
		if (countNbOccurrences(str, '{') != countNbOccurrences(str, '}')) {
			log.warning("Unequal number of braces in string: " + str
					+ ", Problems ahead!");
		}
		if (countNbOccurrences(str, '[') != countNbOccurrences(str, ']')) {
			log.warning("Unequal number of brackets in string: " + str
					+ ", Problems ahead!");
		}

	}

	/**
	 * Performs a lexicographic comparison of the two identifiers. If there is a
	 * difference between the number of primes in the two identifiers, returns
	 * it. Else, returns +1 if id1.compareTo(id2) is higher than 0, and -1
	 * otherwise.
	 * 
	 * @param id1 the first identifier
	 * @param id2 the second identifier
	 * @return the result of the comparison
	 */
	public static int compare(String id1, String id2) {
		if (id1.contains("'") || id2.contains("'")) {
			int count1 = id1.length() - id1.replace("'", "").length();
			int count2 = id2.length() - id2.replace("'", "").length();
			if (count1 != count2) {
				return count2 - count1;
			}
		}
		return (id1.compareTo(id2) < 0) ? +1 : -1;
	}

	/**
	 * Joins the string elements into a single string where the elements are
	 * joined by a specific string.
	 * 
	 * @param elements the string elements
	 * @param jointure the string used to join the elements
	 * @return the concatenated string.
	 */
	public static String join(Collection<? extends Object> elements,
			String jointure) {
		return elements.stream().map(o -> o.toString())
				.collect(Collectors.joining(jointure));
	}

	/**
	 * Returns a table with probabilities from the provided GUI input
	 * 
	 * @param rawText the raw text expressing the table
	 * @return the string values together with their probabilities
	 */
	public static Map<String, Double> getTableFromInput(String rawText) {

		Map<String, Double> table = new HashMap<String, Double>();

		Pattern p = Pattern
				.compile(".*\\(([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)\\).*");

		for (String split : rawText.split(";")) {
			Matcher m = p.matcher(split);
			if (m.find()) {
				String probValueStr = m.group(1);
				double probValue = Double.parseDouble(probValueStr);
				String remainingStr = split.replace("(" + probValueStr + ")",
						"").trim();
				table.put(remainingStr, probValue);
			} else {
				table.put(split.trim(), 1.0);
			}
		}
		return table;
	}

	/**
	 * Counts the occurrences of a particular pattern in the string.
	 * 
	 * @param fullString the string to use
	 * @param pattern the pattern to search for
	 * @return the number of occurrences.
	 */
	public static int countOccurrences(String fullString, String pattern) {
		int lastIndex = 0;
		int count = 0;

		while (lastIndex != -1) {

			lastIndex = fullString.indexOf(pattern, lastIndex);

			if (lastIndex != -1) {
				count++;
				lastIndex += pattern.length();
			}
		}
		return count;
	}


	/**
	 * Returns true if the string corresponds to an arithmetic expression, and false
	 * otherwise
	 * 
	 * @param exp the string to check
	 * @return true if the string is an arithmetic expression, false otherwise
	 */
	public static boolean isArithmeticExpression(String exp) {
		return !exp.contains("{") && mathExpression.matcher(exp).matches();
	}



	public static String escape(String init) {
		StringBuilder builder = new StringBuilder();
		char[] charArr = init.toCharArray();

		for (int i = 0; i < charArr.length; i++) {
			if (charArr[i] == '(') {
				builder.append("\\(");
			} else if (charArr[i] == ')') {
				builder.append("\\)");
			} else if (charArr[i] == '[') {
				builder.append("\\[");
			} else if (charArr[i] == ']') {
				builder.append("\\]");
			} else if (charArr[i] == '?') {
				builder.append("\\?");
			} else if (charArr[i] == ' ') {
				builder.append(" ");
				for (int j = i + 1; j < charArr.length; j++) {
					if (charArr[j] == ' ') { i++; } 
					else { break; }
				}
			} else if (charArr[i] == '.') {
				builder.append("\\.");
			} else if (charArr[i] == '!') {
				builder.append("\\!");
			} else if (charArr[i] == '^') {
				builder.append("\\^");
			}
			else if (charArr[i] == '{' && charArr[i + 1] == '}') {
				builder.append("\\{\\}");
				i++;
			} else {
				builder.append(charArr[i]);
			}
		}
		return builder.toString();
	}


	/**
	 * Formats the regular expression corresponding to the provided string
	 * 
	 * @param init the initial string
	 * @return the corresponding expression
	 */
	public static String constructRegex(String init) {

		boolean hasStars = init.chars().anyMatch(c -> c=='*');
		boolean hasAlternatives = init.chars().anyMatch(c -> c=='|'|c=='?');
		
		init = (hasStars)? replaceStars(init) : init;
		init = (hasAlternatives)? replaceComplex(init) : init;
		
		return init;		
	}


	/**
	 * Replaces the * characters in the string by a proper regular expression
	 * 
	 * @param init the initial string
	 * @return the formatted expression
	 */
	private static String replaceStars(String init) {
		StringBuilder builder = new StringBuilder();
		char[] chars = init.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '*' && i == 0 && chars.length > 1
					&& chars[i + 1] == ' ') {
				builder.append("(?:.+ |)");
				i++;
			} else if (chars[i] == '*' && i < (chars.length - 1) && i > 0
					&& chars[i + 1] == ' ' && chars[i - 1] == ' ') {
				builder.deleteCharAt(builder.length() - 1);
				builder.append("(?:.+|)");
			} else if (chars[i] == '*' && i == (chars.length - 1) && i > 0
					&& chars[i - 1] == ' ') {
				builder.deleteCharAt(builder.length() - 1);
				builder.append("(?: .+|)");
			} else if (chars[i] == '*') {
				builder.append("(?:.*)");
			}
			else {
				builder.append(chars[i]);
			}
		}
		return builder.toString();
	}


	/**
	 * Replace the alternative or optional elements by a proper regular expression
	 * 
	 * @param init the initial string
	 * @return the formatted expression
	 */
	private static String replaceComplex(String init) {

		StringBuilder builder = new StringBuilder(init);
		Matcher m = complexRegex.matcher(builder.toString());
		while (m.find()) {
			String core = m.group(1);
			if (m.group(0).endsWith("?")) {
				// need to remove whitespaces at specific positions
				if (m.end() < builder.length()
						&& builder.charAt(m.end()) == ' ') {
					String replace = "(?:" + core.replaceAll("\\|", " \\|")
							+ " )?";
					builder = builder.replace(m.start(), m.end() + 1,
							replace);
				} else if (m.end() >= builder.length() && m.start() > 0
						&& builder.charAt(m.start() - 1) == ' ') {
					String replace = "(?: "
							+ core.replaceAll("\\|", "\\| ") + ")?";
					builder = builder.replace(m.start() - 1, m.end(),
							replace);
				} else {
					builder = builder.replace(m.start(), m.end(), "(?:"
							+ core + ")?");
				}
			} else {
				builder = builder.replace(m.start(), m.end(), "(?:" + core
						+ ")");
			}
			m = complexRegex.matcher(builder.toString());
		}
		return builder.toString();
	}


}
