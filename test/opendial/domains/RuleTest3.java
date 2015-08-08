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
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import opendial.DialogueSystem;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.modules.ForwardPlanner;
import opendial.modules.StatePruner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class RuleTest3 {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test/domains/rulepriorities.xml";

	public static final String test1domainFile = "test//domains//domain5.xml";
	public static final String test2domainFile = "test//domains//domainthesis.xml";
	public static final String predictDomainFile = "test//domains//prediction.xml";
	public static final String inconditionFile = "test//domains//incondition.xml";

	static InferenceChecks inference;
	static DialogueSystem system;

	@Test
	public void priorityTest() throws InterruptedException {

		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = false;
		system.startSystem();
		assertEquals(system.getContent("a_u").getProb("Opening"), 0.8, 0.01);
		assertEquals(system.getContent("a_u").getProb("Nothing"), 0.1, 0.01);
		assertEquals(system.getContent("a_u").getProb("start"), 0.0, 0.01);
		assertFalse(system.getContent("a_u").toDiscrete()
				.hasProb(ValueFactory.create("start")));
	}

	@Test
	public void test1() {

		Domain domain = XMLDomainReader.extractDomain(test1domainFile);
		inference = new InferenceChecks();
		inference.EXACT_THRESHOLD = 0.06;
		system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.startSystem();

		inference.checkProb(system.getState(), "found", "A", 0.7);

		inference.checkProb(system.getState(), "found2", "D", 0.3);
		inference.checkProb(system.getState(), "found2", "C", 0.5);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test2() throws InterruptedException {
		inference = new InferenceChecks();

		Domain domain = XMLDomainReader.extractDomain(test2domainFile);
		system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.startSystem();
		inference.checkProb(system.getState(), "graspable(obj1)", "true", 0.81);

		inference.checkProb(system.getState(), "graspable(obj2)", "true", 0.16);
		inference.checkUtil(system.getState(), "a_m'", "grasp(obj1)", 0.592);
		// inference.checkUtil(system.getState(), "a_m'", "grasp(obj2)", -2.0);
		// inference.checkUtil(system.getState(), "a_m'", "grasp(obj3)", -2.0);

		StatePruner.ENABLE_REDUCTION = true;

	}

	@Test
	public void testOutputs() {

		List<BasicEffect> effects = new ArrayList<BasicEffect>();
		assertEquals(new Effect(effects), Effect.parseEffect("Void"));
		effects.add(new BasicEffect("v1", "val1"));
		assertEquals(new Effect(effects), Effect.parseEffect("v1:=val1"));

		effects.add(
				new BasicEffect("v2", ValueFactory.create("val2"), 1, false, false));
		assertEquals(new Effect(effects), Effect.parseEffect("v1:=val1 ^ v2+=val2"));

		effects.add(
				new BasicEffect("v2", ValueFactory.create("val3"), 1, true, true));
		assertEquals(new Effect(effects),
				Effect.parseEffect("v1:=val1 ^ v2+=val2 ^ v2!=val3"));
	}

	@Test
	public void testIncondition() throws InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(inconditionFile);

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.startSystem();
		assertEquals(0.56,
				system.getContent("out").getProb("val1 is in [val1, val2]") + system
						.getContent("out").getProb("val1 is in [val2, val1]"),
				0.01);
		assertEquals(0.5,
				system.getContent("out2").getProb("this is a string is matched"),
				0.01);
	}

}
