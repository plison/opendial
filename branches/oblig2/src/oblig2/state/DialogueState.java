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

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import oblig2.ConfigParameters;
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

	ConfigParameters parameters;
	
	// the dialogue history
	Stack<DialogueTurn> history;	
	
	// the listeners attached to this state
	List<DialogueStateListener> listeners;
	
	/**
	 * Creates a new dialogue state with empty history
	 */
	public DialogueState(ConfigParameters parameters) {
		this.parameters = parameters;
		history = new Stack<DialogueTurn>();
		listeners = new LinkedList<DialogueStateListener>();
	}
	
	/**
	 * Attaches a new listener to the state
	 * 
	 * @param listener the listener
	 */
	public void addListener(DialogueStateListener listener) {
		listeners.add(0,listener);
	}
	
	/**
	 * Indicates that a new speech signal has been recorded in an audio
	 * stream, and informs its listeners
	 */
	public void newSpeechSignal(InputStream istream) { 
		for (DialogueStateListener listener: listeners) {
			listener.newSpeechSignal(istream);
		}
	}
	
	
	/**
	 * Indicates that a new user utterance has been recognised,
	 * updates the history, and informs its listeners
	 * 
	 * @param u_u the user utterance
	 */
	public void addUserUtterance(NBest u_u) {
		history.add(new DialogueTurn(parameters.username, u_u));
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
		DialogueTurn systemTurn = new DialogueTurn(parameters.machinename, new NBest(u_m.getUtterance(), 1.0f));
		history.add(systemTurn);
		for (DialogueStateListener listener: listeners) {
			listener.processSystemOutput(u_m);
		}
	}
	
	
	/**
	 * Returns the dialogue history recorded by the system.
	 * 
	 * @return the dialogue history
	 */
	public Stack<DialogueTurn> getDialogueHistory() {
		return history;
	}
	
	
	/**
	 * Returns a string representation of the dialogue state
	 *
	 * @return the string
	 */
	public String toString() {
		String s = "";
		for (DialogueTurn turn : history) {
			s += turn.toString() + "\n";
		}
		return s;
	}
	
	
	
	/**
	 * Representation of a dialogue turn, which can be either from the user
	 * or from the system.  The turn contains the name of the agent, its
	 * utterance (as N-Best list), and the timing of the utterance as recorded 
	 * in the dialogue history.
	 * 
	 *
	 */
	public final class DialogueTurn {
		
		// name of the agent
		String agent;
		
		// the utterance
		NBest utterance;
		
		// utterance timing
		long time;
		
		
		
		/**
		 * Creates a new dialogue turn
		 * 
		 * @param agent name of the agent
		 * @param utterance the utterance
		 */
		public DialogueTurn(String agent, NBest utterance) {
			this.agent = agent;
			this.utterance = utterance;
			time = System.currentTimeMillis();
		}
		
		/**
		 * Returns the name of the agent
		 * 
		 * @return the agent
		 */
		public String getAgent() {
			return agent;
		}
		
		
		/**
		 * Returns the utterance for the turn (as an N-Best list)
		 * 
		 * @return the utterance
		 */
		public NBest getUtterance() {
			return utterance;
		}
		
		/**
		 * Returns the timing (in milliseconds) for the utterance
		 * 
		 * @return the timing
		 */
		public long getTime() {
			return time;
		}
		
		
		/**
		 * Returns a string representation of the dialogue turn
		 *
		 * @return the string
		 */
		public String toString() {
			return getAgent() + ":\t" + getUtterance();
		}
	}
	
}
