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

package opendial.domains;


import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.common.InferenceChecks;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.readers.XMLDomainReader;

public class QuantificationTest {

	// logger
	public static Logger log = new Logger("QuantificationTest", Logger.Level.DEBUG);

	public static final String domainFile = "domains//testing//domain5.xml";

	static InferenceChecks inference;
	static DialogueSystem system;

	static {
		try { 
			Domain domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
			Settings.activatePlanner = false;
			Settings.activatePruning =true;
			inference.EXACT_THRESHOLD = 0.05;
			system = new DialogueSystem(domain);
			system.startSystem(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Test
	public void test() throws DialException {

		ProbQuery query = new ProbQuery(system.getState().getNetwork(),"found");
		inference.checkProb(query, new Assignment("found", "A"), 0.7);

		query = new ProbQuery(system.getState().getNetwork(),"found2");
		inference.checkProb(query, new Assignment("found2", "D"), 0.3);
		inference.checkProb(query, new Assignment("found2", "C"), 0.5);
}

}

