// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   
                                                                

package opendial.inference;


import static org.junit.Assert.*;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

public class IncrementalTest {

	// logger
	public static Logger log = new Logger("IncrementalTest", Logger.Level.DEBUG);
	
	
	public static final String domainFile = "test//domains//incremental-domain.xml";

	
	@Test
	public void test1() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = true;
		system.getSettings().recording = Settings.Recording.ALL;
		system.startSystem();
		system.addContent(new Assignment("floor", "user"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "go")), false);
		Thread.sleep(100);
		assertTrue(system.getContent("u_u").getValues().contains(new Assignment("u_u", "go")));
		assertTrue(system.getState().hasChanceNode("nlu"));
		CategoricalTable t = new CategoricalTable();
		t.addRow(new Assignment("u_u", "forward"), 0.7);
		t.addRow(new Assignment("u_u", "backward"), 0.2);
		system.incrementContent(t, true);
		Thread.sleep(100);
		assertTrue(system.getContent("u_u").getValues().contains(new Assignment("u_u", "go forward")));
		assertEquals(system.getContent("u_u").toDiscrete().getProb(new Assignment("u_u", "go backward")), 0.2, 0.001);
		assertTrue(system.getState().hasChanceNode("nlu"));
		system.addContent(new Assignment("floor", "free"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "please")), true);
		assertEquals(system.getContent("u_u").toDiscrete().getProb(new Assignment("u_u", "go please")), 0.1, 0.001);
		assertTrue(system.getState().hasChanceNode("nlu"));
		Thread.sleep(Settings.incrementalTimeOut + 100);
		assertFalse(system.getState().hasChanceNode("nlu"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "turn left")), true);
		assertTrue(system.getContent("u_u").getValues().contains(new Assignment("u_u", "turn left")));
		assertTrue(system.getState().hasChanceNode("nlu"));
		system.getState().setAsCommitted("u_u");
		assertFalse(system.getState().hasChanceNode("nlu"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "yes that is right")), false);
		assertTrue(system.getContent("u_u").getValues().contains(new Assignment("u_u", "yes that is right")));
		assertTrue(system.getState().hasChanceNode("nlu"));
	}

}

