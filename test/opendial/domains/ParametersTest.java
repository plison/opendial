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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import opendial.DialogueSystem;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.rules.Rule;
import opendial.domains.rules.RuleOutput;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.parameters.ComplexParameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

import org.junit.Test;

public class ParametersTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testParam1() throws InterruptedException {

		inference.EXACT_THRESHOLD = 0.1;

		// Settings.gui.showGUI = true;

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		assertTrue(system.getState().hasChanceNode("theta_1"));
		inference.checkCDF(system.getState(), "theta_1", 0.5, 0.5);
		inference.checkCDF(system.getState(), "theta_1", 5, 0.99);

		inference.checkCDF(system.getState(), "theta_2", 1, 0.07);
		inference.checkCDF(system.getState(), "theta_2", 2, 0.5);

		system.startSystem();
		system.addContent("u_u", "hello there");
		UtilityTable utils =
				((new SamplingAlgorithm()).queryUtil(system.getState(), "u_m'"));
		assertTrue(utils
				.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) > 0);
		assertTrue(utils.getUtil(new Assignment("u_m'", "so interesting!")) > 1.7);
		assertTrue(utils
				.getUtil(new Assignment("u_m'", "yeah yeah talk to my hand")) < utils
						.getUtil(new Assignment("u_m'", "so interesting!")));
		assertEquals(11, system.getState().getNodeIds().size());

		// Thread.sleep(30000000);
	}

	@Test
	public void testParam2() throws InterruptedException {

		inference.EXACT_THRESHOLD = 0.1;

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;

		assertTrue(system.getState().hasChanceNode("theta_3"));
		inference.checkCDF(system.getState(), "theta_3", 0.6, 0.0);
		inference.checkCDF(system.getState(), "theta_3", 0.8, 0.5);
		inference.checkCDF(system.getState(), "theta_3", 0.95, 1.0);

		system.startSystem();
		system.addContent("u_u", "brilliant");
		IndependentDistribution distrib = system.getContent("a_u");

		assertEquals(0.8, distrib.getProb("Approval"), 0.05);

	}

	@Test
	public void testParam3() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();

		List<Rule> rules =
				new ArrayList<Rule>(domain1.getModels().get(0).getRules());
		RuleOutput outputs = rules.get(1).getOutput(new Assignment("u_u", "no no"));
		Effect o = new Effect(new BasicEffect("a_u", "Disapproval"));
		assertTrue(outputs.getParameter(o) instanceof SingleParameter);
		Assignment input =
				new Assignment("theta_4", ValueFactory.create("[0.36, 0.64]"));
		assertEquals(0.64, outputs.getParameter(o).getValue(input), 0.01);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "no no");
		assertEquals(0.36, system.getState().queryProb("theta_4").toContinuous()
				.getFunction().getMean()[0], 0.1);

		assertEquals(0.64, system.getContent("a_u").getProb("Disapproval"), 0.1);

	}

	@Test
	public void paramTest4() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain1);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();

		List<Rule> rules =
				new ArrayList<Rule>(domain1.getModels().get(1).getRules());
		RuleOutput outputs =
				rules.get(0).getOutput(new Assignment("u_u", "my name is"));
		Effect o = new Effect(new BasicEffect("u_u^p", "Pierre"));
		assertTrue(outputs.getParameter(o) instanceof SingleParameter);
		Assignment input =
				new Assignment("theta_5", ValueFactory.create("[0.36, 0.24, 0.40]"));
		assertEquals(0.36, outputs.getParameter(o).getValue(input), 0.01);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "my name is");

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "Pierre");

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "my name is");

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "Pierre");

		assertEquals(0.3, system.getState().queryProb("theta_5").toContinuous()
				.getFunction().getMean()[0], 0.12);
	}

	@Test
	public void testParam5() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain2);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem();

		List<Rule> rules =
				new ArrayList<Rule>(domain2.getModels().get(0).getRules());
		RuleOutput outputs =
				rules.get(0).getOutput(new Assignment("u_u", "brilliant"));
		Effect o = new Effect(new BasicEffect("a_u", "Approval"));
		assertTrue(outputs.getParameter(o) instanceof ComplexParameter);
		Assignment input = new Assignment(new Assignment("theta_6", 2.1),
				new Assignment("theta_7", 1.3));
		assertEquals(0.74, outputs.getParameter(o).getValue(input), 0.01);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent("u_u", "brilliant");

		assertEquals(1.0, system.getState().queryProb("theta_6").toContinuous()
				.getFunction().getMean()[0], 0.08);

		assertEquals(0.63, system.getContent("a_u").getProb("Approval"), 0.08);
		assertEquals(0.3, system.getContent("a_u").getProb("Irony"), 0.08);

	}

	@Test
	public void testParam6() {
		DialogueSystem system = new DialogueSystem(
				XMLDomainReader.extractDomain("test/domains/testparams3.xml"));
		system.getSettings().showGUI = false;
		system.startSystem();
		CategoricalTable table = system.getContent("b").toDiscrete();
		assertEquals(6, table.size());
		assertEquals(0.45, table.getProb("something else"), 0.05);
		assertEquals(0.175, table.getProb("value: first with type 1"), 0.05);
		assertEquals(0.05, table.getProb("value: second with type 2"), 0.05);
	}

}
