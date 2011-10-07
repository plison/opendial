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
import opendial.inputs.Observation;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PerceptionProcess extends Thread {

	static Logger log = new Logger("PerceptionProcess", Logger.Level.NORMAL);
	
	DialogueState state;
	
	Domain domain;
	
	Queue<Observation> observationsToProcess;

	
	public PerceptionProcess(DialogueState state, Domain domain) {
		this.state = state;
		this.domain = domain;
		observationsToProcess = new LinkedList<Observation>();
	}
	
	public void updateState() {
		state.dummyChange();
	}

	/**
	 * 
	 * @param nbestList
	 */
	public synchronized void addObservation(Observation obs) {
		observationsToProcess.add(obs);
		log.info("New observation perceived!");
		notify();
	}
	
	
	@Override
	public void run () {
		while (true) {		
		           
				Observation newObs = observationsToProcess.poll();
				
				while (newObs != null) {
					log.info("Trying to process observation...");
					updateState();
					newObs = observationsToProcess.poll();
				}

				synchronized (this) {
				try { wait();  }
				catch (InterruptedException e) {  }
			}
		}
	}


}
