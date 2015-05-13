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

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.common.InferenceChecks;
import opendial.domains.rules.effects.Effect;
import opendial.modules.ForwardPlanner;
import opendial.modules.StatePruner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class DialogueStateTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//domain1.xml";

	static Domain domain;
	static InferenceChecks inference;

	static {
		try {
			domain = XMLDomainReader.extractDomain(domainFile);
			inference = new InferenceChecks();
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testStateCopy() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_REDUCTION = false;

		system.getSettings().showGUI = false;
		system.startSystem();

		DialogueState initialState = system.getState().copy();

		String ruleId = "";
		for (String id : system.getState().getNode("u_u2").getOutputNodesIds()) {
			if (system.getContent(id).toString().contains("+=HowAreYou")) {
				ruleId = id;
			}
		}

		inference.checkProb(initialState, ruleId,
				Effect.parseEffect("a_u2+=HowAreYou"), 0.9);
		inference.checkProb(initialState, ruleId, Effect.parseEffect("Void"), 0.1);

		inference.checkProb(initialState, "a_u2", "[HowAreYou]", 0.2);
		inference.checkProb(initialState, "a_u2", "[Greet, HowAreYou]", 0.7);
		inference.checkProb(initialState, "a_u2", "[]", 0.1);

		StatePruner.ENABLE_REDUCTION = true;
	}

	@Test
	public void testStateCopy2() throws InterruptedException {

		inference.EXACT_THRESHOLD = 0.08;

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		system.startSystem();

		DialogueState initialState = system.getState().copy();

		inference.checkProb(initialState, "a_u2", "[HowAreYou]", 0.2);
		inference.checkProb(initialState, "a_u2", "[Greet, HowAreYou]", 0.7);
		inference.checkProb(initialState, "a_u2", "[]", 0.1);

	}

}
