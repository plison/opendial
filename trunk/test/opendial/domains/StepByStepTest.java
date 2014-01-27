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

import java.util.Arrays;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;

public class StepByStepTest {

	// logger
	public static Logger log = new Logger("StepByStepTest", Logger.Level.NORMAL);
	
	@Test
	public void domain () throws DialException {
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain("domains/examples/example-step-by-step.xml"));
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
	//	Settings.nbSamples = Settings.nbSamples / 2;
		system.startSystem();
		
		CategoricalTable o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "move a little bit left"), 0.4);
		o1.addRow(new Assignment("u_u", "please move a little right"), 0.3);
		system.addContent(o1);
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(-0.1, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);
		
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "move a little bit left"), 0.5);
		o1.addRow(new Assignment("u_u", "please move a little left"), 0.2);
		system.addContent(o1);		

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.2, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "now move right please"), 0.8);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.3, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "move left"), 0.7);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.2, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "turn left"), 0.32);
		o1.addRow(new Assignment("u_u", "move left again"), 0.3);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.1, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "and do that again"), 0.0);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "turn left"), 1.0);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.5, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "turn right"), 0.4);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(-0.1, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "please turn right"), 0.8);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.3, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		o1 = new CategoricalTable();
		o1.addRow(new Assignment("u_u", "and turn a bit left"), 0.3);
		o1.addRow(new Assignment("u_u", "move a bit left"), 0.3);
		system.addContent(o1);	
		
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.1, system.getState().queryUtil(Arrays.asList("a_m'")).getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
	//	Settings.nbSamples = Settings.nbSamples * 2;
	}

}

