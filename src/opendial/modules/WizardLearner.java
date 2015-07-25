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

package opendial.modules;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.EmpiricalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.inference.Query;
import opendial.inference.approximate.Sample;
import opendial.inference.approximate.SamplingAlgorithm;

/**
 * Module employed to update parameters when provided with gold-standard actions from
 * Wizard-of-Oz data.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class WizardLearner implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;

	SamplingAlgorithm sampler;

	/** geometric factor used in supervised learning from Wizard-of-Oz data */
	public static final double GEOMETRIC_FACTOR = 0.5;

	public WizardLearner(DialogueSystem system) {
		this.system = system;
		sampler = new SamplingAlgorithm();
	}

	@Override
	public void start() {
	}

	@Override
	public void pause(boolean shouldBePaused) {
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!state.getActionNodeIds().isEmpty()) {
			if (state.getEvidence().containsVars(state.getActionNodeIds())) {
				try {
					Assignment wizardAction =
							state.getEvidence().getTrimmed(state.getActionNodeIds());
					state.clearEvidence(wizardAction.getVariables());
					learnFromWizardAction(wizardAction);
					state.addToState(wizardAction.removePrimes());
				}
				catch (RuntimeException e) {
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
	 * @param wizardAction the wizard action
	 * @return the list of updated parameters
	 */
	protected Set<String> learnFromWizardAction(Assignment wizardAction) {

		DialogueState state = system.getState();
		// determine the relevant parameters (discard the isolated ones)
		Set<String> relevantParams =
				state.getParameterIds()
						.stream().filter(p -> !state.getChanceNode(p)
								.getOutputNodes().isEmpty())
				.collect(Collectors.toSet());

		if (!relevantParams.isEmpty()) {
			try {
				List<String> queryVars = new ArrayList<String>(relevantParams);
				queryVars.addAll(wizardAction.getVariables());

				Query query =
						new Query.UtilQuery(state, queryVars, new Assignment());
				EmpiricalDistribution empiricalDistrib = sampler.getWeightedSamples(
						query, cs -> reweightSamples(cs, wizardAction));

				for (String param : relevantParams) {
					ChanceNode paramNode = state.getChanceNode(param);

					ProbDistribution newDistrib = empiricalDistrib.getMarginal(param,
							paramNode.getInputNodeIds());
					paramNode.setDistrib(newDistrib);
				}
			}
			catch (RuntimeException e) {
				log.warning("cannot update parameters based on wizard action: " + e);
			}
		}

		return relevantParams;
	}

	private static void reweightSamples(Collection<Sample> samples,
			Assignment wizardAction) {
		Set<String> actionVars = wizardAction.getVariables();

		UtilityTable averages = new UtilityTable();
		for (Sample sample : samples) {
			Assignment action = sample.getTrimmed(actionVars);
			averages.incrementUtil(action, sample.getUtility());
		}
		if (averages.getTable().size() == 1) {
			return;
		}
		for (Sample sample : samples) {

			UtilityTable copy = averages.copy();
			Assignment sampleAssign = sample.getTrimmed(actionVars);
			copy.setUtil(sampleAssign, sample.getUtility());
			int ranking = copy.getRanking(wizardAction, 0.1);
			if (ranking != -1) {
				double logweight = Math.log(
						(GEOMETRIC_FACTOR * Math.pow(1 - GEOMETRIC_FACTOR, ranking))
								+ 0.00001);
				sample.addLogWeight(logweight);
			}
		}
	}

}
