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

import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.common.InferenceChecks;
import opendial.modules.ForwardPlanner;
import opendial.modules.StatePruner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleTest1 {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//domain1.xml";

	static InferenceChecks inference;
	static Domain domain;

	static {
		try {
			domain = XMLDomainReader.extractDomain(domainFile);
			inference = new InferenceChecks();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test1() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();

		inference.checkProb(system.getState(), "a_u", "Greeting", 0.8);
		inference.checkProb(system.getState(), "a_u", "None", 0.2);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test2() {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();

		inference.checkProb(system.getState(), "i_u", "Inform", 0.7 * 0.8);
		inference.checkProb(system.getState(), "i_u", "None", 1 - 0.7 * 0.8);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test3() throws InterruptedException {

		inference.EXACT_THRESHOLD = 0.06;

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();
		inference.checkProb(system.getState(), "direction", "straight", 0.79);
		inference.checkProb(system.getState(), "direction", "left", 0.20);
		inference.checkProb(system.getState(), "direction", "right", 0.01);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test4() {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();

		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.3);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1",
				0.2);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3",
				0.28);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test5() {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();

		inference.checkProb(system.getState(), "o2", "here is value1", 0.35);
		inference.checkProb(system.getState(), "o2", "and value2 is over there",
				0.07);
		inference.checkProb(system.getState(), "o2", "value3, finally", 0.28);

		StatePruner.ENABLE_REDUCTION = true;

	}

	@Test
	public void test6() throws InterruptedException {

		inference.EXACT_THRESHOLD = 0.06;

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();
		CategoricalTable.Builder builder = new CategoricalTable.Builder("var1");
		builder.addRow("value2", 0.9);
		system.addContent(builder.build());

		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.9);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1",
				0.05);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3",
				0.04);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void test7() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;
		system.getSettings().showGUI = false;
		system.startSystem();
		inference.checkProb(system.getState(), "a_u2", "[Greet, HowAreYou]", 0.7);
		inference.checkProb(system.getState(), "a_u2", "[]", 0.1);
		inference.checkProb(system.getState(), "a_u2", "[HowAreYou]", 0.2);

		StatePruner.ENABLE_REDUCTION = true;
	}

}
