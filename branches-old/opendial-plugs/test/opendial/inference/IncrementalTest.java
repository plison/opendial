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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.incremental.IncrementalUnit;
import opendial.bn.distribs.incremental.IncrementalDistribution;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.modules.core.DialogueRecorder;
import opendial.readers.XMLDomainReader;

public class IncrementalTest {

	// logger
	public static Logger log = new Logger("IncrementalTest",
			Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain-demo.xml";

	
	@Test
	public void wordLattice1() throws DialException {	
		IncrementalUnit iu1 = new IncrementalUnit("u_u", "einen");
		IncrementalUnit iu2 = new IncrementalUnit("u_u", "wettbewerbsbedingten");
		iu2.connectTo(iu1, 0.5);
		IncrementalUnit iu3 = new IncrementalUnit("u_u","wettbewerbs");
		iu3.connectTo(iu1, 0.25);
		IncrementalUnit iu4 = new IncrementalUnit("u_u","wettbewerb");
		iu4.connectTo(iu1, 0.25);
		IncrementalUnit iu5 = new IncrementalUnit("u_u", "bedingten");
		iu5.connectTo(iu3, 1.0);
		iu5.connectTo(iu4, 1.0);
		IncrementalUnit iu6 = new IncrementalUnit("u_u", "preissturz");
		iu6.connectTo(iu2, 0.5);
		iu6.connectTo(iu5, 0.5);
		IncrementalUnit iu7 = new IncrementalUnit("u_u", "preis");
		iu7.connectTo(iu2, 0.5);
		iu7.connectTo(iu5, 0.5);
		IncrementalUnit iu8 = new IncrementalUnit("u_u", "sturz");
		iu8.connectTo(iu7, 1.0);
		
		IncrementalDistribution lattice = new IncrementalDistribution(Arrays.asList(iu1, iu2, iu3, iu4, iu5, iu6, iu7, iu8));
		
		CategoricalTable table = lattice.toDiscrete();
		assertEquals(6, table.getRows().size());
		assertEquals("einen wettbewerbsbedingten preissturz", table.getBest().getValue("u_u").toString());
		assertEquals(0.25, table.getProb(new Assignment("u_u", "einen wettbewerbsbedingten preis sturz")), 0.01);
		
		Map<Assignment, Integer> samples = new HashMap<Assignment, Integer>();
		for (int i = 0 ; i < 1000 ; i++) {
			Assignment sample = lattice.sample();
			if (!samples.containsKey(sample)) {
				samples.put(sample, 0);
			}
			samples.put(sample, samples.get(sample) + 1);
		}
		assertEquals(6, samples.size());
		assertEquals(250, samples.get(new Assignment("u_u", "einen wettbewerbsbedingten preis sturz")), 100);	
	}
	
	
	@Test
	public void wordLattice2() throws DialException {
		IncrementalUnit iu1 = new IncrementalUnit("u_u", "einen");
		IncrementalDistribution lattice = new IncrementalDistribution(iu1);
		IncrementalUnit iu2 = new IncrementalUnit("u_u", "wettbewerbsbedingten");
		iu2.connectTo(iu1, 0.5);
		lattice.addUnit(iu2);
		IncrementalUnit iu3 = new IncrementalUnit("u_u","wettbewerbs");
		iu3.connectTo(iu1, 0.25);
		IncrementalUnit iu4 = new IncrementalUnit("u_u","wettbewerb");
		iu4.connectTo(iu1, 0.25);
		IncrementalUnit iu5 = new IncrementalUnit("u_u", "bedingten");
		iu5.connectTo(iu3, 1.0);
		iu5.connectTo(iu4, 1.0);
		lattice.addUnits(Arrays.asList(iu3, iu4, iu5));
		IncrementalUnit iu6 = new IncrementalUnit("u_u", "preissturz");
		iu6.connectTo(iu2, 0.5);
		iu6.connectTo(iu5, 0.5);
		lattice.addUnit(iu6);
		IncrementalUnit iu7 = new IncrementalUnit("u_u", "preis");
		iu7.connectTo(iu2, 0.5);
		iu7.connectTo(iu5, 0.5);
		IncrementalUnit iu8 = new IncrementalUnit("u_u", "sturz");
		iu8.connectTo(iu7, 1.0);
		lattice.addUnits(Arrays.asList(iu7, iu8));
		
		CategoricalTable table = lattice.toDiscrete();
		assertEquals(6, table.getRows().size());
		assertEquals("einen wettbewerbsbedingten preissturz", table.getBest().getValue("u_u").toString());
		assertEquals(0.25, table.getProb(new Assignment("u_u", "einen wettbewerbsbedingten preis sturz")), 0.01);

		table = lattice.copy().toDiscrete();
		assertEquals(6, table.getRows().size());
		assertEquals("einen wettbewerbsbedingten preissturz", table.getBest().getValue("u_u").toString());
		assertEquals(0.25, table.getProb(new Assignment("u_u", "einen wettbewerbsbedingten preis sturz")), 0.01);

		
		Map<Assignment, Integer> samples = new HashMap<Assignment, Integer>();
		for (int i = 0 ; i < 1000 ; i++) {
			Assignment sample = lattice.sample();
			if (!samples.containsKey(sample)) {
				samples.put(sample, 0);
			}
			samples.put(sample, samples.get(sample) + 1);
		}
		assertEquals(6, samples.size());
		assertEquals(250, samples.get(new Assignment("u_u", "einen wettbewerbsbedingten preis sturz")), 100);	
	}
	
	
	/**
	 * Creates a simple test with an incrementally constructed word lattice.
	 * 
	 * @throws DialException
	 * @throws InterruptedException
	 */
	@Test
	public void wordLattice3() throws DialException, InterruptedException {
		final DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = true;
		system.startSystem();
		
		IncrementalUnit iu1 = new IncrementalUnit("u_u", "now");
		system.addContent(iu1);
		IncrementalUnit iu2 = new IncrementalUnit("u_u", "turn");
		iu2.connectTo(iu1, 0.8);
		system.addContent(iu2);
		IncrementalUnit iu3 = new IncrementalUnit("u_u", "left");
		iu3.connectTo(iu2, 0.5);
		system.addContent(iu3);
		IncrementalUnit iu4 = new IncrementalUnit("u_u", "right");
		iu4.connectTo(iu3, 0.8);
		system.addContent(iu4);	
		IncrementalUnit iu5 = new IncrementalUnit("u_u", "please");
		iu5.connectTo(iu4, 0.7);
		iu5.connectTo(iu3, 0.15);
		system.addContent(iu5);		
		system.setAsCommitted("u_u", true);
		assertEquals(system.getContent("a_m").toDiscrete().getBest().getValue("a_m"), ValueFactory.create("Confirm(Move(Left))"));
		String record = system.getModule(DialogueRecorder.class).getRecord();
		assertTrue(record.length() > 340 && record.length() < 350);
		assertTrue(record.contains("<interaction><userTurn><variable id=\"u_u\"><value prob=\"0.1"));
		assertTrue(record.contains("now turn left please</value><value prob=\"0.4"));
		assertTrue(record.contains(">now turn left right please</value><value prob=\"0."));
		assertTrue(record.contains("None</value></variable></userTurn><systemTurn>"
				+ "<variable id=\"u_m\"><value>Should I move left?</value></variable>"
				+ "</systemTurn></interaction>"));
		system.getModule(GUIFrame.class).getFrame().dispose();
	}

 
}

