package opendial.inference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.bn.distribs.CategoricalTable;
import opendial.domains.Domain;
import opendial.domains.rules.distribs.AnchoredRule;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class RuleAndParams {

	public static final String domainFile = "test//domains//rulesandparams.xml";

	@Test
	public void RuleAndParamsTest() throws InterruptedException {

		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.startSystem();
		assertEquals(system.getContent("theta_moves").toContinuous().getFunction()
				.getMean()[0], 0.2, 0.02);
		assertEquals(system.getContent("a_u^p").getProb("I want left"), 0.12, 0.03);
		assertEquals(system.getState().getChanceNode("theta_moves")
				.getOutputNodesIds().size(), 1);
		assertTrue(system.getState().hasChanceNode("movements"));
		assertTrue(system.getState().getChanceNode("movements")
				.getDistrib() instanceof AnchoredRule);
		CategoricalTable.Builder t = new CategoricalTable.Builder("a_u");
		t.addRow("I want left", 0.8);
		t.addRow("I want forward", 0.1);
		system.addContent(t.build());
		assertEquals(system.getState().getChanceNode("theta_moves")
				.getOutputNodesIds().size(), 0);
		assertFalse(system.getState().hasChanceNode("movements"));
		assertEquals(system.getContent("theta_moves").toContinuous().getFunction()
				.getMean()[0], 2.0 / 6, 0.07);
		system.addContent("a_m", "turning left");
		assertEquals(system.getContent("a_u^p").getProb("I want left"), 0.23, 0.04);
		assertEquals(system.getState().getChanceNode("theta_moves")
				.getOutputNodesIds().size(), 1);
	}
}
