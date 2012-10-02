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

import java.io.File;

import oblig2.actions.Action;
import oblig2.actions.CompoundAction;
import oblig2.actions.DialogueAction;
import oblig2.actions.PhysicalAction;
import oblig2.gui.DialogueSystemGUI;
import oblig2.state.DialogueState;
import oblig2.state.DialogueStateListener;
import oblig2.state.WorldState;
import oblig2.util.Logger;
import oblig2.util.ServerConnection;


/**
 * Top class for the dialogue system, controlling the dialogue flow,
 * and connecting to the GUI and external processes (e.g. ASR and TTS
 * from AT&T servers).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueSystem implements DialogueStateListener {

	// logger
	public static Logger log = new Logger("DialogueSystem", Logger.Level.NORMAL);

	// system parameters
	ConfigParameters parameters;

	// dialogue policy to follow
	DialoguePolicy policy;
	
	// dialogue state
	DialogueState dstate;
	
	// world state
	WorldState wstate;
	
	
	/**
	 * Creates a new dialogue system with the given policy and parameters
	 * 
	 * @param policy the policy to follow 
	 * @param parameters various parameters
	 */
	public DialogueSystem(DialoguePolicy policy, ConfigParameters parameters) {
		log.info("starting up spoken dialogue system");
		
		// sets the policy and parameters
		this.policy = policy;
		this.parameters = parameters;

		// creates the basic state representations
		dstate = new DialogueState();
		wstate = new WorldState(parameters);
		dstate.addListener(this);

		// starts up the GUI and remove connection
		try {
		new DialogueSystemGUI(this);
		new ServerConnection(this);	
		}
		catch (Exception e) {
			log.warning("fatal error while starting up the system, must exit. Reason: " + e.toString());
		}
	}

	
	/**
	 * Processes the user input by applying the policy on it, and then
	 * executing the selected action.
	 *
	 * @param nbest the N-best list corresponding to the user input
	 */
	@Override
	public void processUserInput(NBest nbest) {
		Action result = policy.processInput(nbest, dstate, wstate);
		executeAction(result);
	}
	
	
	/**
	 * Executes the action given as argument
	 * 
	 * @param action the action to execute
	 */
	public void executeAction(Action action) {
		if (action instanceof DialogueAction) {
			dstate.executeAction((DialogueAction)action);
		}
		else if (action instanceof PhysicalAction) {
			wstate.executeAction((PhysicalAction)action);
		}
		else if (action instanceof CompoundAction) {
			for (Action a : ((CompoundAction)action).getBasicActions()) {
				executeAction(a);
			}
		}
	}
	
	
	/**
	 * Does nothing
	 */
	@Override
	public void processSystemOutput(DialogueAction action) {	}
	
	/**
	 * Does nothing
	 */
	@Override
	public void newSpeechSignal(File audioFile) {	}
	


	/**
	 * Returns the system parameters
	 * 
	 * @return the parameters
	 */
	public ConfigParameters getParameters() {
		return parameters;
	}
	
	
	/**
	 * Returns the current dialogue state 
	 * 
	 * @return dialogue state
	 */
	public DialogueState getDialogueState() {
		return dstate;
	}
	
	
	/**
	 * Returns the current world state
	 * 
	 * @return world state
	 */
	public WorldState getWorldState() {
		return wstate;
	}
	
}
