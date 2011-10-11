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

package opendial.domains.actions;

import java.util.List;

import opendial.utils.Logger;
import opendial.utils.StringUtils;

/**
 * A surface realisation template, containing a main string content
 * (the utterance to synthesise), plus an optional number of slots
 * to fill.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SurfaceRealisationTemplate implements ActionTemplate<String> {

	// the logger
	static Logger log = new Logger("SurfaceRealisationTemplate", Logger.Level.NORMAL);
	
	// the content of the template
	String content;
	
	// the slots of the template (might be empty)
	List<String> slots;
	
	
	/**
	 * Creates a new surface realisation template, with the given content.
	 * The positions for the slots must be denoted by ${variable}.
	 * 
	 * @param content the content of the template
	 */
	public SurfaceRealisationTemplate(String content) {
		this.content = content;		
		slots = StringUtils.extractSlots(content);
	}
	

	/**
	 * Returns the content of the template
	 *
	 * @return the content of the template
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Returns the (possible empty) collection of slots for the template
	 * 
	 * @return the list of slots (ordering does not matter)
	 */
	public List<String> getSlots() {
		return slots;
	}
	
	
	/**
	 * Returns a string representation of the template
	 *
	 * @return the string representation
	 */
	public String toString() {
		return content;
	}
}
