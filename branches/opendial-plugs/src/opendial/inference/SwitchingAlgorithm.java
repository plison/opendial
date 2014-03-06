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


import java.util.Collection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution.DistribType;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;


/**
 * Switching algorithms that alternative between an exact algorithm (variable elimination) and
 * an approximate algorithm (likelihood weighting) depending on the query.
 * 
 * <p>The switching mechanism is defined via three thresholds: <ul>
 * <li> one threshold on the maximum branching factor of the network
 * <li> one threshold on the maximum number of query variables 
 * <li> one threshold on the maximum number of continuous variables
 * </ul>
 * 
 * <p>If at least one of these thresholds is exceeded, the selected algorithm will be likelihood
 * weighting.  Variable elimination is selected in the remaining cases.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class SwitchingAlgorithm implements InferenceAlgorithm {

	// logger
	public static Logger log = new Logger("SwitchingAlgorithm", Logger.Level.DEBUG);
	
	// maximum branching factor (in-degree)
	public static int MAX_BRANCHING_FACTOR = 4;
	
	// maximum number of query variables
	public static int MAX_QUERYVARS = 6;
	
	// maximum number of continuous variables
	public static int MAX_CONTINUOUS = 0;
	
	VariableElimination ve;
	LikelihoodWeighting lw;
	
	public SwitchingAlgorithm() {
		this.ve = new VariableElimination();
		this.lw = new LikelihoodWeighting();
	}
	
	/**
	 * Selects the best algorithm for performing the inference on the provided
	 * probability query and return its result.
	 * 
	 * @param query the probability query
	 * @return the inference result
	 */
	@Override
	public IndependentProbDistribution queryProb(ProbQuery query) throws DialException {
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
	public UtilityTable queryUtil(UtilQuery query) throws DialException {
		InferenceAlgorithm algo = selectBestAlgorithm(query);
		return algo.queryUtil(query);
	}

	
	/**
	 * Reduces a Bayesian network to a subset of variables.  The method is divided in 
	 * three steps: <ul>
	 * <li> The method first checks whether inference is necessary at all or whether 
	 * the current network can be returned as it is.  
	 * <li> If inference is necessary, the algorithm divides the network into cliques
	 * and performs inference on each clique separately.
	 * <li> Finally, if only one clique is present, the reduction selects the best
	 * algorithm and return the result of the reduction process.
	 * </ul>
	 * 
	 * @param query the reduction query
	 * @return the reduced network
	 */
	@Override
	public BNetwork reduce(ReductionQuery query) throws DialException {
		
		// if the current network can be returned as such, do it
		if (query.getQueryVars().containsAll(query.getNetwork().getNodeIds()) && query.getEvidence().isEmpty()) {	
			BNetwork subnetwork = query.getNetwork();
			return subnetwork;
		}
		
		// if all query variables are included in the evidence, no inference is needed
		else if (query.getEvidence().containsVars(query.getQueryVars())) {
			BNetwork subnetwork = new BNetwork();
			for (String qvar : query.getQueryVars()) {
				ChanceNode newNode = new ChanceNode(qvar, new CategoricalTable
						(new Assignment(qvar, query.getEvidence().getValue(qvar))));
				subnetwork.addNode(newNode);
			}
			return subnetwork;
		}
		
		// if the network can be divided into cliques, extract the cliques
		// and do a separate reduction for each
		else if (query.getNetwork().getCliques().size() > 1) {
			BNetwork fullNetwork = new BNetwork();
			for (BNetwork clique : query.getNetwork().createCliques()) {
				Collection<String> subQueryVars = query.getQueryVars();
				subQueryVars.retainAll(clique.getNodeIds());
				if (!subQueryVars.isEmpty()) {
				Assignment evidence = query.getEvidence().getTrimmed(clique.getNodeIds());
				ReductionQuery subQuery = new ReductionQuery(clique, subQueryVars, evidence);
				BNetwork subnetwork = reduce(subQuery);
				fullNetwork.addNetwork(subnetwork);			
				}
			}
			return fullNetwork;
		}
		
		// else, select the best reduction algorithm and performs the reduction
		InferenceAlgorithm algo = selectBestAlgorithm(query);
		BNetwork result = algo.reduce(query);	
		return result;
	}
	

	
	
	public InferenceAlgorithm selectBestAlgorithm (Query query) {
			
		int nbQueries = query.getQueryVars().size();
		int branchingFactor = 0;
		int nbContinuous = 0;
		for (BNode node : query.getFilteredSortedNodes()) {
			if (node.getInputNodeIds().size() > branchingFactor) {
				branchingFactor = node.getInputNodeIds().size();
			}
			if (node instanceof ChanceNode && ((ChanceNode)node).getDistrib().
					getPreferredType() != DistribType.DISCRETE) {
				nbContinuous++;
			}
		}		
		if (nbContinuous > MAX_CONTINUOUS ||
				branchingFactor > MAX_BRANCHING_FACTOR || 
				nbQueries > MAX_QUERYVARS) {	
			return lw;
		
		}
		else {
			return ve;
		}
	}


}

