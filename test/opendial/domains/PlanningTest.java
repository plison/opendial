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

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.inference.queries.ProbQuery;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;

public class PlanningTest {

	// logger
	public static Logger log = new Logger("PlanningTest", Logger.Level.DEBUG);


	public static final String domainFile = "domains//testing//domain3.xml";
	public static final String domainFile2 = "domains//testing//basicplanning.xml";
	public static final String domainFile3 = "domains//testing//planning2.xml";

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
	public void planning() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		assertEquals(3, system.getState().getNetwork().getNodes().size());
		assertEquals(3, system.getState().getNetwork().getChanceNodes().size());
		assertEquals(0, system.getState().getEvidence().getVariables().size());
		inference.checkProb(new ProbQuery(system.getState(), "a_m3"), new Assignment("a_m3", "Do"), 1.0);
		inference.checkProb(new ProbQuery(system.getState(), "obj(a_m3)"), new Assignment("obj(a_m3)", "A"), 1.0);
	}

	@Test
	public void planning2() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.startSystem(); 
		log.debug("Nodes: " + system.getState().getNetwork().getNodeIds());
		assertEquals(2, system.getState().getNetwork().getNodeIds().size());
		assertFalse(system.getState().getNetwork().hasChanceNode("a_m"));

	}
	
	@Test
	public void planning3() throws DialException, InterruptedException {
		
		Settings.getInstance().planning.horizon =2;
		DialogueSystem system = new DialogueSystem(domain2);
		system.startSystem(); 
		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "AskRepeat"), 1.0);
		Settings.getInstance().planning.horizon =1;
	}
	
	
	@Test
	public void planning4() throws DialException, InterruptedException {
		inference = new InferenceChecks();

		Settings.getInstance().planning.horizon =3;
		DialogueSystem system = new DialogueSystem(domain3);
		system.startSystem(); 
		
		SimpleTable t1 = new SimpleTable();
		t1.addRow(new Assignment("a_u", "Ask(Coffee)"), 0.95);
		t1.addRow(new Assignment("a_u", "Ask(Tea)"), 0.02);
		system.getState().addContent(t1, "planning3");
		
		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "Do(Coffee)"), 1.0);
				
		Settings.getInstance().planning.horizon =1;
	}
	
	
	@Test
	public void planning5() throws DialException, InterruptedException {
		inference = new InferenceChecks();

		DialogueSystem system = new DialogueSystem(domain3);
		Settings.getInstance().planning.horizon = 3;
		system.startSystem(); 
		
		SimpleTable t1 = new SimpleTable();
		t1.addRow(new Assignment("a_u", "Ask(Coffee)"), 0.3);
		t1.addRow(new Assignment("a_u", "Ask(Tea)"), 0.3);
		system.getState().addContent(t1, "planning4");

		inference.checkProb(new ProbQuery(system.getState(), "a_m"), new Assignment("a_m", "AskRepeat"), 1.0);
		
		Settings.getInstance().planning.horizon =1;
	}

}

