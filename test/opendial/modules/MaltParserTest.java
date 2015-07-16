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

import java.util.logging.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import opendial.DialogueSystem;
import opendial.bn.values.ValueFactory;
import opendial.plugins.MaltParser;
import opendial.plugins.ParseValue;
import opendial.readers.XMLDomainReader;

public class MaltParserTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String DOMAIN_FILE = "test/domains/parsingtest.xml";
	public static final String TAGGING_MODEL =
			"resources/english-left3words-distsim.tagger";
	public static final String PARSING_MODEL = "resources/engmalt.linear-1.7.mco";

	// @Test
	public void parsingTest() throws InterruptedException {
		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(DOMAIN_FILE));
		system.getSettings().showGUI = false;
		system.getSettings().params.setProperty("taggingmodel", TAGGING_MODEL);
		system.getSettings().params.setProperty("parsingmodel", PARSING_MODEL);
		system.attachModule(MaltParser.class);
		system.startSystem();
		system.addUserInput("move to the left");
		assertTrue(system.getState().hasChanceNode("parse(u_u)"));
		assertTrue(system.getState().hasChanceNode("a_u"));
		assertEquals(system.getContent("a_u").toDiscrete().getBest().toString(),
				"Move(left)");
		system.addUserInput("what do you see now?");
		assertTrue(system.getState().hasChanceNode("parse(u_u)"));
		assertEquals(system.getContent("a_u").toDiscrete().getBest().toString(),
				"Move(left)");
		Map<String, Double> table = new HashMap<String, Double>();
		table.put("move a little bit to the left", 0.7);
		table.put("move a bit to the left", 0.1);
		system.addUserInput(table);
		assertTrue(system.getState().hasChanceNode("parse(u_u)"));
		assertTrue(system.getState().hasChanceNode("a_u"));
		assertEquals(system.getContent("a_u").toDiscrete().getProb("Move(left)"),
				0.8, 0.01);

		table = new HashMap<String, Double>();
		table.put("now move a bit to the right please", 0.6);
		system.addUserInput(table);
		assertTrue(system.getState().hasChanceNode("parse(u_u)"));
		assertTrue(system.getState().hasChanceNode("a_u"));
		assertEquals(system.getContent("a_u").toDiscrete().getProb("Move(right)"),
				0.6, 0.01);
		ParseValue pv = (ParseValue) system.getContent("parse(u_u)").getValues()
				.stream().filter(v -> v instanceof ParseValue).findFirst().get();
		assertTrue(pv.contains(ValueFactory.create("TO DT JJ")));
		assertFalse(pv.contains(ValueFactory.create("DT TT JJ")));
		assertTrue(pv.contains(ValueFactory.create("(*,the,*,det,7)")));
		assertTrue(pv.contains(ValueFactory.create("(7,*,JJ,*,*)")));
		assertTrue(pv.contains(ValueFactory.create("(*,*,DT,det,7)")));
		assertTrue(pv.contains(ValueFactory.create("(7,right,*,*,*)")));
		assertFalse(pv.contains(ValueFactory.create("(7,left,*,*,*)")));
		assertTrue(pv.contains(ValueFactory.create("TO the JJ")));
		assertTrue(pv.contains(ValueFactory.create("to the JJ")));
		assertTrue(pv.contains(ValueFactory.create("TO DT right")));
		assertFalse(pv.contains(ValueFactory.create("TO DT left")));
		assertTrue(pv.contains(ValueFactory.create("to/TO the/DT right/JJ")));
		assertFalse(pv.contains(ValueFactory.create("to/JJ the/DT right/JJ")));
		assertTrue(pv.contains(ValueFactory.create("(*,the,DT,det,7)")));
		assertTrue(pv.contains(ValueFactory.create("(7,right,JJ,*,*)")));
		assertTrue(pv.contains(ValueFactory.create("JJ")));
		assertFalse(pv.contains(ValueFactory.create("RBR")));
		table = new HashMap<String, Double>();
		table.put("this is a gnome", 0.6);
		system.addUserInput(table);
		pv = (ParseValue) system.getContent("parse(u_u)").getValues().stream()
				.filter(v -> v instanceof ParseValue).findFirst().get();
		assertTrue(pv.contains(ValueFactory.create("DT VBZ DT NN")));
		assertEquals("Test successful",
				system.getContent("i_u").getBest().toString());

	}
}
