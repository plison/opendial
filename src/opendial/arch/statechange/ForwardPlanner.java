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

package opendial.arch.statechange;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.arch.ConfigurationSettings;
import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.UtilQuery;
import opendial.utils.CombinatoricsUtils;

public class ForwardPlanner extends Thread {
	
	// logger
	public static Logger log = new Logger("ForwardPlanner", Logger.Level.DEBUG);

	DialogueState state;
	
	public ForwardPlanner(DialogueState state) {
		this.state = state;
	}
	
	@Override
	public void run() {
		log.debug("planner is running...");
		
		Set<String> actionNodes = state.getNetwork().getActionNodeIds();
		
		try {
		InferenceAlgorithm inference = ConfigurationSettings.getInstance().
				getInferenceAlgorithm().newInstance();
		
		UtilQuery query = new UtilQuery(state, actionNodes);
		UtilityTable distrib = inference.queryUtility(query);
		
		double highestUtility = 0;
		Assignment bestAction = getDefaultAssign();
		for (Assignment a : distrib.getTable().keySet()) {
			double utility = distrib.getUtility(a);
			if (utility > highestUtility) {
				bestAction = a;
				highestUtility = utility;
			}
		}
		
		state.getNetwork().removeNodes(bestAction.getVariables());
		state.getNetwork().removeNodes(state.getNetwork().getUtilityNodeIds());
		for (String actionVar: bestAction.getVariables()) {
			ChanceNode newNode = new ChanceNode(actionVar);
			newNode.addProb(bestAction.getValue(actionVar), 1.0);
			state.getNetwork().addNode(newNode);
		}
		log.debug("planning is finished, selected action: " + bestAction + "(Q=" + highestUtility+")");
		}
		catch (Exception e) {
			log.warning("cannot select optimal action: " + e);
		}
		state.getController().setAsCompleted(this);		
	}

	
	private Assignment getDefaultAssign() {
		Assignment a = new Assignment();
		for (String actionNode: state.getNetwork().getActionNodeIds()) {
			a.addPair(actionNode, ValueFactory.none());
		}
		return a;
	}
}

