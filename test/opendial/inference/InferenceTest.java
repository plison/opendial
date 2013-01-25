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
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.common.NetworkExamples;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:14:37 #$
 *
 */
public class InferenceTest {

	// logger
	public static Logger log = new Logger("InferenceTest", Logger.Level.DEBUG);
		
	
	public static void main(String[] args) throws DialException {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		log.info((new VariableElimination()).queryProb(new ProbQuery(bn, 
				Arrays.asList("Burglary"), new Assignment(new Assignment
						("JohnCalls", true), new Assignment("MaryCalls", false)))));
	}
	
	
	@Test
	public void bayesianNetworkTest1() throws DialException {
		
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		Map<Assignment,Double> fullJoint = NaiveInference.getFullJoint(bn, false);

		assertEquals(0.000628f, fullJoint.get(new Assignment(
				Arrays.asList("JohnCalls", "MaryCalls", "Alarm", "!Burglary",
						"!Earthquake"))), 0.000001f);
		
		assertEquals(0.9367428f, fullJoint.get(new Assignment(
				Arrays.asList("!JohnCalls", "!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		NaiveInference naive = new NaiveInference();
		
		ProbDistribution query = naive.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.71367f, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.0001f);
		assertEquals(0.286323, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", true)), 0.0001f);
		

		ProbDistribution query2 = naive.queryProb(new ProbQuery(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls"))));
		
		assertEquals(0.623974, query2.toDiscrete().getProb(new Assignment(), 
				new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);

	} 
	
	
	@Test
	public void bayesianNetworkTest1bis() throws DialException {
		
		BNetwork bn = NetworkExamples.constructBasicNetwork2();
		Map<Assignment,Double> fullJoint = NaiveInference.getFullJoint(bn, false);

		
		assertEquals(0.000453599f, fullJoint.get(new Assignment(
				Arrays.asList("JohnCalls", "MaryCalls", "Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		assertEquals(0.6764828f, fullJoint.get(new Assignment(
				Arrays.asList("!JohnCalls", "!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		NaiveInference naive = new NaiveInference();
		
		ProbDistribution query = naive.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.360657, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.0001f);
		assertEquals(0.639343, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", true)), 0.0001f);
		

		ProbDistribution query2 = naive.queryProb(new ProbQuery(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls"))));
		
		assertEquals(0.3577609, query2.toDiscrete().getProb(new Assignment(), 
				new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);

	} 
	
	@Test
	public void bayesianNetworkTest2() throws DialException {
		
		VariableElimination ve = new VariableElimination();
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		
		ProbDistribution distrib = ve.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.713676, distrib.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.0001f);
		assertEquals(0.286323, distrib.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", true)), 0.0001f);
		
		ProbDistribution query2 = ve.queryProb(new ProbQuery(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls"))));
		
		assertEquals(0.623974, query2.toDiscrete().getProb(new Assignment(), 
				new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);
	}

	
	@Test
	public void bayesianNetworkTest2bis() throws DialException {
		
		VariableElimination ve = new VariableElimination();
		BNetwork bn = NetworkExamples.constructBasicNetwork2();
		
		ProbDistribution query = ve.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.360657, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.0001f);
		assertEquals(0.63934, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", true)), 0.0001f);
		
		ProbDistribution query2 = ve.queryProb(new ProbQuery(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls"))));
		
		assertEquals(0.3577609, query2.toDiscrete().getProb(new Assignment(), 
				new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);
	}
	

	@Test
	public void bayesianNetworkTest3bis() throws DialException {
		
		ImportanceSampling is = new ImportanceSampling(4000, 300);
		BNetwork bn = NetworkExamples.constructBasicNetwork2();
		
		ProbDistribution query = is.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.362607f, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.05f);
		assertEquals(0.637392, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", true)), 0.05f);
		
		ProbDistribution query2 = is.queryProb(new ProbQuery(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls"))));
		
		assertEquals(0.35970f, query2.toDiscrete().getProb(new Assignment(), 
				new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.05f);
	}

	
	@Test
	public void conditionalProbsTest() throws DialException {
		
		BNetwork bn2 = NetworkExamples.constructBasicNetwork2();
		NaiveInference naive = new NaiveInference();
		ProbQuery query = new ProbQuery(bn2, Arrays.asList("MaryCalls", "JohnCalls"),
				new Assignment(), Arrays.asList("Burglary"));
		
		ProbDistribution distrib1 = naive.queryProb(query);
		
		VariableElimination ve = new VariableElimination();
		ProbDistribution distrib2 = ve.queryProb(query);
		
		assertEquals(distrib1.toDiscrete().getProb(new Assignment("Burglary"), 
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls"))), 
				distrib2.toDiscrete().getProb(new Assignment("Burglary"), 
						new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls"))), 0.001);
		
		ProbDistribution distrib3 = new ImportanceSampling(4000, 300).queryProb(query);
		
		assertEquals(distrib3.toDiscrete().getProb(new Assignment("Burglary"), 
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls"))), 
				distrib2.toDiscrete().getProb(new Assignment("Burglary"), 
						new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls"))), 0.1);
	}
	
	
	@Test
	public void utilityTest() throws DialException {
		BNetwork network = NetworkExamples.constructBasicNetwork2();

		VariableElimination ve = new VariableElimination();
		NaiveInference naive = new NaiveInference();
		ImportanceSampling is = new ImportanceSampling(3000, 300);
		UtilQuery query1 = new UtilQuery(network, Arrays.asList("Action"),
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls")));

		assertEquals(-0.680, ve.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, naive.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, is.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.5);
		assertEquals(-6.213, ve.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, naive.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, is.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 1.0);
		
		UtilQuery query2 = new UtilQuery(network, Arrays.asList("Burglary"),
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls")));
		
		assertEquals(-0.1667, ve.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.1667, naive.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.25, is.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.5);
		assertEquals(-3.5, ve.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.001);
		assertEquals(-3.5, naive.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.001);
		assertEquals(-3.5, is.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.8);
	
	}
	
	
	@Test
	public void switchingTest() throws DialException {
		
		BNetwork network = NetworkExamples.constructBasicNetwork2();

		ProbQuery query = new ProbQuery(network, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		ProbDistribution distrib = (new SwitchingAlgorithm()).queryProb(query);
		assertTrue(distrib instanceof SimpleTable);
		
		ChanceNode n1 = new ChanceNode("n1");
		n1.addProb(ValueFactory.create("aha"), 1.0);
		network.addNode(n1);
		ChanceNode n2 = new ChanceNode("n2");
		n2.addProb(ValueFactory.create("oho"), 0.7);
		network.addNode(n2);
		network.getNode("Alarm").addInputNode(n1);
		network.getNode("Alarm").addInputNode(n2);
		
		distrib = (new SwitchingAlgorithm()).queryProb(query);
		assertTrue(distrib instanceof SimpleEmpiricalDistribution); 
		
		network.removeNode(n1.getId());
		network.removeNode(n2.getId());
		
		distrib = (new SwitchingAlgorithm()).queryProb(query);
		assertTrue(distrib instanceof SimpleTable);

		n1 = new ChanceNode("n1");
		n1.setDistrib(new FunctionBasedDistribution("n1", new UniformDensityFunction(-2, 2)));
		n2 = new ChanceNode("n2");
		n2.setDistrib(new FunctionBasedDistribution("n2", new GaussianDensityFunction(-1, 3)));
		network.addNode(n1);
		network.addNode(n2);
		network.getNode("Earthquake").addInputNode(n1);
		network.getNode("Earthquake").addInputNode(n2);
		
		distrib = (new SwitchingAlgorithm()).queryProb(query);
		assertTrue(distrib instanceof SimpleEmpiricalDistribution); 

	}
	
	@Test
	public void specialUtilQueryTest() throws DialException {
		
		BNetwork network = new BNetwork();
		ChanceNode n = new ChanceNode("s");
		n.addProb(ValueFactory.create("val1"), 0.3);
		n.addProb(ValueFactory.create("val2"), 0.7);
		network.addNode(n);
		
		UtilityNode u = new UtilityNode("u");
		u.addUtility(new Assignment("s", "val1"), +2);
		u.addUtility(new Assignment("s", "val2"), -1);
		u.addInputNode(n);
		network.addNode(u);
		
		UtilQuery query = new UtilQuery(network, new LinkedList<String>());
		InferenceChecks inference = new InferenceChecks();
		inference.checkUtil(query, new Assignment(), -0.1);
		
	}
	
}
