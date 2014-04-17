
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


package oblig2;

import oblig2.actions.Action;
import oblig2.state.DialogueState;
import oblig2.state.WorldState;

/**
 * Interface for the dialogue policy mapping an input (N-Best list) to an
 * output action (which might be a dialogue action, a physical action, or 
 * not action at all).
 * 
 * This interface must be implemented by all policies used in the dialogue
 * system.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface DialoguePolicy  {

	/**
	 * Processes the user utterance given as input, and selects the corresponding
	 * action to execute. The dialogue state and world state are given as arguments 
	 * to inform the decision process.  
	 * 
	 * @param u_u the user utterance (N-Best list)
	 * @param dstate the dialogue state (dialogue history)
	 * @param wstate the world state (robot position, visual objects, etc.)
	 * @return the resulting action to execute by the system
	 */
	public Action processInput(NBest u_u, DialogueState dstate, WorldState wstate);
}
