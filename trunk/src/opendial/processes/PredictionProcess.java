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

package opendial.processes;

import java.util.LinkedList;
import java.util.Queue;

import opendial.domains.Domain;
import opendial.outputs.Action;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * Prediction process, responsible for updating the dialogue state after
 * the execution of an action, + (POSSIBLY) predicting the next user actions 
 * or observations to come. 
 * 
 * More precisely, the process works as such: <ul>
 * <li> the process waits for a new system action to be posted on the queue;
 * <li> when such action is available, the process performs inference on the system
 * transition to determine the updated dialogue state
 * <li> the procedure is repeated till no other actions needs to be processed</ul>
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PredictionProcess extends Thread {

	// logger
	static Logger log = new Logger("PredictionProcess", Logger.Level.NORMAL);
	
	// dialogue state
	DialogueState state;
	
	// dialogue domain
	Domain domain;
	
	// queue of system actions to process
	Queue<Action> actionsToProcess ;
	
	
	/**
	 * Creates a new prediction process, with the dialogue state and domain
	 * 
	 * @param state the dialogue state
	 * @param domain the dialogue domain
	 */
	public PredictionProcess(DialogueState state, Domain domain) {
		this.state = state;
		this.domain = domain;
		actionsToProcess = new LinkedList<Action>();
	}

	/**
	 * Adds a system action to the queue of actions to process (in order to
	 * perform the post-action dialogue update)
	 * 
	 * @param action the system action
	 */
	public synchronized void addSystemAction(Action action) {
		actionsToProcess.add(action);
		notify();
	}
	
	
	/**
	 * Runs the transition/prediction loop, by polling an action from the
	 * queue and updating the dialogue state with it.  The procedure is repeated
	 * till the queue is empty.  
	 * 
	 * <p>When this happens, the process goes to sleep.
	 */
	@Override
	public void run () {
		while (true) {		
		           
				Action action = actionsToProcess.poll();
				
				while (action != null) {
					log.info("Trying to perform action transition...");
					updateState(action);
					action = actionsToProcess.poll();
				}

				synchronized (this) {
				try { wait();  }
				catch (InterruptedException e) {  }
			}
		}
	}
	
	
	/**
	 * Update the dialogue state (post-action update), given the system
	 * action which is being executed
	 * 
	 * @param action the action
	 */
	public void updateState(Action action) {
		state.dummyChange();
		log.info("Action transition performed...");
	}
	
}
