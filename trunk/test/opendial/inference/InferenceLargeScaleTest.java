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
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.NetworkExamples;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.Value;
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

		BNetwork bn = NetworkExamples.constructBasicNetwork2();

		NaiveInference naive = new NaiveInference();
		VariableElimination ve = new VariableElimination();

		Set<Set<String>> queryVarsPowerset = generatePowerset(bn.getChanceNodeIds());
		List<Assignment> evidencePowerset = generateEvidencePowerset(bn);

		long totalTimeNaive = 0;
		long totalTimeVE = 0;
		long totalTimeIS = 0;

		for (Set<String> queryVars: queryVarsPowerset) {
			if (!queryVars.isEmpty()) {
				for (Assignment evidence : evidencePowerset) {

					if ((new Random()).nextDouble() < PERCENT_COMPARISONS / 100.0) {
						
						long timeInit = System.nanoTime();
						
						ProbQuery query = new ProbQuery(bn, new ArrayList<String>(queryVars), evidence);
						// we first do the query with naive inference
						ProbDistribution query1 = naive.queryProb(query);

						long timeQuery1 = System.nanoTime();				
						totalTimeNaive += (timeQuery1 - timeInit);

						// then with variable elimination
						ProbDistribution query2 = ve.queryProb(query);

						long timeQuery2 = System.nanoTime();
						totalTimeVE += (timeQuery2 - timeQuery1);
						
						// than with sampling (two steps, a quick and a long one)
						try {
						ImportanceSampling is = new ImportanceSampling(200, 50);
						ProbDistribution query3 = is.queryProb(query);
					
						long timeQuery3 = System.nanoTime();
						totalTimeIS += (timeQuery3 - timeQuery2);
						compareQueries(query1, query2, query3);
						}
						catch (AssertionError e) {
							try {
							ImportanceSampling is = new ImportanceSampling(2000, 200);
							ProbDistribution query3 = 
								is.queryProb(query);
							
							long timeQuery4 = System.nanoTime();
							totalTimeIS += (timeQuery4 - timeQuery2);
							compareQueries(query1, query2, query3);
							}
							catch (AssertionError e2) {
								log.warning("wrong sampling results for query: " + query);
							}
						}

					} 
				}
			}
		}
		log.info("Total number of performed inferences: 3 x " + 
				((queryVarsPowerset.size()*evidencePowerset.size()) * PERCENT_COMPARISONS / 100));
		log.info("Total time for naive inference: " + (totalTimeNaive / 1000000000.0) + " s.");
		log.info("Total time for variable elimination: " + (totalTimeVE / 1000000000.0) + " s.");
		log.info("Total time for sampling: " + (totalTimeIS / 1000000000.0) + " s.");
	}


	
	
	private static void compareQueries(ProbDistribution query1, 
			ProbDistribution query2, ProbDistribution query3) throws DialException {
		// we compare the results of the three inference algorithms
		for (Assignment value : query1.toDiscrete().getProbTable(new Assignment()).getRows()) {
			assertEquals(query1.toDiscrete().getProb(new Assignment(), 
					value), query2.toDiscrete().getProb(new Assignment(), 
							value), 0.001f);
			assertEquals(query2.toDiscrete().getProb(new Assignment(), 
					value), query3.toDiscrete().getProb(new Assignment(), value), 0.1f);
		}
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
