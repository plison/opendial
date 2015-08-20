// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.modules.Module;
import opendial.readers.XMLDomainReader;
import opendial.utils.StringUtils;

/**
 * Simulator for the user/environment. The simulator generated new environment
 * observations and user utterances based on a dialogue domain.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class Simulator implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueState simulatorState;

	Domain domain;

	// the main system (to which the simulator is attached)
	DialogueSystem system;

	/**
	 * Creates a new user/environment simulator.
	 * 
	 * @param system the main dialogue system to which the simulator should connect
	 * @param simulatorDomain the dialogue domain for the simulator simulator could
	 *            not be created
	 */
	public Simulator(DialogueSystem system, String simulatorDomain) {
		this(system, extractDomain(simulatorDomain));
	}

	/**
	 * Creates a new user/environment simulator.
	 * 
	 * @param system the main dialogue system to which the simulator should connect
	 * @param domain the dialogue domain for the simulator not be created
	 */
	public Simulator(DialogueSystem system, Domain domain) {
		this.system = system;
		this.domain = domain;
		simulatorState = domain.getInitialState().copy();
		simulatorState.setParameters(domain.getParameters());
		this.system.changeSettings(domain.getSettings());
	}

	/**
	 * Adds an empty action to the dialogue system to start the interaction.
	 */
	@Override
	public void start() {
		Assignment emptyAction = new Assignment(system.getSettings().systemOutput,
				ValueFactory.none());
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
	public void pause(boolean toPause) {
	}

	/**
	 * Triggers the simulator by updating the simulator state and generating new
	 * observations and user inputs.
	 * 
	 * @param systemState the dialogue state of the main dialogue system
	 * @param updatedVars the updated variables in the dialogue system
	 */
	@Override
	public void trigger(final DialogueState systemState,
			Collection<String> updatedVars) {
		if (updatedVars.contains(system.getSettings().systemOutput)) {
			(new Thread(() -> performTurn())).start();
		}
	}

	private static Domain extractDomain(String simulatorDomain) {
		if (simulatorDomain == null) {
			throw new RuntimeException("Required parameter: simulatorDomain");
		}
		return XMLDomainReader.extractDomain(simulatorDomain);
	}

	private void performTurn() {
		DialogueState systemState = system.getState();
		final String outputVar = system.getSettings().systemOutput;
		try {

			Value systemAction = ValueFactory.none();
			if (systemState.hasChanceNode(outputVar)) {
				systemAction = systemState.queryProb(outputVar).getBest();
			}

			log.fine("Simulator input: " + systemAction);
			boolean turnPerformed = performTurn(systemAction);
			int repeat = 0;
			while (!turnPerformed && repeat < 5
					&& system.getModules().contains(this)) {
				turnPerformed = performTurn(systemAction);
				repeat++;
			}
		}
		catch (RuntimeException e) {
			log.fine("cannot update simulator: " + e);
		}
	}

	/**
	 * Performs the dialogue turn in the simulator.
	 * 
	 * @param systemAction the last system action. @
	 */
	private synchronized boolean performTurn(Value systemAction) {

		boolean turnPerformed = false;
		simulatorState.setParameters(domain.getParameters());
		Assignment systemAssign =
				new Assignment(system.getSettings().systemOutput, systemAction);
		simulatorState.addToState(systemAssign);

		while (!simulatorState.getNewVariables().isEmpty()) {
			Set<String> toProcess = simulatorState.getNewVariables();
			simulatorState.reduce();

			for (Model model : domain.getModels()) {
				if (model.isTriggered(simulatorState, toProcess)) {
					boolean change = model.trigger(simulatorState);
					if (change && model.isBlocking()) {
						break;
					}
				}
			}

			if (!simulatorState.getUtilityNodeIds().isEmpty()) {
				double reward = simulatorState.queryUtil();
				String comment = "Reward: " + StringUtils.getShortForm(reward);
				system.displayComment(comment);
				system.getState().addEvidence(new Assignment(
						"R(" + systemAssign.addPrimes() + ")", reward));
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
	 * @return whether a user input has been generated @
	 */
	private boolean addNewObservations() {
		List<String> newObsVars = new ArrayList<String>();
		for (String var : simulatorState.getChanceNodeIds()) {
			if (var.contains("^o'")) {
				newObsVars.add(var);
			}
		}
		if (!newObsVars.isEmpty()) {
			MultivariateDistribution newObs = simulatorState.queryProb(newObsVars);
			for (String newObsVar : newObsVars) {
				newObs.modifyVariableId(newObsVar, newObsVar.replace("^o'", ""));
			}
			while (system.isPaused()) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
				}
			}
			if (!newObs.getValues().isEmpty()) {
				if (newObs.getVariables().contains(system.getSettings().userInput)) {
					log.fine("Simulator output: " + newObs + "\n --------------");
					system.addContent(newObs);
					return true;
				}
				else {
					log.fine("Contextual variables: " + newObs);
					system.addContent(newObs);
				}
			}
		}
		return false;
	}

}
