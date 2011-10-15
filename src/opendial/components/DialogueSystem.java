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

package opendial.components;


import opendial.arch.DialogueInterface;
import opendial.domains.Domain;
import opendial.inputs.Observation;
import opendial.outputs.Action;
import opendial.processes.DecisionProcess;
import opendial.processes.PerceptionProcess;
import opendial.processes.PredictionProcess;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * Top class for the dialogue system.  The dialogue system is defined by a dialogue
 * domain, which comprise type declarations, initial state, and several rule-based
 * probabilistic models.
 * 
 * <p>Once created, the dialogue system can receive new observation, and outputs new
 * actions to execute.  Internally, the system is made of three threads executed in
 * parallel: <ul>
 * <li> one perception process, which deals with new observations;
 * <li> one decision process, performing action selection;
 * <li> one prediction process, for system transition and user prediction.
 *  
 *  <p>Dialogue interfaces can be attached to the system, and listen to new input 
 *  observations and output actions.  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueSystem {

	// logger
	static Logger log = new Logger("DialogueSystem", Logger.Level.NORMAL);
	
	// the dialogue domain
	Domain domain;
	
	// the current dialogue state
	DialogueState state;
		
	// the perception process
	PerceptionProcess perception;
	
	// the decision process
	DecisionProcess decision;
	
	// the transition process
	PredictionProcess transition;
		
	
	/**
	 * Creates a new dialogue system, based on the specified dialogue domain
	 * 
	 * @param domain the dialogue domain
	 */
	public DialogueSystem(Domain domain) {
		
		// initialise the system
		this.domain = domain;
		state = domain.getInitialState().copy();
		log.info("initial state: \n" + state.toString());
		
		// start up the perception process
		perception = new PerceptionProcess(state,domain);
		perception.start();
		
		// start up the decision process
		decision = new DecisionProcess(state,domain);
		decision.start();
		
		// start up the prediction process
		transition = new PredictionProcess(state,domain);
		transition.start();
		
		log.info("Dialogue system started, dialogue domain: " + domain.getName());
	}
	  
	
	/**
	 * Adds a dialogue interface listening to new input observations and 
	 * actions selected by the system.  
	 * 
	 * @param interface1 the interface
	 */
	public void addInterface(DialogueInterface interface1) {
		perception.addInterface(interface1);
		decision.addInterface(interface1);
	}
	
	
	/**
	 * Adds a new observation to process by the system 
	 * 
	 * @param obs the observation
	 */
	public void addObservation(Observation obs) {
		// TODO: provide the predicted bayesian network to the perception component
		perception.addObservation(obs);
	}
	

	/**
	 * Poll the next action selected by the system
	 * 
	 * @return the next action
	 */
	public Action pollNextAction() {
		Action action = decision.pollAvailableAction();
		transition.addSystemAction(action);
		return action;
	}

	
	
}
