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
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PredictionProcess extends Thread {

	static Logger log = new Logger("PredictionProcess", Logger.Level.NORMAL);
	
	DialogueState state;
	Domain domain;
	
	Queue<Action> actionsToProcess ;
	
	public PredictionProcess(DialogueState state, Domain domain) {
		this.state = state;
		this.domain = domain;
		actionsToProcess = new LinkedList<Action>();
	}

	/**
	 * 
	 * @param action
	 */
	public synchronized void performTransition(Action action) {
		actionsToProcess.add(action);
		notify();
	}
	
	@Override
	public void run () {
		while (true) {		
		           
				Action action = actionsToProcess.poll();
				
				while (action != null) {
					log.info("Trying to perform action transition...");
					updateState();
					action = actionsToProcess.poll();
				}

				synchronized (this) {
				try { wait();  }
				catch (InterruptedException e) {  }
			}
		}
	}
	
	
	public void updateState() {
		state.dummyChange();
		log.info("Action transition performed...");
	}
	
}
