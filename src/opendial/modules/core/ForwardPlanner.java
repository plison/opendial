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

package opendial.modules.core;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.UtilityTable;
import opendial.datastructs.Assignment;
import opendial.domains.Model;
import opendial.modules.Module;
import opendial.state.DialogueState;


/**
 * Online forward planner for OpenDial. The planner constructs a lookahead tree (with a 
 * depth corresponding to the planning horizon) that explores possible actions and their
 * expected consequences on the future dialogue state. The final utility values for each
 * action is then estimated, and the action with highest utility is selected.
 * 
 * <p>The planner is an anytime process.  It can be interrupted at any time and yield a 
 * result. The quality of the utility estimates is of course improving over time.
 * 
 * <p>The planning algorithm is described in pages 121-123 of Pierre Lison's PhD thesis 
 * [http://folk.uio.no/plison/pdfs/thesis/thesis-plison2013.pdf]
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ForwardPlanner implements Module {

	// logger
	public static Logger log = new Logger("ForwardPlanner", Logger.Level.DEBUG);


	/** Maximum number of actions to consider at each planning step */
	public static int NB_BEST_ACTIONS = 100;
	
	/** Maximum number of alternative observations to consider at each planning step */
	public static int NB_BEST_OBSERVATIONS = 3;
	
	/** Minimum probability for the generated observations */
	public static double MIN_OBSERVATION_PROB = 0.1;
	
	DialogueSystem system;
	
	/** Current planning process (if active) */
	PlannerProcess currentProcess;

	boolean paused = false;

	//scheduled thread pool to terminate planning once the time limit is reached
	static ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

	
	/**
	 * Constructs a forward planner for the dialogue system.
	 * 
	 * @param system the dialogue system associated with the planner.
	 */
	public ForwardPlanner(DialogueSystem system) {
		this.system = system;
	}

	/**
	 * Pauses the forward planner
	 */
	@Override
	public void pause(boolean shouldBePaused) {	
		paused = shouldBePaused;
		if (currentProcess != null && !currentProcess.isTerminated) {
			log.debug("trying to terminate the process?");
			currentProcess.isTerminated = true;
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void start()  {	}


	/**
	 * Returns true if the planner is not paused.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

	/**
	 * Triggers the planning process.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		if (!paused && !state.getActionNodeIds().isEmpty()) {
				currentProcess = new PlannerProcess(state);
		}
	} 


	/**
	 * Planner process, which can be terminated before the end of the horizon
	 * @author  Pierre Lison (plison@ifi.uio.no)
	 * @version $Date::                      $
	 */
	public class PlannerProcess {

		DialogueState initState;

		boolean isTerminated = false;

		/**
		 * Creates the planning process.  Timeout is set to twice the maximum sampling time. 
		 * Then, runs the planner until the horizon has been reached, or the planner has 
		 * run out of time.  Adds the best action to the dialogue state.
		 * 
		 * @param initState initial dialogue state.
		 */
		public PlannerProcess(DialogueState initState) {
			this.initState = initState;
			service.schedule(() -> isTerminated=true, Settings.maxSamplingTime*2, TimeUnit.MILLISECONDS);

	
			try {
				UtilityTable evalActions =getQValues(initState, system.getSettings().horizon);

				Assignment bestAction =  evalActions.getBest().getKey(); 
				initState.removeNodes(initState.getUtilityNodeIds());
				initState.removeNodes(initState.getActionNodeIds());
				
				if (evalActions.getUtil(bestAction) < 0.001) {
					bestAction = Assignment.createDefault(bestAction.getVariables());
				}
				
				// only executes action if the user is not currently speaking 
				// (simplifying assumption, could be corrected afterwards)
				if (!initState.hasChanceNode(system.getSettings().userSpeech)) {
					initState.addToState(bestAction.removePrimes());
				}
				
				isTerminated = true;
			}
			catch (Exception e) {
				log.warning("could not perform planning, aborting action selection: " + e);
				e.printStackTrace();
			}
		}



		/**
		 * Returns the Q-values for the dialogue state, assuming a particular horizon.
		 * 
		 * @param state the dialogue state
		 * @param horizon the planning horizon
		 * @return the estimated utility table for the Q-values
		 * @throws DialException
		 */
		private UtilityTable getQValues (DialogueState state, int horizon) throws DialException {
			Set<String> actionNodes = state.getActionNodeIds();

			if (actionNodes.isEmpty()) {
				return new UtilityTable();
			}
			UtilityTable rewards = state.queryUtil(actionNodes);
			if (horizon ==1) {
				return rewards;
			}

			UtilityTable qValues = new UtilityTable();

			for (Assignment action : rewards.getRows()) {
				double reward = rewards.getUtil(action);
				qValues.setUtil(action, reward);

				if (horizon > 1 && !isTerminated && !paused && hasTransition(action)) {

					DialogueState copy = state.copy();
					copy.addToState(action.removePrimes());
					updateState(copy);
					
					if (!action.isDefault()) {
						double expected = system.getSettings().discountFactor * getExpectedValue(copy, horizon - 1);
						qValues.setUtil(action, qValues.getUtil(action) + expected);
					}	
				}
			}
			return qValues;
		}


		/**
		 * Adds a particular content to the dialogue state
		 * @param state the dialogue state
		 * @param newContent the content to add
		 * @throws DialException if the update operation could not be performed
		 */
		private void updateState(DialogueState state) throws DialException {
			
			while (!state.getNewVariables().isEmpty()) {
				Set<String> toProcess = state.getNewVariables();
				state.reduce();	
				for (Model model : system.getDomain().getModels()) {
					model.trigger(state, toProcess);
				}
			}
		}


		/**
		 * Returns true if the dialogue domain specifies a transition model for 
		 * the particular action assignment.
		 * 
		 * @param action the assignment of action values
		 * @return true if a transition is defined, false otherwise.
		 */
		private boolean hasTransition(Assignment action) {
			for (Model m : system.getDomain().getModels()) {
				if (m.isTriggered(action.removePrimes().getVariables())) {
					return true;
				}
			}
			return false;
		}


		/**
		 * Estimates the expected value (V) of the dialogue state in the current planning
		 * horizon.
		 * 
		 * @param state the dialogue state
		 * @param horizon the planning horizon
		 * @return the expected value.
		 * @throws DialException
		 */
		private double getExpectedValue(DialogueState state, int horizon) throws DialException {

			MultivariateTable observations = getObservations(state);
			MultivariateTable nbestObs = observations.getNBest(NB_BEST_OBSERVATIONS);
			double expectedValue = 0.0;
			for (Assignment obs : nbestObs.getValues()) {
				double obsProb = nbestObs.getProb(obs);
				if (obsProb > MIN_OBSERVATION_PROB) {
					DialogueState copy = state.copy();
					copy.addToState(obs);
					updateState(copy);

					UtilityTable qValues = getQValues(copy, horizon);
					if (!qValues.getRows().isEmpty()) {
						Assignment bestAction = qValues.getBest().getKey();
						double afterObs = qValues.getUtil(bestAction);
						expectedValue += obsProb * afterObs;
					}
				}
			}	

			return expectedValue;
		}



		/**
		 * Returns the possible observations that are expected to be perceived
		 * from the dialogue state
		 * @param state the dialogue state from which to extract observations
		 * @return the inferred observations
		 * @throws DialException
		 */
		private MultivariateTable getObservations (DialogueState state) throws DialException {
			Set<String> predictionNodes = new HashSet<String>();
			for (String nodeId: state.getChanceNodeIds()) {
				if (nodeId.contains("^p")) {
					predictionNodes.add(nodeId);
				}
			}
			// intermediary observations
			for (String nodeId: new HashSet<String>(predictionNodes)) {
				if (state.getChanceNode(nodeId).hasDescendant(predictionNodes)) {
					predictionNodes.remove(nodeId);
				}
			}

			MultivariateTable modified = new MultivariateTable();
			if (!predictionNodes.isEmpty()) {
				MultivariateDistribution observations = state.queryProb(predictionNodes);

				for (Assignment a : observations.getValues()) {
					Assignment newA = new Assignment();
					for (String var : a.getVariables()) {
						newA.addPair(var.replace("^p", ""), a.getValue(var));
					}
					modified.addRow(newA, observations.getProb(a));
				}
			}
			return modified;
		}

	}


}

