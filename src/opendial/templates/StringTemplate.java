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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import opendial.datastructs.Assignment;
import opendial.utils.StringUtils;

/**
 * Template for a string without any underspecified or optional elements. In other
 * words, the match, find can be simplified to the usual string matching methods. The
 * fillSlots method returns the string.
 * 
 *
 */
public class StringTemplate implements Template {

	// the string corresponding to the template
	final String string;

	// whether the string represents a whole word or phrase (and not a
	// punctuation)
	final boolean whole;

	// empty set of slots
	final Set<String> slots = Collections.emptySet();

	/**
	 * Creates a new string template.
	 * 
	 * @param string the string object
	 */
	protected StringTemplate(String str) {
		this.string = str;
		whole = (str.length() != 1 || !StringUtils.isDelimiter(str.charAt(0)));
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
	 * Returns a match result if the provided value is identical to the string
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