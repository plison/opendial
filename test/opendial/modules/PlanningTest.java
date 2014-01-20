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

package opendial.modules;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.inference.queries.ProbQuery;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class PlanningTest {

	// logger
	public static Logger log = new Logger("PlanningTest", Logger.Level.DEBUG);


	public static final String domainFile = "test//domains//domain3.xml";
	public static final String domainFile2 = "test//domains//basicplanning.xml";
	public static final String domainFile3 = "test//domains//planning2.xml";
	public static final String settingsFile = "test//domains//settings_test2.xml";

	static InferenceChecks inference;
	static Domain domain;
	static Domain domain2;
	static Domain domain3;
	static {
		try { 
			domain = XMLDomainReader.extractDomain(domainFile); 
			domain2 = XMLDomainReader.extractDomain(domainFile2); 
			domain3 = XMLDomainReader.extractDomain(domainFile3); 
			inference = new InferenceChecks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Test
	public void testPlanning() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.startSystem(); 
		assertEquals(3, system.getState().getNodes().size());
		assertEquals(3, system.getState().getChanceNodes().size());
		assertEquals(0, system.getState().getEvidence().getVariables().size());
		inference.checkProb(new ProbQuery(system.getState(), "a_m3"), new Assignment("a_m3", "Do"), 1.0);
		inference.checkProb(new ProbQuery(system.getState(), "obj(a_m3)"), new Assignment("obj(a_m3)", "A"), 1.0);
	}

	@Test
	public void testPlanning2() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.getSettings().showGUI = false;

		system.startSystem(); 
		log.debug("nodes " + system.getState().getNodeIds());
		assertEquals(2, system.getState().getNodeIds().size());
		assertFalse(system.getState().hasChanceNode("a_m"));

	}
	
	@Test
	public void testPlanning3() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 2;
		system.startSystem(); 
		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "AskRepeat"), 1.0);
	}
	
	
	@Test
	public void testPlanning4() throws DialException, InterruptedException {
	
		DialogueSystem system = new DialogueSystem(domain3);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 3;
		system.startSystem(); 
		
		CategoricalTable t1 = new CategoricalTable();
		t1.addRow(new Assignment("a_u", "Ask(Coffee)"), 0.95);
		t1.addRow(new Assignment("a_u", "Ask(Tea)"), 0.02);
		system.addContent(t1);

		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "Do(Coffee)"), 1.0);
				
	}
	
	
	@Test
	public void testPlanning5() throws DialException, InterruptedException {


		DialogueSystem system = new DialogueSystem(domain3);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 3;
		system.startSystem(); 
		
		CategoricalTable t1 = new CategoricalTable();
		t1.addRow(new Assignment("a_u", "Ask(Coffee)"), 0.3);
		t1.addRow(new Assignment("a_u", "Ask(Tea)"), 0.3);
		system.addContent(t1);

		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "AskRepeat"), 1.0);
		
	}

}

