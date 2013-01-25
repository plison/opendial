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

package opendial.inference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.inference.datastructs.DoubleFactor;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.InferenceUtils;

/**
 * Algorithm for naive probabilistic inference, based on computing the full
 * joint distribution, and then summing everything.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class NaiveInference implements InferenceAlgorithm {

	public static Logger log = new Logger("NaiveInference", Logger.Level.DEBUG);


	/**
	 * Queries the probability distribution encoded in the Bayesian Network, given
	 * a set of query variables, and some evidence.
	 * 
	 * @param query the full query
	 * @throws DialException 
	 */
	@Override
	public DiscreteProbDistribution queryProb (ProbQuery query) throws DialException {

		if (!query.getConditionalVars().isEmpty()) {
			return queryConditionalProb(query);
		}

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();

		// generates the full joint distribution
		Map<Assignment, Double> fullJoint = getFullJoint(network, false);

		// generates all possible value assignments for the query variables
		SortedMap<String,Set<Value>> queryValues = new TreeMap<String,Set<Value>>();
		for (ChanceNode n : network.getChanceNodes()) {
			if (queryVars.contains(n.getId())) {
				queryValues.put(n.getId(), n.getValues());
			}
		}
		Set<Assignment> queryAssigns = CombinatoricsUtils.getAllCombinations(queryValues);

		Map<Assignment, Double> queryResult = new HashMap<Assignment,Double>();

		// calculate the (unnormalised) probability for each assignment of the query variables
		for (Assignment queryA : queryAssigns) {
			double sum = 0.0f;
			for (Assignment a: fullJoint.keySet()) {
				if (a.contains(queryA) && a.contains(evidence)) {
					sum += fullJoint.get(a);
				}
			}
			queryResult.put(queryA, sum);
		}

		// normalise the end result
		queryResult = InferenceUtils.normalise(queryResult);

		// write the result in a probability table
		SimpleTable distrib = new SimpleTable();
		distrib.addRows(queryResult);

		return distrib;
	}


	/**
	 * Computes the full joint probability distribution for the Bayesian Network
	 * 
	 * @param bn the Bayesian network
	 * @param includeActions whether to include action nodes or not
	 * @return the resulting joint distribution
	 */
	static Map<Assignment, Double> getFullJoint(BNetwork bn, boolean includeActions) {

		SortedMap<String,Set<Value>> allValues = new TreeMap<String,Set<Value>>();
		for (ChanceNode n : bn.getChanceNodes()) {
			allValues.put(n.getId(), n.getValues());
		}
		if (includeActions) {
			for (ActionNode n : bn.getActionNodes()) {
				allValues.put(n.getId(), n.getValues());
			}
		}

		Set<Assignment> fullAssigns = CombinatoricsUtils.getAllCombinations(allValues);
		Map<Assignment,Double> result = new HashMap<Assignment, Double>();
		for (Assignment singleAssign : fullAssigns) {
			double jointLogProb = 0.0f;
			for (ChanceNode n: bn.getChanceNodes()) {
				Assignment trimmedCon = singleAssign.getTrimmed(n.getInputNodeIds());
				jointLogProb += Math.log10(n.getProb(trimmedCon, singleAssign.getValue(n.getId())));
			}
			if (includeActions) {
				for (ActionNode n: bn.getActionNodes()) {
					jointLogProb += Math.log10(n.getProb(singleAssign.getValue(n.getId())));
				}
			}
			result.put(singleAssign, (double)Math.pow(10,jointLogProb));
		}
		return result;
	}


	/**
	 * Computes the utility distribution for the Bayesian network, depending on the value of the action
	 * variables given as parameters. 
	 * 
	 * @param query the full query
	 */
	@Override
	public UtilityTable queryUtil(UtilQuery query) {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();

		// generates the full joint distribution
		Map<Assignment, Double> fullJoint = getFullJoint(network, true);

		// generates all possible value assignments for the query variables
		SortedMap<String,Set<Value>> actionValues = new TreeMap<String,Set<Value>>();
		for (BNode n : network.getNodes()) {
			if (queryVars.contains(n.getId())) {
				actionValues.put(n.getId(), n.getValues());
			}
		}
		Set<Assignment> actionAssigns = CombinatoricsUtils.getAllCombinations(actionValues);
		UtilityTable table = new UtilityTable();
		for (Assignment actionAssign : actionAssigns) {

			double totalUtility = 0.0f;
			double totalProb = 0.0f;
			for (Assignment jointAssign : fullJoint.keySet()) {

				if (jointAssign.contains(evidence)) {
					double totalUtilityForAssign = 0.0f;
					Assignment stateAndActionAssign = new Assignment (jointAssign, actionAssign);

					for (UtilityNode valueNode : network.getUtilityNodes()) {
						double singleUtility = valueNode.getUtility(stateAndActionAssign);
						totalUtilityForAssign += singleUtility;
					}
					totalUtility += (totalUtilityForAssign * fullJoint.get(jointAssign));
					totalProb += fullJoint.get(jointAssign);
				}
			}
			table.setUtil(actionAssign, totalUtility/totalProb);
		}

		return table;	
	}



	/**
	 *
	 * @param query the full query
	 */
	protected DiscreteProbabilityTable queryConditionalProb(ProbQuery query) throws DialException {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();
		Collection<String> condVars = query.getConditionalVars();

		// generates all possible value assignments for the conditional variables
		SortedMap<String,Set<Value>> dependentValues = new TreeMap<String,Set<Value>>();
		for (String condVar : condVars) {
			if (network.hasNode(condVar)) {
				dependentValues.put(condVar, network.getNode(condVar).getValues());
			}
		}
		Set<Assignment> condAssigns = CombinatoricsUtils.getAllCombinations(dependentValues);

		DiscreteProbabilityTable table = new DiscreteProbabilityTable();
		for (Assignment a : condAssigns) {
			table.addRows(a, queryProb(new ProbQuery(network, queryVars, 
					new Assignment(evidence, a))).getProbTable(new Assignment()));
		}
		return table;
	}



	public BNetwork reduceNetwork(ReductionQuery query) throws DialException {

		BNetwork fullNetwork = query.getNetwork();

		BNetwork reduced = fullNetwork.getReducedCopy(query.getQueryVars(), query.getNodesToIsolate());

		// finally, sets the distribution for the nodes to retain, according
		// to the factors generated via V.E.
		for (ChanceNode node: reduced.getChanceNodes()) {	
			ProbQuery subQuery = new ProbQuery(fullNetwork, Arrays.asList(node.getId()), query.getEvidence(), node.getInputNodeIds());
			DiscreteProbDistribution distrib = queryConditionalProb(subQuery);	
			node.setDistrib(distrib);
		}
		for (UtilityNode node : reduced.getUtilityNodes()) {
			UtilQuery subQuery = new UtilQuery(fullNetwork, node.getInputNodeIds(), query.getEvidence());
			UtilityDistribution distrib = queryUtil(subQuery);
			node.setDistrib(distrib);
		}


		return reduced;		
	}

}
