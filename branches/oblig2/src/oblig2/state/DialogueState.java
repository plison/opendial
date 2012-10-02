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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oblig2.NBest;
import oblig2.actions.DialogueAction;
import oblig2.util.Logger;


/**
 * Representation of the dialogue state (currently, simply the history
 * of the dialogue)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueState {

	// logger
	public static Logger log = new Logger("DialogueState", Logger.Level.NORMAL);

	// the ordered list of utterances (both user and system)
	List<NBest> utterances;	
	
	// the listeners attached to this state
	Set<DialogueStateListener> listeners;
	
	/**
	 * Creates a new dialogue state with empty history
	 */
	public DialogueState() {
		utterances = new ArrayList<NBest>();
		listeners = new HashSet<DialogueStateListener>();
	}
	
	/**
	 * Attaches a new listener to the state
	 * 
	 * @param listener the listener
	 */
	public void addListener(DialogueStateListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Indicates that a new speech signal has been recorderd in an audio
	 * file, and informs its listeners
	 */
	public void newSpeechSignal(File audioFile) { 
		for (DialogueStateListener listener: listeners) {
			listener.newSpeechSignal(audioFile);
		}
	}
	
	
	/**
	 * Indicates that a new user utterance has been recognised,
	 * updates the history, and informs its listeners
	 * 
	 * @param u_u the user utterance
	 */
	public void addUserUtterance(NBest u_u) {
		utterances.add(u_u);
		for (DialogueStateListener listener: listeners) {
			listener.processUserInput(u_u);
		}
	}
	
	/**
	 * Indicates that a new system utterance has been selected,
	 * updates the history, and informs its listeners
	 * 
	 * @param u_m the system utterance (action)
	 */
	public void executeAction(DialogueAction u_m) {
		utterances.add(new NBest(u_m.getUtterance(), 1.0f));
		for (DialogueStateListener listener: listeners) {
			listener.processSystemOutput(u_m);
		}
	}
	
}
