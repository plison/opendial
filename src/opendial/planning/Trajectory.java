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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Settings;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Estimate;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.values.ValueFactory;
import opendial.gui.GUIFrame;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.UtilQuery;
import opendial.state.DialogueState;

public class Trajectory {

	// logger
	public static Logger log = new Logger("Trajectory", Logger.Level.DEBUG);

	public static int NB_BEST_ACTIONS = 5;
	
	public static double EXPLORATION_LEVEL = 0.5;

	List<TrajectoryPoint> history;

	ForwardPlanner planner;
	
	DialogueState state;

	int currentHorizon;
	
	public static long actionSamplingTime = 0;
	public static long actionRecordingTime = 0;
	public static long observationSamplingTime = 0;
	public static long observationRecordingTime = 0;



	public Trajectory(ForwardPlanner planner) throws DialException {

		history = new ArrayList<TrajectoryPoint>();
		this.state = planner.getState();
		this.currentHorizon = planner.getPlanningHorizon();
		this.planner = planner;

		if (currentHorizon == 1) {
			ValuedAction ValuedAction = sampleSystemAction();
			history.add(ValuedAction);
		}
		else if (currentHorizon > 1){
			generateFullTrajectory();
		}
		else {
			throw new DialException("horizon must be > 0");
		}

	}


	private void generateFullTrajectory() throws DialException {

	//	log.debug("starting trajectory...");

		state = state.copy();
		state.setAsFictive(true);
		InteractionModel interactionModel = planner.getInteractionModel();

		ValuedAction ValuedAction = sampleSystemAction();
		recordAction(ValuedAction);
		currentHorizon--;

		while (currentHorizon > 0) {
			
			long t1 = System.nanoTime();
			SampledObservation observation = interactionModel.sampleObservation(state);
			observationSamplingTime += System.nanoTime() - t1;
			if (!observation.isNone()) {
			recordObservation(observation);
	
				ValuedAction = sampleSystemAction();
				recordAction(ValuedAction);
				currentHorizon--;
		}
			else {
				currentHorizon = 0;
			} 
		}

	//	log.debug("finished trajectory: " + history);

	}


	public String toString() {
		return history.toString();
	}
	

	public List<Assignment> getActionSequence() {
		List<Assignment> sequence = new ArrayList<Assignment>();
		for (TrajectoryPoint point : history) {
			if (point instanceof ValuedAction) {
				sequence.add(((ValuedAction)point).getAssignment());
			}
		}
		return sequence;
	}

	public double getCumulativeUtility() {
		int nbSteps = 0;
		double utility = 0.0;
		double discountFactor = Settings.discountFactor;
		for (TrajectoryPoint point : history) {
			if (point instanceof ValuedAction) {
				utility += Math.pow(discountFactor, nbSteps) * ((ValuedAction)point).getReward();
				nbSteps++;
			}
		}
		return utility;
	}


	private ValuedAction sampleSystemAction() throws DialException {

		long t1 = System.nanoTime();
		Set<String> actionNodes = state.getNetwork().getActionNodeIds();

		try {
			InferenceAlgorithm inference = Settings.inferenceAlgorithm.newInstance();

			UtilQuery query = new UtilQuery(state, actionNodes);
			query.setAsLightweight(true);
	
			UtilityTable distrib = inference.queryUtility(query);
			
		//	log.debug("utility distrib: "+ distrib);
			
			int nbToSelect = (currentHorizon > 1)? NB_BEST_ACTIONS: 1;
			List<ValuedAction> actions = distrib.getFilteredTable(nbToSelect);

			if (!actions.isEmpty()) {
				ValuedAction ValuedAction = sampleSystemAction(actions);
				ValuedAction.removeSpecifiers();
				actionSamplingTime += System.nanoTime() - t1;
				return ValuedAction;
			}
			else {
				throw new DialException("set of possible actions is empty");
			}
		}
		catch (Exception e) {
			throw new DialException("could not sample action: " + e);
		}
	} 
	
	
	private ValuedAction sampleSystemAction(List<ValuedAction> possibleActions) throws DialException {
		
		Map<Assignment,Estimate> sequences = planner.getEstimates(getActionSequence());
		double min = (sequences.isEmpty())? 0: Double.MAX_VALUE;
		double max = (sequences.isEmpty())? 0: -Double.MAX_VALUE;
		for (Estimate value : sequences.values()) {
			if (value.getValue() < min) {
				min = value.getValue();
			}
			if (value.getValue() > max) {
				max = value.getValue();
			}
		}
		
		Map<ValuedAction,Double> table = new HashMap<ValuedAction,Double>();
		for (ValuedAction action : possibleActions) {
			Assignment formattedAction = action.getAssignment().removeSpecifiers();
			double weight = (min == max)? 1: EXPLORATION_LEVEL*(max-min);
			if (sequences.containsKey(formattedAction)) {
				weight += (sequences.get(formattedAction).getValue() - min);
			}
			table.put(action, weight);
		}
		Intervals<ValuedAction> intervals = new Intervals<ValuedAction>(table);
	//	log.debug("intervals: " + intervals);
		return intervals.sample();
	}
	 
	
	private void recordAction(ValuedAction action) throws DialException {
	//	log.debug("recording " + action);
		long t1 = System.nanoTime();
		history.add(action);
		state.getNetwork().removeNodes(state.getNetwork().getActionNodeIds());
		state.getNetwork().removeNodes(state.getNetwork().getUtilityNodeIds());
		state.activateDecisions(false);
		state.addContent(action.getDistribution(), "planner-a");
		state.activateDecisions(true);
		actionRecordingTime += System.nanoTime() - t1;
	}
	
	
	private void recordObservation(SampledObservation observation) throws DialException {
	//	log.debug("recording " + observation);
		long t1 = System.nanoTime();
		history.add(observation);
		state.addContent(observation.getDistribution(), "planner-o");
		observationRecordingTime += System.nanoTime() - t1;
	}


}

