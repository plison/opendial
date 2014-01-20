// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.modules;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.datastructs.Assignment;
import opendial.domains.Model;
import opendial.state.DialogueState;


public class ForwardPlanner implements Module {

	// logger
	public static Logger log = new Logger("ForwardPlanner", Logger.Level.DEBUG);


	public static int NB_BEST_ACTIONS = 100;
	public static int NB_BEST_OBSERVATIONS = 3;
	public static double MIN_OBSERVATION_PROB = 0.1;

	DialogueSystem system;

	boolean paused = false;

	public ForwardPlanner(DialogueSystem system) {
		this.system = system;
	}
	
	@Override
	public void pause(boolean shouldBePaused) {	
		paused = shouldBePaused;
	}

	@Override
	public void start()  {	}


	@Override
	public boolean isRunning() {
		return !paused;
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!paused && !state.getActionNodeIds().isEmpty()) {
			try {
				PlannerProcess process = new PlannerProcess(state);
				process.start();
				process.join();
			} 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	} 


	/**
	 * Planner process, which can be terminated before the end of the horizon
	 * @author  Pierre Lison (plison@ifi.uio.no)
	 * @version $Date::                      $
	 */
	public class PlannerProcess extends AnytimeProcess {

		DialogueState initState;
		
		/**
		 * Creates the planning process.  Timeout is set to twice the maximum sampling time.
		 */
		public PlannerProcess(DialogueState initState) {
			super(Settings.maxSamplingTime * 2);
			this.initState = initState;
		}
		
		boolean isTerminated = false;

		/**
		 * Runs the planner until the horizon has been reached, or the planner has run out
		 * of time.  Adds the best action to the dialogue state.
		 */
		@Override
		public void run() {
			try {
				UtilityTable evalActions =getQValues(initState, system.getSettings().horizon);
		//		ForwardPlanner.log.debug("Q-values: " + evalActions);
				Assignment bestAction =  evalActions.getBest().getKey(); 

				if (evalActions.getUtil(bestAction) < 0.001) {
					bestAction = Assignment.createDefault(bestAction.getVariables());
				}
				initState.addToState(new CategoricalTable(bestAction.removePrimes()));
				isTerminated = true;
			}
			catch (Exception e) {
				log.warning("could not perform planning, aborting action selection: " + e);
				e.printStackTrace();
			}
		}



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
					addContent(copy, new CategoricalTable(action.removePrimes()));

					if (!action.isDefault()) {
						double expected = system.getSettings().discountFactor * getExpectedValue(copy, horizon - 1);
						qValues.setUtil(action, qValues.getUtil(action) + expected);
					}	
				}
			}
			return qValues;
		}

		
		private void addContent(DialogueState state, CategoricalTable newContent) 
				throws DialException {
			
			state.addToState(newContent);
			
			while (!state.getNewVariables().isEmpty()) {
				Set<String> toProcess = state.getNewVariables();
				state.reduce();	
				for (Model model : system.getDomain().getModels()) {
						model.trigger(state, toProcess);
				}
			}
		}
		

		private boolean hasTransition(Assignment action) {
			for (Model m : system.getDomain().getModels()) {
				if (m.isTriggered(action.removePrimes().getVariables())) {
					return true;
				}
			}
			return false;
		}


		private double getExpectedValue(DialogueState state, int horizon) throws DialException {

			CategoricalTable observations = getObservations(state);
			CategoricalTable nbestObs = observations.getNBest(NB_BEST_OBSERVATIONS);
			double expectedValue = 0.0;
			for (Assignment obs : nbestObs.getRows()) {
				double obsProb = nbestObs.getProb(obs);
				if (obsProb > MIN_OBSERVATION_PROB) {
					DialogueState copy = state.copy();
					addContent(copy, new CategoricalTable(obs));

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




		private CategoricalTable getObservations (DialogueState state) throws DialException {
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

			CategoricalTable modified = new CategoricalTable();
			if (!predictionNodes.isEmpty()) {
				CategoricalTable observations = state.queryProb(predictionNodes).toDiscrete();

				for (Assignment a : observations.getRows()) {
					Assignment newA = new Assignment();
					for (String var : a.getVariables()) {
						newA.addPair(var.replace("^p", ""), a.getValue(var));
					}
					modified.addRow(newA, observations.getProb(a));
				}
			}
			return modified;
		}


		@Override
		public void terminate() {
			isTerminated = true;
		}

		@Override
		public boolean isTerminated() {
			return isTerminated;
		}

	}


}

