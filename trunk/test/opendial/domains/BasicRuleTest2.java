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
import opendial.inference.NaiveInference;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.inference.VariableElimination;
import opendial.readers.XMLDomainReader;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicRuleTest2 {

	// logger
	public static Logger log = new Logger("BasicRuleTest", Logger.Level.DEBUG);

	public static final String domainFile = "domains//testing//domain2.xml";
	public static final String domainFile2 = "domains//testing//domain3.xml";

	static Domain domain;
	static Domain domain2;
	
	VariableElimination ve;
	ImportanceSampling is;
	NaiveInference naive;
	


	public BasicRuleTest2() throws DialException {
		ve = new VariableElimination();
		is = new ImportanceSampling(5000, 200);
		naive = new NaiveInference();
	}

	static {
		try { domain = XMLDomainReader.extractDomain(domainFile); 
		domain2 = XMLDomainReader.extractDomain(domainFile2); } 
		catch (DialException e) { log.warning ("domain cannot be read: " + e); }
	}

	@Test
	public void test() throws DialException, InterruptedException {
		
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		 waitUntilStable(system);
		 
		system.getState().getNetwork().getNode("a_u^p'").setId("a_u^p");
	 	ProbQuery query = new ProbQuery(system.getState(),"a_u^p");
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "Ask(A)"), 0.63);
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "Ask(B)"), 0.27);
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "None"), 0.1);
	 			
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("a_u", "Ask(B)"), 0.8);
		table.addRow(new Assignment("a_u", "None"), 0.2); 
		 
		system.getState().addContent(table, "test");
		waitUntilStable(system);
		
		query = new ProbQuery(system.getState(),"a_u^p");
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "Ask(A)"), 0.0516);
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "Ask(B)"), 0.907);
	 	InferenceChecks.checkProb(query, new Assignment("a_u^p", "None"), 0.041);

	 	ProbQuery query2 = new ProbQuery(system.getState(),"i_u");
	 	InferenceChecks.checkProb(query2, new Assignment("i_u", "Want(A)"), 0.080);
	 	InferenceChecks.checkProb(query2, new Assignment("i_u", "Want(B)"), 0.9197);

	 	ProbQuery query3 = new ProbQuery(system.getState(),"a_u'");
	 	InferenceChecks.checkProb(query3, new Assignment("a_u'", "Ask(B)"), 0.918);
	 	InferenceChecks.checkProb(query3, new Assignment("a_u'", "None"), 0.0820);

	 	ProbQuery query4 = new ProbQuery(system.getState(),"a_u'");
	 	InferenceChecks.checkProb(query4, new Assignment("a_u'", "Ask(B)"), 0.918);
	 	InferenceChecks.checkProb(query4, new Assignment("a_u'", "None"), 0.0820);	
	}
	
	
	@Test
	public void test2() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		 waitUntilStable(system);
		 			
		system.getState().getNetwork().getNode("u_u2^p'").setId("u_u2^p");
	 	ProbQuery query = new ProbQuery(system.getState(),"u_u2^p");
	 	InferenceChecks.checkProb(query, new Assignment("u_u2^p", "Do A"), 0.216);
	 	InferenceChecks.checkProb(query, new Assignment("u_u2^p", "Please do C"), 0.027);
	 	InferenceChecks.checkProb(query, new Assignment("u_u2^p", "Could you do B?"), 0.054);
	 	InferenceChecks.checkProb(query, new Assignment("u_u2^p", "Could you do A?"), 0.162);
	 	InferenceChecks.checkProb(query, new Assignment("u_u2^p", "none"), 0.19);
	 			
	
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("u_u2", "Please do B"), 0.4);
		table.addRow(new Assignment("u_u2", "Do B"), 0.4); 
		 
		system.getState().addContent(table, "test2");
		waitUntilStable(system);
		
		query = new ProbQuery(system.getState(),"i_u2");
	 	InferenceChecks.checkProb(query, new Assignment("i_u2", "Want(B)"), 0.6542);
	 	InferenceChecks.checkProb(query, new Assignment("i_u2", "Want(A)"), 0.1963);
	 	InferenceChecks.checkProb(query, new Assignment("i_u2", "Want(C)"), 0.0327);
	 	InferenceChecks.checkProb(query, new Assignment("i_u2", "none"), 0.1168);
	}
	
	
	@Test
	public void test3() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		waitUntilStable(system);
		
	 	UtilQuery query = new UtilQuery(system.getState(),"a_m'");
	 	InferenceChecks.checkUtil(query, new Assignment("a_m'", "Do(A)"), 0.6);
	 	InferenceChecks.checkUtil(query, new Assignment("a_m'", "Do(B)"), -2.6);
	 
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("a_u", "Ask(B)"), 0.8);
		table.addRow(new Assignment("a_u", "None"), 0.2); 
		 
		system.getState().getNetwork().removeNodes(system.getState().getNetwork().getUtilityNodeIds());
		system.getState().getNetwork().getNode("a_u^p'").setId("a_u^p");
		system.getState().addContent(table, "test");
		waitUntilStable(system);

	 	query = new UtilQuery(system.getState(),"a_m''");
	 	InferenceChecks.checkUtil(query, new Assignment("a_m''", "Do(A)"), -4.357);
	 	InferenceChecks.checkUtil(query, new Assignment("a_m''", "Do(B)"), 2.357);	
	}
	
	

	@Test
	public void test4() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(domain2);
		system.startSystem(); 
		waitUntilStable(system);

		UtilQuery query = new UtilQuery(system.getState(),Arrays.asList("a_m3'", "obj(a_m3)'"));

	 	InferenceChecks.checkUtil(query, new Assignment(new Assignment("a_m3'", "Do"),
	 			new Assignment("obj(a_m3)'", "A")), 0.3);
	 	InferenceChecks.checkUtil(query, new Assignment(new Assignment("a_m3'", "Do"),
	 			new Assignment("obj(a_m3)'", "B")), -2.9);
	 	InferenceChecks.checkUtil(query, new Assignment(new Assignment("a_m3'", "SayHi"),
	 			new Assignment("obj(a_m3)'", "None")), 0.05);
	 	
	 	assertEquals(6, (new ImportanceSampling()).queryUtility(query).getTable().size());
	}
	


	private void waitUntilStable(DialogueSystem system) {
		try {
			Thread.sleep(30);
			int i = 0 ;
			while (!system.getState().isStable()) {
				Thread.sleep(20);
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
