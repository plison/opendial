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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.Logger;


/**
 * Various utilities for manipulating strings
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 *
 */
public class StringUtils {

	// logger
	public static Logger log = new Logger("StringUtils", Logger.Level.DEBUG);
	

	/**
	 * Returns the string version of the double up to a certain decimal point.
	 * 
	 * @param value the double
	 * @return the string
	 */
	public static String getShortForm(double value) {
		return "" + Math.round(value*10000.0)/10000.0;
	}


	/**
	 * Returns a HTML-compatible rendering of the raw string provided as argument
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
			str = str.replace("_{"+subscript+"}", "<sub>"+subscript+"</sub>");
		}
		Matcher matcher2 = Pattern.compile("_(\\p{Alnum}*)").matcher(str);
		while (matcher2.find()) {
			String subscript = matcher2.group(1);
			str = str.replace("_"+subscript, "<sub>"+subscript+"</sub>");
		}
		Matcher matcher3 = Pattern.compile("\\^\\{(\\p{Alnum}*?)\\}").matcher(str);
		while (matcher3.find()) {
			String subscript = matcher3.group(1);
			str = str.replace("^{"+subscript+"}", "<sup>"+subscript+"</sup>");
		}
		Matcher matcher4 = Pattern.compile("\\^(\\S)").matcher(str);
		while (matcher4.find()) {
			String subscript = matcher4.group(1);
			str = str.replace("^"+subscript, "<sup>"+subscript+"</sup>");
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
	public static int countNbOccurrences(String s, char c) {
		int counter = 0;
		for( int i=0; i<s.length(); i++ ) {
		    if( s.charAt(i) == '$' ) {
		        counter++;
		    } 
		}
		return counter;
	}

	/**
	 * Checks the form of the string to ensure that all parentheses, braces and brackets
	 * are balanced.  Logs warning messages if problems are detected.
	 * 
	 * @param str the string
	 */
	public static void checkForm(String str) {

		if (countNbOccurrences(str, '(') != countNbOccurrences(str, ')')) {
			log.warning("Unequal number of parenthesis in string: " + str + ", Problems ahead!");
		}
		if (countNbOccurrences(str, '{') != countNbOccurrences(str, '}')) {
			log.warning("Unequal number of braces in string: " + str + ", Problems ahead!");
		}
		if (countNbOccurrences(str, '[') != countNbOccurrences(str, ']')) {
			log.warning("Unequal number of brackets in string: " + str + ", Problems ahead!");
		}
		
	}


	/**
	 * Performs a lexicographic comparison of the two identifiers.  If there is a difference
	 * between the number of primes in the two identifiers, returns it.  Else, returns +1
	 * if id1.compareTo(id2) is higher than 0, and -1 otherwise.
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

	
	public static List<String> addPrimes (List<String> original) {
		List<String> newSet = new ArrayList<String>();
		for (String var : original) {
			newSet.add(var+"'");
		}
		return newSet;
	}


	public static int countOccurrences(String fullString, String pattern) {
		int lastIndex = 0;
		int count =0;

		while(lastIndex != -1){

		       lastIndex = fullString.indexOf(pattern,lastIndex);

		       if( lastIndex != -1){
		             count ++;
		             lastIndex+=pattern.length();
		      }
		}
		return count;
	}
	
}
