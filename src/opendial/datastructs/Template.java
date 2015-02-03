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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.utils.MathUtils;
import opendial.utils.StringUtils;

/**
 * Representation of a string object containing a variable number (from 0 to n) of slots
 * to be filled, forming a template. 
 * 
 * <p>For instance, the raw string "Take the {BOX} to the {LOCATION}$ contains two slots:
 * a slot labeled BOX and a slot labeled LOCATION.
 * 
 * <p>The class offers several methods for constructing template strings, matching them against
 * strings, and filling their slots with specific values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Template {

	// logger
	public static Logger log = new Logger("Template", Logger.Level.DEBUG);

	// the initial string, containing the slots in raw form
	String rawString;

	// the regular expression pattern corresponding to the template
	Pattern pattern;

	// the slots, as a mapping between slot labels and their 
	// group number in the pattern
	Map<String,Integer> slots;

	// regular expression to detect algebraic expressions
	static Pattern mathExpression = Pattern.compile("[0-9|\\-\\.\\s]+[+\\-*/][0-9|\\-\\.\\s]+");

	// regular expression for complex regex (alternatives, optional elements)
	static Pattern complexRegex = Pattern.compile("\\\\\\((.+?)\\\\\\)(\\\\\\?)?");

	// ===================================
	//  TEMPLATE CONSTRUCTION
	// ===================================


	/**
	 * Creates a new template string, based on the raw string provided as
	 * argument.  The raw string must signal its slots by surrounding its 
	 * slots labels with { ... $}.
	 * 
	 * @param value the string value.
	 */
	public Template(String value) {

		rawString = value.trim();

		StringUtils.checkForm(rawString);

		slots = constructSlots(rawString);

		// string processing to avoid special characters for the pattern
		String regex = constructRegex(rawString);

		for (String slot : slots.keySet()) {
			regex = regex.replace("{"+constructRegex(slot)+"}", "(.*)");
		}

		// compiling the associated pattern
		try {
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}
		catch (PatternSyntaxException e) {
			log.warning("illegal pattern syntax: " + regex);
			pattern = Pattern.compile("bogus pattern");
		}

		if (slots.isEmpty() && mathExpression.matcher(rawString).matches()) {
			rawString = "" + MathUtils.evaluateExpression(rawString);
		}

	}


	protected Pattern getPattern() {
		return pattern;
	}

	public Template(Collection<Template> alternatives) {
		rawString = alternatives.toString().trim();
		slots = new HashMap<String, Integer>();
		String regex = "";
		for (Template t : alternatives) {
			regex += "(" + t.getPattern().pattern() + ")|";
		}
		regex = (!alternatives.isEmpty())? regex.substring(0, regex.length()-1) : regex;
		// compiling the associated pattern
		try {
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}
		catch (PatternSyntaxException e) {
			log.warning("illegal pattern syntax: " + regex);
			pattern = Pattern.compile("bogus pattern");
		}	
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the raw string for the template
	 * 
	 * @return the raw string
	 */
	public String getRawString() {
		return rawString;
	}


	/**
	 * Returns the (possibly empty) set of slots for the template
	 * 
	 * @return the set of slots
	 */
	public Set<String> getSlots() {
		return slots.keySet();
	}


	/**
	 * Returns true if the template contains an underspecified element (slot to fill
	 * or wildcard), and false otherwise.
	 * 
	 * @return true if the template is underspecified, false otherwsie
	 */
	public boolean isUnderspecified() {
		return pattern.toString().contains(".*") || pattern.toString().contains("(?:") 
				|| pattern.toString().contains("|");
	}



	/**
	 * Checks whether the string is matching the template or not. The matching result contains
	 * a boolean representing the outcome of the process, as well (if the match is successful)
	 * as the boundaries of the match and the extracted slot values.
	 * 
	 * @param str the string to check
	 * @param fullMatch whether to use full or partial matching
	 * @return the matching result
	 */
	public MatchResult match (String str, boolean fullMatch) {
		String input = str.trim();
		if (!isUnderspecified() && fullMatch) {
			return new MatchResult(input.equalsIgnoreCase(rawString));
		}
		Matcher matcher = pattern.matcher(input);

		if ((fullMatch && matcher.matches()) || (!fullMatch && matcher.find())) {
			int start = input.indexOf(matcher.group(0));
			int end = input.indexOf(matcher.group(0)) + matcher.group(0).length();

			if (!fullMatch && ((start!=0 && !isWhitespaceOrPunctuation(input.charAt(start-1)))
					|| (end < input.length() && !isWhitespaceOrPunctuation(input.charAt(end))))) {
				return new MatchResult(false);
			}
			MatchResult match = new MatchResult(true);
			match.setBoundaries(start, end);
			for (String slot : slots.keySet()) {
				String filledValue = matcher.group(slots.get(slot)+1).trim();
				if (filledValue.indexOf(')') < filledValue.indexOf('(')) {
					return new MatchResult(false);
				}
				match.addFilledSlot(slot, filledValue);
			}
			return match;
		}
		return new MatchResult(false);
	}



	/**
	 * Checks whether two strings match one another, taking regular expression
	 * patterns into account in both strings.
	 * 
	 * @param str1 the first string
	 * @param str2 the second string
	 * @return whether the two strings can match
	 */
	public static boolean match(String str1, String str2) {
		if (str1.contains("*") || str1.contains(")?") || str1.contains("|")) {
			Template t1 = new Template(str1);
			return t1.match(str2, true).isMatching();
		}
		else if (str2.contains("*") || str2.contains(")?") || str2.contains("|")) {
			Template t2 = new Template(str2);
			return t2.match(str1, true).isMatching();
		}
		return str1.equalsIgnoreCase(str2);
	}



	// ===================================
	//  SLOT FILLING METHODS
	// ===================================


	/**
	 * Returns true if the provided variables cover all of the slots in the 
	 * template. Otherwise, returns false.
	 * 
	 * @param input the input
	 * @return true if all slots can be filled, and false otherwise.
	 */
	public boolean isFilledBy(Assignment input) {
		for (String slot : slots.keySet()) {
			if (!input.containsVar(slot) || input.getValue(slot).equals(ValueFactory.none())) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Fills the template with the given content, and returns the filled string.
	 * The content provided in the form of a slot:filler mapping.  For
	 * instance, given a template: "my name is {name}" and a filler "name:Pierre",
	 * the method will return "my name is Pierre".
	 * 
	 * <p>In addition, the slot {random} is associated with a random integer number.
	 * 
	 * @param fillers the content associated with each slot.
	 * @return the template filled with the given content
	 */
	public Template fillSlots(Assignment fillers) {

		if (slots.isEmpty()) {
			return this;
		}
		String filledTemplate = rawString;
		for (String slot : slots.keySet()) {
			if (fillers.getValue(slot) != ValueFactory.none()) {
				filledTemplate = filledTemplate.replace("{"+slot+"}", fillers.getValue(slot).toString());
			}
		}
		return new Template(filledTemplate);

	}


	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	/**
	 * Returns the hashcode for the template string
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return rawString.hashCode();
	}

	/**
	 * Returns a string representation for the template
	 * (the raw string)
	 * 
	 * @return the raw string
	 */
	@Override
	public String toString() {
		return rawString;
	}

	/**
	 * Returns true if the object o is identical to the current template, and
	 * false otherwise
	 *
	 * @param o the object to compare
	 * @return true if identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Template) {
			if (((Template)o).getRawString().equals(rawString) && 
					((Template)o).getSlots().equals(slots.keySet())) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns a copy of the template.
	 * 
	 * @return the copy.
	 */
	public Template copy() {
		return new Template(getRawString());
	}

	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Returns the slots defined in the string
	 * 
	 * @param str the string in which to extract the slots
	 * @return the (possibly empty) list of slots, as a mapping between 
	 *         the slot label and its group position in the pattern
	 */
	private static Map<String,Integer> constructSlots(String str) {

		Map<String,Integer> vars = new HashMap<String,Integer>();

		Pattern p = Pattern.compile("\\{(.+?)\\}");
		Matcher m = p.matcher(str);
		int incr = 0;
		while (m.find()) {
			String var = m.group(1);
			if (!vars.containsKey(var)) {
				vars.put(var,incr);
				incr++;
			}
		}
		return vars;
	}


	public boolean isRawSlot() {
		return slots.size()==1 && rawString.equals("{"+slots.keySet().iterator().next()+"}");
	}


	/**
	 * Returns true is the character is a white space character or
	 * a punctuation
	 * 
	 * @param c the character
	 * @return true if c is a white space or a punctuation
	 */
	public static boolean isWhitespaceOrPunctuation(char c) {
		return Character.isWhitespace(c) 
				||	c == ','
				|| c == '.'
				|| c == '!'
				|| c == '?'
				|| c == ':'
				|| c == ';'	
				|| c == '('
				|| c == ')'
				|| c == '['
				|| c == ']'
				;
	}


	/**
	 * Constructs the regular expression corresponding to the initial string
	 * 
	 * @param init the initial string
	 * @return the formatted string for the regular expression.
	 */
	private static String constructRegex(String init) {
		StringBuilder builder = new StringBuilder();
		char[] charArr = init.toLowerCase().toCharArray();

		boolean hasComplexRegex = false;
		for(int i = 0; i < charArr.length; i++) {
			if (charArr[i] == '(') { builder.append("\\("); }
			else if (charArr[i] == ')') { builder.append("\\)"); }
			else if (charArr[i] == '[') { builder.append("\\["); }
			else if (charArr[i] == ']') { builder.append("\\]"); }
			else if (charArr[i] == '?') { builder.append("\\?"); }
			else if (charArr[i] == '.') { builder.append("\\."); }
			else if (charArr[i] == '!') { builder.append("\\!"); }
			else if (charArr[i] == '^') { builder.append("\\^"); }
			else if (charArr[i] == '*' && i < (charArr.length-1) && charArr[i+1]==' ') { builder.append("(?:.*)"); i++; }
			else if (charArr[i] == '*') { builder.append("(?:.*)"); }
			else if (charArr[i] == '{' && charArr[i+1]=='}') { builder.append("\\{\\}"); i++; }
			else {
				builder.append(charArr[i]);
			}
			if (charArr[i] == '|' || (charArr[i]=='?' && i > 0 && charArr[i-1]==')')) {
				hasComplexRegex = true;
			}
		}

		if (hasComplexRegex) {
			Matcher m = complexRegex.matcher(builder.toString());
			while (m.find()) {
				String core = m.group(1);
				if (m.group(0).endsWith("?")) {

					// need to remove whitespaces at specific positions
					if (m.start() > 0 && builder.charAt(m.start()-1)==' ' &&
							(m.end() >= builder.length() || builder.charAt(m.end())==' ')) {
						String replace =  "(?: " + core.replaceAll(" \\|", " \\|") + ")?";
						builder = builder.replace(m.start()-1, m.end(), replace);
					}
					else if (m.end() < builder.length() && builder.charAt(m.end())==' ' &&
							(m.start() == 0 || builder.charAt(m.start()-1)==' ')) {
						String replace =  "(?: " + core.replaceAll(" \\|", " \\|") + ")?";
						builder = builder.replace(m.start(), m.end()+1, replace);
					}

					else {
						builder = builder.replace(m.start(), m.end(), "(?:"+core+")?");	    			
					}
				}
				else {
					builder = builder.replace(m.start(), m.end(), "(?:"+core+")");	 
				}
				m = complexRegex.matcher(builder.toString());
			}		
		}
		return builder.toString();
	}


	/**
	 * Representation of a matching result
	 */
	public class MatchResult {

		boolean isMatching;

		Integer[] boundaries = new Integer[2];

		Assignment filledSlots = new Assignment();

		/**
		 * Construction of a match result, which can be positive or negative.
		 * 
		 * @param isMatching whether the match result is positive or negative
		 */
		private MatchResult(boolean isMatching) {
			this.isMatching = isMatching;
			boundaries[0] = -1;
			boundaries[1] = -1;
		}

		/**
		 * Sets the match boundaries for the result.
		 * 
		 * @param start the start boundary
		 * @param end the end boundary
		 */
		private void setBoundaries(int start, int end) {
			boundaries[0] = start;
			boundaries[1] = end;
		}

		/**
		 * Adds a filled slot.
		 * 
		 * @param slot the filled slot
		 * @param filledValue the value for the slot
		 */
		private void addFilledSlot(String slot, String filledValue) {
			filledSlots.addPair(slot, filledValue);
		}

		/**
		 * Returns true is the match is positive, and false if it is negative.
		 * 
		 * @return true if there is a match, else false.
		 */
		public boolean isMatching() {
			return this.isMatching;
		}


		/**
		 * Returns the match boundaries as an array of two integers.
		 * 
		 * @return the boundaries
		 */
		public Integer[] getBoundaries() {
			return boundaries;
		}

		/**
		 * Returns the filled slots as an assignment
		 * 
		 * @return the assignment
		 */
		public Assignment getFilledSlots() {
			return filledSlots;
		}


		/**
		 * Returns the string representation for the matching result
		 */
		@Override
		public String toString() {
			return isMatching + " (" + filledSlots+")";
		}
	}




}
