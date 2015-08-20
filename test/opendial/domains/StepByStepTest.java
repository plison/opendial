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

import java.util.logging.*;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class StepByStepTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void domain() {
		DialogueSystem system = new DialogueSystem(XMLDomainReader
				.extractDomain("test/domains/example-step-by-step_params.xml"));
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		// Settings.nbSamples = Settings.nbSamples / 2;
		system.startSystem();

		CategoricalTable.Builder o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("move a little bit left", 0.4);
		o1.addRow("please move a little right", 0.3);
		system.addContent(o1.build());
		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(-0.1, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("move a little bit left", 0.5);
		o1.addRow("please move a little left", 0.2);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.2, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("now move right please", 0.8);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.3, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("move left", 0.7);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.2, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("turn left", 0.32);
		o1.addRow("move left again", 0.3);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.1, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("and do that again", 0.0);
		system.addContent(o1.build());

		// assertFalse(system.getState().hasActionNode("a_m'"));

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("turn left", 1.0);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.5, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("turn right", 0.4);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(-0.1, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("please turn right", 0.8);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.3, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Right)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		o1 = new CategoricalTable.Builder("u_u");
		o1.addRow("and turn a bit left", 0.3);
		o1.addRow("move a bit left", 0.3);
		system.addContent(o1.build());

		assertEquals(0.0, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "AskRepeat")), 0.3);
		assertEquals(0.1, system.getState().queryUtil(Arrays.asList("a_m'"))
				.getUtil(new Assignment("a_m'", "Move(Left)")), 0.15);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		// Settings.nbSamples = Settings.nbSamples * 2;
	}

}
