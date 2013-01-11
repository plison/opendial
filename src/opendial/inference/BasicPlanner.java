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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.values.Value;
import opendial.inference.queries.UtilQuery;
import opendial.utils.CombinatoricsUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicPlanner {

	// logger
	public static Logger log = new Logger("BasicPlanner", Logger.Level.NORMAL);
	

	InferenceAlgorithm algo;
	
	public BasicPlanner() {
		algo = new ImportanceSampling(200, 500);
	}
	
	public BasicPlanner(InferenceAlgorithm algo) {
		this.algo = algo;
	}
	
	
	/**
	 *
	 * @param network
	 * @param evidence
	 * @return
	 * @throws DialException 
	 */
	public Assignment getOptimalActions(BNetwork network, Assignment evidence) throws DialException {

		UtilityDistribution distrib = algo.queryUtility(new UtilQuery(network, network.getActionNodeIds(), evidence));

		Assignment bestAssign = new Assignment();
		double bestValue= - Float.MAX_VALUE;

		Map<String,Set<Value>> actionValues = new HashMap<String,Set<Value>>();
		for (String actionVar : network.getActionNodeIds()) {
			actionValues.put(actionVar, network.getActionNode(actionVar).getValues());
		}
		Set<Assignment> combinations = CombinatoricsUtils.getAllCombinations(actionValues);
		for (Assignment a : combinations) {
			if (distrib.getUtility(a) > bestValue) {
				bestAssign = a;
				bestValue = distrib.getUtility(a);
			}
		}
		return bestAssign;
	}


}
