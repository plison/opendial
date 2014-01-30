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
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.other.EmpiricalDistribution;
import opendial.bn.distribs.other.MarginalEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.approximate.SamplingProcess;
import opendial.inference.approximate.WeightedSample;
import opendial.inference.queries.UtilQuery;
import opendial.state.DialogueState;

/**
 * Module employed to update parameters when provided with gold-standard
 * actions from Wizard-of-Oz data.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class WizardLearner implements Module {

	// logger
	public static Logger log = new Logger("WizardLearner", Logger.Level.DEBUG);

	DialogueSystem system;
	
	/** geometric factor used in supervised learning from Wizard-of-Oz data */
	public static final double GEOMETRIC_FACTOR = 0.5;



	public WizardLearner(DialogueSystem system) {
		this.system = system;
	}
	
	@Override
	public void start() {	}

	@Override
	public void pause(boolean shouldBePaused) {	}

	@Override
	public boolean isRunning() {  return true;	}

	
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!state.getActionNodeIds().isEmpty()) {
			if (state.getEvidence().containsVars(state.getActionNodeIds())) {
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


	/**
	 * Updates the domain parameters given the wizard action selected at the provided 
	 * dialogue state, and returns the list of updated parameters.
	 * 
	 * @param state the dialogue state to update
	 * @param wizardAction the wizard action
	 * @return the list of updated parameters
	 * @throws DialException if the update fails
	 */
	protected static Set<String> learnFromWizardAction(DialogueState state, 
			Assignment wizardAction) throws DialException {
			
			// determine the relevant parameters (discard the isolated ones)
			Set<String> relevantParams = new HashSet<String>();
			for (String param : state.getParameterIds()) {
				if (!state.getChanceNode(param).getOutputNodesIds().isEmpty()) {
					relevantParams.add(param);
				}
			}			
			if (!relevantParams.isEmpty()) {
								
				// creates a new query thread
				SamplingProcess isquery = new SamplingProcess
						(new UtilQuery(state, relevantParams), 
								Settings.nbSamples, Settings.maxSamplingTime);
						
				// extract and redraw the samples according to their weight.
				Stack<WeightedSample> samples = isquery.getSamples();
				reweightSamples(samples, wizardAction);
				samples = LikelihoodWeighting.redrawSamples(samples);

				// creates an empirical distribution from the samples
				EmpiricalDistribution empiricalDistrib = new EmpiricalDistribution();
				for (WeightedSample sample : samples) {
					sample.trim(relevantParams);
					empiricalDistrib.addSample(sample);
				}
							
				for (String param : relevantParams) {
					ChanceNode paramNode = state.getChanceNode(param);
					MarginalEmpiricalDistribution newDistrib = new MarginalEmpiricalDistribution
							(Arrays.asList(param), paramNode.getInputNodeIds(), empiricalDistrib);
					paramNode.setDistrib(newDistrib);
				}
			}
			
			return relevantParams;
	}

	


	private static void reweightSamples(Stack<WeightedSample> samples,
			Assignment wizardAction) {

		UtilityTable averages = new UtilityTable();

		Set<String> actionVars = wizardAction.getVariables();
		for (WeightedSample sample : samples) {
			Assignment action = sample.getTrimmed(actionVars);
			averages.incrementUtil(action, sample.getUtility());
		}
		if (averages.getTable().size() == 1) {
			return;
		}

	/**	log.debug("Utilities : " + averages.toString().replace("\n", ", ") 
				+ " ==> gold action = " + wizardAction); */

		for (WeightedSample sample : samples) {

			UtilityTable copy = averages.copy();
			Assignment sampleAssign = sample.getTrimmed(actionVars);
			copy.setUtil(sampleAssign, sample.getUtility());
			int ranking = copy.getRanking(wizardAction);
			if (ranking != -1) {
				double logweight = Math.log((GEOMETRIC_FACTOR 
						* Math.pow(1-GEOMETRIC_FACTOR, ranking)) + 0.00001);
				sample.addLogWeight(logweight);
			}				
		}
	}
	


}

