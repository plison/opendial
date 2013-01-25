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

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.common.InferenceChecks;
import opendial.inference.ImportanceSampling;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

public class ParametersTest {

	// logger
	public static Logger log = new Logger("ParametersTest", Logger.Level.DEBUG);
	
	public static final String domainFile = "domains//testing//testwithparams.xml";
	public static final String paramFile = "domains//testing//params.xml";

	
	@Test
	public void paramTest1() throws DialException {
		
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.addParameters(params);
		Settings.planningHorizon = 1;
		Settings.activatePlanner = true;
		Settings.activatePruning = true;
		
		assertTrue(system.getState().getNetwork().hasChanceNode("theta_1"));
		InferenceChecks inference = new InferenceChecks();
		inference.EXACT_THRESHOLD = 0.1;
		ProbQuery query = new ProbQuery(system.getState(), "theta_1");
		inference.checkCDF(query, new Assignment("theta_1", 0.5), 0.5);
		inference.checkCDF(query, new Assignment("theta_1", 5), 0.99);
		
		query = new ProbQuery(system.getState(), "theta_2");
		inference.checkCDF(query, new Assignment("theta_2", 1), 0.07);
		inference.checkCDF(query, new Assignment("theta_2", 2), 0.5);
		
		system.startSystem();
		system.getState().addContent(new Assignment("u_u", "hello there"), "test");
		assertEquals(4, system.getState().getNetwork().getNodeIds().size());
		
	}

}

