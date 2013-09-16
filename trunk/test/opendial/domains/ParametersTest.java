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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.values.ValueFactory;
import opendial.bn.values.VectorVal;
import opendial.common.InferenceChecks;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.rules.parameters.CompositeParameter;
import opendial.domains.rules.parameters.DirichletParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.gui.GUIFrame;
import opendial.inference.ImportanceSampling;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;
import opendial.state.rules.Rule;

public class ParametersTest {

	// logger
	public static Logger log = new Logger("ParametersTest", Logger.Level.DEBUG);
	
	public static final String domainFile = "domains//testing//testwithparams.xml";
	public static final String domainFile2 = "domains//testing//testwithparams2.xml";
	public static final String paramFile = "domains//testing//params.xml";

	
	@Test
	public void paramTest1() throws DialException, InterruptedException {
		Settings.getInstance().activatePlanner = false;
	//	Settings.getInstance().gui.showGUI = true;
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.addParameters(params);
		
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
		UtilityTable utils = ((new ImportanceSampling()).queryUtil(new UtilQuery(system.getState(), "u_m'")));
		assertTrue(utils.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) > 0);
		assertTrue(utils.getUtil(new Assignment("u_m'", "so interesting!")) > 1.7);
		assertTrue(utils.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) < 
				utils.getUtil(new Assignment("u_m'", "so interesting!")));
//		assertEquals(11, system.getState().getNetwork().getNodeIds().size());
		assertEquals(12, system.getState().getNetwork().getNodeIds().size());
		Settings.getInstance().activatePlanner = true;
//		Thread.sleep(30000000);
	}
	
	
	@Test
	public void paramTest2() throws DialException, InterruptedException {
		Settings.getInstance().activatePlanner = false;
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.addParameters(params);
		
		assertTrue(system.getState().getNetwork().hasChanceNode("theta_3"));
		InferenceChecks inference = new InferenceChecks();
		inference.EXACT_THRESHOLD = 0.1;
		ProbQuery query = new ProbQuery(system.getState(), "theta_3");
		inference.checkCDF(query, new Assignment("theta_3", 0.6), 0.0);
		inference.checkCDF(query, new Assignment("theta_3", 0.8), 0.5);
		inference.checkCDF(query, new Assignment("theta_3", 0.95), 1.0);

		system.startSystem();
		system.getState().addContent(new Assignment("u_u", "brilliant"), "test");
		ProbDistribution distrib = system.getState().getContent("a_u", true);
		Settings.getInstance().activatePlanner = true;

		assertEquals(0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u", "Approval")), 0.05);
	}
	

	@Test
	public void paramTest3() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.getState().activateDecisions(false);
		system.addParameters(params);
		
		List<Rule> rules = new ArrayList<Rule>(domain.getModels().get(1).getRules());
		OutputTable outputs = rules.get(1).getEffectOutputs(new Assignment("u_u", "no no"));
		Output o = new Output();
		o.setValueForVariable("a_u", ValueFactory.create("Disapproval"));
		assertTrue(outputs.getParameter(o) instanceof DirichletParameter);
		Assignment input = new Assignment("theta_4", ValueFactory.create("(0.36, 0.64)"));
		assertEquals(0.64, outputs.getParameter(o).getParameterValue(input), 0.01);
		
		system.getState().addContent(new Assignment("u_u", "no no"), "test");

		assertEquals(0.36, ((MultivariateDistribution)system.getState().
				getContent("theta_4", true).toContinuous()).getMean()[0], 0.1);
		
		assertEquals(0.64, system.getState().getContent("a_u", true).toDiscrete().
				getProb(new Assignment(), new Assignment("a_u", "Disapproval")), 0.1);
	}
	
	
	@Test
	public void paramTest4() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.getState().activateDecisions(false);
		system.addParameters(params);
		
		List<Rule> rules = new ArrayList<Rule>(domain.getModels().get(2).getRules());
		OutputTable outputs = rules.get(0).getEffectOutputs(new Assignment("u_u", "my name is"));
		Output o = new Output();
		o.setValueForVariable("u_u^p", ValueFactory.create("Pierre"));
		assertTrue(outputs.getParameter(o) instanceof DirichletParameter);
		Assignment input = new Assignment("theta_5", ValueFactory.create("(0.36, 0.24, 0.40)"));
		assertEquals(0.36, outputs.getParameter(o).getParameterValue(input), 0.01);
				
	 	system.getState().addContent(new Assignment("u_u", "my name is"), "test");
		
	 	system.getState().addContent(new Assignment("u_u", "Pierre"), "test");
		
		system.getState().addContent(new Assignment("u_u", "my name is"), "test");
		
		system.getState().addContent(new Assignment("u_u", "Pierre"), "test");
		
		assertEquals(0.3, ((MultivariateDistribution)system.getState().
				getContent("theta_5", true).toContinuous()).getMean()[0], 0.12); 
	}
	
	

	@Test
	public void paramTest5() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile2);
		DialogueSystem system = new DialogueSystem(domain);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		system.getState().activateDecisions(false);
		system.addParameters(params);
		
		List<Rule> rules = new ArrayList<Rule>(domain.getModels().get(0).getRules());
		OutputTable outputs = rules.get(0).getEffectOutputs(new Assignment("u_u", "brilliant"));
		Output o = new Output();
		o.setValueForVariable("a_u", ValueFactory.create("Approval"));
		assertTrue(outputs.getParameter(o) instanceof CompositeParameter);
		Assignment input = new Assignment(new Assignment("theta_6", 2.1), new Assignment("theta_7", 1.3));
		assertEquals(3.4, outputs.getParameter(o).getParameterValue(input), 0.01);
		
		system.getState().addContent(new Assignment("u_u", "brilliant"), "test");

		assertEquals(1.0, ((UnivariateDistribution)system.getState().
				getContent("theta_6", true).toContinuous()).getMean(), 0.06);
		
		assertEquals(0.72, ((SimpleTable)system.getState().getContent("a_u", true).toDiscrete()).
				getProb(new Assignment("a_u", "Approval")), 0.06);
		
		assertEquals(0.28, ((SimpleTable)system.getState().getContent("a_u", true).toDiscrete()).
				getProb(new Assignment("a_u", "Irony")), 0.06);
	}
	
}

