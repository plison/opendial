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

package opendial.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.util.Log;
import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.values.Value;
import opendial.inference.ImportanceSampling;
import opendial.inference.NaiveInference;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.UtilQuery;

public class InferenceTesting {


	// logger
	public static Logger log = new Logger("InferenceTesting",
			Logger.Level.DEBUG);
	
	
	static VariableElimination ve;
	static ImportanceSampling is;
	static ImportanceSampling is2;
	static NaiveInference naive;

	public static boolean INCLUDE_NAIVE = false;


	static Map<Query, List<DiscreteProbDistribution>> probQueryCache;
	static Map<Query, List<UtilityDistribution>> utilQueryCache;

	static {

		ve = new VariableElimination();
		is = new ImportanceSampling(600, 200);
		is2 = new ImportanceSampling(2000, 400);
		naive = new NaiveInference();
		probQueryCache = new HashMap<Query, List<DiscreteProbDistribution>>();
		utilQueryCache = new HashMap<Query, List<UtilityDistribution>>();
	}


	public static void checkProb (Query query, Map<Assignment, Double> expected) throws DialException {

		if (query instanceof ProbQuery) {
			ProbDistribution distrib1 = ve.queryProb((ProbQuery)query);
			ProbDistribution distrib2 = is.queryProb((ProbQuery)query);


			for (Assignment a : expected.keySet()) {
				assertEquals(expected.get(a).doubleValue(), 
						distrib1.toDiscrete().getProb(new Assignment(), a), 0.01);
				assertEquals(expected.get(a).doubleValue(), 
						distrib2.toDiscrete().getProb(new Assignment(), a), 0.08);
			}

			if (INCLUDE_NAIVE) {
				ProbDistribution distrib3 = naive.queryProb((ProbQuery)query);

				for (Assignment a : expected.keySet()) {
					assertEquals(expected.get(a).doubleValue(), 
							distrib3.toDiscrete().getProb(new Assignment(), a), 0.01);
				}
			}
		}
	}


	public static void checkProb (ProbQuery query, Assignment a, double expected) throws DialException {

			DiscreteProbDistribution distrib1 = null;
			DiscreteProbDistribution distrib2 = null;
			if (!probQueryCache.containsKey(query)) {
				distrib1 = ve.queryProb((ProbQuery)query).toDiscrete();
				distrib2 = is.queryProb((ProbQuery)query).toDiscrete();
				probQueryCache.put(query, Arrays.asList(distrib1, distrib2));
			}
			else {
				distrib1 = probQueryCache.get(query).get(0);
				distrib2 = probQueryCache.get(query).get(1);
			}

			assertEquals(expected, distrib1.getProb(new Assignment(), a), 0.01);
		
			try { assertEquals(expected, distrib2.getProb(new Assignment(), a), 0.05);	}
			catch (AssertionError e) {
				distrib2 = is2.queryProb((ProbQuery)query).toDiscrete();
				assertEquals(expected, distrib2.getProb(new Assignment(), a), 0.05);
				probQueryCache.put(query, Arrays.asList(distrib1, distrib2));
			}

			if (INCLUDE_NAIVE) {
				DiscreteProbDistribution distrib3 = null;
				if (probQueryCache.get(query).size() == 2) {
					distrib3 = naive.queryProb((ProbQuery)query).toDiscrete();	
					List<DiscreteProbDistribution> distribs = probQueryCache.get(query);
					distribs.add(distrib3);
					probQueryCache.put(query, distribs);
				}
				else if (probQueryCache.get(query).size() == 3) {
					distrib3 = probQueryCache.get(query).get(2);
				}
				assertEquals(expected, distrib3.toDiscrete().getProb(new Assignment(), a), 0.01);
			}
	}


	public static void checkUtil(UtilQuery query, Assignment a, double expected) throws DialException {
			UtilityDistribution distrib1 = null;
			UtilityDistribution distrib2 = null;
			if (!utilQueryCache.containsKey(query)) {
				distrib1 = ve.queryUtility((UtilQuery)query);
				distrib2 = is.queryUtility((UtilQuery)query);
				utilQueryCache.put(query, Arrays.asList(distrib1, distrib2));
			}
			else {
				distrib1 = utilQueryCache.get(query).get(0);
				distrib2 = utilQueryCache.get(query).get(1);
			}

			assertEquals(expected, distrib1.getUtility(a), 0.01);
			try { assertEquals(expected, distrib2.getUtility(a), 0.5);	}
			catch (AssertionError e) {
				distrib2 = is2.queryUtility(query);
				assertEquals(expected, distrib2.getUtility(a), 0.5);
				utilQueryCache.put(query, Arrays.asList(distrib1, distrib2));
			}

			if (INCLUDE_NAIVE) {
				UtilityDistribution distrib3 = null;
				if (utilQueryCache.get(query).size() == 2) {
					distrib3 = naive.queryUtility((UtilQuery)query);	
					List<UtilityDistribution> distribs = utilQueryCache.get(query);
					distribs.add(distrib3);
					utilQueryCache.put(query, distribs);
				}
				else if (utilQueryCache.get(query).size() == 3) {
					distrib3 = utilQueryCache.get(query).get(2);
				}
				assertEquals(expected, distrib3.getUtility(a), 0.01);
			}
		}
}
