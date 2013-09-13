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

package opendial.modules.asr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.arch.DialException;
import opendial.arch.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RecognitionGrammar {

	static Logger log = new Logger("RecognitionGrammar", Logger.Level.DEBUG);

	String topElement;

	Map<String,CFGRule> cfgRules;

	public RecognitionGrammar(String topElement) {
		this.topElement = topElement;
		cfgRules = new HashMap<String,CFGRule>();
	}

	public void addRule(CFGRule rule) {
		cfgRules.put(rule.getLeftHandSide(), rule);
	}


	public List<String> generateAll() {
		if (cfgRules.containsKey(topElement)) {
			try {
				return generateAll(topElement);
			} catch (DialException e) {
				log.debug("generation error, aborting: " + e.toString());
				return new ArrayList<String>();
			}
		}
		else {
			return new ArrayList<String>();
		}
	}

	private List<String> generateAll(String lhs) throws DialException {
		CFGRule rule = cfgRules.get(lhs);
		List<String> instances = new ArrayList<String>();
		
		for (String rhs: rule.getAllRightHandSides()) {
			List<String> instancesRHS = new ArrayList<String>();
			instancesRHS.add("");
			String[] elements = rhs.split(" ");
			for (int i = 0 ; i < elements.length ; i++) {
				String element = elements[i];
				
				if (element.contains("(") && element.contains(")")) {
					String trimmedElement = element.replace("(", "").replace(")", "");
					if (isAllUpper(trimmedElement) && cfgRules.containsKey(trimmedElement)) {
						List<String> subValues = generateAll(trimmedElement);
						instancesRHS = combineLists(instancesRHS, subValues, true);
					}
					else {
						throw new DialException("grammar not complete : missing " + trimmedElement);
					}
				}
				else if (isAllUpper(element)) {
					if (cfgRules.containsKey(element)) {
						List<String> subValues = generateAll(element);
						instancesRHS = combineLists(instancesRHS, subValues, false);
					}
					else {
						throw new DialException("grammar not complete : missing " + element);
					}
				}
				else {
					instancesRHS = combineLists (instancesRHS, Arrays.asList(element), false);
				}
			}

			instances.addAll(instancesRHS);
		}
		return instances;
	}

	/**
	 * 
	 * @param instancesRHS
	 * @param subValues
	 * @return
	 */
	private List<String> combineLists(List<String> instances, List<String> newValues, boolean optional) {
		List<String> newInstances = new ArrayList<String>();
		for (String instance : instances) {
			if (optional) {
				newInstances.add(instance);
			}
			for (String newValue : newValues) {
				String newString = instance + " "  + newValue;
				newString = newString.replace("   ", " ").replace("  ", " ");
				newInstances.add(newString);
			}
		}
		return newInstances;
	}

	private static boolean isAllUpper(String s) {
		for(char c : s.toCharArray()) {
			if(Character.isLetter(c) && Character.isLowerCase(c)) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		String str = "";
		for (CFGRule r: cfgRules.values()) {
			str += r;
		}
		return str;
	}

}
