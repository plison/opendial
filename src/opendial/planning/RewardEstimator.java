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
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.UnivariateDensityFunction;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.nodes.UtilityRuleNode;
import opendial.inference.ImportanceSampling;
import opendial.inference.queries.UtilQuery;
import opendial.state.DialogueState;

public class RewardEstimator {

	// logger
	public static Logger log = new Logger("RewardEstimator",
			Logger.Level.DEBUG);


	public static double GAUSSIAN_VARIANCE = 3.0;

	public static void addRewardEstimate(DialogueState state, Assignment action) {

		if (action.containsVar("a_m'")) {
			List<String> relevantParams = new ArrayList<String>();
			for (UtilityNode utilNode : state.getNetwork().getUtilityNodes()) {
				if (utilNode instanceof UtilityRuleNode) {
					for (ChanceNode paramNode : ((UtilityRuleNode)utilNode).getRule().getParameterNodes()) {
						relevantParams.add(paramNode.getId());
					}
				}
			}

			try {
				UtilQuery query = new UtilQuery(state.getNetwork(), relevantParams, action);
				UtilityTable utilities = (new ImportanceSampling()).queryUtil(query);
				ChanceNode rewardNode = new ChanceNode("R");
				for (String var : relevantParams) {
					rewardNode.addInputNode(state.getNetwork().getChanceNode(var));
				}

				if (relevantParams.isEmpty()) {
					UnivariateDensityFunction function = new GaussianDensityFunction
							(utilities.getUtil(new Assignment()), GAUSSIAN_VARIANCE);
					UnivariateDistribution continuousDistrib = new UnivariateDistribution("R", function);
					rewardNode.setDistrib(continuousDistrib);
				}
				else {
					ContinuousProbabilityTable table = new ContinuousProbabilityTable();
					for (Assignment params : utilities.getRows()) {
						UnivariateDensityFunction function = new GaussianDensityFunction
								(utilities.getUtil(params), GAUSSIAN_VARIANCE);
						UnivariateDistribution continuousDistrib = new UnivariateDistribution("R", function);
						table.addDistrib(params, continuousDistrib);
					}
					rewardNode.setDistrib(table);
				}
				
				if (state.getNetwork().hasNode("R")) {
					state.getNetwork().removeNode("R");
				}
				state.getNetwork().addNode(rewardNode);
			}
			catch (DialException e) {
				log.warning("cannot add reward estimate: " + e);
			}
		}
	}

}

