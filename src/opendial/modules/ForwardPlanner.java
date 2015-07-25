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

package opendial.modules;

import java.util.logging.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.UtilityTable;
import opendial.datastructs.Assignment;
import opendial.domains.Model;

/**
 * Online forward planner for OpenDial. The planner constructs a lookahead tree (with
 * a depth corresponding to the planning horizon) that explores possible actions and
 * their expected consequences on the future dialogue state. The final utility values
 * for each action is then estimated, and the action with highest utility is
 * selected.
 * 
 * <p>
 * The planner is an anytime process. It can be interrupted at any time and yield a
 * result. The quality of the utility estimates is of course improving over time.
 * 
 * <p>
 * The planning algorithm is described in pages 121-123 of Pierre Lison's PhD thesis
 * [http://folk.uio.no/plison/pdfs/thesis/thesis-plison2013.pdf]
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class ForwardPlanner implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** Maximum number of actions to consider at each planning step */
	public static int NB_BEST_ACTIONS = 100;

	/**
	 * Maximum number of alternative observations to consider at each planning step
	 */
	public static int NB_BEST_OBSERVATIONS = 3;

	/** Minimum probability for the generated observations */
	public static double MIN_OBSERVATION_PROB = 0.1;

	DialogueSystem system;

	/** Current planning process (if active) */
	PlannerProcess currentProcess;

	boolean paused = false;

	// scheduled thread pool to terminate planning once the time limit is
	// reached
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
			log.fine("trying to terminate the process?");
			currentProcess.isTerminated = true;
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void start() {
	}

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

		// disallows action selection while the user is still talking
		if (system.getFloor().equals("user")) {
			state.removeNodes(state.getActionNodeIds());
			state.removeNodes(state.getUtilityNodeIds());
		}

		if (!paused && !state.getActionNodeIds().isEmpty()) {
			currentProcess = new PlannerProcess(state);
		}
	}

	/**
	 * Planner process, which can be terminated before the end of the horizon
	 * 
	 * @author Pierre Lison (plison@ifi.uio.no)
	 */
	public class PlannerProcess {

		DialogueState initState;

		boolean isTerminated = false;

		/**
		 * Creates the planning process. Timeout is set to twice the maximum sampling
		 * time. Then, runs the planner until the horizon has been reached, or the
		 * planner has run out of time. Adds the best action to the dialogue state.
		 * 
		 * @param initState initial dialogue state.
		 */
		public PlannerProcess(DialogueState initState) {
			this.initState = initState;
			Settings settings = system.getSettings();

			// setting the timeout for the planning
			long timeout = Settings.maxSamplingTime * 2;
			// if the speech stream is not finished, only allow fast, reactive
			// responses
			timeout = (initState.hasChanceNode(settings.userSpeech)) ? timeout / 5
					: timeout;
			service.schedule(() -> isTerminated = true, timeout,
					TimeUnit.MILLISECONDS);

			try {
				// step 1: extract the Q-values
				UtilityTable evalActions = getQValues(initState, settings.horizon);

				// step 2: find the action with highest utility
				Assignment bestAction = evalActions.getBest().getKey();
				if (evalActions.getUtil(bestAction) < 0.001) {
					bestAction = Assignment.createDefault(bestAction.getVariables());
				}

				// step 3: remove the action and utility nodes
				initState.removeNodes(initState.getUtilityNodeIds());
				Set<String> actionVars =
						new HashSet<String>(initState.getActionNodeIds());
				initState.removeNodes(actionVars);

				// step 4: add the selection action to the dialogue state
				initState.addToState(bestAction.removePrimes());
				// log.fine("BEST ACTION: " + bestAction);
				isTerminated = true;
			}
			catch (RuntimeException e) {
				log.warning("could not perform planning, aborting action selection: "
						+ e);
				e.printStackTrace();
			}
		}

		/**
		 * Returns the Q-values for the dialogue state, assuming a particular
		 * horizon.
		 * 
		 * @param state the dialogue state
		 * @param horizon the planning horizon
		 * @return the estimated utility table for the Q-values @
		 */
		private UtilityTable getQValues(DialogueState state, int horizon) {
			Set<String> actionNodes = state.getActionNodeIds();

			if (actionNodes.isEmpty()) {
				return new UtilityTable();
			}
			UtilityTable rewards = state.queryUtil(actionNodes);
			if (horizon == 1) {
				return rewards;
			}

			UtilityTable qValues = new UtilityTable();
			double discount = system.getSettings().discountFactor;

			for (Assignment action : rewards.getRows()) {
				double reward = rewards.getUtil(action);
				qValues.setUtil(action, reward);

				if (horizon > 1 && !isTerminated && !paused
						&& hasTransition(action)) {

					DialogueState copy = state.copy();
					copy.addToState(action.removePrimes());
					updateState(copy);

					if (!action.isDefault()) {
						double expected =
								discount * getExpectedValue(copy, horizon - 1);
						qValues.setUtil(action, qValues.getUtil(action) + expected);
					}
				}
			}
			return qValues;
		}

		/**
		 * Adds a particular content to the dialogue state
		 * 
		 * @param state the dialogue state
		 * @param newContent the content to add be performed
		 */
		private void updateState(DialogueState state) {

			while (!state.getNewVariables().isEmpty()) {
				Set<String> toProcess = state.getNewVariables();
				state.reduce();
				for (Model model : system.getDomain().getModels()) {
					if (model.isTriggered(state, toProcess)) {
						boolean change = model.trigger(state);
						if (change && model.isBlocking()) {
							break;
						}
					}
				}
			}
		}

		/**
		 * Returns true if the dialogue domain specifies a transition model for the
		 * particular action assignment.
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
		 * Estimates the expected value (V) of the dialogue state in the current
		 * planning horizon.
		 * 
		 * @param state the dialogue state
		 * @param horizon the planning horizon
		 * @return the expected value. @
		 */
		private double getExpectedValue(DialogueState state, int horizon) {

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
		 * Returns the possible observations that are expected to be perceived from
		 * the dialogue state
		 * 
		 * @param state the dialogue state from which to extract observations
		 * @return the inferred observations @
		 */
		private MultivariateTable getObservations(DialogueState state) {
			Set<String> predictionNodes = new HashSet<String>();
			for (String nodeId : state.getChanceNodeIds()) {
				if (nodeId.contains("^p")) {
					predictionNodes.add(nodeId);
				}
			}
			// intermediary observations
			for (String nodeId : new HashSet<String>(predictionNodes)) {
				if (state.getChanceNode(nodeId).hasDescendant(predictionNodes)) {
					predictionNodes.remove(nodeId);
				}
			}

			MultivariateTable.Builder builder = new MultivariateTable.Builder();

			if (!predictionNodes.isEmpty()) {
				MultivariateDistribution observations =
						state.queryProb(predictionNodes);

				for (Assignment a : observations.getValues()) {
					Assignment newA = new Assignment();
					for (String var : a.getVariables()) {
						newA.addPair(var.replace("^p", ""), a.getValue(var));
					}
					builder.addRow(newA, observations.getProb(a));
				}
			}
			return builder.build();
		}

	}

}
