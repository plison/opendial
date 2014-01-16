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


import java.util.HashSet;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.datastructs.Assignment;
import opendial.domains.Model;
import opendial.state.DialogueState;


public class ForwardPlanner implements Module {

	// logger
	public static Logger log = new Logger("ForwardPlanner", Logger.Level.DEBUG);

	public static long MAX_DELAY = 2000;

	public static int NB_BEST_ACTIONS = 100;
	public static int NB_BEST_OBSERVATIONS = 3;
	public static double MIN_OBSERVATION_PROB = 0.1;

	DialogueSystem system;
	boolean paused = false;

	public void pause(boolean shouldBePaused) {	
		paused = shouldBePaused;
	}

	public void start(DialogueSystem system) throws DialException {
		this.system = system;
	}


	public void trigger() {
		if (!paused & system.getSettings().enablePlan 
				&& !system.getState().getActionNodeIds().isEmpty()) {
			try {
				PlannerProcess process = new PlannerProcess();
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

		
		public PlannerProcess() {
			super(MAX_DELAY);
		}
		
		boolean isTerminated = false;

		/**
		 * Runs the planner until the horizon has been reached, or the planner has run out
		 * of time.  Adds the best action to the dialogue state.
		 */
		public void run() {
			try {
				UtilityTable evalActions =getQValues(system);
				log.debug("Q-values: " + evalActions);
				Assignment bestAction =  evalActions.getBest().getKey(); 

				if (evalActions.getUtil(bestAction) < 0.001) {
					bestAction = Assignment.createDefault(bestAction.getVariables());
				}
				system.getState().addToState(new CategoricalTable(bestAction.removePrimes()));
			}
			catch (Exception e) {
				log.warning("could not perform planning, aborting action selection: " + e);
				e.printStackTrace();
			}
		}



		private UtilityTable getQValues (DialogueSystem future) throws DialException {

			Set<String> actionNodes = future.getState().getActionNodeIds();
			if (actionNodes.isEmpty()) {
				return new UtilityTable();
			}
			UtilityTable rewards = future.getState().queryUtil(actionNodes);

			if (future.getSettings().horizon ==1) {
				return rewards;
			}

			UtilityTable qValues = new UtilityTable();

			for (Assignment action : rewards.getRows()) {
				double reward = rewards.getUtil(action);
				qValues.setUtil(action, reward);

				if (future.getSettings().horizon > 1 && !isTerminated && !paused && hasTransition(action)) {
					DialogueSystem copy = future.replicate();
					copy.getSettings().horizon = future.getSettings().horizon - 1;				
					//		copy.getState().reduce(future.getSettings().enablePruning);
					copy.addContent(action.removePrimes());

					if (!action.isDefault()) {
						double expected = future.getSettings().discountFactor * getExpectedValue(copy);
						qValues.setUtil(action, qValues.getUtil(action) + expected);
					}	
				}
			}
			return qValues;
		}


		private boolean hasTransition(Assignment action) {
			for (Model m : system.getDomain().getModels()) {
				if (m.isTriggered(action.getVariables())) {
					return true;
				}
			}
			return false;
		}


		private double getExpectedValue(DialogueSystem future) throws DialException {

			CategoricalTable observations = getObservations(future.getState());
			CategoricalTable nbestObs = observations.getNBest(NB_BEST_OBSERVATIONS);
			double expectedValue = 0.0;
			for (Assignment obs : nbestObs.getRows()) {
				double obsProb = nbestObs.getProb(obs);
				if (obsProb > MIN_OBSERVATION_PROB) {
					DialogueSystem copy = future.replicate();
					copy.addContent(new CategoricalTable(obs));

					UtilityTable qValues = getQValues(copy);

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

		public boolean isTerminated() {
			return isTerminated;
		}

	}
}

