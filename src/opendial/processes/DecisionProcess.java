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
import java.util.List;
import java.util.Queue;

import opendial.domains.Domain;
import opendial.gui.DialogueInterface;
import opendial.outputs.Action;
import opendial.outputs.VoidAction;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * A decision process, which is the process responsible for deciding on the
 * next action to perform.  
 * 
 * <p>More precisely, the decision process: <ul>
 * <li> waits for a change to the dialogue state;
 * <li> once such change occurs, the process performs probabilistic inference 
 * to decide what is the optimal next action;
 * <li> the selected action is then inserted in a queue, to be emptied by
 * the dialogue system for execution. </ul>
 * 
 * Optionally, dialogue interfaces can be defined which are continuously listening 
 * to the process and are informed of newly selected actions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DecisionProcess extends Thread{

	// logger
	static Logger log = new Logger("DecisionProcess", Logger.Level.DEBUG);
	
	// the dialogue state
	DialogueState state;
	
	// the dialogue domain
	Domain domain;
	
	// the queue of actions to execute
	Queue<Action> actionsToExecute;
	
	// interfaces listening to the process
	List<DialogueInterface> interfaces;

	
	/**
	 * Creates a new decision process with the given dialogue state and domain
	 * 
	 * @param state the dialogue state
	 * @param domain the dialogue domain
	 */
	public DecisionProcess(DialogueState state, Domain domain) {
		this.state = state;
		this.domain = domain;
		interfaces = new LinkedList<DialogueInterface>();

		actionsToExecute = new LinkedList<Action>();
	}
	

	/**
	 * Runs the action selection loop, which is triggered each time there is
	 * a change to the dialogue state.  The best next action is then selected
	 * and added to the queue of actions to execute.  
	 */
	@Override
	public void run () {
		while (true) {		
		           
				log.info("--> Initiate action selection");
				
				// decide on the next action
				Action nextAction = decideNextAction();	
				log.info("--> Action selection complete, selected action: " + nextAction);

				// add it to the queue, inform the interfaces
				actionsToExecute.add(nextAction);
				informInterfaces(nextAction);
				
				synchronized (this) { notify(); }
				

				try { synchronized (state) {  state.wait(); }  }
				catch (InterruptedException e) {  }

		}
	}

	
	/**
	 * Adds a dialogue interface as listener to the process
	 * 
	 * @param interface1 the interface
	 */
	public void addInterface(DialogueInterface interface1) {
		interfaces.add(interface1);
	}
	
	
	/**
	 * Informs the dialogue interfaces of the newly selected action
	 * 
	 * @param action the action 
	 */
	private void informInterfaces(Action action) {
		for (DialogueInterface interface1 : interfaces) {
			interface1.showAction(action);
		}
	}

	
	/**
	 * Decide on the next action to perform in the current state
	 * TODO: implement action selection
	 * 
	 * @return the best action
	 */
	private Action decideNextAction() {
		return new VoidAction();
	}
	
	
	
	/**
	 * Poll one action from the queue of actions to execute.  If none
	 * are available, wait for one to appear.  NB: this is a blocking
	 * thread!
	 * 
	 * @return the polled action
	 */
	public synchronized Action pollAvailableAction() {
		Action action = actionsToExecute.poll();
		if (action == null) {
				try {	wait(); }
				catch (InterruptedException e) { }
				action = actionsToExecute.poll();
		}

		log.debug("polledAction: " + action);		
		return action;
	}
	
	
}
