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

package opendial.domains;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class PruningTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//domain1.xml";

	static Domain domain;
	static InferenceChecks inference;
	static DialogueSystem system;

	static {
		try {
			domain = XMLDomainReader.extractDomain(domainFile);
			inference = new InferenceChecks();
			inference.EXACT_THRESHOLD = 0.1;
			inference.SAMPLING_THRESHOLD = 0.1;
			system = new DialogueSystem(domain);
			system.getSettings().showGUI = false;

			system.startSystem();
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPruning0() throws InterruptedException {

		assertEquals(15, system.getState().getNodeIds().size());
		assertEquals(0, system.getState().getEvidence().getVariables().size());
	}

	@Test
	public void testPruning1() throws InterruptedException {

		inference.checkProb(system.getState(), "a_u", "Greeting", 0.8);
		inference.checkProb(system.getState(), "a_u", "None", 0.2);
	}

	@Test
	public void testPruning2() {

		inference.checkProb(system.getState(), "i_u", "Inform", 0.7 * 0.8);
		inference.checkProb(system.getState(), "i_u", "None", 1 - 0.7 * 0.8);
	}

	@Test
	public void testPruning3() {

		inference.checkProb(system.getState(), "direction", "straight", 0.79);
		inference.checkProb(system.getState(), "direction", "left", 0.20);
		inference.checkProb(system.getState(), "direction", "right", 0.01);

	}

	@Test
	public void testPruning4() {

		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.3);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1",
				0.2);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3",
				0.31);
	}

	@Test
	public void testPruning5() {

		inference.checkProb(system.getState(), "o2", "here is value1", 0.35);
		inference.checkProb(system.getState(), "o2", "and value2 is over there",
				0.07);
		inference.checkProb(system.getState(), "o2", "value3, finally", 0.28);

	}

	@Test
	public void testPruning6() throws InterruptedException {

		DialogueState initialState = system.getState().copy();

		CategoricalTable.Builder builder = new CategoricalTable.Builder("var1");
		builder.addRow("value2", 0.9);
		system.getState().addToState(builder.build());

		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.3);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1",
				0.2);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3",
				0.31);

		system.getState().reset(initialState);

	}

	@Test
	public void testPruning7() throws InterruptedException {

		inference.checkProb(system.getState(), "a_u2", "[Greet, HowAreYou]", 0.7);
		inference.checkProb(system.getState(), "a_u2", "none", 0.1);
		inference.checkProb(system.getState(), "a_u2", "[HowAreYou]", 0.2);

	}

	@Test
	public void testPruning8() throws InterruptedException {

		DialogueState initialState = system.getState().copy();

		SortedSet<String> createdNodes = new TreeSet<String>();
		for (String nodeId : system.getState().getNodeIds()) {
			if (nodeId.contains("a_u3^")) {
				createdNodes.add(nodeId);
			}
		}

		assertEquals(2, createdNodes.size());

		String greetNode = "";
		String howareyouNode = "";
		Set<Value> values =
				system.getState().getNode(createdNodes.first() + "").getValues();
		if (values.contains(ValueFactory.create("Greet"))) {
			greetNode = createdNodes.first();
			howareyouNode = createdNodes.last();
		}
		else {
			greetNode = createdNodes.last();
			howareyouNode = createdNodes.first();
		}

		inference.checkProb(system.getState(), "a_u3",
				"[" + howareyouNode + "," + greetNode + "]", 0.7);
		inference.checkProb(system.getState(), "a_u3", "none", 0.1);
		inference.checkProb(system.getState(), "a_u3", "[" + howareyouNode + "]",
				0.2);
		inference.checkProb(system.getState(), greetNode + "", "Greet", 0.7);
		inference.checkProb(system.getState(), howareyouNode + "", "HowAreYou", 0.9);

		system.getState().reset(initialState);

	}
}
