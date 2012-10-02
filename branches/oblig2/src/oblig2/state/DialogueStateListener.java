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

package oblig2.state;

import java.io.File;

import oblig2.NBest;
import oblig2.actions.DialogueAction;


/**
 * Listener on the dialogue state
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface DialogueStateListener {
	
	/**
	 * Reacts to a new speech signal recorded (as a temporary sound file)
	 */
	public void newSpeechSignal(File audioFile);

	/**
	 * Reacts to a new user utterance recognised by the ASR
	 * 
	 * @param u_u the user utterance (NBest list)
	 */
	public void processUserInput (NBest u_u);
	
	
	/**
	 * Reacts to a new system utterance selected by the policy
	 * 
	 * @param action the system utterance (dialogue action)
	 */
	public void processSystemOutput (DialogueAction action);

	
}
