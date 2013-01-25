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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.inference.datastructs.DistributionCouple;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.UtilQuery;
import opendial.utils.CombinatoricsUtils;

/**
 * Abstract class providing basic functionalities for probabilistic inference.
 * In particular, the class re-groups probability and utility queries under the same
 * operation functioning under similar mechanisms.
 * 
 * <p>Concrete classes extending this abstract class must implement the queryHybrid(...)
 * function, returning a probability/utility distribution couple.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class AbstractInference implements InferenceAlgorithm {

	// logger
	public static Logger log = new Logger("AbstractInference", Logger.Level.DEBUG);


	// ===================================
	//  GENERIC METHODS
	// ===================================


	/**
	 * Queries for the probability distribution of the set of random variables in 
	 * the Bayesian network, given the provided evidence
	 * 
	 * @param query the full query
	 * @throws DialException if the inference operation failed
	 */
	@Override
	public ProbDistribution queryProb(ProbQuery query) throws DialException {

		if (!query.getConditionalVars().isEmpty()) {
			return queryConditionalProb(query);
		}

		// do a hybrid probability/utility query
		DistributionCouple couple = queryJoint(query);

		// only returns the probability distribution
		return couple.getProbDistrib();

	}


	/**
	 * Queries for the utility of a particular set of (action) variables, given the
	 * provided evidence
	 * 
	 * @param query the full query
	 * @return the utility distribution
	 * @throws DialException if the inference operation failed
	 */
	@Override
	public UtilityTable queryUtility(UtilQuery query) throws DialException {

		// do a hybrid probability/utility query
		DistributionCouple couple = queryJoint(query);

		// only returns the utility distributon
		return couple.getUtilityDistrib();
	}


	/**
	 * Computes the conditional probability P(X_1,...X_n | Y_1,...Y_n, evidence).  The algorithm
	 * functions as follows:<ol>
	 * <li> compute P(X_1,...X_n, Y_1, .... Y_n | evidence);
	 * <li> compute P(Y_1,...Y_n | evidence) by marginalisation over the above mentioned distribution
	 * <li> Use the chain rule to calculate P(X_1,...X_n | Y_1,...Y_n, evidence) = 
	 *        P(X_1,...X_n, Y_1, .... Y_n | evidence) / P(Y_1,...Y_n | evidence)
	 * </ol>
	 *
	 * @param query the full query
	 * @return the conditional probability distribution
	 * @throws DialException
	 */
	public ProbDistribution queryConditionalProb(Query query) throws DialException {

		BNetwork network = query.getNetwork();
		Collection<String> queryVars = query.getQueryVars();
		Assignment evidence = query.getEvidence();
		Collection<String> condVars = query.getConditionalVars();

		List<String> queryAndCondVars = new ArrayList<String>(queryVars.size()+condVars.size());
		queryAndCondVars.addAll(queryVars);
		queryAndCondVars.addAll(condVars);

		// 1) compute P(X_1,...X_n, Y_1, .... Y_n | evidence)
		ProbDistribution joint = queryProb(new ProbQuery(network, queryAndCondVars, evidence));

		// 2) compute P(Y_1,...Y_n | evidence) by marginalisation
		Map<Assignment,Double> marginalConds = new HashMap<Assignment,Double>();

		for (Assignment jointVal : joint.toDiscrete().getProbTable(new Assignment()).getRows()) {
			Assignment depVal = jointVal.getTrimmed(condVars);
			if (!marginalConds.containsKey(depVal)) {
				marginalConds.put(depVal, 0.0);
			}
			marginalConds.put(depVal, marginalConds.get(depVal) + joint.toDiscrete().getProb(new Assignment(), jointVal));
		}

		// 3) calculate P(X_1,...X_n | Y_1,...Y_n, evidence) with the chain rule
		DiscreteProbabilityTable table = new DiscreteProbabilityTable();
		for (Assignment jointVal : joint.toDiscrete().getProbTable(new Assignment()).getRows()) {
			Assignment depVal = jointVal.getTrimmed(condVars);
			Assignment headVal = jointVal.getTrimmed(queryVars);
			double prob = joint.toDiscrete().getProb(new Assignment(), jointVal) / marginalConds.get(depVal);

			table.addRow(depVal, headVal, prob);
		}

		return table;
	}


	// ===================================
	//  PROTECTED METHODS
	// ===================================



	/**
	 * Returns the probability and utility distributions associated with the set of query
	 * variables, given the evidence.  
	 * 
	 * @param query the full query
	 * @return the couple with the two distributions
	 * @throws DialException if the inference operation failed
	 */
	protected abstract DistributionCouple queryJoint(Query query) throws DialException;




}
