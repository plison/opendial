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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.ArrayVal;
import opendial.datastructs.Assignment;
import opendial.modules.core.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

import org.junit.Test;

public class DemoTest {

	// logger
	public static Logger log = new Logger("ParametersTest", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//thesistest.xml";
	public static final String paramFile = "test//domains//thesisparams.xml";
	public static final String domainFile2 = "test//domains//domain-demo.xml";

	// @Test
	public void testParam1() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile,
				"parameters");
		domain.setParameters(params);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;

		system.startSystem();
		system.addContent(new Assignment("a_m", "AskRepeat"));

		CategoricalTable t = new CategoricalTable("a_u");
		t.addRow("DoA", 0.7);
		t.addRow("a_u", 0.2);
		t.addRow("a_u", 0.1);
		system.addContent(t);
		for (int i = 0; i < 3000; i++) {
			System.out.println(((ArrayVal) system.getState()
					.getChanceNode("theta").sample()).getArray()[0]);
		}
		// System.out.println("DENSITY: " + new
		// ArrayVal(((ProductKernelDensityFunction)((MultivariateDistribution)system.getState().
		// getNetwork().getChanceNode("theta").getDistrib().toContinuous()).getFunction()).getBandwidth()));
	}

	@Test
	public void testDemo() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile2);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		// system.getSettings().recording = Recording.ALL;

		system.startSystem();

		CategoricalTable t = new CategoricalTable("u_u");
		t.addRow("hello there", 0.7);
		t.addRow("hello", 0.2);
		Set<String> updates = system.addContent(t);
		assertTrue(updates.containsAll(Arrays.asList("a_u", "a_m", "u_m")));

		assertEquals("Hi there", system.getContent("u_m").getBest().toString());

		Map<String, Double> t2 = new HashMap<String, Double>();
		t2.put("move forward", 0.06);
		system.addUserInput(t2);

		assertFalse(system.getState().hasChanceNode("u_m"));

		t2 = new HashMap<String, Double>();
		t2.put("move forward", 0.45);
		system.addUserInput(t2);

		assertEquals("OK, moving Forward", system.getContent("u_m").getBest()
				.toString());

		t = new CategoricalTable("u_u");
		t.addRow("now do that again", 0.3);
		t.addRow("move backward", 0.22);
		t.addRow("move a bit to the left", 0.22);
		system.addContent(t);

		assertEquals("Sorry, could you repeat?", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("do that one more time", 0.65);
		system.addContent(t);

		assertEquals("OK, moving Forward", system.getContent("u_m").getBest()
				.toString());

		system.addContent(new CategoricalTable("perceived", "[BlueObj]"));

		t = new CategoricalTable("u_u");
		t.addRow("what do you see", 0.6);
		t.addRow("do you see it", 0.3);
		log.debug("-------------------");
		system.addContent(t);
		assertEquals("I see a blue cylinder", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("pick up the blue object", 0.75);
		t.addRow("turn left", 0.12);
		system.addContent(t);

		assertEquals("OK, picking up the blue object", system.getContent("u_m")
				.getBest().toString());

		system.addContent(new CategoricalTable("perceived", "[]"));
		system.addContent(new CategoricalTable("carried", "[BlueObj]"));

		t = new CategoricalTable("u_u");
		t.addRow("now please move a bit forward", 0.21);
		t.addRow("move backward a little bit", 0.13);
		system.addContent(t);

		assertEquals("Should I move a bit forward?", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("yes", 0.8);
		t.addRow("move backward", 0.1);
		system.addContent(t);

		assertEquals("OK, moving Forward a little bit", system
				.getContent("u_m").getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("and now move forward", 0.21);
		t.addRow("move backward", 0.09);
		system.addContent(t);

		assertEquals("Should I move forward?", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("no", 0.6);
		system.addContent(t);

		assertEquals("Should I move backward?", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("yes", 0.5);
		system.addContent(t);

		assertEquals("OK, moving Backward", system.getContent("u_m").getBest()
				.toString());

		t = new CategoricalTable("u_u");
		t.addRow("now what can you see now?", 0.7);
		system.addContent(t);

		assertEquals("I do not see anything", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("please release the object", 0.5);
		system.addContent(t);

		assertEquals("OK, putting down the object", system.getContent("u_m")
				.getBest().toString());

		t = new CategoricalTable("u_u");
		t.addRow("something unexpected", 0.7);
		system.addContent(t);

		assertFalse(system.getState().hasChanceNode("u_m"));

		t = new CategoricalTable("u_u");
		t.addRow("goodbye", 0.7);
		system.addContent(t);

		assertEquals("Bye, see you next time", system.getContent("u_m")
				.getBest().toString());

	}

}
