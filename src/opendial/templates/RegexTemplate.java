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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.utils.StringUtils;

/**
 * Template based on regular expressions. Syntax for the templates:
 * <ul>
 * <li>underspecified slots, represented with braces, e.g. {Slot}
 * <li>optional elements, surrounded by parentheses followed by a question mark, e.g.
 * (option)?
 * <li>alternative elements, surrounded by parentheses and separated by the |
 * character, i.e. (option1|option2)
 * </ul>
 * .
 *
 */
class RegexTemplate implements Template {

	// raw string for the regular expression
	final String rawString;

	// the regular expression pattern corresponding to the template
	Pattern pattern;

	// underspecified slots, mapped to their group index in the regex
	final Map<String, Integer> slots;

	// regular expression for slots
	final static Pattern slotRegex = Pattern.compile("\\{(.+?)\\}");

	// complex regular expression with alternative or optional elements
	final static Pattern altRegex =
			Pattern.compile("(\\\\\\(((\\(\\?)|[^\\(])+?\\\\\\)\\\\\\?)"
					+ "|(\\\\\\(((\\(\\?)|[^\\(])+?\\|((\\(\\?)"
					+ "|[^\\(])+?\\\\\\)(\\\\\\?)?)");

	/**
	 * Constructs the regular expression, based on the string representation.
	 * 
	 * @param rawString the string
	 */
	public RegexTemplate(String rawString) {
		this.rawString = rawString.trim();
		String escaped = StringUtils.escape(rawString);
		String regex = constructRegex(escaped);

		// the pattern should ignore case, and handle unicode.
		pattern = Pattern.compile(regex,
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		slots = getSlots(rawString);
	}

	/**
	 * Checks whether the string could possibly represent a regular expression (this
	 * is just a first, fast guess, which will need to be verified by actually
	 * constructing the regex using the constructRegex method below).
	 * 
	 * @param str the string
	 * @return true if the string is likely to be a regular expression, else false
	 */
	public static boolean isPossibleRegex(String str) {
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case '*':
				return true;
			case '{':
				if (i < str.length() - 1 && str.charAt(i + 1) != '}') return true;
				break;
			case '|':
				return true;
			case '?':
				if (i > 1 && str.charAt(i - 1) == ')') return true;
				break;
			default:
				break;
			}
		}
		return false;
	}

	/**
	 * Returns the (possibly empty) set of slots for the template
	 * 
	 */
	@Override
	public Set<String> getSlots() {
		return slots.keySet();
	}

	/**
	 * Tries to match the template against the provided string.
	 */
	@Override
	public MatchResult match(String str) {
		String input = str.trim();

		Matcher matcher = pattern.matcher(input);

		if ((matcher.matches())) {

			MatchResult result = new MatchResult(matcher.start(), matcher.end());
			for (String slot : slots.keySet()) {
				String filledValue = matcher.group(slots.get(slot));
				if (!StringUtils.checkForm(filledValue) && permutatePattern()) {
					return match(str);
				}
				result.addPair(slot, filledValue);
			}

			return result;
		}
		return new MatchResult(false);
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isUnderspecified() {
		return true;
	}

	/**
	 * Tries to find all occurrences of the template in the provided string. Stops
	 * after the maximum number of results is reached.
	 */
	@Override
	public List<MatchResult> find(String str, int maxResults) {
		str = str.trim();
		Matcher matcher = pattern.matcher(str);
		List<MatchResult> results = new ArrayList<MatchResult>();

		while ((matcher.find())) {

			int start = matcher.start();
			int end = matcher.end();

			if (!StringUtils.isDelimited(str, start, end)) {
				continue;
			}

			MatchResult result = new MatchResult(start, end);
			for (String slot : slots.keySet()) {
				String filledValue = matcher.group(slots.get(slot)).trim();

				// quick-fix to handle some rare cases where the occurrence found
				// by the regex leads to unbalanced parentheses or brackets.
				if (!StringUtils.checkForm(filledValue) && permutatePattern()) {
					return find(str, maxResults);
				}
				result.addPair(slot, filledValue);
			}

			results.add(result);
			if (results.size() >= maxResults) {
				break;
			}
		}
		return results;
	}

	/**
	 * Returns true if all slots are filled by the assignment. Else, returns false.
	 */
	@Override
	public boolean isFilledBy(Assignment input) {
		return slots.keySet().stream().map(s -> input.getValue(s))
				.noneMatch(v -> v.equals(ValueFactory.none()));
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
	@Override
	public String fillSlots(Assignment fillers) {
		if (slots.isEmpty()) {
			return rawString;
		}
		String filled = rawString;
		for (String slot : slots.keySet()) {
			Value v = fillers.getValue(slot);
			if (v != ValueFactory.none()) {
				String strval = v.toString();
				filled = filled.replace("{" + slot + "}", strval);
			}
		}
		return filled;
	}

	/**
	 * Returns the hashcode for the raw string.
	 */
	@Override
	public int hashCode() {
		return rawString.hashCode();
	}

	/**
	 * Returns the raw string.
	 */
	@Override
	public String toString() {
		return rawString;
	}

	/**
	 * Returns true if o is a RegexTemplate with the same string. Else false.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof RegexTemplate) {
			return ((RegexTemplate) o).rawString.equals(rawString);
		}
		return false;
	}

	/**
	 * Quick fix to make slight changes to the regular expression in case the
	 * templates produces matching results with unbalanced parenthesis/brackets. For
	 * instance, when the template pred({X},{Y}) is matched against a string
	 * pred(foo,bar(1,2)), the resulting match is X="foo,bar(1" and Y="2)". We can
	 * get the desired result X="foo", Y="bar(1,2)" by changing the patterns,
	 * replacing greedy quantifiers by reluctant or possessive ones.
	 * 
	 * @return true if the permutation resulted in a change in the pattern. else,
	 *         false.
	 */
	private boolean permutatePattern() {
		String newPattern = pattern.pattern().replaceFirst("\\(\\.\\+\\)", "(.+?)");
		if (newPattern.equals(pattern.pattern())) {
			newPattern = pattern.pattern().replaceFirst("\\(\\.\\?\\)", "(.++)");
		}
		boolean change = !(newPattern.equals(pattern.pattern()));
		pattern = Pattern.compile(newPattern);
		return change;
	}

	/**
	 * Formats the regular expression corresponding to the provided string
	 * 
	 * @param init the initial string
	 * @return the corresponding expression
	 */
	private static String constructRegex(String init) {

		boolean hasStars = false;
		boolean hasSlots = false;
		boolean hasAlternatives = false;
		for (int i = 0; i < init.length(); i++) {
			switch (init.charAt(i)) {
			case '*':
				hasStars = true;
				break;
			case '{':
				hasSlots = true;
				break;
			case '|':
			case '?':
				hasAlternatives = true;
				break;
			default:
				break;
			}
		}

		String result = (hasStars) ? replaceStars(init) : init;
		result = (hasSlots) ? slotRegex.matcher(result).replaceAll("(.+)") : result;
		result = (hasAlternatives) ? replaceComplex(result) : result;

		return result;
	}

	/**
	 * Returns the slots defined in the string as well as their sequential order
	 * (starting at 1) in the string.
	 * 
	 * @param str the string to analyse
	 * @return the extracted slots
	 */
	public static Map<String, Integer> getSlots(String str) {
		Map<String, Integer> slots = new HashMap<String, Integer>();
		Matcher m = slotRegex.matcher(str);
		while (m.find()) {
			String var = m.group(1);
			if (!slots.containsKey(var)) {
				slots.put(var, slots.size() + 1);
			}
		}
		return slots;
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
			}
			else if (chars[i] == '*' && i < (chars.length - 1) && i > 0
					&& chars[i + 1] == ' ' && chars[i - 1] == ' ') {
				builder.deleteCharAt(builder.length() - 1);
				builder.append("(?:.+|)");
			}
			else if (chars[i] == '*' && i == (chars.length - 1) && i > 0
					&& chars[i - 1] == ' ') {
				builder.deleteCharAt(builder.length() - 1);
				builder.append("(?: .+|)");
			}
			else if (chars[i] == '*') {
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
		Matcher m = altRegex.matcher(builder.toString());
		while (m.find()) {
			if (m.group().endsWith("?") && StringUtils.checkForm(m.group())) {
				String core = m.group().substring(2, m.group().length() - 4);
				if (m.end() < builder.length() && builder.charAt(m.end()) == ' ') {
					String replace = "(?:" + core.replaceAll("\\|", " \\|") + " )?";
					builder = builder.replace(m.start(), m.end() + 1, replace);
				}
				else if (m.end() >= builder.length() && m.start() > 0
						&& builder.charAt(m.start() - 1) == ' ') {
					String replace = "(?: " + core.replaceAll("\\|", "\\| ") + ")?";
					builder = builder.replace(m.start() - 1, m.end(), replace);
				}
				else {
					builder =
							builder.replace(m.start(), m.end(), "(?:" + core + ")?");
				}
				m = altRegex.matcher(builder.toString());
			}
			else if (StringUtils.checkForm(m.group())) {
				String core = m.group().substring(2, m.group(0).length() - 2);
				builder = builder.replace(m.start(), m.end(), "(?:" + core + ")");
				m = altRegex.matcher(builder.toString());
			}
		}

		return builder.toString();
	}

}
