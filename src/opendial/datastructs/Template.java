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

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.utils.StringUtils;

/**
 * Representation of a string object containing a variable number (from 0 to n) of
 * slots to be filled, forming a template.
 * 
 * <p>
 * For instance, the raw string "Take the {BOX} to the {LOCATION}$ contains two
 * slots: a slot labeled BOX and a slot labeled LOCATION.
 * 
 * <p>
 * The class offers several methods for constructing template strings, matching them
 * against strings, and filling their slots with specific values.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Template {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the initial string, containing the slots in raw form
	final String rawString;

	// the regular expression pattern corresponding to the template
	final Pattern pattern;

	// whether the template allows for multiple values
	protected final boolean underspecified;

	// the slots, as a mapping between slot labels and their
	// group number in the pattern
	final Map<String, Integer> slots;

	final static Pattern slotPattern = Pattern.compile("\\{(.+?)\\}");
	final static Pattern trailPattern =
			Pattern.compile("(?:^[\\.,\\s\\?]+)|(?:[\\.,\\s\\?]+$)");
	final static List<Character> punctuation =
			Arrays.asList(',', '.', '!', '?', ':', ';', '(', ')', '[', ']');

	// ===================================
	// TEMPLATE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new template string, based on the raw string provided as argument.
	 * The raw string must signal its slots by surrounding its slots labels with {
	 * ... $}.
	 * 
	 * @param value the string value.
	 */
	public Template(String value) {

		rawString = value;
		// StringUtils.checkForm(rawString);

		slots = constructSlots(value);

		// string processing to avoid special characters for the pattern
		pattern = constructPattern(rawString);

		underspecified = !slots.isEmpty() || pattern.toString().contains("(?:");

	}

	protected Pattern getPattern() {
		return pattern;
	}

	// ===================================
	// GETTERS
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
	 * Returns true if the template is an actual template, i.e. can match multiple
	 * values (due to slots or alternative/optional elements)
	 * 
	 * @return true if underspecified, false otherwise
	 */
	public boolean isUnderspecified() {
		return underspecified;
	}

	/**
	 * Checks whether the string is matching the template or not. The matching result
	 * contains a boolean representing the outcome of the process, as well (if the
	 * match is successful) as the boundaries of the match and the extracted slot
	 * values.
	 * 
	 * @param str the string to check
	 * @return the matching result
	 */
	public MatchResult match(String str) {
		String input = str.trim();
		if (!underspecified) {
			if (input.equalsIgnoreCase(rawString)) {
				return new MatchResult(0, rawString.length(), new Assignment());
			}
			else {
				return new MatchResult();
			}
		}

		Matcher matcher = pattern.matcher(input);
		if ((matcher.matches())) {

			Assignment filledSlots = new Assignment();
			for (String slot : slots.keySet()) {
				String filledValue = matcher.group(slots.get(slot) + 1);
				filledValue = trailPattern.matcher(filledValue).replaceAll("");
				filledSlots.addPair(slot, filledValue);
			}

			return new MatchResult(matcher.start(), matcher.end(), filledSlots);
		}
		return new MatchResult();
	}

	/**
	 * Checks whether the template can be found within the string. The matching
	 * result contains a boolean representing the outcome of the process, as well (if
	 * the match is successful) as the boundaries of the match and the extracted slot
	 * values.
	 * 
	 * @param str the string to check
	 * @return the matching result
	 */
	public MatchResult partialmatch(String str) {
		List<MatchResult> results = find(str, 1);
		if (results.isEmpty()) {
			return new MatchResult();
		}
		else {
			return results.get(0);
		}
	}

	/**
	 * Searches for all occurrences of the template in the str. The maximum number of
	 * occurrences to find is specified in maxResults.
	 * 
	 * @param str the string to check
	 * @param maxResults the maximum number of occurrences
	 * @return the matching results
	 */
	public List<MatchResult> find(String str, int maxResults) {

		String input = str.trim();
		Matcher matcher = pattern.matcher(input);
		List<MatchResult> results = new ArrayList<MatchResult>();
		while ((matcher.find())) {
			
			if (!isValidMatch(input, matcher)) {
				continue;
			}

			Assignment filledSlots = new Assignment();
			for (String slot : slots.keySet()) {
				String filledValue = matcher.group(slots.get(slot) + 1).trim();
				filledValue = trailPattern.matcher(filledValue).replaceAll("");
				filledSlots.addPair(slot, filledValue);
			}
			
			MatchResult result = new MatchResult(matcher.start(), matcher.end(), filledSlots);
			results.add(result);
			if (results.size() >= maxResults) {
				return results;
			}
		}
		return results;
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
			return t1.match(str2).isMatching();
		}
		else if (str2.contains("*") || str2.contains(")?") || str2.contains("|")) {
			Template t2 = new Template(str2);
			return t2.match(str1).isMatching();
		}
		return str1.equalsIgnoreCase(str2);
	}

	// ===================================
	// SLOT FILLING METHODS
	// ===================================

	/**
	 * Returns true if the provided variables cover all of the slots in the template.
	 * Otherwise, returns false.
	 * 
	 * @param input the input
	 * @return true if all slots can be filled, and false otherwise.
	 */
	public boolean isFilledBy(Assignment input) {
		for (String slot : slots.keySet()) {
			if (!input.containsVar(slot)
					|| input.getValue(slot).equals(ValueFactory.none())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Fills the template with the given content, and returns the filled string. The
	 * content provided in the form of a slot:filler mapping. For instance, given a
	 * template: "my name is {name}" and a filler "name:Pierre", the method will
	 * return "my name is Pierre".
	 * 
	 * 
	 * @param fillers the content associated with each slot.
	 * @return the string filled with the given content
	 */
	public String fillSlots(Assignment fillers) {

		if (slots.isEmpty()) {
			return rawString;
		}
		String filledTemplate = rawString;
		for (String slot : slots.keySet()) {
			Value v = fillers.getValue(slot);
			if (v != ValueFactory.none()) {
				filledTemplate =
						filledTemplate.replace("{" + slot + "}", v.toString());
			}
		}
		if (StringUtils.isArithmeticExpression(filledTemplate)) {
			double result = new MathExpression(filledTemplate).evaluate();
			filledTemplate = StringUtils.getShortForm(result);
		}
		else if (filledTemplate.contains("+")) {
			Value concatenation = ValueFactory.none();
			for (String subval : filledTemplate.split("\\+")) {
				concatenation =
						concatenation.concatenate(ValueFactory.create(subval));
			}
			filledTemplate = concatenation.toString();
		}
		return filledTemplate;

	}

	// ===================================
	// UTILITY FUNCTIONS
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
	 * Returns a string representation for the template (the raw string)
	 * 
	 * @return the raw string
	 */
	@Override
	public String toString() {
		return rawString;
	}

	/**
	 * Returns true if the object o is identical to the current template, and false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Template) {
			if (((Template) o).getRawString().equals(rawString)
					&& ((Template) o).getSlots().equals(slots.keySet())) {
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
	// PRIVATE METHODS
	// ===================================

	/**
	 * Returns the slots defined in the string
	 * 
	 * @param str the string in which to extract the slots
	 * @return the (possibly empty) list of slots, as a mapping between the slot
	 *         label and its group position in the pattern
	 */
	private static Map<String, Integer> constructSlots(String str) {

		Map<String, Integer> vars = new HashMap<String, Integer>();

		Matcher m = slotPattern.matcher(str);
		int incr = 0;
		while (m.find()) {
			String var = m.group(1);
			if (!vars.containsKey(var)) {
				vars.put(var, incr);
				incr++;
			}
		}
		return vars;
	}

	public boolean isRawSlot() {
		return slots.size() == 1
				&& rawString.equals("{" + slots.keySet().iterator().next() + "}");
	}

	/**
	 * Returns true if the matcher group is valid (i.e. does not cut through a 
	 * word or a logical expression).
	 * 
	 * @param input the input in which the template was found
	 * @param matcher the matcher object
	 * @return true if the match is valid, else false
	 */
	private boolean isValidMatch(String input, Matcher matcher) {

		int start = matcher.start();
		int end = matcher.end();
		
		if (rawString.length() == 1 && punctuation.contains(rawString.charAt(0))) {
			return true;
		}

		if (start > 0) {
			char prev = input.charAt(start - 1);
			if (!Character.isWhitespace(prev) && !punctuation.contains(prev)) {
				return false;
			}
		}
		if (end < input.length()) {
			char next = input.charAt(end);
			if (!Character.isWhitespace(next) && !punctuation.contains(next)) {
				return false;
			}
		}
		
		for (int slot : slots.values()) {
			String filledValue = matcher.group(slot + 1);
			if (filledValue.indexOf(')') != filledValue.indexOf('(')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Constructs the pattern associated with the string, along with the slots
	 * already extracted.
	 * 
	 * @param str the string from which to construct the pattern
	 * @param slots the slots
	 * @return the corresponding pattern
	 */
	private static Pattern constructPattern(String str) {
		String escaped = StringUtils.escape(str);
		String regex = StringUtils.constructRegex(escaped);
		// compiling the associated pattern
		try {
			return Pattern.compile(regex,
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}
		catch (PatternSyntaxException e) {
			log.warning("illegal pattern syntax: " + regex + " (from " + str + ")");
			return Pattern.compile("bogus pattern");
		}
	}

	/**
	 * Representation of a matching result
	 */
	public class MatchResult {

		boolean isMatching = false;

		Integer[] boundaries = new Integer[] { -1, -1 };
		Assignment filledSlots = new Assignment();

		/**
		 * Construction of a match result, which can be positive or negative.
		 * 
		 * @param isMatching whether the match result is positive or negative
		 */
		private MatchResult() {
		}

		private MatchResult(int start, int end, Assignment filledSlots) {
			this.isMatching = true;
			boundaries = new Integer[] { start, end };
			this.filledSlots = filledSlots;
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
		 * Returns the filled slots as an assignment
		 * 
		 * @return the assignment
		 */
		public Assignment getFilledSlots() {
			return filledSlots;
		}

		public Integer[] getBoundaries() {
			return boundaries;
		}

		/**
		 * Returns the string representation for the matching result
		 */
		@Override
		public String toString() {
			return isMatching + " (" + filledSlots + ")";
		}
	}

}
