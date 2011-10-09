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
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SurfaceTrigger extends Trigger {

	static Logger log = new Logger("SurfaceTrigger", Logger.Level.NORMAL);
	
	public static enum MatchType {SUBSTRING, EXACT};

	MatchType match = MatchType.SUBSTRING;
	
	String content;
	
	List<String> slots;
	
	
	public SurfaceTrigger(String content) {
		this.content = content;
		slots = StringUtils.extractSlots(content);
	}
	
	public void setMatchType(MatchType match) {
		this.match = match;
	}
	
	public String getContent() {
		return content;
	}
	
	public MatchType getMatchType() {
		return match;
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getSlots() {
		return slots;
	}
}
