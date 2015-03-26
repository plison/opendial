// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.modules.core;


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
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * Module employed to update parameters when provided with gold-standard
 * actions from Wizard-of-Oz data.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-16 17:34:31 #$
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

