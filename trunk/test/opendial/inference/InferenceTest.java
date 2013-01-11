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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.NetworkExamples;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.values.Value;
import opendial.gui.GUIFrame;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.ReductionQuery;
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
	public static Logger log = new Logger("BNetworkInferenceTest",
			Logger.Level.DEBUG);
		
	
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
		
		ProbDistribution query = ve.queryProb(new ProbQuery(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		
		assertEquals(0.713676, query.toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary", false)), 0.0001f);
		assertEquals(0.286323, query.toDiscrete().getProb(new Assignment(), 
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
		
		ImportanceSampling is = new ImportanceSampling(6000, 200);
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
		
		ProbDistribution distrib3 = new ImportanceSampling(6000, 200).queryProb(query);
		
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
		ImportanceSampling is = new ImportanceSampling(5000, 400);
		UtilQuery query1 = new UtilQuery(network, Arrays.asList("Action"),
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls")));

		assertEquals(-0.680, ve.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, naive.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, is.queryUtility(query1).getUtility(new Assignment("Action", "CallPolice")), 0.5);
		assertEquals(-6.213, ve.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, naive.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, is.queryUtility(query1).getUtility(new Assignment("Action", "DoNothing")), 0.8);
		
		UtilQuery query2 = new UtilQuery(network, Arrays.asList("Burglary"),
				new Assignment(new Assignment("JohnCalls"), new Assignment("MaryCalls")));
		
		assertEquals(-0.25, ve.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.25, naive.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.25, is.queryUtility(query2).getUtility(new Assignment("!Burglary")), 0.5);
		assertEquals(-5.25, ve.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.001);
		assertEquals(-5.25, naive.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.001);
		assertEquals(-5.25, is.queryUtility(query2).getUtility(new Assignment("Burglary")), 0.8);
	
	}
	
}
