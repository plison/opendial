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

import java.util.List;

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
	static Logger log = new Logger("SurfaceTrigger", Logger.Level.NORMAL);
	
	// possible matches: exact or substring
	public static enum MatchType {SUBSTRING, EXACT};

	// match type for the trigger
	MatchType match = MatchType.SUBSTRING;
	
	// content of the trigger
	String content;
	
	// (possibly empty) list of slots
	List<String> slots;
	
	
	/**
	 * Creates a new surface trigger.  Default match type is "substring",
	 * and the slots are defined in the content by the notation ${variable}.
	 * 
	 * @param content the content of the surface trigger
	 */
	public SurfaceTrigger(String content) {
		this.content = content;
		slots = StringUtils.extractSlots(content);
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
}
