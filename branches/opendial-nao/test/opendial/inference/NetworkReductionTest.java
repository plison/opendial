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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.common.NetworkExamples;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.NaiveInference;
import opendial.inference.exact.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NetworkReductionTest {

	// logger
	public static Logger log = new Logger("PruningTest", Logger.Level.DEBUG);

	BNetwork network;

	VariableElimination ve;
	LikelihoodWeighting is;
	NaiveInference naive;
	SwitchingAlgorithm sw;

	public NetworkReductionTest() throws DialException {
		network = NetworkExamples.constructBasicNetwork2();

		ve = new VariableElimination();
		is = new LikelihoodWeighting(5000, 200);
		naive = new NaiveInference();
		sw = new SwitchingAlgorithm();
	}


	@Test
	public void test1() throws DialException, InterruptedException {
		
		ReductionQuery redQuery = new ReductionQuery(network, "Burglary", "Earthquake", "MaryCalls");
		
		BNetwork reducedNet = ve.reduce(redQuery);
		BNetwork reducedNet2 = naive.reduce(redQuery);
		BNetwork reducedNet3 = is.reduce(redQuery);
		BNetwork reducedNet4 = sw.reduce(redQuery);

		assertEquals(3, reducedNet.getNodes().size());
		assertEquals(3, reducedNet2.getNodes().size());
		assertEquals(3, reducedNet3.getNodes().size());
		assertEquals(3, reducedNet4.getNodes().size());

		ProbQuery query1 = new ProbQuery(network, Arrays.asList("MaryCalls"),
				new Assignment("Burglary"));
		ProbQuery query2 = new ProbQuery(reducedNet, Arrays.asList("MaryCalls"),
				new Assignment("Burglary"));
		ProbQuery query3 = new ProbQuery(reducedNet2, Arrays.asList("MaryCalls"),
				new Assignment("Burglary"));
		ProbQuery query4 = new ProbQuery(reducedNet3, Arrays.asList("MaryCalls"),
				new Assignment("Burglary"));
		ProbQuery query4b = new ProbQuery(reducedNet4, Arrays.asList("MaryCalls"),
				new Assignment("Burglary"));

		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.0001);
		
		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				ve.queryProb(query3).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.0001);
		
		
		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				is.queryProb(query4).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.1);
		
		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				is.queryProb(query4b).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.1);


		ProbQuery query5 = new ProbQuery(network, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));
		ProbQuery query6 = new ProbQuery(reducedNet, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));
		ProbQuery query7 = new ProbQuery(reducedNet2, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));
		ProbQuery query8 = new ProbQuery(reducedNet3, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));
		ProbQuery query8b = new ProbQuery(reducedNet4, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));

		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.0001);
		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				ve.queryProb(query7).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.0001);
		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				is.queryProb(query8).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.05);
		
		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				is.queryProb(query8b).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.05);

	}



	@Test
	public void test2() throws DialException, InterruptedException {

		ReductionQuery redQuery = new ReductionQuery(network, 
				Arrays.asList("Burglary", "MaryCalls"), new Assignment("!Earthquake"));
		BNetwork reducedNet = ve.reduce(redQuery);
		BNetwork reducedNet2 = naive.reduce(redQuery);
		BNetwork reducedNet3 = is.reduce(redQuery);
		BNetwork reducedNet4 = sw.reduce(redQuery);

		//	GUIFrame.getSingletonInstance().recordState(new DialogueState(network), "original");
		//	GUIFrame.getSingletonInstance().recordState(new DialogueState(reducedNet), "reducedNet");
		//	Thread.sleep(30000);

		assertEquals(2, reducedNet.getNodes().size());
		assertEquals(2, reducedNet2.getNodes().size());
		assertEquals(2, reducedNet3.getNodes().size());
		assertEquals(2, reducedNet4.getNodes().size());

		ProbQuery query1 = new ProbQuery(network, Arrays.asList("MaryCalls"),
				new Assignment("!Earthquake"));
		ProbQuery query2 = new ProbQuery(reducedNet, Arrays.asList("MaryCalls"));
		ProbQuery query3 = new ProbQuery(reducedNet2, Arrays.asList("MaryCalls"));
		ProbQuery query4 = new ProbQuery(reducedNet3, Arrays.asList("MaryCalls"));
		ProbQuery query4b = new ProbQuery(reducedNet4, Arrays.asList("MaryCalls"));

		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.0001);
		assertEquals(ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				naive.queryProb(query3).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.0001);
		assertEquals(ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				is.queryProb(query4).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.05);
		
		assertEquals(ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
				new Assignment("MaryCalls")), 
				is.queryProb(query4b).toDiscrete().getProb(new Assignment(), 
						new Assignment("MaryCalls")), 0.05);


		ProbQuery query5 = new ProbQuery(network, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("!MaryCalls", "!Earthquake")));
		ProbQuery query6 = new ProbQuery(reducedNet, Arrays.asList("Burglary"),
				new Assignment("!MaryCalls"));
		ProbQuery query7 = new ProbQuery(reducedNet2, Arrays.asList("Burglary"),
				new Assignment("!MaryCalls"));
		ProbQuery query8 = new ProbQuery(reducedNet3, Arrays.asList("Burglary"),
				new Assignment("!MaryCalls"));
		ProbQuery query8b = new ProbQuery(reducedNet4, Arrays.asList("Burglary"),
				new Assignment("!MaryCalls"));

		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.0001);
		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				naive.queryProb(query7).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.0001);
		assertEquals(ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				is.queryProb(query8).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.05);

		assertEquals(ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				is.queryProb(query8b).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.05);
	}

	@Test
	public void test3() throws DialException, InterruptedException {

		ReductionQuery redQuery = new ReductionQuery(network, 
				Arrays.asList("Burglary", "Earthquake"), new Assignment("JohnCalls"));
		BNetwork reducedNet = ve.reduce(redQuery);
		BNetwork reducedNet2 = naive.reduce(redQuery);
		BNetwork reducedNet3 = is.reduce(redQuery);
		BNetwork reducedNet4 = sw.reduce(redQuery);

		assertEquals(2, reducedNet.getNodes().size());
		assertEquals(2, reducedNet2.getNodes().size());
		assertEquals(2, reducedNet3.getNodes().size());
		assertEquals(2, reducedNet4.getNodes().size());

		//	GUIFrame.getSingletonInstance().recordState(new DialogueState(network), "original");
		//	GUIFrame.getSingletonInstance().recordState(new DialogueState(reducedNet), "reducedNet");
		//	Thread.sleep(30000);

		ProbQuery query1 = new ProbQuery(network, Arrays.asList("Burglary"),
				new Assignment("JohnCalls"));
		ProbQuery query2 = new ProbQuery(reducedNet, Arrays.asList("Burglary"));
		ProbQuery query3 = new ProbQuery(reducedNet2, Arrays.asList("Burglary"));
		ProbQuery query4 = new ProbQuery(reducedNet3, Arrays.asList("Burglary"));
		ProbQuery query4b = new ProbQuery(reducedNet4, Arrays.asList("Burglary"));

		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				is.queryProb(query2).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.1);
		assertEquals(ve.queryProb(query1).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				naive.queryProb(query3).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.0001);
		assertEquals(ve.queryProb(query2).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				naive.queryProb(query4).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.05);
		assertEquals(ve.queryProb(query3).toDiscrete().getProb(new Assignment(), 
				new Assignment("Burglary")), 
				naive.queryProb(query4b).toDiscrete().getProb(new Assignment(), 
						new Assignment("Burglary")), 0.05);

		ProbQuery query5 = new ProbQuery(network, Arrays.asList("Earthquake"),
				new Assignment(Arrays.asList("JohnCalls")));
		ProbQuery query6 = new ProbQuery(reducedNet, Arrays.asList("Earthquake"));
		ProbQuery query7 = new ProbQuery(reducedNet2, Arrays.asList("Earthquake"));
		ProbQuery query8 = new ProbQuery(reducedNet3, Arrays.asList("Earthquake"));
		ProbQuery query8b = new ProbQuery(reducedNet4, Arrays.asList("Earthquake"));

		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.0001);	
		assertEquals(ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				is.queryProb(query7).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.05);
		assertEquals(ve.queryProb(query5).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				naive.queryProb(query8).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.05);
		assertEquals(ve.queryProb(query6).toDiscrete().getProb(new Assignment(), 
				new Assignment("Earthquake")), 
				naive.queryProb(query8b).toDiscrete().getProb(new Assignment(), 
						new Assignment("Earthquake")), 0.05);

	}

	
	@Test
	public void test5() throws DialException {
		ReductionQuery redQuery = new ReductionQuery(network, 
				Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		BNetwork reducedNet = ve.reduce(redQuery);
		BNetwork reducedNet2 = is.reduce(redQuery);
		
		reducedNet.addNode(network.getNode("Action").copy());
		reducedNet.addNode(network.getNode("Util1").copy());
		reducedNet.addNode(network.getNode("Util2").copy());
		reducedNet.getNode("Util1").addInputNode(reducedNet.getNode("Burglary"));
		reducedNet.getNode("Util1").addInputNode(reducedNet.getNode("Action"));
		reducedNet.getNode("Util2").addInputNode(reducedNet.getNode("Burglary"));
		reducedNet.getNode("Util2").addInputNode(reducedNet.getNode("Action"));
		
		UtilQuery query = new UtilQuery(reducedNet, "Action");
		UtilQuery query2 = new UtilQuery(network, Arrays.asList("Action"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		UtilityTable table1 = ve.queryUtil(query);
		UtilityTable table2 = ve.queryUtil(query2);
		
		for (Assignment a : table1.getTable().keySet()) {
			assertEquals(table1.getUtil(a), table2.getUtil(a), 0.01);
		}
		
		reducedNet2.addNode(network.getNode("Action").copy());
		reducedNet2.addNode(network.getNode("Util1").copy());
		reducedNet2.addNode(network.getNode("Util2").copy());
		reducedNet2.getNode("Util1").addInputNode(reducedNet2.getNode("Burglary"));
		reducedNet2.getNode("Util1").addInputNode(reducedNet2.getNode("Action"));
		reducedNet2.getNode("Util2").addInputNode(reducedNet2.getNode("Burglary"));
		reducedNet2.getNode("Util2").addInputNode(reducedNet2.getNode("Action"));
		
		UtilQuery query3 = new UtilQuery(reducedNet2, "Action");
		UtilityTable table3 = ve.queryUtil(query3);
		
		for (Assignment a : table1.getTable().keySet()) {
			assertEquals(table1.getUtil(a), table3.getUtil(a), 0.6);
		}
	}
	
	
	@Test
	public void test6() throws DialException, InterruptedException {
		BNetwork old = network.copy();
		
		network.getNode("Alarm").removeInputNode("Earthquake");
		network.getNode("Alarm").removeInputNode("Burglary");
		network.getChanceNode("Alarm").setDistrib(new CategoricalTable(new Assignment("Alarm", "False")));
		
		test1();
		test2();
		test3();
		
		network = old;
	}
	
	

}