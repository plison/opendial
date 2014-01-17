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


import java.util.Arrays;
import java.util.Collection;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.other.EmpiricalDistribution;
import opendial.bn.distribs.other.MarginalEmpiricalDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.queries.UtilQuery;
import opendial.state.DialogueState;


public class WizardLearner implements Module {

	// logger
	public static Logger log = new Logger("WizardLearner", Logger.Level.DEBUG);

	DialogueSystem system;


	public WizardLearner(DialogueSystem system) {
		this.system = system;
	}
	
	public void start() {	}

	public void pause(boolean shouldBePaused) {	}

	public boolean isRunning() {  return true;	}

	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!state.getActionNodeIds().isEmpty()) {
			if (state.getEvidence().getVariables().containsAll(state.getActionNodeIds())) {
			try {
				Assignment wizardAction = state.getEvidence().getTrimmed(state.getActionNodeIds());
				state.clearEvidence(wizardAction.getVariables());
				learnFromWizardAction(state, wizardAction);
				state.addToState(wizardAction.removePrimes());
			}
			catch (DialException e) {
				log.warning("could not learn from wizard actions: " + e);
			}
			}
			else {
				state.removeNodes(state.getActionNodeIds());
				state.removeNodes(state.getUtilityNodeIds());
			}
		}
	}



	private void learnFromWizardAction(DialogueState state, Assignment wizardAction) {
		try {
			if (!state.getParameterIds().isEmpty()) {
				LikelihoodWeighting lw = new LikelihoodWeighting();
				UtilQuery query = new UtilQuery(state, state.getParameterIds());
				EmpiricalDistribution empirical = lw.queryWizard(query, wizardAction);
				for (String param : state.getParameterIds()) {
					ChanceNode paramNode = state.getChanceNode(param);
					MarginalEmpiricalDistribution newDistrib = new MarginalEmpiricalDistribution
							(Arrays.asList(param), paramNode.getInputNodeIds(), empirical);
					paramNode.setDistrib(newDistrib);
				}
			}
		}
		catch (DialException e) {
			log.warning("could not learn from wizard action: " + e);
		}
	}



}

