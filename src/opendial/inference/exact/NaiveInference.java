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

package opendial.inference.exact;

import java.util.logging.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.Query;
import opendial.utils.InferenceUtils;

/**
 * Algorithm for naive probabilistic inference, based on computing the full joint
 * distribution, and then summing everything.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaiveInference implements InferenceAlgorithm {

	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Queries the probability distribution encoded in the Bayesian Network, given a
	 * set of query variables, and some evidence.
	 * 
	 * @param query the full query result
	 */
	@Override
	public MultivariateTable queryProb(Query.ProbQuery query) {

		BNetwork network = query.getNetwork();
		Set<String> queryVars = new HashSet<String>(query.getQueryVars());
		Assignment evidence = query.getEvidence();

		// generates the full joint distribution
		Map<Assignment, Double> fullJoint = getFullJoint(network, false);

		// generates all possible value assignments for the query variables
		SortedMap<String, Set<Value>> queryValues =
				new TreeMap<String, Set<Value>>();
		for (ChanceNode n : network.getChanceNodes()) {
			if (queryVars.contains(n.getId())) {
				queryValues.put(n.getId(), n.getValues());
			}
		}
		Set<Assignment> queryAssigns =
				InferenceUtils.getAllCombinations(queryValues);

		MultivariateTable.Builder queryResult = new MultivariateTable.Builder();

		// calculate the (unnormalised) probability for each assignment of the
		// query variables
		for (Assignment queryA : queryAssigns) {
			double sum = 0.0f;
			for (Assignment a : fullJoint.keySet()) {
				if (a.contains(queryA) && a.contains(evidence)) {
					sum += fullJoint.get(a);
				}
			}
			queryResult.addRow(queryA, sum);
		}

		queryResult.normalise();
		return queryResult.build();
	}

	/**
	 * Computes the full joint probability distribution for the Bayesian Network
	 * 
	 * @param bn the Bayesian network
	 * @param includeActions whether to include action nodes or not
	 * @return the resulting joint distribution
	 */
	public static Map<Assignment, Double> getFullJoint(BNetwork bn,
			boolean includeActions) {

		SortedMap<String, Set<Value>> allValues = new TreeMap<String, Set<Value>>();
		for (ChanceNode n : bn.getChanceNodes()) {
			allValues.put(n.getId(), n.getValues());
		}
		if (includeActions) {
			for (ActionNode n : bn.getActionNodes()) {
				allValues.put(n.getId(), n.getValues());
			}
		}

		Set<Assignment> fullAssigns = InferenceUtils.getAllCombinations(allValues);
		Map<Assignment, Double> result = new HashMap<Assignment, Double>();
		for (Assignment singleAssign : fullAssigns) {
			double jointLogProb = 0.0f;
			for (ChanceNode n : bn.getChanceNodes()) {
				Assignment trimmedCon = singleAssign.getTrimmed(n.getInputNodeIds());
				jointLogProb += Math.log10(
						n.getProb(trimmedCon, singleAssign.getValue(n.getId())));
			}
			if (includeActions) {
				for (ActionNode n : bn.getActionNodes()) {
					jointLogProb +=
							Math.log10(n.getProb(singleAssign.getValue(n.getId())));
				}
			}
			result.put(singleAssign, Math.pow(10, jointLogProb));
		}
		return result;
	}

	/**
	 * Computes the utility distribution for the Bayesian network, depending on the
	 * value of the action variables given as parameters.
	 * 
	 * @param query the full query
	 * @return the corresponding utility table
	 */
	@Override
	public UtilityTable queryUtil(Query.UtilQuery query) {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();

		// generates the full joint distribution
		Map<Assignment, Double> fullJoint = getFullJoint(network, true);

		// generates all possible value assignments for the query variables
		SortedMap<String, Set<Value>> actionValues =
				new TreeMap<String, Set<Value>>();
		for (BNode n : network.getNodes()) {
			if (queryVars.contains(n.getId())) {
				actionValues.put(n.getId(), n.getValues());
			}
		}
		Set<Assignment> actionAssigns =
				InferenceUtils.getAllCombinations(actionValues);
		UtilityTable table = new UtilityTable();
		for (Assignment actionAssign : actionAssigns) {

			double totalUtility = 0.0f;
			double totalProb = 0.0f;
			for (Assignment jointAssign : fullJoint.keySet()) {

				if (jointAssign.contains(evidence)) {
					double totalUtilityForAssign = 0.0f;
					Assignment stateAndActionAssign =
							new Assignment(jointAssign, actionAssign);

					for (UtilityNode valueNode : network.getUtilityNodes()) {
						double singleUtility =
								valueNode.getUtility(stateAndActionAssign);
						totalUtilityForAssign += singleUtility;
					}
					totalUtility +=
							(totalUtilityForAssign * fullJoint.get(jointAssign));
					totalProb += fullJoint.get(jointAssign);
				}
			}
			table.setUtil(actionAssign, totalUtility / totalProb);
		}

		return table;
	}

	/**
	 * Reduces the Bayesian network to a subset of its variables. This reduction
	 * operates here by generating the possible conditional assignments for every
	 * retained variables, and calculating the distribution for each assignment.
	 * 
	 * @param query the reduction query
	 * @return the reduced network
	 */
	@Override
	public BNetwork reduce(Query.ReduceQuery query) {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();

		List<String> sortedNodesIds = network.getSortedNodesIds();
		sortedNodesIds.retainAll(queryVars);
		Collections.reverse(sortedNodesIds);

		BNetwork reduced = new BNetwork();
		for (String var : sortedNodesIds) {
			Set<String> directAncestors =
					network.getNode(var).getAncestorsIds(queryVars);

			// generating the conditional assignments for var
			Map<String, Set<Value>> inputValues = new HashMap<String, Set<Value>>();
			for (String input : directAncestors) {
				inputValues.put(input, network.getNode(var).getValues());
			}
			Set<Assignment> inputs = InferenceUtils.getAllCombinations(inputValues);

			// creating a conditional probability table for the variable
			ConditionalTable.Builder builder = new ConditionalTable.Builder(var);
			for (Assignment a : inputs) {
				Assignment evidence2 = new Assignment(evidence, a);
				CategoricalTable result =
						(CategoricalTable) queryProb(network, var, evidence2);
				builder.addRows(a, result.getTable());
			}

			// creating the node
			ChanceNode cn = new ChanceNode(var, builder.build());
			for (String ancestor : directAncestors) {
				cn.addInputNode(reduced.getNode(ancestor));
			}
			reduced.addNode(cn);
		}

		return reduced;
	}

}
