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
import opendial.arch.timing.AnytimeProcess;
import opendial.arch.timing.StopProcessTask;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Estimate;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
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
 
	public static long MAX_DELAY = 10000;

	DialogueState state;

	boolean isTerminated = false;

	InteractionModel interactionModel;
	
	Map<List<Assignment>, Estimate> sequences;


	public ForwardPlanner(DialogueState state) {
		this.state = state;
		interactionModel = new InteractionModel();
		sequences = new HashMap<List<Assignment>, Estimate>();
	}
	

	@Override
	public void run() {
		isTerminated = false;
	//	log.debug("planner is running... (" + getNbTrajectories() + " trajectories to collect)");
		Timer timer = new Timer();
		timer.schedule(new StopProcessTask(this, MAX_DELAY), MAX_DELAY);
		
		for (int i = 0 ; i < getNbTrajectories() && !isTerminated ; i++) {	
			try {
				Trajectory trajectory = new Trajectory(this);
		//		log.debug("trajectory " + trajectory + " finished");
				List<Assignment> actionSequence = trajectory.getActionSequence();
				double utility = trajectory.getCumulativeUtility();
				if (sequences.containsKey(actionSequence)) {
					sequences.get(actionSequence).updateEstimate(utility);
				}
				else {
					sequences.put(actionSequence, new Estimate(utility));
				}
			}
			catch (DialException e) {
				log.warning("could not generate trajectory: " + e);
			}
		}
		timer.cancel();
		List<Assignment> optimalSequence = findOptimalSequence();
		log.debug("optimal sequence is : " + optimalSequence + " with Q=" + sequences.get(optimalSequence).getValue());
		
		sequences.clear();

		if (!optimalSequence.isEmpty()) {
			Assignment bestAction = optimalSequence.get(0);
			addAction(bestAction);
		}
		else {
			log.warning("optimal sequence has no action");
			addAction(new Assignment());
		}
		
	}
	
	
	
	private int getNbTrajectories() {
		if (Settings.planningHorizon <=1) {
			return 1;
		}
		else {		
			return 30* Settings.planningHorizon;
		}
	}
	
	
	private void addAction(Assignment bestAction) {
		state.getNetwork().removeNodes(state.getNetwork().getActionNodeIds());
		this.state.getNetwork().removeNodes(this.state.getNetwork().getUtilityNodeIds());
		try {
			state.addContent(bestAction, "planner");
		}
		catch (DialException e) {
			log.warning("cannot add selected action to state");
		}
	}
	
	
	private List<Assignment> findOptimalSequence() {
		
		List<Assignment> bestSequence = new LinkedList<Assignment>();
		double maxUtility = - Double.MAX_VALUE;
		for (List<Assignment> sequence : sequences.keySet()) {
			double estimate = sequences.get(sequence).getValue();
			if (estimate > maxUtility) {
				maxUtility = estimate;
				bestSequence = sequence;
			}
	//		log.debug("Q("+sequence + ")=" + estimate + " with " + sequences.get(sequence).getNbCounts() + " counts");
		}
		return bestSequence;
	}
	
	
	public Map<Assignment,Estimate> getEstimates(List<Assignment> partialSequence) {
		
		Map<Assignment, Estimate> relevantSequences = 
				new HashMap<Assignment, Estimate>();
		
		for (List<Assignment> sequence : sequences.keySet()) {
			if (sequence.containsAll(partialSequence) && sequence.size() > partialSequence.size()) {
				relevantSequences.put(sequence.get(partialSequence.size()), sequences.get(sequence));
			}
		}
		return relevantSequences;
	}
	
	

	@Override
	public boolean isTerminated() {
		return isTerminated;
	}

	@Override
	public void terminate() {
		isTerminated = true;
	}

	
	
	public boolean isPlanningNeeded() {
		return (Settings.activatePlanner 
				&& !state.getNetwork().getActionNodeIds().isEmpty() && !state.isFictive());
	}


	public String toString() {
		return "Forward planner (give more useful information here)";
	}

	protected InteractionModel getInteractionModel() {
		return interactionModel;
	}

	public DialogueState getState() {
		return state;
	}
	
	public int getPlanningHorizon() {
		return Settings.planningHorizon;
	}

}

