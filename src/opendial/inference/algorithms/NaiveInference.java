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

package opendial.inference.algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.inference.distribs.Distribution;
import opendial.inference.distribs.ProbabilityTable;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * Algorithm for naive probabilistic inference, based on computing the full
 * joint distribution, and then summing everything.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaiveInference {

	static Logger log = new Logger("NaiveInference", Logger.Level.DEBUG);
	

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
	public static Distribution query
		(BNetwork bn, List<String> queryVars, Assignment evidence) throws DialException {
		
		Map<Assignment, Float> fullJoint = getFullJoint(bn);
		SortedMap<String,Set<Object>> queryValues = new TreeMap<String,Set<Object>>();
		for (BNode n : bn.getNodes()) {
			if (queryVars.contains(n.getId())) {
				queryValues.put(n.getId(), n.getValues());
			}
		}
		List<Assignment> queryAssigns = InferenceUtils.getAllCombinations(queryValues);
		Map<Assignment, Float> queryResult = new HashMap<Assignment,Float>();
		
		for (Assignment queryA : queryAssigns) {
			float sum = 0.0f;
			for (Assignment a: fullJoint.keySet()) {
				if (a.contains(queryA) && a.contains(evidence)) {
					sum += fullJoint.get(a);
				}
			}
			queryResult.put(queryA, sum);
		}
		
		queryResult = InferenceUtils.normalise(queryResult);
		
		Distribution distrib = new ProbabilityTable(queryResult);
		
		return distrib;
	}
	
	
	
	public static Map<Assignment, Float> getFullJoint(BNetwork bn) {
		
		SortedMap<String,Set<Object>> allValues = new TreeMap<String,Set<Object>>();
		for (BNode n : bn.getNodes()) {
			allValues.put(n.getId(), n.getValues());
		}
		List<Assignment> fullAssigns = InferenceUtils.getAllCombinations(allValues);

		Map<Assignment,Float> result = new HashMap<Assignment, Float>();
		for (Assignment singleAssign : fullAssigns) {
			
			double jointLogProb = 0.0f;
			for (BNode n: bn.getNodes()) {
				Assignment trimedAssign = singleAssign.getTrimmed(n.getVariables());
				jointLogProb += Math.log(n.getProb(trimedAssign));
			}
			result.put(singleAssign, (float)Math.exp(jointLogProb));
		}
		
		return result;
	}
	
	
}
