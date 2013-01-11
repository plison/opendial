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

package opendial.domains;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.gui.GUIFrame;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.inference.VariableElimination;
import opendial.readers.XMLDomainReader;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicRuleTest {

	// logger
	public static Logger log = new Logger("BasicRuleTest", Logger.Level.DEBUG);

	public static final String domainFile = "domains//testing//domain1.xml";

	static Domain domain;

	static {
		try { domain = XMLDomainReader.extractDomain(domainFile); } 
		catch (DialException e) { log.warning ("domain cannot be read"); }
	}

	@Test
	public void test() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"a_u'");
		InferenceAlgorithm inference;
		inference= new VariableElimination();
		ProbDistribution distrib = inference.queryProb(query);
		assertEquals(0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u'", "Greeting")), 0.01);
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u'", "None")), 0.01);

		inference = new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u'", "Greeting")), 0.1);
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u'", "None")), 0.1);
	}


	@Test
	public void test2() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"i_u'");
		ProbDistribution distrib = null;
		InferenceAlgorithm inference;
		inference= new VariableElimination();
		distrib = inference.queryProb(query);
		assertEquals(0.7*0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("i_u'", "Inform")), 0.01);
		assertEquals(1-0.7*0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("i_u'", "None")), 0.01);

		inference = new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.7*0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("i_u'", "Inform")), 0.1);
		assertEquals(1-0.7*0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("i_u'", "None")), 0.1);

	}

	@Test
	public void test3() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"direction'");
		ProbDistribution distrib = null;
		InferenceAlgorithm inference;
		inference= new VariableElimination();
		distrib = inference.queryProb(query);
		assertEquals(0.79, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "straight")), 0.01);
		assertEquals(0.20, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "left")), 0.01);		
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "right")), 0.01);		

		inference= new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.77, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "straight")), 0.1);
		assertEquals(0.22, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "left")), 0.1);		
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment(), new Assignment("direction'", "right")), 0.1);		

	}


	@Test
	public void test4() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o'");
		ProbDistribution distrib = null;
		InferenceAlgorithm inference;
		inference= new VariableElimination();
		distrib = inference.queryProb(query);
		assertEquals(0.3, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have var1=value2")), 0.01);
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value1")), 0.01);		
		assertEquals(0.28, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value3")), 0.01);		

		inference= new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.3, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have var1=value2")), 0.1);
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value1")), 0.1);		
		assertEquals(0.28, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value3")), 0.1);		
	}


	@Test
	public void test5() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o2'");
		ProbDistribution distrib = null;	
		InferenceAlgorithm inference;
		inference= new VariableElimination();
		distrib = inference.queryProb(query);
		assertEquals(0.35, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "here is value1")), 0.01);
		assertEquals(0.07, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "and value2 is over there")), 0.01);		
		assertEquals(0.28, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "value3, finally")), 0.01);		

		inference= new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.35, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "here is value1")), 0.1);
		assertEquals(0.07, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "and value2 is over there")), 0.1);		
		assertEquals(0.28, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o2'", "value3, finally")), 0.1);		
	}

	@Test
	public void test6() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		// Making up for the lack of pruner for the moment
		for (BNode n : new ArrayList<BNode>(
				system.getState().getNetwork().getNodes())) {
			if (!n.getId().contains("'")) {
				n.setId(n.getId()+"^o");
			}
			else {
				n.setId(n.getId().replace("'", ""));
			}
		}
		ChanceNode node = new ChanceNode("var1");
		node.addProb(ValueFactory.create("value2"), 0.9);
		system.getState().addNode(node);

		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o'");
		ProbDistribution distrib = null;	
		InferenceAlgorithm inference;

		inference= new VariableElimination(); 
		distrib = inference.queryProb(query);
		assertEquals(0.93, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have var1=value2")), 0.01);
		assertEquals(0.02, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value1")), 0.01);		
		assertEquals(0.03, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value3")), 0.01);		

		inference= new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.93, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have var1=value2")), 0.1);
		assertEquals(0.02, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value1")), 0.1);		
		assertEquals(0.03, distrib.toDiscrete().getProb(new Assignment(), new Assignment("o'", "and we have localvar=value3")), 0.1);		
	}


	@Test
	public void test7() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"a_u2'");
		ProbDistribution distrib = null;	
		InferenceAlgorithm inference;

		inference= new VariableElimination(); 
		distrib = inference.queryProb(query);

		distrib = inference.queryProb(query);
		assertEquals(0.7, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "[HowAreYou, Greet]")), 0.01);
		assertEquals(0.1, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "none")), 0.01);		
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "[HowAreYou]")), 0.01);		

		inference= new ImportanceSampling();
		distrib = inference.queryProb(query);
		assertEquals(0.7, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "[HowAreYou, Greet]")), 0.1);
		assertEquals(0.1, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "none")), 0.1);		
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u2'", "[HowAreYou]")), 0.1);		

	}

	@Test
	public void test8() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"a_u3'");
		ProbDistribution distrib = null;	
		InferenceAlgorithm inference;

		SortedSet<String> createdNodes = new TreeSet<String>();
		for (String nodeId: system.getState().getNetwork().getNodeIds()) {
			if (nodeId.contains("a_u3^")) {
				createdNodes.add(nodeId.replace(".start", "").
						replace(".end", "").replace("'", ""));
			}
		}
		assertEquals(2, createdNodes.size());

		inference= new VariableElimination(); 
		distrib = inference.queryProb(query);

		String greetNode = "";
		String howareyouNode = "";
		Set<Value> values = system.getState().getNetwork().getNode(createdNodes.first()+"'").getValues();
		if (values.contains(ValueFactory.create("Greet"))) {
			greetNode = createdNodes.first();
			howareyouNode = createdNodes.last();
		}
		else {
			greetNode = createdNodes.last();
			howareyouNode = createdNodes.first();
		}

		ProbQuery query2 = new ProbQuery(system.getState().getNetwork(),greetNode+".start'");
		ProbQuery query3 = new ProbQuery(system.getState().getNetwork(),greetNode+".end'");
		ProbQuery query4 = new ProbQuery(system.getState().getNetwork(),howareyouNode+".start'");
		ProbQuery query5 = new ProbQuery(system.getState().getNetwork(),howareyouNode+".end'");
		ProbQuery query6 = new ProbQuery(system.getState().getNetwork(),greetNode+"'");
		ProbQuery query7 = new ProbQuery(system.getState().getNetwork(),howareyouNode+"'");

		ProbDistribution distrib2 = inference.queryProb(query2);
		ProbDistribution distrib3 = inference.queryProb(query3);
		ProbDistribution distrib4 = inference.queryProb(query4);
		ProbDistribution distrib5 = inference.queryProb(query5);
		ProbDistribution distrib6 = inference.queryProb(query6);
		ProbDistribution distrib7 = inference.queryProb(query7);
		assertEquals(0.7, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "["+greetNode +"," + howareyouNode +"]")), 0.01);
		assertEquals(0.1, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "none")), 0.01);		
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "[" + howareyouNode + "]")), 0.01); 		
		assertEquals(0.7, distrib2.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+".start'", 0)), 0.01);
		assertEquals(0.7, distrib3.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+".end'", 5)), 0.01);
		assertEquals(0.7, distrib4.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".start'", 12)), 0.01);
		assertEquals(0.7, distrib5.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".end'", 23)), 0.01);
		assertEquals(0.2, distrib5.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".end'", 22)), 0.01);
		assertEquals(0.7, distrib6.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+"'", "Greet")), 0.01);
		assertEquals(0.9, distrib7.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+"'", "HowAreYou")), 0.01);

		inference= new ImportanceSampling();
		distrib2 = inference.queryProb(query2);
		distrib3 = inference.queryProb(query3);
		distrib4 = inference.queryProb(query4);
		distrib5 = inference.queryProb(query5);
		distrib6 = inference.queryProb(query6);
		distrib7 = inference.queryProb(query7);
		assertEquals(0.7, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "["+greetNode +"," + howareyouNode +"]")), 0.1);
		assertEquals(0.1, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "none")), 0.1);		
		assertEquals(0.2, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u3'", "[" + howareyouNode + "]")), 0.1); 		
		assertEquals(0.7, distrib2.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+".start'", 0)), 0.1);
		assertEquals(0.7, distrib3.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+".end'", 5)), 0.1);
		assertEquals(0.7, distrib4.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".start'", 12)), 0.1);
		assertEquals(0.7, distrib5.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".end'", 23)), 0.1);
		assertEquals(0.2, distrib5.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+".end'", 22)), 0.1);
		assertEquals(0.7, distrib6.toDiscrete().getProb(new Assignment(), new Assignment(greetNode+"'", "Greet")), 0.1);
		assertEquals(0.9, distrib7.toDiscrete().getProb(new Assignment(), new Assignment(howareyouNode+"'", "HowAreYou")), 0.1);

	}


	private void waitUntilStable(DialogueSystem system) {
		try {
			Thread.sleep(30);
			while (!system.getState().isStable()) {
				Thread.sleep(50);
			}
		}
		catch (InterruptedException e) { }
	}
}
