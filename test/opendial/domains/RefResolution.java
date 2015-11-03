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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import opendial.DialogueSystem;
import opendial.bn.values.ValueFactory;
import opendial.modules.StatePruner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class RefResolution {

	Domain domain = XMLDomainReader.extractDomain("test/domains/refres.xml");
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void nluTest() throws InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.startSystem();
		system.addUserInput("take the red box");
		assertEquals(ValueFactory.create("[type=box, def=def, nb=sg, attr=red]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("take the big yellow box");
		assertEquals(
				ValueFactory
						.create("[type=box, def=def, nb=sg, attr=big, attr=yellow]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("take the big and yellow box");
		assertEquals(
				ValueFactory
						.create("[type=box, def=def, nb=sg, attr=big, attr=yellow]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("take the big box on your left");
		assertEquals(
				ValueFactory
						.create("[rel=left(agent), type=box, def=def, nb=sg, attr=big]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("take the big box on the left");
		assertEquals(0.5,
				system.getContent("properties(ref_main)").toDiscrete().getProb(
						"[rel=left(agent), type=box, def=def, nb=sg, attr=big]"),
				0.01);
		assertEquals(0.5,
				system.getContent("properties(ref_main)").toDiscrete().getProb(
						"[rel=left(spk), type=box, def=def, nb=sg, attr=big]"),
				0.01);
		system.addUserInput("take one box now");
		assertEquals(ValueFactory.create("[def=indef, nb=sg, type=box]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("take the small and ugly box ");
		assertEquals(
				ValueFactory
						.create("[type=box, def=def, nb=sg, attr=small, attr=ugly]"),
				system.getContent("properties(ref_main)").getBest());
		system.addUserInput("now please pick up the book that is behind you");
		assertEquals(
				ValueFactory
						.create("[type=book, def=def, nb=sg, rel=behind(ref_behind)]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("you"),
				system.getContent("ref_behind").getBest());
		assertEquals(ValueFactory.create("the book"),
				system.getContent("ref_main").getBest());
		system.addUserInput("could you take the red ball on the desk");
		assertEquals(
				ValueFactory
						.create("[type=ball, attr=red, rel=on(ref_on), def=def, nb=sg]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("the red ball"),
				system.getContent("ref_main").getBest());
		assertEquals(ValueFactory.create("the desk"),
				system.getContent("ref_on").getBest());
		system.addUserInput("could you take the red ball next to the window");
		assertEquals(
				ValueFactory
						.create("[type=ball, attr=red, rel=next to(ref_next to), def=def, nb=sg]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("the red ball"),
				system.getContent("ref_main").getBest());
		assertEquals(ValueFactory.create("the window"),
				system.getContent("ref_next to").getBest());

		system.addUserInput(
				"could you take the big red ball near the window to your left");
		assertEquals(
				ValueFactory
						.create("[type=ball, attr=red, attr=big, rel=near(ref_near), def=def, nb=sg]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("the big red ball"),
				system.getContent("ref_main").getBest());
		assertEquals(
				ValueFactory
						.create("[type=window, rel=left(agent), def=def, nb=sg]"),
				system.getContent("properties(ref_near)").getBest());
		assertEquals(ValueFactory.create("the window"),
				system.getContent("ref_near").getBest());

		system.addUserInput(
				"could you take the big red ball near the window and to your left");
		assertEquals(
				ValueFactory
						.create("[type=ball, attr=red, attr=big, rel=left(agent), rel=near(ref_near), def=def, nb=sg]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("the big red ball"),
				system.getContent("ref_main").getBest());
		assertEquals(ValueFactory.create("[type=window, def=def, nb=sg]"),
				system.getContent("properties(ref_near)").getBest());
		assertEquals(ValueFactory.create("the window"),
				system.getContent("ref_near").getBest());

		system.addUserInput(
				"and now pick up the books that are on top of the shelf");
		assertEquals(
				ValueFactory.create("[type=book, rel=top(ref_top), def=def, nb=pl]"),
				system.getContent("properties(ref_main)").getBest());
		assertEquals(ValueFactory.create("[type=shelf,def=def, nb=sg]"),
				system.getContent("properties(ref_top)").getBest());
		system.addUserInput("and now pick up one book which is big");
		assertEquals(ValueFactory.create("[type=book,def=indef, attr=big, nb=sg]"),
				system.getContent("properties(ref_main)").getBest());

		Map<String, Double> nbest = new HashMap<String, Double>();
		nbest.put("and take the red book", 0.5);
		nbest.put("and take the wred hook", 0.1);
		system.addUserInput(nbest);
		assertEquals(0.5, system.getContent("properties(ref_main)")
				.getProb("[type=book,attr=red,def=def,nb=sg]"), 0.01);
		assertEquals(0.1, system.getContent("properties(ref_main)")
				.getProb("[type=hook,attr=wred,def=def,nb=sg]"), 0.01);
	}

	@Test
	public void resolutionTest() throws InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.startSystem();

		system.addUserInput("take the red ball");
		assertEquals("Select(object_1)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.94,
				system.getContent("matches(ref_main)").getProb("[object_1]"), 0.05);

		system.addUserInput("take the red object");
		assertEquals("AskConfirm(object_1)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.39, system.getContent("matches(ref_main)")
				.getProb("[object_1,object_3]"), 0.05);
		assertEquals(0.108,
				system.getContent("matches(ref_main)").getProb("[object_1]"), 0.05);

		system.addUserInput("take the box");
		assertEquals("Select(object_2)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.7,
				system.getContent("matches(ref_main)").getProb("[object_2]"), 0.05);
		assertEquals(0.3, system.getContent("matches(ref_main)").getProb("[]"),
				0.05);

		Map<String, Double> nbest = new HashMap<String, Double>();
		nbest.put("and take the ball now", 0.3);
		system.addUserInput(nbest);
		assertEquals("AskConfirm(object_1)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.27,
				system.getContent("matches(ref_main)").getProb("[object_1]"), 0.005);
		system.addUserInput("yes");
		assertEquals("Select(object_1)",
				system.getContent("a_m").getBest().toString());

		system.addUserInput("pick up the newspaper");
		assertEquals("Failed(the newspaper)",
				system.getContent("a_m").getBest().toString());

		system.addUserInput("pick up an object");
		assertEquals("AskConfirm(object_1)",
				system.getContent("a_m").getBest().toString());
		assertEquals(1.0, system.getContent("matches(ref_main)")
				.getProb("[object_1,object_2,object_3]"), 0.005);
		system.addUserInput("no");
		assertEquals("AskConfirm(object_2)",
				system.getContent("a_m").getBest().toString());
		system.addUserInput("yes");
		assertEquals("Select(object_2)",
				system.getContent("a_m").getBest().toString());

		system.addUserInput("pick up the ball to the left of the box");
		assertEquals("Select(object_1)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.75,
				system.getContent("matches(ref_main)").getProb("[object_1]"), 0.05);

		system.addUserInput("pick up the box to the left of the ball");
		assertEquals("Failed(the box)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.34,
				system.getContent("matches(ref_main)").getProb("[object_2]"), 0.05);
	}

	@Test
	public void underspecTest1() throws InterruptedException {
		Domain domain =
				XMLDomainReader.extractDomain("test/domains/underspectest.xml");
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		StatePruner.ENABLE_REDUCTION = false;
		system.startSystem();
		assertEquals(0.66, system.getContent("match").getProb("obj_1"), 0.05);
		assertEquals(0.307, system.getContent("match").getProb("obj_3"), 0.05);
		assertEquals(14, system.getState().getChanceNodeIds().size());
		StatePruner.ENABLE_REDUCTION = true;
	}
}
