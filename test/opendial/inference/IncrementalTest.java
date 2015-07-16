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

import java.util.logging.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.SingleValueDistribution;
import opendial.bn.values.ValueFactory;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class IncrementalTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//incremental-domain.xml";

	@Test
	public void test1() throws InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.getSettings().recording = Settings.Recording.ALL;
		system.startSystem();
		system.addContent(system.getSettings().userSpeech, "busy");
		system.addIncrementalContent(new SingleValueDistribution("u_u", "go"),
				false);
		Thread.sleep(100);
		assertTrue(system.getContent("u_u").getValues()
				.contains(ValueFactory.create("go")));
		CategoricalTable.Builder t = new CategoricalTable.Builder("u_u");
		t.addRow("forward", 0.7);
		t.addRow("backward", 0.2);
		system.addIncrementalContent(t.build(), true);
		Thread.sleep(100);
		assertTrue(system.getContent("u_u").getValues()
				.contains(ValueFactory.create("go forward")));
		assertEquals(system.getContent("u_u").getProb("go backward"), 0.2, 0.001);
		assertTrue(system.getState().hasChanceNode("nlu"));
		system.addContent(system.getSettings().userSpeech, "None");
		system.addIncrementalContent(new SingleValueDistribution("u_u", "please"),
				true);
		assertEquals(system.getContent("u_u").getProb("go please"), 0.1, 0.001);
		assertTrue(system.getState().hasChanceNode("nlu"));
		system.getState().setAsCommitted("u_u");
		assertFalse(system.getState().hasChanceNode("nlu"));
		CategoricalTable.Builder t2 = new CategoricalTable.Builder("u_u");
		t2.addRow("I said go backward", 0.3);
		system.addIncrementalContent(t2.build(), true);
		assertEquals(system.getContent("a_u").getProb("Request(Backward)"), 0.82,
				0.05);
		assertTrue(system.getContent("u_u").getValues()
				.contains(ValueFactory.create("I said go backward")));
		assertTrue(system.getState().hasChanceNode("nlu"));
		system.getState().setAsCommitted("u_u");
		assertFalse(system.getState().hasChanceNode("nlu"));
		system.addIncrementalContent(
				new SingleValueDistribution("u_u", "yes that is right"), false);
		assertTrue(system.getContent("u_u").getValues()
				.contains(ValueFactory.create("yes that is right")));
	}

}
