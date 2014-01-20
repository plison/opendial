// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.domains;


import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.domains.rules.effects.Effect;
import opendial.inference.queries.ProbQuery;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.state.StatePruner;

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

