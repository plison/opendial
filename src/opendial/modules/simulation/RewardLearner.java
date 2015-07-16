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

package opendial.modules.simulation;

import java.util.logging.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.EmpiricalDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;
import opendial.datastructs.Assignment;
import opendial.inference.Query;
import opendial.inference.approximate.Sample;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.modules.Module;

/**
 * Module employed during simulated dialogues to automatically estimate a utility
 * model from rewards produced by the simulator.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class RewardLearner implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the dialogue system
	DialogueSystem system;

	// previous dialogue states with a decision network. The states are indexed
	// by the label of their action variables.
	Map<Set<String>, DialogueState> previousStates;

	SamplingAlgorithm sampler;

	/**
	 * Creates the reward learner for the dialogue system.
	 * 
	 * @param system the dialogue system.
	 */
	public RewardLearner(DialogueSystem system) {
		this.system = system;
		previousStates = new HashMap<Set<String>, DialogueState>();
		sampler = new SamplingAlgorithm();
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void start() {
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void pause(boolean shouldBePaused) {
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isRunning() {
		return true;
	}

	/**
	 * Triggers the reward learner. The module is only triggered whenever a variable
	 * of the form R(assignment of action values) is included in the dialogue state
	 * by the simulator. In such case, the module checks whether a past dialogue
	 * state contains a decision for these action variables, and if yes, update their
	 * parameters to reflect the actual received reward.
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the list of recently updated variables.
	 * 
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		for (String evidenceVar : state.getEvidence().getVariables()) {
			if (evidenceVar.startsWith("R(") && evidenceVar.endsWith(")")) {
				Assignment actualAction = Assignment.createFromString(
						evidenceVar.substring(2, evidenceVar.length() - 1));
				double actualUtility =
						((DoubleVal) state.getEvidence().getValue(evidenceVar))
								.getDouble();

				if (previousStates.containsKey(actualAction.getVariables())) {
					DialogueState previousState =
							previousStates.get(actualAction.getVariables());
					learnFromFeedback(previousState, actualAction, actualUtility);
				}
				state.clearEvidence(Arrays.asList(evidenceVar));
			}
		}

		if (!state.getActionNodeIds().isEmpty()) {
			try {
				previousStates.put(new HashSet<String>(state.getActionNodeIds()),
						state.copy());
			}
			catch (RuntimeException e) {
				log.warning("cannot copy state: " + e);
			}
		}
	}

	/**
	 * Re-estimate the posterior distribution for the domain parameters in the
	 * dialogue state given the actual system decision and its resulting utility
	 * (provided by the simulator).
	 * 
	 * @param state the dialogue state
	 * @param actualAction the action that was selected
	 * @param actualUtility the resulting utility for the action.
	 */
	private void learnFromFeedback(DialogueState state, Assignment actualAction,
			double actualUtility) {

		try {

			// determine the relevant parameters (discard the isolated ones)
			Set<String> relevantParams =
					state.getParameterIds()
							.stream().filter(p -> !state.getChanceNode(p)
									.getOutputNodes().isEmpty())
					.collect(Collectors.toSet());

			if (!relevantParams.isEmpty()) {

				Query query =
						new Query.UtilQuery(state, relevantParams, actualAction);
				EmpiricalDistribution empiricalDistrib = sampler.getWeightedSamples(
						query, cs -> reweightSamples(cs, actualUtility));

				for (String param : relevantParams) {
					ChanceNode paramNode = system.getState().getChanceNode(param);
					ProbDistribution newDistrib = empiricalDistrib.getMarginal(param,
							paramNode.getInputNodeIds());
					paramNode.setDistrib(newDistrib);
				}
			}
		}
		catch (RuntimeException e) {
			log.warning("could not learn from action feedback: " + e);
		}

	}

	private static void reweightSamples(Collection<Sample> samples,
			double actualUtility) {
		samples.stream().forEach(s -> {
			double weight = 1.0 / (Math.abs(s.getUtility() - actualUtility) + 1);
			s.addLogWeight(Math.log(weight));
		});
	}

}
