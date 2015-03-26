// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.modules.simulation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.modules.Module;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

/**
 * Simulator for the user/environment.  The simulator generated new environment observations
 * and user utterances based on a dialogue domain.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-16 17:34:31 #$
 */
public class Simulator implements Module {

	// logger
	public static Logger log = new Logger("Simulator", Logger.Level.DEBUG);

	DialogueState simulatorState;

	Domain domain;

	// the main system (to which the simulator is attached)
	DialogueSystem system;


	/**
	 * Creates a new user/environment simulator.
	 * 
	 * @param system the main dialogue system to which the simulator should connect
	 * @throws DialException if the simulator could not be created
	 */
	public Simulator(DialogueSystem system) throws DialException  {
		this(system, extractDomain(system.getSettings()));
	}


	/**
	 * Creates a new user/environment simulator.
	 * 
	 * @param system the main dialogue system to which the simulator should connect
	 * @param domain the dialogue domain for the simulator
	 * @throws DialException if the simulator could not be created
	 */
	public Simulator(DialogueSystem system, Domain domain) throws DialException {
		this.system = system;
		this.domain = domain;
		simulatorState = domain.getInitialState().copy();
		simulatorState.setParameters(domain.getParameters());
		this.system.changeSettings(domain.getSettings());
	}


	/**
	 * Extracts the simulator domain from the parameter "simulator-domain" in the
	 * settings, if it is mentioned.  Else, throw an exception.
	 * 
	 * @param settings the system settings
	 * @return the dialogue domain for the simulator
	 * @throws DialException if the simulator domain is not specified or ill-formatted.
	 */
	private static Domain extractDomain(Settings settings) throws DialException {
		if (!settings.params.containsKey("simulator-domain")) {
			throw new MissingParameterException("simulator-domain");
		}
		return XMLDomainReader.extractDomain(settings.params.getProperty("simulator-domain"));
	}


	/**
	 * Adds an empty action to the dialogue system to start the interaction.
	 */
	@Override
	public void start() throws DialException {
		Assignment emptyAction = new Assignment(system.getSettings().systemOutput, ValueFactory.none());
		if (system.isPaused()) {
			system.getState().addToState(emptyAction);
		}
		else {
			system.addContent(emptyAction);
		}
		system.attachModule(RewardLearner.class);
	}


	/**
	 * Returns true if the system is not paused, and false otherwise
	 */
	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public void pause(boolean toPause) {	}


	/**
	 * Triggers the simulator by updating the simulator state and generating new observations
	 * and user inputs.
	 * 
	 * @param systemState the dialogue state of the main dialogue system
	 * @param updatedVars the updated variables in the dialogue system
	 */
	@Override
	public void trigger(final DialogueState systemState, Collection<String> updatedVars) {
		final String outputVar = system.getSettings().systemOutput;
		if (updatedVars.contains(outputVar)) {
			(new Thread() {
				@Override
				public void run() {
					try {
						synchronized (systemState) {

							Assignment systemAction = (systemState.hasChanceNode(outputVar))? 
									systemState.queryProb(outputVar).toDiscrete().getBest()
									: Assignment.createDefault(outputVar);

									log.debug("Simulator input: " + systemAction);
									boolean turnPerformed = performTurn(systemAction);
									int repeat = 0 ;
									while (!turnPerformed && repeat < 5) {
										turnPerformed = performTurn(systemAction);
									}
						}
					}
					catch (DialException e) {
						log.debug("cannot update simulator: " + e);
					}
				}
			}).start();
		}
	}


	/**
	 * Performs the dialogue turn in the simulator.
	 * 
	 * @param systemAction the last system action.
	 * @throws DialException
	 */
	private synchronized boolean performTurn(Assignment systemAction) throws DialException {

		boolean turnPerformed = false;
		simulatorState.setParameters(domain.getParameters());
		simulatorState.addToState(systemAction);
	
		while (!simulatorState.getNewVariables().isEmpty()) {

			Set<String> toProcess = simulatorState.getNewVariables();
			simulatorState.reduce();	

			for (Model model : domain.getModels()) {
				model.trigger(simulatorState, toProcess);
			}

			if (!simulatorState.getUtilityNodeIds().isEmpty()) {
				double reward = simulatorState.queryUtil();
				String comment = "Reward: " + StringUtils.getShortForm(reward);
				system.recordComment(comment);
				log.debug(comment);
				system.getState().addEvidence(new Assignment(
						"R(" + systemAction.addPrimes()+")", reward));
				simulatorState.removeNodes(simulatorState.getUtilityNodeIds());
			}

			 if (addNewObservations()) {
				turnPerformed = true;
			 }

			simulatorState.addEvidence(simulatorState.getSample());
		}
		return turnPerformed;
	}


	/**
	 * Generates new simulated observations and adds them to the dialogue state. The 
	 * method returns true when a new user input has been generated, and false 
	 * otherwise.
	 * 
	 * @return whether a user input has been generated
	 * @throws DialException
	 */
	private boolean addNewObservations() throws DialException {
		List<String> newObsVars = new ArrayList<String>();
		for (String var : simulatorState.getChanceNodeIds()) {
			if (var.contains("^o'")){
				newObsVars.add(var);
			}
		}
		if (!newObsVars.isEmpty()) {
		CategoricalTable newObs = simulatorState.queryProb(newObsVars).toDiscrete().copy();
		for (String newObsVar : newObsVars) {
			newObs.modifyVariableId(newObsVar, newObsVar.replace("^o'", ""));
		}
		while (system.isPaused()) {
			try { Thread.sleep(50); } catch (InterruptedException e) { }
		}
		if (!newObs.isEmpty()) {
			if (newObs.getHeadVariables().contains(system.getSettings().userInput)) {
				log.debug("Simulator output: " + newObs + "\n --------------");
				system.addContent(newObs.copy());
				return true;
			}
			else {
				log.debug("Contextual variables: " + newObs);
				system.addContent(newObs.copy());
			}
		}
		}
		return false;
	}


}

