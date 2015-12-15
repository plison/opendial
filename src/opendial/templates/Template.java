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

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import opendial.Settings;
import opendial.datastructs.Assignment;
import opendial.datastructs.Graph;

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
public interface Template extends Comparable<Template> {

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
		if (Settings.isFunction(value)) {
			return new FunctionalTemplate(value);
		}
		if (Graph.isRelational(value)) {
			try {
				return new RelationalTemplate(value);
			}
			catch (Exception e) {
				log.warning("illegal relational structure: " + value + ")");
				return new StringTemplate(value);
			}
		}
		else if (RegexTemplate.isPossibleRegex(value)) {
			try {
				if (ArithmeticTemplate.isArithmeticExpression(value)) {
					return new ArithmeticTemplate(value);
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
	 * Compares the templates (based on their string value)
	 * 
	 * @param other the second template
	 * @return the comparison result
	 */
	@Override
	public default int compareTo(Template other) {
		return toString().compareTo(other.toString());
	}

	/**
	 * Representation of a matching result
	 */
	class MatchResult extends Assignment {

		final boolean isMatching;

		MatchResult(boolean isMatching) {
			super();
			this.isMatching = isMatching;
		}

		MatchResult(int start, int end) {
			super();
			this.isMatching = true;
		}

		public boolean isMatching() {
			return this.isMatching;
		}

		@Override
		public String toString() {
			return isMatching + " (" + super.toString() + ")";
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof MatchResult)) {
				return false;
			}
			MatchResult mr = (MatchResult) o;
			return mr.map.equals(this.map) && mr.isMatching == isMatching;
		}

		@Override
		public int hashCode() {
			return (new Boolean(isMatching)).hashCode() - super.hashCode();
		}

		@Override
		public MatchResult copy() {
			MatchResult copy = new MatchResult(isMatching);
			copy.addAssignment(this);
			return copy;
		}
	}

}
