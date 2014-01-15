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

package opendial.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;


/**
 * Various utilities for manipulating strings
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
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
	 * Checks the form of the string to ensure that all parentheses, braces and brackets
	 * are balanced.  Logs warning messages if problems are detected.
	 * 
	 * @param str the string
	 */
	public static void checkForm(String str) {

		int nbParenthesisLeft = (str+" ").split("\\(").length - 1;
		int nbParenthesisRight = (str+" ").split("\\)").length - 1;
		if (nbParenthesisLeft != nbParenthesisRight) {
			log.warning("Unequal number of parenthesis in string: " + str 
					+ "(" + nbParenthesisLeft + " vs. " + nbParenthesisRight + ") Problems ahead!");
		}
		int nbBracesLeft = (str+" ").split("\\{").length - 1;
		int nbBracesRight = (str+" ").split("\\}").length - 1;
		if (nbBracesLeft != nbBracesRight) {
			log.warning("Unequal number of braces in string: " + str + 
					"(" + nbBracesLeft + " vs. " + nbBracesRight + "). Problems ahead!");
			Thread.dumpStack();
		}
		
		int nbBracketsLeft = (str+" ").split("\\{").length - 1;
		int nbBracketsRight = (str+" ").split("\\}").length - 1;
		if (nbBracketsLeft != nbBracketsRight) {
			log.warning("Unequal number of brackets in string: " + str + 
					"(" + nbBracketsLeft + " vs. " + nbBracketsRight + "). Problems ahead!");
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
