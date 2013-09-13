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

package opendial.bn.nodes;

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

import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.RuleDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.RuleUtilDistribution;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Output;
import opendial.domains.rules.parameters.Parameter;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.state.rules.AnchoredRule;
import opendial.utils.CombinatoricsUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UtilityRuleNode extends UtilityNode {

	// logger
	public static Logger log = new Logger("UtilityRuleNode", Logger.Level.DEBUG);

	AnchoredRule rule;
	
	public static final double LIKELY_VALUE_THRESHOLD = 0.2;
	

	public UtilityRuleNode(AnchoredRule rule) throws DialException {
		super(rule.getId());
		this.rule = rule;
		distrib = new RuleUtilDistribution(rule);
	}


	@Override
	public void addInputNode(BNode node) throws DialException {
		super.addInputNode(node);
		if (node instanceof DerivedActionNode) {
			((RuleUtilDistribution)distrib).addActionVariable(node.getId());
		}
	}
	
	/**
	 * Returns the set of all possible actions that are allowed by the node
	 * 
	 * @return the set of all relevant action values
	 */ 
	@Override 
	public void buildRelevantActionsCache() {

		relevantActionsCache = new HashMap<Assignment,Parameter>();

		try {
		for (Assignment input : getPossibleInputs().getAboveThreshold(LIKELY_VALUE_THRESHOLD).getRows()) {
			relevantActionsCache.putAll(distrib.getRelevantActions(input));
		}
		}
		catch (DialException e) {
			log.warning("could not build relevant actions cache: " + e);
		}
	}


	private SimpleTable getPossibleInputs () throws DialException {
		BNetwork network = null; 
		for (IdChangeListener listener : idChangeListeners) {
			if (listener instanceof BNetwork) {
				network = (BNetwork)listener;
			}
		}
		if (network == null) {
			throw new DialException("could not find including network");
		}
		Set<String> queryIds = new HashSet<String>();
		for (String var : rule.getInputVariables()) {
			if (network.hasChanceNode(var)) {
				queryIds.add(var);
			}
		}
		SimpleTable inputVals = (new SwitchingAlgorithm()).queryProb
				(new ProbQuery(network, queryIds)).toDiscrete().getProbTable(new Assignment());
		return inputVals;
	}
		
	
	@Override
	public UtilityRuleNode copy() throws DialException {
		return new UtilityRuleNode(rule);
	}


	public AnchoredRule getRule() {
		return rule;
	}

	
	
}
