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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edu.stanford.nlp.semgraph.semgrex.SemgrexMatcher;
import edu.stanford.nlp.semgraph.semgrex.SemgrexPattern;
import opendial.bn.values.RelationalVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.utils.StringUtils;

/**
 * Representation of a (possibly underspecified) template for a Value. An example of
 * template is "Foobar({X},{Y})", where {X} and {Y} are underspecified slots that can
 * take any value. One can also use template to encode simple regular expressions
 * such as "the (big)? (box|cylinder)", which can match strings such as "the box" or
 * "the big cylinder".
 * 
 * <p>
 * Template objects supports 3 core operations:
 * <ul>
 * <li>match(string): full match of the template against the string. If the template
 * contains slots, returns their values resulting from the match.
 * <li>find(string): find all possible occurrences of the template in the string.
 * <li>fillSlots(assignment): returns the string resulting from the replacement of
 * the slot values by their values in the assignment.
 * </ul>
 * 
 * <p>
 * Different implements of the template interface are provided, to allow for
 * "full string" templates (without any underspecification), templates based on
 * regular expressions, mathematical expressions, and templates operating on semantic
 * graphs.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface Template {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Creates a new template based on the string value. This method finds the best
	 * template representation for the string and returns the result.
	 * 
	 * @param value the string for the template
	 * @return the corresponding template object
	 */
	public static Template create(String value) {
		if (StringUtils.isPossibleSemgrex(value)) {
			return new SemgrexTemplate(value);
		}
		else if (StringUtils.isPossibleRegex(value)) {
			try {
				if (StringUtils.isFunctionalExpression(value)) {
					return new FunctionalTemplate(value);
				}
				return new RegexTemplate(value);
			}
			catch (PatternSyntaxException e) {
				log.warning("illegal pattern: " + value + ")");
				return new StringTemplate(value);
			}
		}
		else {
			return new StringTemplate(value);
		}

	}

	/**
	 * Returns the (possibly empty) set of slots for the template
	 * 
	 * @return the set of slots
	 */
	public Set<String> getSlots();

	/**
	 * Returns true if the template is an actual template, i.e. can match multiple
	 * values (due to slots or alternative/optional elements)
	 * 
	 * @return true if underspecified, false otherwise
	 */
	public boolean isUnderspecified();

	/**
	 * Checks whether the string is matching the template or not. The matching result
	 * contains a boolean representing the outcome of the process, as well (if the
	 * match is successful) as the boundaries of the match and the extracted slot
	 * values.
	 * 
	 * @param str the string to check
	 * @return the matching result
	 */
	public MatchResult match(String str);

	/**
	 * Checks whether the template can be found within the string. The matching
	 * result contains a boolean representing the outcome of the process, as well (if
	 * the match is successful) as the boundaries of the match and the extracted slot
	 * values.
	 * 
	 * @param str the string to check
	 * @return the matching result
	 */
	public default MatchResult partialmatch(String str) {
		List<MatchResult> results = find(str, 1);
		if (results.isEmpty()) {
			return new MatchResult(false);
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
	public List<MatchResult> find(String str, int maxResults);

	/**
	 * Returns true if the provided variables cover all of the slots in the template.
	 * Otherwise, returns false.
	 * 
	 * @param input the input
	 * @return true if all slots can be filled, and false otherwise.
	 */
	public boolean isFilledBy(Assignment input);

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
	public String fillSlots(Assignment fillers);

	/**
	 * Representation of a matching result
	 */
	class MatchResult extends Assignment {

		final boolean isMatching;
		final Integer[] boundaries;

		MatchResult(boolean isMatching) {
			super();
			this.isMatching = isMatching;
			boundaries = new Integer[] { -1, -1 };
		}

		MatchResult(int start, int end) {
			super();
			this.isMatching = true;
			boundaries = new Integer[] { start, end };
		}

		public boolean isMatching() {
			return this.isMatching;
		}

		public Integer[] getBoundaries() {
			return boundaries;
		}

		@Override
		public String toString() {
			return isMatching + " (" + super.toString() + ")";
		}
	}

}

/**
 * Template for a string without any underspecified or optional elements. In other
 * words, the match, find can be simplified to the usual string matching methods. The
 * fillSlots method returns the string.
 * 
 *
 */
class StringTemplate implements Template {

	// the string corresponding to the template
	final String string;

	// whether the string represents a whole word or phrase (and not a
	// punctuation)
	final boolean whole;

	// empty set of slots
	final Set<String> slots;

	/**
	 * Creates a new string template.
	 * 
	 * @param string the string object
	 */
	protected StringTemplate(String string) {
		this.string = string;
		whole = (StringUtils.delimiters.indexOf(this.string) < 0);
		slots = Collections.emptySet();
	}

	/**
	 * Returns an empty set.
	 */
	@Override
	public Set<String> getSlots() {
		return slots;
	}

	/**
	 * Returns false.
	 */
	@Override
	public boolean isUnderspecified() {
		return false;
	}

	/**
	 * Returns a match result if the provided string is identical to the string
	 * template. Else, returns an unmatched result.
	 */
	@Override
	public MatchResult match(String str) {
		String input = str.trim();

		if (input.equalsIgnoreCase(string)) {
			return new MatchResult(0, string.length());
		}
		else {
			return new MatchResult(false);
		}
	}

	/**
	 * Searches for all possible occurrences of the template in the provided string.
	 * Stops if the maximum number of results is reached.
	 */
	@Override
	public List<MatchResult> find(String str, int maxResults) {

		str = str.trim();
		List<MatchResult> results = new ArrayList<MatchResult>();
		int start = 0;
		while (start != -1) {
			start = str.indexOf(string, start);
			if (start != -1) {
				int end = start + string.length();
				if (!whole || StringUtils.isDelimited(str, start, end)) {
					results.add(new MatchResult(start, end));
				}
				if (results.size() >= maxResults) {
					return results;
				}
				start = end;
			}
		}
		return results;
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isFilledBy(Assignment input) {
		return true;
	}

	/**
	 * Returns the string itself.
	 */
	@Override
	public String fillSlots(Assignment fillers) {
		return string;
	}

	/**
	 * Returns the hashcode for the string.
	 */
	@Override
	public int hashCode() {
		return string.hashCode();
	}

	/**
	 * Returns the string itself.
	 */
	@Override
	public String toString() {
		return string;
	}

	/**
	 * Returns true if the object is an identical string template.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof StringTemplate) {
			return ((StringTemplate) o).string.equals(string);
		}
		return false;
	}

}

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

	/**
	 * Constructs the regular expression, based on the string representation.
	 * 
	 * @param rawString the string
	 */
	public RegexTemplate(String rawString) {
		this.rawString = rawString;
		String escaped = StringUtils.escape(rawString);
		String regex = StringUtils.constructRegex(escaped);

		// the pattern should ignore case, and handle unicode.
		pattern = Pattern.compile(regex,
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		slots = StringUtils.getSlots(rawString);
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
				String strval = (v instanceof RelationalVal)
						? ((RelationalVal) v).getRootValue() : v.toString();
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
}

/**
 * Template for a functional (arithmetic) template, such as "exp(({A}+{B})/{C})".
 * When filling the slots of the template, the function is evaluated.
 *
 */
class FunctionalTemplate extends RegexTemplate {

	public FunctionalTemplate(String rawString) {
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

		if (StringUtils.isFunctionalExpression(filled)) {
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
}

class SemgrexTemplate implements Template {

	final String rawString;
	final SemgrexPattern pattern;
	final Set<String> slots;

	public SemgrexTemplate(String value) {
		this.rawString = value;
		slots = StringUtils.getSlots(value).keySet();
		String semgrex = StringUtils.constructSemgrex(value);
		pattern = SemgrexPattern.compile(semgrex);
	}

	@Override
	public Set<String> getSlots() {
		return slots;
	}

	@Override
	public boolean isUnderspecified() {
		return true;
	}

	@Override
	public MatchResult match(String str) {
		String input = str.trim();
		if (input.equalsIgnoreCase(rawString)) {
			return new MatchResult(0, rawString.length());
		}
		else {
			return new MatchResult(false);
		}
	}

	@Override
	public List<MatchResult> find(String str, int maxResults) {
		Value v = ValueFactory.create(str);
		List<MatchResult> results = new ArrayList<MatchResult>();
		if (v instanceof RelationalVal) {
			RelationalVal rv = (RelationalVal) v;
			SemgrexMatcher m = pattern.matcher(rv.getGraph());
			while (m.find()) {
				MatchResult result = new MatchResult(true);
				for (String slot : slots) {
					if (m.getNode(slot) != null) {
						RelationalVal subgraph =
								rv.getSubGraph(m.getNode(slot).index());
						result.addPair(slot, subgraph);
					}
					else {
						result.addPair(slot, m.getRelnString(slot));
					}
				}
				results.add(result);
			}
		}
		return results;
	}

	@Override
	public boolean isFilledBy(Assignment input) {
		return slots.stream().map(s -> input.getValue(s))
				.noneMatch(v -> v.equals(ValueFactory.none()));
	}

	@Override
	public String fillSlots(Assignment fillers) {
		if (slots.isEmpty()) {
			return rawString;
		}
		String filled = rawString;
		for (String slot : slots) {
			Value v = fillers.getValue(slot);
			if (v != ValueFactory.none()) {
				String strval = (v instanceof RelationalVal)
						? ((RelationalVal) v).getRootValue() : v.toString();
				filled = filled.replace("{" + slot + "}", strval);
			}
		}
		return filled;
	}

	@Override
	public int hashCode() {
		return rawString.hashCode();
	}

	@Override
	public String toString() {
		return rawString.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SemgrexTemplate) {
			return ((SemgrexTemplate) o).equals(o);
		}
		return false;
	}

}
