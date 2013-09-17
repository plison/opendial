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

package opendial.state.rules;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;

import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.IdChangeListener;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.state.rules.Rule.RuleType;
import opendial.utils.CombinatoricsUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AnchoredRuleCache {

	// logger
	public static Logger log = new Logger("AnchoredValueCache", Logger.Level.DEBUG);

	AnchoredRule rule;

	public static final double PROB_THRESHOLD = 0.1;

	Set<Assignment> likelyConditions = new HashSet<Assignment>();

	Set<Assignment> possibleConditions = new HashSet<Assignment>();

	// this table does not reflect real distributions
	Map<Output, Set<Parameter>> cachedValues ;

	public AnchoredRuleCache (AnchoredRule rule) {
		this.rule = rule;

		cachedValues = new HashMap<Output, Set<Parameter>>();

		if (rule.getRule() instanceof DecisionRule) {
			extractLikelyConditions();
			if (shouldBeDiscarded()) {
				return;
			}		
		}
		
		extractPossibleConditions();

		for (Assignment condition : possibleConditions) {
			OutputTable conditionedOutput = rule.getEffectOutputs(condition);

			for (Output o : conditionedOutput.getOutputs()) {
				if (!cachedValues.containsKey(o)) {
					cachedValues.put(o, new HashSet<Parameter>());
				}
				cachedValues.get(o).add(conditionedOutput.getParameter(o));
			}
			if (conditionedOutput.getFixedMass() < 0.98 && rule.getRule().getRuleType() == RuleType.PROB) {
				Output emptyOutput = new Output();
				if (!cachedValues.containsKey(emptyOutput)) {
					cachedValues.put(emptyOutput, new HashSet<Parameter>());
				}
				cachedValues.get(emptyOutput).add(new FixedParameter(1-conditionedOutput.getFixedMass()));
			}
		}

	}



	private boolean shouldBeDiscarded() {
		boolean shouldBeDiscarded = true;
		for (Assignment cond : likelyConditions) {
			OutputTable conditionedOutput = rule.getEffectOutputs(cond);

			for (Output o : conditionedOutput.getOutputs()) {
				if (!o.isVoid()) {
					shouldBeDiscarded = false;
				}
			}
		}
		return shouldBeDiscarded;
	}



	public Set<Output> getOutputs() {
		return cachedValues.keySet();
	}

	public Set<Assignment> getPossibleConditions() {
		return possibleConditions;
	}


	public Set<Parameter> getParameters(Output o) {
		return cachedValues.get(o);
	}





	/**
	 * Returns the list of possible assignment of input values for the node.  If the
	 * node has no input, returns a list with a single, empty assignment.
	 * 
	 * <p>NB: this is a combinatorially expensive operation, use with caution.
	 * 
	 * @return the (unordered) list of possible conditions.  
	 */
	private void extractPossibleConditions() {
		Map<String,Set<Value>> possibleInputValues = new HashMap<String,Set<Value>>();
		for (ChanceNode inputNode : rule.getInputNodes().values()) {
			possibleInputValues.put(inputNode.getId(), inputNode.getValues());
		}

		int nbCombinations = 
				CombinatoricsUtils.getEstimatedNbCombinations(possibleInputValues);

		if (nbCombinations < 100) {
			possibleConditions = 
					CombinatoricsUtils.getAllCombinations(possibleInputValues);
		}
		else {
			possibleConditions = new HashSet<Assignment>();
			for (int i = 0 ; i < Settings.getInstance().nbSamples/4 ; i++) {
				Assignment sampledA = new Assignment();
				for (ChanceNode inputNode: rule.getInputNodes().values()) {
					Value sampledValue = null;
					try { 
						sampledValue = inputNode.sample();
						sampledA.addPair(inputNode.getId(), sampledValue); }
					catch (DialException e) { log.warning("cannot sample " + inputNode.getId()+")"); }
					catch (NullPointerException f) { 
						log.debug("sampledA: " + sampledA  + "inputNodeId: " + inputNode.getId() + " sampled: " + sampledValue); 
					}
				}
				possibleConditions.add(sampledA);
			}
		}
	}


	private void extractLikelyConditions() {

		try {
			SimpleTable inputVals = (new SwitchingAlgorithm()).queryProb
					(new ProbQuery(rule.state, rule.getInputNodes().keySet())).toDiscrete().getProbTable(new Assignment());

			likelyConditions = inputVals.getAboveThreshold(PROB_THRESHOLD).getRows();
		}
		catch (DialException e) {
			log.warning("could not extract the input values for the decision rule " + rule);
		}
	}


	public Set<Assignment> getLikelyConditions() {
		return likelyConditions;
	}



}
