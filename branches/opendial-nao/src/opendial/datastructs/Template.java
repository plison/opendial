// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
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



	// ===================================
	//  TEMPLATE CONSTRUCTION
	// ===================================


	/**
	 * Creates a new template string, based on the raw string provided as
	 * argument.  The raw string must signal its slots by surrounding its 
	 * slots labels with { ... $}.
	 * 
	 * @param value
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
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		}
		catch (PatternSyntaxException e) {
			log.warning("illegal pattern syntax: " + regex);
			pattern = Pattern.compile("bogus pattern");
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
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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


	public boolean isUnderspecified() {
		return pattern.toString().contains(".*");
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
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			int start = input.indexOf(matcher.group(0));
			int end = input.indexOf(matcher.group(0)) + matcher.group(0).length();
			boolean isMatching = false;		
			if (fullMatch) {
				isMatching = (start==0 && end==(input.length()));
			}
			else {
				boolean leftOK = (start == 0 || isWhitespaceOrPunctuation(input.charAt(start-1)));
				boolean rightOK = (end >= input.length() || isWhitespaceOrPunctuation(input.charAt(end)));
				isMatching = leftOK && rightOK;
			}

			if (isMatching) {
				MatchResult match = new MatchResult(true);
				match.setBoundaries(start, end);
				for (String slot : slots.keySet()) {
					String filledValue = matcher.group(slots.get(slot)+1).trim();
					match.addFilledSlot(slot, filledValue);
				}
				return match;
			}
		}
		return new MatchResult(false);
	}


	// ===================================
	//  SLOT FILLING METHODS
	// ===================================


	public boolean isFilledBy(Collection<String> variables) {
		return variables.containsAll(this.slots.keySet());
	}


	/**
	 * Fills the template with the given content, and returns the filled string.
	 * The content provided in the form of a slot --> filler mapping.  For
	 * instance, given a template: "my name is {name}" and a filler "name --> Pierre",
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
			else {
				log.warning("same variable " + var + " is used twice in the template, " +
						"ignoring the occurrence");
			}
		}
		return vars;
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
				||	c == '\''
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


	private String constructRegex(String init) {
		String regex = new String(init);
		regex = regex.replace("(", "\\(").replace(")", "\\)");
		regex = regex.replace("[", "\\[").replace("]", "\\]");
		regex = regex.replace("?", "\\?");
		regex = regex.replace("* ", "*");
		regex = regex.replace("{}", "\\{\\}");
		regex = regex.replace("*", "(?:.*)");
		regex = regex.replace("!", "\\!");
		regex = regex.replace("^", "\\^");
		return regex;
	}


	public class MatchResult {

		boolean isMatching;

		Integer[] boundaries = new Integer[2];

		Assignment filledSlots = new Assignment();

		public MatchResult(boolean isMatching) {
			this.isMatching = isMatching;
			boundaries[0] = -1;
			boundaries[1] = -1;
		}

		public void setBoundaries(int start, int end) {
			boundaries[0] = start;
			boundaries[1] = end;
		}

		public void addFilledSlot(String slot, String filledValue) {
			filledSlots.addPair(slot, filledValue);
		}

		public boolean isMatching() {
			return this.isMatching;
		}

		public Integer[] getBoundaries() {
			return boundaries;
		}

		public Assignment getFilledSlots() {
			return filledSlots;
		}

		@Override
		public String toString() {
			return isMatching + " (" + filledSlots+")";
		}
	}


}
