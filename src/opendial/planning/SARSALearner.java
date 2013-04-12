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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.timing.StopProcessTask;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.FuzzyDistribution;
import opendial.bn.distribs.discrete.DeterministicDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.functions.AdditionFunction;
import opendial.bn.distribs.discrete.functions.UtilDistributionWrapper;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.DoubleVal;
import opendial.gui.GUIFrame;
import opendial.state.DialogueState;

public class SARSALearner extends ForwardPlanner {

	// logger
	public static Logger log = new Logger("SARSALearner", Logger.Level.DEBUG);
	
	public static double EPSILON = 0.2;
	
	public static DialogueState lastDS;
	
	public SARSALearner(DialogueState state) {
		super(state);
	}
	
	@Override
	public void run() {
		isTerminated = false;
		Timer timer = new Timer();
		timer.schedule(new StopProcessTask(this, MAX_DELAY), MAX_DELAY);

		UtilityTable evalActions = evaluateActions();

		Map.Entry<Assignment, Double> bestAction = selectAction(evalActions);
		log.debug("selected action: " + bestAction.getKey());
		if (lastDS != null) {
			updateParameters(bestAction);
		}
		
		copyState(bestAction.getKey());
		
		if (!bestAction.getKey().isEmpty()) {
			recordAction(currentState, bestAction.getKey());
		}
		timer.cancel();	

	}
	
	
	private Map.Entry<Assignment, Double> selectAction(UtilityTable evalActions) {
		
		if ((new Random()).nextDouble() < EPSILON) {
			log.debug("selecting sub-optimal action");
			Map<Assignment,Double> table = evalActions.getNBest(3).getTable();
			int selection = (new Random()).nextInt(table.size());
			return new ArrayList<Map.Entry<Assignment,Double>>(table.entrySet()).get(selection);
		}
		return evalActions.getBest();
	}
	
	
	
	private void updateParameters(Map.Entry<Assignment, Double> bestAction) {
		double expectedValue = 0.0;
		try {
			log.debug("estimated Q value for previous action: " + lastDS.getContent("q", true));
			
			if (currentState.getNetwork().hasChanceNode("r")) {
			DoubleVal value = (DoubleVal)currentState.getContent("r", true).sample(new Assignment()).getValue("r");
			expectedValue += value.getDouble();
		//	log.debug("reward: " + expectedValue);
		}
		else {
			log.debug("no reward!" + currentState.getNetwork().getNodeIds());
		}
		}
		catch (DialException e) {
			log.warning("cannot extract last reward");
		}
		expectedValue += Settings.getInstance().planning.discountFactor * bestAction.getValue();
		log.debug("updated Q-value for previous action: " + expectedValue);
		lastDS.addEvidence(new Assignment("q", expectedValue));
		lastDS.triggerUpdates();
		
		for (String var : lastDS.getNetwork().getNodeIds()) {
			if (lastDS.isParameter(var)) {
				ProbDistribution newDistrib = lastDS.getNetwork().getChanceNode(var).getDistrib();
				try {
				currentState.getNetwork().getChanceNode(var).setDistrib(newDistrib);
				}
				catch (DialException e) {
					log.warning("could not update parameter: " + e);
				}
			}
		}
	}
	
	private void copyState(Assignment bestAction) {
		
		try {
			lastDS = currentState.copy();
			
			// change action node into a chance node with a single action
			for (String var : bestAction.getVariables()) {
				ActionNode aNode = lastDS.getNetwork().getActionNode(var);
				ChanceNode newNode = new ChanceNode(var);
				newNode.addProb(new Assignment(), bestAction.getValue(var), 1.0);
				Set<String> outputNodes = new HashSet<String>(aNode.getOutputNodesIds());
				lastDS.getNetwork().removeNode(var);
				for (String oNodeId : outputNodes) {
					BNode oNode = lastDS.getNetwork().getNode(oNodeId);
					oNode.addInputNode(newNode);
				}
				lastDS.getNetwork().addNode(newNode);				
			}
			
			if (lastDS.getNetwork().hasNode("q")) {
			lastDS.getNetwork().removeNode("q");
			}
			ChanceNode totalNode = new ChanceNode("q");
			totalNode.setDistrib(new FuzzyDistribution("q", new AdditionFunction(), 10));
			lastDS.getNetwork().addNode(totalNode);
			
			for (String utilVar : new HashSet<String>(lastDS.getNetwork().getUtilityNodeIds())) {
				UtilityNode utilNode = lastDS.getNetwork().getUtilityNode(utilVar);
				
				Set<String> inputNodes = new HashSet<String>(utilNode.getInputNodeIds());
				DeterministicDistribution newDistrib = new DeterministicDistribution(utilVar, 
						new UtilDistributionWrapper(utilNode.getDistribution()));
				lastDS.getNetwork().removeNode(utilVar);
				ChanceNode newNode = new ChanceNode(utilVar);
				newNode.setDistrib(newDistrib);
				for (String iNodeId : inputNodes) {
					BNode iNode = lastDS.getNetwork().getNode(iNodeId);
					newNode.addInputNode(iNode);
				}
				lastDS.getNetwork().addNode(newNode);
				totalNode.addInputNode(newNode);
			}
			
		//	GUIFrame.getInstance().recordState(lastDS, "lastSARSA");
			}
			catch (DialException e) {
				log.warning("cannot copy current dialogue state");
			}
		
	}
	
}

