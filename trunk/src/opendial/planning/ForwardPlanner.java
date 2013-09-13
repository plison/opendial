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

package opendial.planning;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings.PlanSettings;
import opendial.arch.timing.AnytimeProcess;
import opendial.arch.timing.StopProcessTask;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.datastructs.Estimate;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.DerivedActionNode;
import opendial.bn.nodes.UtilityRuleNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.Model;
import opendial.domains.rules.PredictionRule;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.modules.SynchronousModule;
import opendial.state.DialogueState;
import opendial.utils.CombinatoricsUtils;

public class ForwardPlanner implements AnytimeProcess {

	// logger
	public static Logger log = new Logger("ForwardPlanner", Logger.Level.DEBUG);

	public static long MAX_DELAY = 200000;

	public static int NB_BEST_ACTIONS = 100;
	public static int NB_BEST_OBSERVATIONS = 3;
	public static double MIN_OBSERVATION_PROB = 0.1;

	protected DialogueState currentState;

	protected boolean isTerminated = false;


	public ForwardPlanner(DialogueState state) {
		this.currentState = state;
	}


	@Override
	public void run() {
		isTerminated = false;
		Timer timer = new Timer();
		timer.schedule(new StopProcessTask(this, MAX_DELAY), MAX_DELAY);
		
		UtilityTable evalActions = evaluateActions();
		Assignment bestAction = evalActions.getBest().getKey();
		
		if (evalActions.getUtil(bestAction) < 0.001) {
			bestAction = Assignment.createDefault(bestAction.getVariables());
		}
		
		log.debug("executing action " + bestAction);
		recordAction(currentState, bestAction);	
		
		timer.cancel();	

	}
	
	protected UtilityTable evaluateActions() {
		try {
			
			Set<String> actionNodes = currentState.getNetwork().getActionNodeIds();
			int horizon = Settings.getInstance().planning.getHorizon(actionNodes);
			double discountFactor = Settings.getInstance().planning.getDiscountFactor(actionNodes);
			
		//	log.debug("planner is running for action nodes: " + actionNodes + 
		//			"with horizon " + horizon + " and discount factor " + discountFactor);
			long initTime = System.currentTimeMillis();

			UtilityTable qValues = getQValues(currentState, horizon, discountFactor);

			log.debug("rows in Q-values: " + qValues.getRows());
		//	log.debug("planning time: " + (System.currentTimeMillis() - initTime));
			log.debug("Q values: " + qValues);
	//		Map.Entry<Assignment, Double> bestAction = qValues.getBest();

			return qValues;
		}
		catch (Exception e) {
			log.warning("could not perform planning, aborting action selection: " + e);
		}
		return new UtilityTable();
	}



	private UtilityTable getQValues (DialogueState state, int horizon, double discountFactor) throws DialException {

		UtilityTable rewards = getRewardValues(state);
		if (horizon ==1) {
			return rewards;
		}
		
		UtilityTable qValues = new UtilityTable();
		for (Assignment action : rewards.getRows()) {
			double reward = rewards.getUtil(action);
			qValues.setUtil(action, reward);

			if (horizon > 1 && !isTerminated) {
				DialogueState copy = state.copy();
				copy.setAsFictive(true);
				copy.activateDecisions(false);
				recordAction(copy, action);
				copy.activateDecisions(true);

				double expected = 0;
				if (!action.isDefault()) {
					expected = discountFactor * getExpectedValue(copy, horizon-1, discountFactor);
				}
				qValues.setUtil(action, qValues.getUtil(action) + expected);
			}
		}
		return qValues;
	}



	private UtilityTable getRewardValues(DialogueState state) throws DialException {

		Set<String> actionNodes = state.getNetwork().getActionNodeIds();

		try {
			InferenceAlgorithm inference = Settings.getInstance().inferenceAlgorithm.newInstance();
			UtilQuery query = new UtilQuery(state, actionNodes);

			UtilityTable distrib = inference.queryUtil(query);

			return distrib.getNBest(NB_BEST_ACTIONS);
		}
		catch (Exception e) {
			throw new DialException("could not sample action: " + e);
		}		
	}


	private double getExpectedValue(DialogueState state, int horizon, double discountFactor) throws DialException {

		SimpleTable observations = getObservations(state);
		SimpleTable nbestObs = observations.getNBest(NB_BEST_OBSERVATIONS);
		double expectedValue = 0.0;
		for (Assignment obs : nbestObs.getRows()) {
			double obsProb = nbestObs.getProb(obs);
			if (obsProb > MIN_OBSERVATION_PROB) {
				DialogueState copy = state.copy();
				copy.setAsFictive(true);

				copy.addContent(obs, "planner");

				UtilityTable qValues = getQValues(copy, horizon, discountFactor);
				Assignment bestAction = qValues.getBest().getKey();
				double afterObs = qValues.getUtil(bestAction);
				expectedValue += obsProb * afterObs;
			}
		}	
		return expectedValue;
	}



	protected void recordAction(DialogueState state, Assignment action) {
		state.getNetwork().removeNodes(state.getNetwork().getActionNodeIds());
		state.getNetwork().removeNodes(state.getNetwork().getUtilityNodeIds());
		
		try {
			if (!action.isDefault()) {
				state.addContent(new Assignment(action.removeSpecifiers()), "planner");
			}
		}
		catch (DialException e) { log.warning("could not insert new action: " + e); }
	}


	public SimpleTable getObservations (DialogueState state) throws DialException {
		Set<String> predictionNodes = new HashSet<String>();
		for (String nodeId: state.getNetwork().getChanceNodeIds()) {
			if (nodeId.contains("^p")) {
				predictionNodes.add(nodeId);
			}
		}
		// intermediary observations
		for (String nodeId: new HashSet<String>(predictionNodes)) {
			if (state.getNetwork().getChanceNode(nodeId).hasDescendant(predictionNodes)) {
				predictionNodes.remove(nodeId);
			}
		}

		ProbDistribution observations = state.getContent(predictionNodes, true);
		SimpleTable table = observations.toDiscrete().getProbTable(new Assignment());

		SimpleTable modified = new SimpleTable();
		for (Assignment a : table.getRows()) {
			Assignment newA = new Assignment();
			for (String var : a.getVariables()) {
				newA.addPair(var.replace("^p", ""), a.getValue(var));
			}
			modified.addRow(newA, table.getProb(a));
		}
		return modified;
	}


	@Override
	public void terminate() {
		isTerminated = true;
	}



	public boolean isPlanningNeeded() {
		return (Settings.getInstance().activatePlanner 
				&& !currentState.getNetwork().getActionNodeIds().isEmpty() && !currentState.isFictive());
	}


	public String toString() {
		return "Forward planner (give more useful information here)";
	}
	
	

}

