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


import static org.junit.Assert.*;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import opendial.arch.ConfigurationSettings;
import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.common.MiscUtils;
import opendial.inference.queries.ProbQuery;
import opendial.readers.XMLDomainReader;

public class PlanningTest {

	// logger
	public static Logger log = new Logger("PruningTest", Logger.Level.DEBUG);


	public static final String domainFile = "domains//testing//domain3.xml";

	static InferenceChecks inference;
	static DialogueSystem system;

	static {
		try { 
			Domain domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
			ConfigurationSettings.getInstance().activatePlanner(true);
			ConfigurationSettings.getInstance().activatePruning(true);
			system = new DialogueSystem(domain);
			system.startSystem(); 
			MiscUtils.waitUntilStable(system);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void planning() throws DialException, InterruptedException {

		assertEquals(3, system.getState().getNetwork().getNodes().size());
		assertEquals(3, system.getState().getNetwork().getChanceNodes().size());
		assertEquals(0, system.getState().getEvidence().getVariables().size());
		inference.checkProb(new ProbQuery(system.getState(), "a_m3"), new Assignment("a_m3", "Do"), 1.0);
		inference.checkProb(new ProbQuery(system.getState(), "obj(a_m3)"), new Assignment("obj(a_m3)", "A"), 1.0);
	}


}

