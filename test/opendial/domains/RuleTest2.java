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
import java.util.Arrays;

import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.rules.distribs.EquivalenceDistribution;
import opendial.modules.ForwardPlanner;
import opendial.modules.StatePruner;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleTest2 {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//domain2.xml";
	public static final String domainFile2 = "test//domains//domain3.xml";
	public static final String domainFile3 = "test//domains//domain4.xml";
	public static final String domainFile4 = "test//domains//thesistest2.xml";

	static Domain domain;

	static InferenceChecks inference;

	static {
		try {
			domain = XMLDomainReader.extractDomain(domainFile);
			inference = new InferenceChecks();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;

		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		system.startSystem();
		inference.checkProb(system.getState(), "a_u^p", "Ask(A)", 0.63);
		inference.checkProb(system.getState(), "a_u^p", "Ask(B)", 0.27);
		inference.checkProb(system.getState(), "a_u^p", "None", 0.1);

		CategoricalTable.Builder builder = new CategoricalTable.Builder("a_u");
		builder.addRow("Ask(B)", 0.8);
		builder.addRow("None", 0.2);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());

		system.addContent(builder.build());

		inference.checkProb(system.getState(), "i_u", "Want(A)", 0.090);
		inference.checkProb(system.getState(), "i_u", "Want(B)", 0.91);

		inference.checkProb(system.getState(), "a_u^p", "Ask(B)", 0.91 * 0.9);
		inference.checkProb(system.getState(), "a_u^p", "Ask(A)", 0.09 * 0.9);
		inference.checkProb(system.getState(), "a_u^p", "None", 0.1);

		inference.checkProb(system.getState(), "a_u", "Ask(B)", 0.918);
		inference.checkProb(system.getState(), "a_u", "None", 0.081);

		EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;

	}

	@Test
	public void test2() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;
		system.startSystem();

		inference.checkProb(system.getState(), "u_u2^p", "Do A", 0.216);
		inference.checkProb(system.getState(), "u_u2^p", "Please do C", 0.027);
		inference.checkProb(system.getState(), "u_u2^p", "Could you do B?", 0.054);
		inference.checkProb(system.getState(), "u_u2^p", "Could you do A?", 0.162);
		inference.checkProb(system.getState(), "u_u2^p", "none", 0.19);

		CategoricalTable.Builder builder = new CategoricalTable.Builder("u_u2");
		builder.addRow("Please do B", 0.4);
		builder.addRow("Do B", 0.4);

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(builder.build());

		inference.checkProb(system.getState(), "i_u2", "Want(B)", 0.654);
		inference.checkProb(system.getState(), "i_u2", "Want(A)", 0.1963);
		inference.checkProb(system.getState(), "i_u2", "Want(C)", 0.0327);
		inference.checkProb(system.getState(), "i_u2", "none", 0.1168);

		EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;
	}

	@Test
	public void test3() throws InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;
		system.startSystem();

		inference.checkUtil(system.getState(), "a_m'", "Do(A)", 0.6);
		inference.checkUtil(system.getState(), "a_m'", "Do(B)", -2.6);

		CategoricalTable.Builder builder = new CategoricalTable.Builder("a_u");
		builder.addRow("Ask(B)", 0.8);
		builder.addRow("None", 0.2);
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(builder.build());

		inference.checkUtil(system.getState(), "a_m'", "Do(A)", -4.35);
		inference.checkUtil(system.getState(), "a_m'", "Do(B)", 2.357);

		EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;
	}

	@Test
	public void test4() throws InterruptedException {

		Domain domain2 = XMLDomainReader.extractDomain(domainFile2);
		DialogueSystem system2 = new DialogueSystem(domain2);
		system2.getSettings().showGUI = false;
		system2.detachModule(ForwardPlanner.class);
		system2.startSystem();

		inference.checkUtil(system2.getState(), Arrays.asList("a_m3'", "obj(a_m3)'"),
				new Assignment(new Assignment("a_m3'", "Do"),
						new Assignment("obj(a_m3)'", "A")),
				0.3);
		inference.checkUtil(system2.getState(), Arrays.asList("a_m3'", "obj(a_m3)'"),
				new Assignment(new Assignment("a_m3'", "Do"),
						new Assignment("obj(a_m3)'", "B")),
				-1.7);
		inference.checkUtil(system2.getState(), Arrays.asList("a_m3'", "obj(a_m3)'"),
				new Assignment(new Assignment("a_m3'", "SayHi"),
						new Assignment("obj(a_m3)'", "None")),
				-0.9);
		// assertEquals(5, (new
		// LikelihoodWeighting()).queryUtil(query).getTable().size());
		// assertEquals(6, (new
		// LikelihoodWeighting()).queryUtil(query).getTable().size());

	}

	@Test
	public void test5() throws InterruptedException {

		Domain domain2 = XMLDomainReader.extractDomain(domainFile3);
		DialogueSystem system2 = new DialogueSystem(domain2);
		system2.getSettings().showGUI = false;
		system2.detachModule(ForwardPlanner.class);
		system2.startSystem();

		// log.fine((new VariableElimination()).queryUtility(query));

		inference.checkUtil(system2.getState(),
				Arrays.asList("a_ml'", "a_mg'", "a_md'"),
				new Assignment(new Assignment("a_ml'", "SayYes"),
						new Assignment("a_mg'", "Nod"),
						new Assignment("a_md'", "None")),
				2.4);
		inference.checkUtil(system2.getState(),
				Arrays.asList("a_ml'", "a_mg'", "a_md'"),
				new Assignment(new Assignment("a_ml'", "SayYes"),
						new Assignment("a_mg'", "Nod"),
						new Assignment("a_md'", "DanceAround")),
				-0.6);
		inference.checkUtil(system2.getState(),
				Arrays.asList("a_ml'", "a_mg'", "a_md'"),
				new Assignment(new Assignment("a_ml'", "SayYes"),
						new Assignment("a_mg'", "None"),
						new Assignment("a_md'", "None")),
				1.6);

	}

	@Test
	public void test6() {

		Domain domain2 = XMLDomainReader.extractDomain(domainFile4);
		DialogueSystem system2 = new DialogueSystem(domain2);
		system2.getSettings().showGUI = false;
		system2.detachModule(ForwardPlanner.class);
		system2.startSystem();
		inference.checkProb(system2.getState(), "A", ValueFactory.create("[a1,a2]"),
				1.0);

		inference.checkProb(system2.getState(), "a_u", "Request(ball)", 0.5);
	}

}
