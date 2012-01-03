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

package opendial.utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StringUtils {

	// logger
	static Logger log = new Logger("StringUtils", Logger.Level.DEBUG);
	
	
	/**
	 * Extracts slots in a content.  Slots must be written as ${variable},
	 * where "variable" can be anything
	 * 
	 * @param content the content to extract
	 * @return the list of slots
	 */
	public static List<String> extractSlots(String content) {
		
		List<String> slots = new LinkedList<String>();
		
		StringTokenizer tokenizer = new StringTokenizer(content);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
	//		log.debug("token: " + token);
			if (token.contains("${") && token.contains("}")) {
				String strippedToken = token.replace("${", "").replace("}", "")
				.trim().replace(",", "").replace(".", "").replace("!", "");
				slots.add(strippedToken);
			}
		}
		
		return slots;
	}
	
	

	public static List<String> splitContent (String content, List<String> slots) {
		if (slots.isEmpty()) {
			return Arrays.asList(content);
		}
		else {
			List<String> splits = new LinkedList<String>();
			splits.add(content);
			for (String slot : slots) {
				List<String> splits_copy = new LinkedList<String>();
				for (String split : splits) {
					String[] onesplit = split.split("\\$\\{"+slot+"\\}");
					splits_copy.addAll(Arrays.asList(onesplit));
				}
				splits = splits_copy;
			}	
			return splits;
		}
	}

	
	/**
	 * Returns an string indent of a given length
	 * 
	 * @param length the length of the string
	 * @return the string
	 */
	public static String makeIndent(int length) {
		String str = "";
		for (int i = 0 ; i < length ; i++) {
			str += " ";
		}
		return str;
	}
	
}
