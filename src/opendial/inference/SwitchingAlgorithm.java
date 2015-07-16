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

package opendial.inference;

import java.util.logging.*;

import opendial.bn.BNetwork;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.inference.exact.VariableElimination;

/**
 * Switching algorithms that alternates between an exact algorithm (variable
 * elimination) and an approximate algorithm (likelihood weighting) depending on the
 * query.
 * 
 * <p>
 * The switching mechanism is defined via two thresholds:
 * <ul>
 * <li>one threshold on the maximum branching factor of the network
 * <li>one threshold on the maximum number of combination of values in a node factor
 * </ul>
 * 
 * <p>
 * If one of these threshold is exceeded or if the Bayesian network contains a
 * continuous distribution, the selected algorithm will be likelihood weighting.
 * Variable elimination is selected in the remaining cases.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class SwitchingAlgorithm implements InferenceAlgorithm {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// maximum branching factor (in-degree) for VE
	public static int MAX_BRANCHING_FACTOR = 10;

	// maximum number of values to use VE
	public static int MAX_NBVALUES = 5000;

	VariableElimination ve;
	SamplingAlgorithm lw;

	public SwitchingAlgorithm() {
		this.ve = new VariableElimination();
		this.lw = new SamplingAlgorithm();
	}

	/**
	 * Selects the best algorithm for performing the inference on the provided
	 * probability query and return its result.
	 * 
	 * @param query the probability query
	 * @return the inference result
	 */
	@Override
	public MultivariateDistribution queryProb(Query.ProbQuery query) {
		InferenceAlgorithm algo = selectBestAlgorithm(query);
		return algo.queryProb(query);
	}

	/**
	 * Selects the best algorithm for performing the inference on the provided
	 * utility query and return its result.
	 * 
	 * @param query the utility query
	 * @return the inference result
	 */
	@Override
	public UtilityTable queryUtil(Query.UtilQuery query) {
		InferenceAlgorithm algo = selectBestAlgorithm(query);
		return algo.queryUtil(query);
	}

	/**
	 * Reduces a Bayesian network to a subset of variables. The method is divided in
	 * three steps:
	 * <ul>
	 * <li>The method first checks whether inference is necessary at all or whether
	 * the current network can be returned as it is.
	 * <li>If inference is necessary, the algorithm divides the network into cliques
	 * and performs inference on each clique separately.
	 * <li>Finally, if only one clique is present, the reduction selects the best
	 * algorithm and return the result of the reduction process.
	 * </ul>
	 * 
	 * @param query the reduction query
	 * @return the reduced network
	 */
	@Override
	public BNetwork reduce(Query.ReduceQuery query) {
		// select the best reduction algorithm and performs the reduction
		InferenceAlgorithm algo = selectBestAlgorithm(query);
		BNetwork result = algo.reduce(query);
		return result;
	}

	public InferenceAlgorithm selectBestAlgorithm(Query query) {

		for (BNode node : query.getFilteredSortedNodes()) {
			if (node.getInputNodeIds().size() > MAX_BRANCHING_FACTOR) {
				return lw;
			}
			if (node instanceof ChanceNode) {
				if (((ChanceNode) node)
						.getDistrib() instanceof ContinuousDistribution) {
					return lw;
				}
				int nbValues = ((ChanceNode) node).getNbValues();
				for (ChanceNode i : node.getInputNodes(ChanceNode.class)) {
					nbValues *= i.getNbValues();
				}
				if (nbValues > MAX_NBVALUES) {
					return lw;
				}
			}
		}
		return ve;
	}

}
