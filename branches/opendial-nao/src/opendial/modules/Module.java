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

package opendial.modules;


import opendial.DialogueSystem;
import opendial.arch.DialException;


/**
 * Representation of a system module.  The module should be attached to the dialogue
 * system through the method "attachModule" in DialogueSystem.  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Module {
		
	
	/**
	 * Starts the module.  The initialisation provides a reference to the main dialogue
	 * system (to which the module is attached).
	 * 
	 * @param system the dialogue system
	 * @throws DialException if the initialisation fails
	 */
	public void start(DialogueSystem system) throws DialException;

	/**
	 * Triggers the module after a state update
	 */
	public void trigger();
	
	/**
	 * Pauses the current module
	 * 
	 * @param toPause whether to pause or resume the module
	 */
	public void pause(boolean toPause);


}
