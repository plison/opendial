// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.domains.datastructs;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.StringVal;
import opendial.bn.values.ValueFactory;

/**
 * Representation of a string object containing a variable number (from 0 to n) of slots
 * to be filled, forming a template. 
 * 
 * TODO: allow variables to be either x.blabla or blabla(x).
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
	public static Logger log = new Logger("TemplateString", Logger.Level.DEBUG);

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
		slots = extractSlots(rawString);

		// string processing to avoid special characters for the pattern
		String regex = format(rawString);

		for (String slot : slots.keySet()) {
			regex = regex.replace("{"+format(slot)+"}", "(.*)");
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
	
	
	
	private String format(String init) {
		String regex = new String(init);
		regex = regex.replace("(", "\\(").replace(")", "\\)");
		regex = regex.replace("[", "\\[").replace("]", "\\]");
		regex = regex.replace("?", "\\?");
		regex = regex.replace("* ", "*");
		regex = regex.replace("*", "(?:.*)");
		regex = regex.replace("!", "\\!");
		regex = regex.replace("^", "\\^");
		return regex;
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
	 * Returns true if the string given as argument is matching the template.
	 * 
	 * <p>The match can be either a partial match or a full match, as specified in 
	 * the second argument.
	 * 
	 * @param str
	 * @param partialMatch
	 * @return
	 */
	public boolean isMatching(String str, boolean partialMatch) {
		String input = str.trim();
		Matcher matcher = pattern.matcher(input);
		matcher = pattern.matcher(input);

		if (matcher.find()) {
			int indexBefore = input.indexOf(matcher.group(0));
			int indexAfter = input.indexOf(matcher.group(0)) + matcher.group(0).length();

			if (partialMatch) {
				boolean leftOK = (indexBefore == 0 
						|| isWhitespaceOrPunctuation(input.charAt(indexBefore-1)));
				boolean rightOK = (indexAfter >= input.length() 
						|| isWhitespaceOrPunctuation(input.charAt(indexAfter)));
				return (leftOK && rightOK);
			}
			else {
				return (indexBefore==0 && indexAfter==(input.length()));
			}
		}
		return false;
	}


	/** 
	 * Returns the start and end boundaries for the match of the template with
	 * the provided string, if any match can be found.  If no match can be found,
	 * returns (-1,-1).
	 * 
	 * @param str the string for which to find the possible match
	 * @param partial whether the match is partial or full
	 * @return an assignment with two variables, "match.start" and "match.end".
	 */
	public Assignment getMatchBoundaries(String str, boolean partial) {

		Assignment boundaries = new Assignment();
		if (isMatching(str, partial)) {
			String input = str.trim();
			Matcher matcher = pattern.matcher(input);

			matcher.find();
			int indexBefore = input.indexOf(matcher.group(0));
			int indexAfter = input.indexOf(matcher.group(0)) + matcher.group(0).length();

			boundaries.addPair("match.start", indexBefore);
			boundaries.addPair("match.end", indexAfter);
		}
		else {
			boundaries.addPair("match.start", -1);
			boundaries.addPair("match.end", -1);
		}
		return boundaries;
	}


	/**
	 * Extracts the fillers corresponding to the slots of the utterance, in the 
	 * form of a map slot --> filler.  The extraction only works if the utterance 
	 * matches the template - if not, it returns an empty assignment.
	 * 
	 * @param utterance the utterance on which to extract the content of the slots
	 * @return the content of the slots
	 */
	//	@Override
	public Assignment extractParameters (String str, boolean partialMatch) {

		Assignment filledSlots = new Assignment();

		if (isMatching(str, partialMatch)) {
			Matcher matcher = pattern.matcher(str.trim());
			matcher.find();
			for (String slot : slots.keySet()) {
				String filledSlot = matcher.group(slots.get(slot)+1).trim();
				filledSlot = filledSlot.replaceAll("[^\\p{L}\\s0-9\\(\\)]", "");
				filledSlots.addPair(slot, filledSlot);
			}
		}		
		return filledSlots;	
	}



	// ===================================
	//  SLOT FILLING METHODS
	// ===================================



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
	 * @throws DialException if the fillers do not contain all the required slots
	 */
	public String fillSlots(Assignment fillers) throws DialException {

		Assignment values = new Assignment(fillers);

		if (values.getVariables().containsAll(slots.keySet())) {
			String filledTemplate = rawString;
			for (String slot : slots.keySet()) {
				if (values.getValue(slot) != null && (values.getValue(slot) instanceof StringVal)) {
					filledTemplate = filledTemplate.replace("{"+slot+"}", 
							values.getValue(slot).toString());
				}
				else {
					throw new DialException("fillers contains a ill-formatted slot (not a string): "
							+ slot +  ". fillers is " + values);
				}
			}
			return filledTemplate;
		}
		throw new DialException("fillers do not contain all required slots: " + slots +
				" fillers was "+ values);
	}


	/**
	 * Returns another template string where all slots than can be filled with
	 * variables in the "fillers" argument are filled.  The other slots are
	 * left unchanged.
	 * 
	 * <p>In addition, the slot {random} is associated with a random integer number.
	 * 
	 * @param fillers the filler values
	 * @return another template string, with nb slots <= nb slots in current template
	 */
	public Template fillSlotsPartial(Assignment fillers) {
		String filledTemplate = rawString;
		for (String slot : slots.keySet()) {
			if (fillers.getValue(slot) != null) {
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
	public int hashCode() {
		return rawString.hashCode() + slots.hashCode();
	}

	/**
	 * Returns a string representation for the template
	 * (the raw string)
	 * 
	 * @return the raw string
	 */
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
	public boolean equals(Object o) {
		if (o instanceof Template) {
			if (((Template)o).getRawString().equals(rawString) && 
					((Template)o).getSlots().equals(slots.keySet())) {
				return true;
			}
		}
		return false;
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
	private static Map<String,Integer> extractSlots(String str) {

		Map<String,Integer> vars = new HashMap<String,Integer>();

		Pattern p = Pattern.compile("\\{(.*?)\\}");
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
				|| c == '.'
				|| c == '!'
				|| c == '?'
				|| c == ':'
				|| c == ';'
				;
	}



	public boolean isSingleSlot() {
		return rawString.startsWith("{") && rawString.endsWith("}") 
				&& rawString.indexOf("}") == (rawString.length() - 1);
	}
	
	public boolean containsTemplate() {
		return pattern.toString().contains(".*");
	}


}
