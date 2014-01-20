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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.Rule;
import opendial.domains.rules.RuleCase;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.CompositeParameter;
import opendial.domains.rules.parameters.StochasticParameter;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

import org.junit.Test;

public class ParametersTest {

	// logger
	public static Logger log = new Logger("ParametersTest", Logger.Level.DEBUG);
	
	public static final String domainFile = "test//domains//testwithparams.xml";
	public static final String domainFile2 = "test//domains//testwithparams2.xml";
	public static final String paramFile = "test//domains//params.xml";

	
	static InferenceChecks inference;
	static Domain domain1;
	static Domain domain2;
	static BNetwork params;

	static {
		try { 
			params = XMLStateReader.extractBayesianNetwork(paramFile, "parameters");

			domain1 = XMLDomainReader.extractDomain(domainFile); 
			domain1.setParameters(params);

			domain2 = XMLDomainReader.extractDomain(domainFile2); 
			domain2.setParameters(params);

			inference = new InferenceChecks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	@Test
	public void testParam1() throws DialException, InterruptedException {

		inference.EXACT_THRESHOLD = 0.1;

		//	Settings.gui.showGUI = true;

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		
		assertTrue(system.getState().hasChanceNode("theta_1"));
		ProbQuery query = new ProbQuery(system.getState(), "theta_1");
		inference.checkCDF(query, new Assignment("theta_1", 0.5), 0.5);
		inference.checkCDF(query, new Assignment("theta_1", 5), 0.99);
		
		query = new ProbQuery(system.getState(), "theta_2");
		inference.checkCDF(query, new Assignment("theta_2", 1), 0.07);
		inference.checkCDF(query, new Assignment("theta_2", 2), 0.5);
		
		system.startSystem();
		system.addContent(new Assignment("u_u", "hello there"));
		UtilityTable utils = ((new LikelihoodWeighting()).queryUtil(new UtilQuery(system.getState(), "u_m'")));
		assertTrue(utils.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) > 0);
		assertTrue(utils.getUtil(new Assignment("u_m'", "so interesting!")) > 1.7);
		assertTrue(utils.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) < 
				utils.getUtil(new Assignment("u_m'", "so interesting!")));
		assertEquals(12, system.getState().getNodeIds().size());

		//		Thread.sleep(30000000);
	}
	
	
	@Test
	public void testParam2() throws DialException, InterruptedException {

		inference.EXACT_THRESHOLD = 0.1;
		
		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		
		assertTrue(system.getState().hasChanceNode("theta_3"));
		ProbQuery query = new ProbQuery(system.getState(), "theta_3");
		inference.checkCDF(query, new Assignment("theta_3", 0.6), 0.0);
		inference.checkCDF(query, new Assignment("theta_3", 0.8), 0.5);
		inference.checkCDF(query, new Assignment("theta_3", 0.95), 1.0);

		system.startSystem();
		system.addContent(new Assignment("u_u", "brilliant"));
		ProbDistribution distrib = system.getContent("a_u");

		assertEquals(0.8, distrib.toDiscrete().getProb(new Assignment(), new Assignment("a_u", "Approval")), 0.05);

}
	

	@Test
	public void testParam3() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();
		
		List<Rule> rules = new ArrayList<Rule>(domain1.getModels().get(0).getRules());
		RuleCase outputs = rules.get(1).getMatchingCase(new Assignment("u_u", "no no"));
		Effect o = new Effect();
		o.addSubEffect(new BasicEffect(new Template("a_u"), new Template("Disapproval"), EffectType.SET));
		assertTrue(outputs.getParameter(o) instanceof StochasticParameter);
		Assignment input = new Assignment("theta_4", ValueFactory.create("[0.36, 0.64]"));
		assertEquals(0.64, outputs.getParameter(o).getParameterValue(input), 0.01);
		
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(new Assignment("u_u", "no no"));
		assertEquals(0.36, system.getState().
				queryProb("theta_4").toContinuous().getFunction().getMean()[0], 0.1);
		
		assertEquals(0.64, system.getContent("a_u").toDiscrete().
				getProb(new Assignment("a_u", "Disapproval")), 0.1);
		
}
	
	
	@Test
	public void paramTest4() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();
	
		List<Rule> rules = new ArrayList<Rule>(domain1.getModels().get(1).getRules());
		RuleCase outputs = rules.get(0).getMatchingCase(new Assignment("u_u", "my name is"));
		Effect o = new Effect();
		o.addSubEffect(new BasicEffect(new Template("u_u^p"), new Template("Pierre"), EffectType.SET));		
		assertTrue(outputs.getParameter(o) instanceof StochasticParameter);
		Assignment input = new Assignment("theta_5", ValueFactory.create("[0.36, 0.24, 0.40]"));
		assertEquals(0.36, outputs.getParameter(o).getParameterValue(input), 0.01);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
	 	system.addContent(new Assignment("u_u", "my name is"));

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
	 	system.addContent(new Assignment("u_u", "Pierre"));

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(new Assignment("u_u", "my name is"));
		
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(new Assignment("u_u", "Pierre"));
		
		assertEquals(0.3,system.getState().
				queryProb("theta_5").toContinuous().getFunction().getMean()[0], 0.12); 
	}
	
	

	@Test
	public void testParam5() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();

		List<Rule> rules = new ArrayList<Rule>(domain2.getModels().get(0).getRules());
		RuleCase outputs = rules.get(0).getMatchingCase(new Assignment("u_u", "brilliant"));
		Effect o = new Effect();
		o.addSubEffect(new BasicEffect(new Template("a_u"), new Template("Approval"), EffectType.SET));
		
		assertTrue(outputs.getParameter(o) instanceof CompositeParameter);
		Assignment input = new Assignment(new Assignment("theta_6", 2.1), new Assignment("theta_7", 1.3));
		assertEquals(3.4, outputs.getParameter(o).getParameterValue(input), 0.01);
		
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(new Assignment("u_u", "brilliant"));
		
		assertEquals(1.0, system.getState(). 
				queryProb("theta_6").toContinuous().getFunction().getMean()[0], 0.07);
		
		assertEquals(0.72, system.getContent("a_u").toDiscrete().
				getProb(new Assignment("a_u", "Approval")), 0.07);
		
		assertEquals(0.28, system.getContent("a_u").toDiscrete().
				getProb(new Assignment("a_u", "Irony")), 0.07);
		
}
	
}

