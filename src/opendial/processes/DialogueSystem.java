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


import opendial.domains.Domain;
import opendial.inputs.NBestList;
import opendial.inputs.Observation;
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
public class DialogueSystem {

	static Logger log = new Logger("DialogueSystem", Logger.Level.NORMAL);
	
	Domain domain;
	
	DialogueState state;
		
	PerceptionProcess perception;
	DecisionProcess decision;
	PredictionProcess transition;
	
	
	public DialogueSystem(Domain domain) {
		
		this.domain = domain;
		state = domain.getInitialState().copy();
		log.info("initial state: \n" + state.toString());
		
		perception = new PerceptionProcess(state,domain);
		perception.start();
		
		decision = new DecisionProcess(state,domain);
		decision.start();
		
		transition = new PredictionProcess(state,domain);
		transition.start();

		log.info("Dialogue system started, dialogue domain: " + domain.getName());
	}
	
	
	public void addObservation(Observation obs) {
		perception.addObservation(obs);
	}
	

	public Action getNextAction() {
		Action action = decision.pollAvailableAction();
		transition.performTransition(action);
		return action;
	}

	
	
}
