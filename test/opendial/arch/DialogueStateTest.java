// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.arch;


import org.junit.Test;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.common.InferenceChecks;
import opendial.domains.Domain;
import opendial.domains.datastructs.Output;
import opendial.inference.queries.ProbQuery;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;

public class DialogueStateTest {

	// logger
	public static Logger log = new Logger("DialogueStateTest", Logger.Level.DEBUG);

	public static final String domainFile = "domains//testing//domain1.xml";

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
	public void stateCopyTest() throws DialException, InterruptedException {

		Settings.getInstance().activatePlanner = false;	
		Settings.getInstance().activatePruning = false;
		DialogueSystem	system = new DialogueSystem(domain);
		system.startSystem(); 

		DialogueState initialState = system.getState().copy();
		
		String ruleId = "";
		for (String id : system.getState().getNetwork().getNode("u_u2").getOutputNodesIds()) {
			if (system.getState().getContent(id, true).toString().contains("+=HowAreYou")) {
				ruleId = id;
			}
		}
		ProbQuery query = new ProbQuery(initialState, ruleId);			

		inference.checkProb(query, new Assignment(ruleId, Output.parseOutput("a_u2+=HowAreYou")), 0.9);
		inference.checkProb(query, new Assignment(ruleId, Output.parseOutput("Void")), 0.1);

		query = new ProbQuery(initialState,"a_u2'");			

		inference.checkProb(query, new Assignment("a_u2'", "[HowAreYou]"), 0.2);
		inference.checkProb(query, new Assignment("a_u2'", "[HowAreYou, Greet]"), 0.7);
		inference.checkProb(query, new Assignment("a_u2'", "None"), 0.1); 	
		
		Settings.getInstance().activatePlanner = true;	
		Settings.getInstance().activatePruning = true;
	}


	@Test
	public void stateCopyTest2() throws DialException, InterruptedException {

		DialogueSystem	system = new DialogueSystem(domain);
		Settings.getInstance().activatePlanner = false;	
		inference.EXACT_THRESHOLD = 0.08;
		system.startSystem(); 

		DialogueState initialState = system.getState().copy();

		ProbQuery query = new ProbQuery(initialState,"a_u2");			

		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou]"), 0.2);
		inference.checkProb(query, new Assignment("a_u2", "[HowAreYou, Greet]"), 0.7);
		inference.checkProb(query, new Assignment("a_u2", "None"), 0.1); 	
		
		Settings.getInstance().activatePlanner = true;	
	}

}

