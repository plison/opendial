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
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.statechange.DistributionRule;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
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
		catch (DialException e) { log.warning ("domain cannot be read " + e); }
	}

	@Test
	public void test() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"a_u'");
		
		InferenceChecks.checkProb(query, new Assignment("a_u'", "Greeting"), 0.8);
		InferenceChecks.checkProb(query, new Assignment("a_u'", "None"), 0.2);
	}


	@Test 
	public void test2() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"i_u'");
		
		InferenceChecks.checkProb(query, new Assignment("i_u'", "Inform"), 0.7*0.8);
		InferenceChecks.checkProb(query, new Assignment("i_u'", "None"), 1-0.7*0.8);
	}

	@Test
	public void test3() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"direction'");
		
		InferenceChecks.checkProb(query, new Assignment("direction'", "straight"), 0.79);
		InferenceChecks.checkProb(query, new Assignment("direction'", "left"), 0.20);
		InferenceChecks.checkProb(query, new Assignment("direction'", "right"), 0.01);
		
	}


	@Test
	public void test4() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o'");

		InferenceChecks.checkProb(query, new Assignment("o'", "and we have var1=value2"), 0.3);
		InferenceChecks.checkProb(query, new Assignment("o'", "and we have localvar=value1"), 0.20);
		InferenceChecks.checkProb(query, new Assignment("o'", "and we have localvar=value3"), 0.28);
	}


	@Test
	public void test5() throws DialException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o2'");
	
		InferenceChecks.checkProb(query, new Assignment("o2'", "here is value1"), 0.35);
		InferenceChecks.checkProb(query, new Assignment("o2'", "and value2 is over there"), 0.07);
		InferenceChecks.checkProb(query, new Assignment("o2'", "value3, finally"), 0.28);

	}

	@Test
	public void test6() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("var1", "value2"), 0.9);
		system.getState().addContent(table, "test6");
		waitUntilStable(system);
		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"o''");
		
		InferenceChecks.checkProb(query, new Assignment("o''", "and we have var1=value2"), 0.93);
		InferenceChecks.checkProb(query, new Assignment("o''", "and we have localvar=value1"), 0.02);
		InferenceChecks.checkProb(query, new Assignment("o''", "and we have localvar=value3"), 0.028);

	}


	@Test
	public void test7() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"a_u2'");
		
		InferenceChecks.checkProb(query, new Assignment("a_u2'", "[HowAreYou, Greet]"), 0.7);
		InferenceChecks.checkProb(query, new Assignment("a_u2'", "none"), 0.1);
		InferenceChecks.checkProb(query, new Assignment("a_u2'", "[HowAreYou]"), 0.2);

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

		InferenceChecks.checkProb(query, new Assignment("a_u3'", "["+greetNode +"," + howareyouNode +"]"), 0.7);
		InferenceChecks.checkProb(query, new Assignment("a_u3'", "none"), 0.1);
		InferenceChecks.checkProb(query, new Assignment("a_u3'", "[" + howareyouNode + "]"), 0.2);
		InferenceChecks.checkProb(query2, new Assignment(greetNode+".start'", 0), 0.7);
		InferenceChecks.checkProb(query3, new Assignment(greetNode+".end'", 5), 0.7);
		InferenceChecks.checkProb(query4, new Assignment(howareyouNode+".start'", 12), 0.7);
		InferenceChecks.checkProb(query5, new Assignment(howareyouNode+".end'", 23), 0.7);
		InferenceChecks.checkProb(query5, new Assignment(howareyouNode+".end'", 22), 0.2);
		InferenceChecks.checkProb(query6,  new Assignment(greetNode+"'", "Greet"), 0.7);
		InferenceChecks.checkProb(query7,  new Assignment(howareyouNode+"'", "HowAreYou"), 0.9);
		
	}

	private void waitUntilStable(DialogueSystem system) {
		try {
			Thread.sleep(30);
			int i = 0 ;
			while (!system.getState().isStable()) {
				Thread.sleep(30);
				i++;
				if (i > 30) {
					log.debug("dialogue state: " + system.getState().toString());
				}
			}
		}
		catch (InterruptedException e) { 
			log.debug ("interrupted exception: " + e.toString());
		}
	}
}
