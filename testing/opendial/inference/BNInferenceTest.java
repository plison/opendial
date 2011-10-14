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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.inference.algorithms.NaiveInference;
import opendial.inference.algorithms.VariableElimination;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.inference.bn.distribs.GenericDistribution;
import opendial.inference.bn.distribs.ProbabilityTable;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNInferenceTest {

	static Logger log = new Logger("BNInferenceTest", Logger.Level.DEBUG);
	
	public static int PERCENT_COMPARISONS = 5;
	
	
	public BNetwork constructBayesianNetwork() throws DialException {
		BNetwork bn = new BNetwork();
				
		BNode b = new BNode("Burglary");
		ProbabilityTable distrib_b = new ProbabilityTable(b);
		distrib_b.addRow(new Assignment("Burglary"), 0.001f);
		b.setDistribution(distrib_b);
		bn.addNode(b);
		
		BNode e = new BNode("Earthquake");
		ProbabilityTable distrib_e = new ProbabilityTable(e);
		distrib_e.addRow(new Assignment("Earthquake"), 0.002f);
		e.setDistribution(distrib_e);
		bn.addNode(e);

		BNode a = new BNode("Alarm");
		a.addInputNode(b);
		a.addInputNode(e);
		ProbabilityTable distrib_a = new ProbabilityTable(a);
		distrib_a.addRow(new Assignment(Arrays.asList("Burglary", "Earthquake", "Alarm")), 0.95f);
		distrib_a.addRow(new Assignment(Arrays.asList("Burglary", "!Earthquake", "Alarm")), 0.94f);
		distrib_a.addRow(new Assignment(Arrays.asList("!Burglary", "Earthquake", "Alarm")), 0.29f);
		distrib_a.addRow(new Assignment(Arrays.asList("!Burglary", "!Earthquake", "Alarm")), 0.001f);
		a.setDistribution(distrib_a);
		bn.addNode(a);

		BNode mc = new BNode("MaryCalls");
		mc.addInputNode(a);
		ProbabilityTable distrib_mc = new ProbabilityTable(mc);
		distrib_mc.addRow(new Assignment(Arrays.asList("Alarm", "MaryCalls")), 0.7f);
		distrib_mc.addRow(new Assignment(Arrays.asList("!Alarm", "MaryCalls")), 0.01f);
		mc.setDistribution(distrib_mc);
		bn.addNode(mc);
		
		BNode jc = new BNode("JohnCalls");
		jc.addInputNode(a);
		ProbabilityTable distrib_jc = new ProbabilityTable(jc);
		distrib_jc.addRow(new Assignment(Arrays.asList("Alarm", "JohnCalls")), 0.9f);
		distrib_jc.addRow(new Assignment(Arrays.asList("!Alarm", "JohnCalls")), 0.05f);
		jc.setDistribution(distrib_jc);
		bn.addNode(jc);
		
		return bn;
	}
	
	
	@Test
	public void bayesianNetworkTest1() throws DialException {
		
		BNetwork bn = constructBayesianNetwork();
		
		Map<Assignment,Float> fullJoint = NaiveInference.getFullJoint(bn);

		assertEquals(0.000628f, fullJoint.get(new Assignment(
				Arrays.asList("JohnCalls", "MaryCalls", "Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		assertEquals(0.9367428f, fullJoint.get(new Assignment(
				Arrays.asList("!JohnCalls", "!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		GenericDistribution query = NaiveInference.query(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		assertEquals(0.7158281f, query.getProb(new Assignment("Burglary", Boolean.FALSE)), 0.0001f);
		assertEquals(0.28417188f, query.getProb(new Assignment("Burglary", Boolean.TRUE)), 0.0001f);
		

		GenericDistribution query2 = NaiveInference.query(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls")));
		
		assertEquals(0.62644875f, query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);

	}
	
	@Test
	public void bayesianNetworkTest2() throws DialException {
		
		BNetwork bn = constructBayesianNetwork();
		
		GenericDistribution query = VariableElimination.query(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		assertEquals(0.7158281f, query.getProb(new Assignment("Burglary", Boolean.FALSE)), 0.0001f);
		assertEquals(0.28417188f, query.getProb(new Assignment("Burglary", Boolean.TRUE)), 0.0001f);
		
		GenericDistribution query2 = VariableElimination.query(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls")));
		
		assertEquals(0.62644875f, query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);
	}


	@Test
	public void bayesianNetworkTest3() throws DialException {

		BNetwork bn = constructBayesianNetwork();

		Set<Set<String>> queryVarsPowerset = generatePowerset(bn.getNodeIds());
		List<Assignment> evidencePowerset = generateEvidencePowerset(bn);
		
		long totalTimeNaive = 0;
		long totalTimeVE = 0;
		
		for (Set<String> queryVars: queryVarsPowerset) {
			for (Assignment evidence : evidencePowerset) {

				if ((new Random()).nextFloat() < PERCENT_COMPARISONS / 100.0) {
					long timeInit = System.currentTimeMillis();
					
					GenericDistribution query1 = 
						NaiveInference.query(bn, new ArrayList<String>(queryVars), evidence);
					
					long timeQuery1 = System.currentTimeMillis();				
					totalTimeNaive += (timeQuery1 - timeInit);
					
					GenericDistribution query2 = 
						VariableElimination.query(bn, new ArrayList<String>(queryVars), evidence);
					
					long timeQuery2 = System.currentTimeMillis();
					totalTimeVE += (timeQuery2 - timeQuery1);
					
					for (Assignment value : query1.getTable()) {
						if (!query2.hasProb(value)) {
							log.debug("------");
							log.debug("Query: P(" + queryVars + "| " + evidence + ")");
							log.debug("value not provided by VE: " + value);
							log.debug("Query result 1: " + query1);
							log.debug("Query result 2: " + query2);
							throw new DialException("distribution does not contained query value: " + value);
						}
						else {
							assertEquals(query1.getProb(value), query2.getProb(value), 0.0001f);
						}
					}				
				}

			}
		}
		log.info("Total number of performed inferences: 2 x " + (queryVarsPowerset.size()*evidencePowerset.size()));
		log.info("Total time for naive inference: " + (totalTimeNaive / 1000.0) + " s.");
		log.info("Total time for variable elimination: " + (totalTimeVE / 1000.0) + " s.");

	}
	
	
	public static <T> Set<Set<T>> generatePowerset(Set<T> fullSet) {
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
	
	
	public List<Assignment> generateEvidencePowerset(BNetwork bn) {
		List<Assignment> allAssignments = new LinkedList<Assignment>();
		
		Map<Assignment,Float> fullJoint = NaiveInference.getFullJoint(bn);
		for (Assignment a: fullJoint.keySet()) {
			Set<Set<Entry<String,Object>>> partialAssigns = generatePowerset(a.getPairs().entrySet());
			for (Set<Entry<String,Object>> partial: partialAssigns) {
				Assignment p = new Assignment(partial);
				allAssignments.add(p);
			}
		}
		return allAssignments;
	}
}
