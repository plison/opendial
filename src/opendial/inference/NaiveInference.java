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
import opendial.bn.distribs.ProbabilityTable;
import opendial.bn.nodes.ChanceNode;
import opendial.utils.InferenceUtils;

/**
 * Algorithm for naive probabilistic inference, based on computing the full
 * joint distribution, and then summing everything.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaiveInference {

	public static Logger log = new Logger("NaiveInference", Logger.Level.DEBUG);
	

	/**
	 * Queries the probability distribution encoded in the Bayesian Network, given
	 * a set of query variables, and some evidence.
	 * 
	 * @param bn the Bayesian Network
	 * @param queryVars the query variables
	 * @param evidence the evidence
	 * @return the resulting probability distribution
	 * @throws DialException 
	 */
	public static ProbDistribution query
		(BNetwork bn, Collection<String> queryVars, Assignment evidence) throws DialException {
		
	//	log.debug("query var: " + queryVars + " and evidence: " + evidence);
		
		// generates the full joint distribution
		Map<Assignment, Float> fullJoint = getFullJoint(bn);
		
		// generates all possible value assignments for the query variables
		SortedMap<String,Set<Object>> queryValues = new TreeMap<String,Set<Object>>();
		for (ChanceNode n : bn.getChanceNodes()) {
			if (queryVars.contains(n.getId())) {
				queryValues.put(n.getId(), n.getValues());
			}
		}
		List<Assignment> queryAssigns = InferenceUtils.getAllCombinations(queryValues);
		Map<Assignment, Float> queryResult = new HashMap<Assignment,Float>();
		
		// calculate the (unnormalised) probability for each assignment of the query variables
		for (Assignment queryA : queryAssigns) {
			float sum = 0.0f;
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
		ProbabilityTable distrib = new ProbabilityTable();
		distrib.addRows(new Assignment(), queryResult);
		
		return distrib;
	}
	
	
	/**
	 * Computes the full joint probability distribution for the Bayesian Network
	 * 
	 * @param bn the Bayesian network
	 * @return the resulting joint distribution
	 */
	public static Map<Assignment, Float> getFullJoint(BNetwork bn) {
		
		SortedMap<String,Set<Object>> allValues = new TreeMap<String,Set<Object>>();
		for (ChanceNode n : bn.getChanceNodes()) {
			allValues.put(n.getId(), ((ChanceNode)n).getValues());
		}
		List<Assignment> fullAssigns = InferenceUtils.getAllCombinations(allValues);

		Map<Assignment,Float> result = new HashMap<Assignment, Float>();
		for (Assignment singleAssign : fullAssigns) {
			
			double jointLogProb = 0.0f;
			for (ChanceNode n: bn.getChanceNodes()) {
				Assignment trimedCondition = singleAssign.getTrimmed(n.getInputNodesIds());
				jointLogProb += Math.log(n.getProb(trimedCondition, singleAssign.getValue(n.getId())));
			}
			result.put(singleAssign, (float)Math.exp(jointLogProb));
		}
		
		return result;
	}
	
	
}
