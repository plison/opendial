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


public class WizardLearner implements Module {

	// logger
	public static Logger log = new Logger("WizardLearner", Logger.Level.DEBUG);

	DialogueSystem system;


	public void start(DialogueSystem system) {
		this.system = system;
	}

	public void pause(boolean shouldBePaused) {	}


	public void trigger() {
		if (!system.getState().getActionNodeIds().isEmpty()) {
			if (system.getState().getEvidence().getVariables().containsAll(system.getState().getActionNodeIds())) {
			try {
				Assignment wizardAction = system.getState().getEvidence().getTrimmed(system.getState().getActionNodeIds());
				system.getState().clearEvidence(wizardAction.getVariables());
				learnFromWizardAction(wizardAction);
				system.getState().addToState(wizardAction.removePrimes());
			}
			catch (DialException e) {
				log.warning("could not learn from wizard actions: " + e);
			}
			}
			else {
				system.getState().removeNodes(system.getState().getActionNodeIds());
				system.getState().removeNodes(system.getState().getUtilityNodeIds());
			}
		}
	}



	private void learnFromWizardAction(Assignment wizardAction) {
		try {
			if (!system.getState().getParameterIds().isEmpty()) {
				LikelihoodWeighting lw = new LikelihoodWeighting();
				UtilQuery query = new UtilQuery(system.getState(), system.getState().getParameterIds());
				EmpiricalDistribution empirical = lw.queryWizard(query, wizardAction);
				for (String param : system.getState().getParameterIds()) {
					ChanceNode paramNode = system.getState().getChanceNode(param);
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

