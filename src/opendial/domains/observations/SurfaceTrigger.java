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

package opendial.domains.observations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.inputs.NBestList;
import opendial.inputs.Observation;
import opendial.utils.Logger;
import opendial.utils.StringUtils;

/**
 * Surface trigger for the observation of an user utterance.  The trigger
 * defines a particular substring which must be matched for the trigger to 
 * be activated.  
 * 
 * Two alternative matching strategies are possible: exact string, or an 
 * included substring.  Additionally, a number of slots can be defined, which
 * function as a wildcard accepting any substring.  The content of the slot
 * will then be encoded as a feature of the observation variable.  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SurfaceTrigger implements Trigger<String> {

	// logger
	static Logger log = new Logger("SurfaceTrigger", Logger.Level.DEBUG);
	
	// possible matches: exact or substring
	public static enum MatchType {SUBSTRING, EXACT};

	// match type for the trigger
	MatchType match = MatchType.SUBSTRING;
	
	// content of the trigger
	String content;
	
	List<String> splitContent;
	
	// (possibly empty) list of slots
	List<String> slots;
	
	
	/**
	 * Creates a new surface trigger.  Default match type is "substring",
	 * and the slots are defined in the content by the notation ${variable}.
	 * 
	 * @param content the content of the surface trigger
	 */
	public SurfaceTrigger(String content) {
		this.content = content.toLowerCase().trim();
		slots = StringUtils.extractSlots(this.content);
		splitContent = StringUtils.splitContent(this.content, slots);
	}
	
	/**
	 * Sets the match type of the surface trigger
	 * 
	 * @param match
	 */
	public void setMatchType(MatchType match) {
		this.match = match;
	}
	
	
	/**
	 * Returns the content of the surface trigger
	 *
	 * @return trigger content
	 */
	public String getContent() {
		return content;
	}
	
	
	/**
	 * Returns the match type of the trigger
	 * 
	 * @return match type of trigger
	 */
	public MatchType getMatchType() {
		return match;
	}

	/**
	 * Returns the (possibly empty) list of slots for the trigger
	 * 
	 * @return the list of slots
	 */
	public List<String> getSlots() {
		return slots;
	}
	
	
	/**
	 * Returns a string representation of the trigger
	 *
	 * @return the string representation
	 */
	public String toString() {
		return content;
	}

	/**
	 * TODO: take into account slots and match type!
	 * @param obs
	 * @return
	 */
	@Override
	public float getProb(Observation obs) {
		float prob = 0.0f;
		if (obs instanceof NBestList) {
			for (String utt : ((NBestList)obs).getUtterances().keySet()) {
				if (matches(utt)) {
					prob += ((NBestList)obs).getUtterances().get(utt).floatValue();
				}
			}
			if (((NBestList)obs).getTotalProbability() < 1.0f && content.equals("*")) {
				prob += 1.0f - ((NBestList)obs).getTotalProbability();
			}
		}
		return prob;
	}
	
	
	public boolean matches (String utterance) {
		
		utterance = utterance.toLowerCase().trim();
		
		if (content.equals("*")) {
			return true;
		}
		else if (slots.isEmpty()) {
			if (match.equals(MatchType.SUBSTRING)) {
				if (utterance.endsWith(content)) {
					return true;
				}
				else if (utterance.contains(content)) {
					return !(Character.isLetter(utterance.charAt(utterance.indexOf(content)+content.length())));
				}
				return false;
			}
			else if (match.equals(MatchType.EXACT)) {
				return utterance.equals(content);
			}
		}
		else{
			int curIndex = 0;
			for (String split : splitContent) {
				String substring = utterance.substring(curIndex, utterance.length());
				if (!substring.contains(split)) {
					return false;
				}
				curIndex = content.indexOf(split) + split.length();
			}
			if (match.equals(MatchType.EXACT)) {
				if (content.endsWith("${"+ slots.get(slots.size()-1)+"}")) {
					return true;
				}
				else {
					return (curIndex == utterance.length() + 1);
				}
			}
			else if (match.equals(MatchType.SUBSTRING)) {
				return true;
			}
		}
		return false;
	}
	
	
	public Map<String,Map<String,Float>> fillSlots (Observation obs) {
		
		Map<String,Map<String,Float>> filledSlots = new HashMap<String,Map<String,Float>>();
		
		if (obs instanceof NBestList) {
			for (String utt : ((NBestList)obs).getUtterances().keySet()) {
				if (matches(utt)) {
					Map<String,String> slotsForUtterance = fillSlots(utt);
					for (String slot : slotsForUtterance.keySet()) {
						if (!filledSlots.containsKey(slot)) {
							filledSlots.put(slot, new HashMap<String,Float>());
							filledSlots.get(slot).put(slotsForUtterance.get(slot), 
									((NBestList)obs).getUtterances().get(utt));
						}
						else {
							if (!filledSlots.get(slot).containsKey(slotsForUtterance.get(slot))) {
								filledSlots.get(slot).put(slotsForUtterance.get(slot), 
										((NBestList)obs).getUtterances().get(utt));
							}
							else {
								filledSlots.get(slot).put(slotsForUtterance.get(slot), 
										filledSlots.get(slot).get(slotsForUtterance.get(slot)) +
										((NBestList)obs).getUtterances().get(utt));
							}
						}
					}
				}
			}
		}
		
		return filledSlots;
	}
	
	public Map<String,String> fillSlots (String utt) {
		
		utt = utt.toLowerCase().trim();
		
		Map<String,String> filledSlots = new HashMap<String,String>();
		
		int indent = 0;
		for (int i = 0 ; i < slots.size(); i++) {
			String slot = slots.get(i);
			int lowBound = 0 ;
			int highBound = 0;
			if (content.startsWith("${"+ slot + "}")) {
				lowBound = 0;
			}
			else {
				lowBound = utt.indexOf(splitContent.get(i)) + splitContent.get(i).length();
			}
			if (content.endsWith("${"+slot+"}")) {
				highBound = utt.length();
			}
			else {
				highBound = utt.indexOf(splitContent.get(i+1));
			}
			filledSlots.put(slot, utt.substring(lowBound,highBound).trim());
			indent += highBound-lowBound;
		}
		
		return filledSlots;
	}
	

	/**
	 *
	 * @param t2
	 * @return
	 */
	@Override
	public boolean subsumes(Trigger t2) {
		if (t2 instanceof SurfaceTrigger) {
			if (content.contains("*") && !((SurfaceTrigger)t2).getContent().contains("*")) {
				return true;
			}
		}
		return false;
	}
}
