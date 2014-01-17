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


import java.util.Arrays;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.StatePruner;

public class QuantificationTest {

	// logger
	public static Logger log = new Logger("QuantificationTest", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain5.xml";
	public static final String domainFile2 = "test//domains//domainthesis.xml";

	static InferenceChecks inference;
	static DialogueSystem system;



	@Test
	public void test() throws DialException {

			Domain domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
			inference.EXACT_THRESHOLD = 0.06;
			system = new DialogueSystem(domain);
			system.getSettings().showGUI = false;
			system.detachModule(ForwardPlanner.class);
			StatePruner.ENABLE_PRUNING = false;
			system.startSystem(); 
		

		ProbQuery query = new ProbQuery(system.getState(),"found");
		inference.checkProb(query, new Assignment("found", "A"), 0.7);

		query = new ProbQuery(system.getState(),"found2");
		inference.checkProb(query, new Assignment("found2", "D"), 0.3);
		inference.checkProb(query, new Assignment("found2", "C"), 0.5);
		
		StatePruner.ENABLE_PRUNING = true;
}
	

	@Test
	public void thesisTests() throws DialException {
		inference = new InferenceChecks();

		Domain domain = XMLDomainReader.extractDomain(domainFile2); 
		system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.startSystem(); 
		ProbQuery query = new ProbQuery(system.getState(),"graspable(obj1)");
		inference.checkProb(query, new Assignment("graspable(obj1)", "true"), 0.81);

		query = new ProbQuery(system.getState(),"graspable(obj2)");
		inference.checkProb(query, new Assignment("graspable(obj2)", "true"), 0.16);
		UtilQuery query2 = new UtilQuery(system.getState(),"a_m'");
		inference.checkUtil(query2, new Assignment("a_m'", "grasp(obj1)"), 0.592);
		inference.checkUtil(query2, new Assignment("a_m'", "grasp(obj2)"), -2.0);
		inference.checkUtil(query2, new Assignment("a_m'", "grasp(obj3)"), -2.0);
		
		StatePruner.ENABLE_PRUNING = true;
		
	}	

}

