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

package opendial.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.logging.Logger;

import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.common.InferenceChecks;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class PlanningTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPlanning() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.startSystem();
		assertEquals(3, system.getState().getNodes().size());
		assertEquals(3, system.getState().getChanceNodes().size());
		assertEquals(0, system.getState().getEvidence().getVariables().size());
		inference.checkProb(system.getState(), "a_m3", "Do", 1.0);
		inference.checkProb(system.getState(), "obj(a_m3)", "A", 1.0);
	}

	@Test
	public void testPlanning2() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.getSettings().showGUI = false;

		system.startSystem();
		assertEquals(2, system.getState().getNodeIds().size());
		assertFalse(system.getState().hasChanceNode("a_m"));

	}

	@Test
	public void testPlanning3() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 2;
		system.startSystem();
		inference.checkProb(system.getState(), "a_m", "AskRepeat", 1.0);
	}

	@Test
	public void testPlanning4() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain3);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 3;
		system.startSystem();

		CategoricalTable.Builder t1 = new CategoricalTable.Builder("a_u");
		t1.addRow("Ask(Coffee)", 0.95);
		t1.addRow("Ask(Tea)", 0.02);
		system.addContent(t1.build());
		inference.checkProb(system.getState(), "a_m", "Do(Coffee)", 1.0);

	}

	@Test
	public void testPlanning5() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain3);
		system.getSettings().showGUI = false;

		system.getSettings().horizon = 3;
		system.startSystem();

		CategoricalTable.Builder t1 = new CategoricalTable.Builder("a_u");
		t1.addRow("Ask(Coffee)", 0.3);
		t1.addRow("Ask(Tea)", 0.3);
		system.addContent(t1.build());

		inference.checkProb(system.getState(), "a_m", "AskRepeat", 1.0);

	}

}
