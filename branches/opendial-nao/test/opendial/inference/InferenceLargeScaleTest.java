// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.Value;
import opendial.common.InferenceChecks;
import opendial.common.NetworkExamples;
import opendial.datastructs.Assignment;
import opendial.inference.exact.NaiveInference;
import opendial.inference.queries.ProbQuery;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class InferenceLargeScaleTest {

	// logger
	public static Logger log = new Logger("InferenceLargeScaleTest", Logger.Level.NORMAL);

	public static int PERCENT_COMPARISONS = 1;


	@Test
	public void bayesianNetworkTest4() throws DialException {

		InferenceChecks inference = new InferenceChecks();
		inference.includeNaive(true);
		
		BNetwork bn = NetworkExamples.constructBasicNetwork2();

		Set<Set<String>> queryVarsPowerset = generatePowerset(bn.getChanceNodeIds());
		List<Assignment> evidencePowerset = generateEvidencePowerset(bn);

		int nbErrors = 0;
		for (Set<String> queryVars: queryVarsPowerset) {
			if (!queryVars.isEmpty()) {
				for (Assignment evidence : evidencePowerset) {

					if ((new Random()).nextDouble() < PERCENT_COMPARISONS / 100.0) {
												
						ProbQuery query = new ProbQuery(bn, new ArrayList<String>(queryVars), evidence);
											
						try {inference.checkProb(query);}
						catch (AssertionError e) {
							nbErrors++;
							if (nbErrors > 2) {
								throw new AssertionError ("more than 2 sampling errors");
							}
						}
					} 
				}
			}
		}
		inference.showPerformance();
	}



	
	private static <T> Set<Set<T>> generatePowerset(Set<T> fullSet) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		if (fullSet.isEmpty()) {
			sets.add(new HashSet<T>());
			return sets;
		}
		List<T> list = new ArrayList<T>(fullSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
		for (Set<T> set : generatePowerset(rest)) {
			Set<T> newSet = new HashSet<T>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}           
		return sets;
	}


	private List<Assignment> generateEvidencePowerset(BNetwork bn) {
		List<Assignment> allAssignments = new LinkedList<Assignment>();

		Map<Assignment,Double> fullJoint = NaiveInference.getFullJoint(bn, false);
		for (Assignment a: fullJoint.keySet()) {
			Set<Set<Entry<String,Value>>> partialAssigns = generatePowerset(a.getPairs().entrySet());
			for (Set<Entry<String,Value>> partial: partialAssigns) {
				Assignment p = new Assignment(partial);
				allAssignments.add(p);
			}
		}
		return allAssignments;
	}

}
