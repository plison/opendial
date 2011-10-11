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

package opendial.domains.types.values;

import opendial.domains.actions.ActionTemplate;

/**
 * Representation of an action value for a variable type.  The action
 * value is defined as a value label (which must be a string) associated 
 * with an action template (any class implementing ActionTemplate).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ActionValue<T> extends BasicValue<String> {

//	static Logger log = new Logger("ActionValue", Logger.Level.NORMAL);
	
	// the action template
	ActionTemplate<T> template;
	
	/**
	 * Creates a new action value given a label and an action template
	 * 
	 * @param label the label for the action value
	 * @param template the action template
	 */
	public ActionValue(String label, ActionTemplate<T> template) {
		super(label);
		this.template = template;
	}
	
	/**
	 * Returns the template associated with the action value
	 * 
	 * @return
	 */
	public ActionTemplate<T> getTemplate() {
		return template;
	}
	
	
	/**
	 * Returns a string representation of the action value
	 *
	 * @return the string representation
	 */
	public String toString() {
		String s = super.toString();
		s += ": " + template.toString();
		return s;
	}
}
