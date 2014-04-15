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


import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.rules.effects.Effect;
import opendial.inference.queries.ProbQuery;
import opendial.modules.core.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.state.StatePruner;

import org.junit.Test;

public class DialogueStateTest {

	// logger
	public static Logger log = new Logger("DialogueStateTest", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain1.xml";

	static Domain domain;
	static InferenceChecks inference;

	static {
		try { 
			domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
		} 
		catch (DialException e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testStateCopy() throws DialException, InterruptedException {
	
		DialogueSystem	system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		
		system.getSettings().showGUI = false;
		system.startSystem(); 

		DialogueState initialState = system.getState().copy();
		
		String ruleId = "";
		for (String id : system.getState().getNode("u_u2").getOutputNodesIds()) {
			if (system.getContent(id).toString().contains("+=HowAreYou")) {
				ruleId = id;
			}
		}
		ProbQuery query = new ProbQuery(initialState, ruleId);			

		inference.checkProb(query, new Assignment(ruleId, Effect.parseEffect("a_u2+=HowAreYou")), 0.9);
		inference.checkProb(query, new Assignment(ruleId, Effect.parseEffect("Void")), 0.1);

		query = new ProbQuery(initialState,"a_u2");			
		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou]"), 0.2);
		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou, Greet]"), 0.7);
		inference.checkProb(query, new Assignment("a_u2", "None"), 0.1); 	
		
		StatePruner.ENABLE_PRUNING = true;
	}


	@Test
	public void testStateCopy2() throws DialException, InterruptedException {

		inference.EXACT_THRESHOLD = 0.08;

		DialogueSystem	system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		system.startSystem(); 

		DialogueState initialState = system.getState().copy();

		ProbQuery query = new ProbQuery(initialState,"a_u2");			

		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou]"), 0.2);
		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou, Greet]"), 0.7);
		inference.checkProb(query, new Assignment("a_u2", "None"), 0.1); 
		
		
	}

}

