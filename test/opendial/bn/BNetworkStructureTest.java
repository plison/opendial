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

package opendial.bn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.logging.Logger;

import opendial.bn.distribs.CategoricalTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.common.NetworkExamples;
import opendial.datastructs.Assignment;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 * 
 *
 */
public class BNetworkStructureTest {

	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void testBuildBasicNetwork() {

		BNetwork bn = NetworkExamples.constructBasicNetwork();

		assertEquals(8, bn.getNodes().size());

		assertEquals(3, bn.getNode("Burglary").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getInputNodes().size());
		assertEquals(2, bn.getNode("Util1").getInputNodes().size());

		assertEquals(2, bn.getNode("Burglary").getValues().size());
		assertEquals(2, bn.getNode("Alarm").getValues().size());
		assertEquals(2, bn.getNode("MaryCalls").getValues().size());

		assertTrue(bn.getNode("Burglary").getValues()
				.contains(ValueFactory.create(true)));

		assertEquals(0.001f,
				bn.getChanceNode("Burglary").getProb(ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.95f,
				bn.getChanceNode("Alarm").getProb(
						new Assignment(Arrays.asList("Burglary", "Earthquake")),
						ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.9f, bn.getChanceNode("JohnCalls").getProb(
				new Assignment("Alarm"), ValueFactory.create(true)), 0.0001f);

		assertEquals(3, bn.getActionNode("Action").getValues().size());
		assertEquals(-10f,
				bn.getUtilityNode("Util2")
						.getUtility(new Assignment(new Assignment("Burglary"),
								"Action", ValueFactory.create("DoNothing"))),
				0.0001f);
	}

	@Test
	public void testCopy() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		BNetwork bn2 = bn.copy();

		ChanceNode b = bn.getChanceNode("Burglary");
		CategoricalTable.Builder builder = new CategoricalTable.Builder("Burglary");
		builder.addRow(ValueFactory.create(true), 0.2f);
		builder.addRow(ValueFactory.create(false), 0.8f);
		b.setDistrib(builder.build());

		UtilityNode value = bn.getUtilityNode("Util1");
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("DoNothing")), -20.0f);

		assertEquals(3, bn.getNode("Burglary").getOutputNodes().size());
		assertEquals(3, bn2.getNode("Burglary").getOutputNodes().size());

		assertEquals(2, bn.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn2.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getInputNodes().size());
		assertEquals(2, bn2.getNode("Alarm").getInputNodes().size());

		assertEquals(2, bn.getNode("Util1").getInputNodes().size());
		assertEquals(2, bn2.getNode("Util1").getInputNodes().size());

		assertEquals(2, bn2.getNode("Burglary").getValues().size());
		assertEquals(2, bn2.getNode("Alarm").getValues().size());
		assertEquals(2, bn2.getNode("MaryCalls").getValues().size());

		assertTrue(bn2.getNode("Burglary").getValues()
				.contains(ValueFactory.create(true)));

		assertEquals(0.001f,
				bn2.getChanceNode("Burglary").getProb(ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.95f,
				bn2.getChanceNode("Alarm").getProb(
						new Assignment(Arrays.asList("Burglary", "Earthquake")),
						ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.9f, bn2.getChanceNode("JohnCalls").getProb(
				new Assignment("Alarm"), ValueFactory.create(true)), 0.0001f);

		assertEquals(3, bn2.getActionNode("Action").getValues().size());
		assertEquals(-10f,
				bn2.getUtilityNode("Util2")
						.getUtility(new Assignment(new Assignment("Burglary"),
								"Action", ValueFactory.create("DoNothing"))),
				0.0001f);
	}

	@Test
	public void testStructure() {

		BNetwork bn = NetworkExamples.constructBasicNetwork();
		assertEquals(5, bn.getNode("Burglary").getDescendantIds().size());
		assertTrue(bn.getNode("Burglary").getDescendantIds().contains("Alarm"));
		assertTrue(bn.getNode("Burglary").getDescendantIds().contains("MaryCalls"));

		assertEquals(3, bn.getNode("MaryCalls").getAncestorIds().size());
		assertTrue(bn.getNode("MaryCalls").getAncestorIds().contains("Alarm"));
		assertTrue(bn.getNode("MaryCalls").getAncestorIds().contains("Earthquake"));
		assertEquals(0, bn.getNode("MaryCalls").getDescendantIds().size());

		assertEquals(2, bn.getNode("Util1").getAncestorIds().size());
		assertTrue(bn.getNode("Util1").getAncestorIds().contains("Action"));
		assertEquals(0, bn.getNode("Util1").getDescendantIds().size());

		assertEquals(0, bn.getNode("Action").getAncestorIds().size());
		assertTrue(bn.getNode("Action").getDescendantIds().contains("Util1"));
		assertEquals(2, bn.getNode("Action").getDescendantIds().size());
	}

	@Test
	public void testRemoval() {

		BNetwork bn = NetworkExamples.constructBasicNetwork();
		bn.removeNode("Earthquake");
		assertEquals(1, bn.getChanceNode("Alarm").getInputNodes().size());
		bn.removeNode("Alarm");
		assertEquals(0, bn.getChanceNode("MaryCalls").getInputNodes().size());
		assertEquals(2, bn.getChanceNode("Burglary").getOutputNodes().size());
		assertEquals(6, bn.getNodes().size());

		bn = NetworkExamples.constructBasicNetwork();
		ChanceNode e = bn.getChanceNode("Alarm");
		e.removeInputNode("Earthquake");
		assertEquals(1, bn.getChanceNode("Alarm").getInputNodes().size());
		assertEquals(0, bn.getChanceNode("Earthquake").getOutputNodes().size());
	}

	@Test
	public void testIdChance() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		BNode node = bn.getNode("Alarm");
		node.setId("Alarm2");
		assertTrue(bn.hasNode("Alarm2"));
		assertFalse(bn.hasNode("Alarm"));
		assertTrue(bn.hasChanceNode("Alarm2"));
		assertFalse(bn.hasChanceNode("Alarm"));
		assertTrue(bn.getNode("Burglary").getOutputNodesIds().contains("Alarm2"));
		assertFalse(bn.getNode("Burglary").getOutputNodesIds().contains("Alarm"));
		assertTrue(bn.getNode("MaryCalls").getInputNodeIds().contains("Alarm2"));
		assertFalse(bn.getNode("MaryCalls").getInputNodeIds().contains("Alarm"));
	}

	@Test
	public void testCopyIdChange() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		BNetwork bn2 = bn.copy();
		BNode node = bn.getNode("Earthquake");
		node.setId("Earthquake2");
		assertFalse(bn2.getNode("Alarm").getInputNodeIds().contains("Earthquake2"));
		assertFalse(bn2.getNode("Alarm").getInputNodeIds().contains("Earthquake2"));
		BNode node2 = bn.getNode("Alarm");
		node2.setId("Alarm2");
		assertFalse(bn2.getNode("MaryCalls").getInputNodeIds().contains("Alarm2"));
		assertFalse(bn2.getNode("Burglary").getOutputNodesIds().contains("Alarm2"));
		assertTrue(bn2.getNode("Burglary").getOutputNodesIds().contains("Alarm"));
		assertTrue(bn2.getNode("MaryCalls").getInputNodeIds().contains("Alarm"));
	}

	@Test
	public void tableExpansion() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		CategoricalTable.Builder builder = new CategoricalTable.Builder("HouseSize");
		builder.addRow(ValueFactory.create("Small"), 0.7f);
		builder.addRow(ValueFactory.create("Big"), 0.2f);
		builder.addRow(ValueFactory.create("None"), 0.1f);
		ChanceNode node = new ChanceNode("HouseSize", builder.build());
		bn.addNode(node);
		bn.getNode("Burglary").addInputNode(node);
		assertEquals(0.001f,
				bn.getChanceNode("Burglary").getProb(
						new Assignment("HouseSize", "Small"),
						ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.001f,
				bn.getChanceNode("Burglary").getProb(
						new Assignment("HouseSize", "Big"),
						ValueFactory.create(true)),
				0.0001f);
		bn.getNode("Alarm").addInputNode(node);
		assertEquals(0.95f,
				bn.getChanceNode("Alarm").getProb(
						new Assignment(Arrays.asList("Burglary", "Earthquake")),
						ValueFactory.create(true)),
				0.0001f);
		assertEquals(0.95f,
				bn.getChanceNode("Alarm")
						.getProb(
								new Assignment(
										new Assignment(Arrays.asList("Burglary",
												"Earthquake")),
										"HouseSize", ValueFactory.create("None")),
								ValueFactory.create(true)),
				0.0001f);
	}

	@Test
	public void testDefaultValue() {
		CategoricalTable.Builder builder = new CategoricalTable.Builder("Burglary");
		builder.addRow(ValueFactory.create(false), 0.8);
		assertEquals(builder.build().getProb(ValueFactory.none()), 0.199, 0.01);
		builder.removeRow(ValueFactory.create(false));
		assertEquals(builder.build().getProb(ValueFactory.none()), 0.999, 0.01);
		// assertTrue(node.hasProb(new Assignment(), ValueFactory.none()));
		builder = new CategoricalTable.Builder("Burglary");
		builder.addRow(ValueFactory.create(false), 0.999);
		assertEquals(builder.build().getProb(ValueFactory.none()), 0.0, 0.01);
		// assertFalse(node.hasProb(new Assignment(), ValueFactory.none()));
	}

	@Test
	public void testSortedNodes() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		assertEquals("Action", bn.getSortedNodes().get(7).getId());
		assertEquals("Burglary", bn.getSortedNodes().get(6).getId());
		assertEquals("Earthquake", bn.getSortedNodes().get(5).getId());
		assertEquals("Alarm", bn.getSortedNodes().get(4).getId());
		assertEquals("Util1", bn.getSortedNodes().get(3).getId());
		assertEquals("Util2", bn.getSortedNodes().get(2).getId());
		assertEquals("JohnCalls", bn.getSortedNodes().get(1).getId());
		assertEquals("MaryCalls", bn.getSortedNodes().get(0).getId());
		ActionNode d1 = new ActionNode("a_m'");
		ActionNode d2 = new ActionNode("a_m.obj'");
		ActionNode d3 = new ActionNode("a_m.place'");
		BNetwork bn2 = new BNetwork();
		bn2.addNode(d1);
		bn2.addNode(d2);
		bn2.addNode(d3);
		assertEquals("a_m'", bn2.getSortedNodes().get(2).getId());
		assertEquals("a_m.obj'", bn2.getSortedNodes().get(1).getId());
		assertEquals("a_m.place'", bn2.getSortedNodes().get(0).getId());
	}

	@Test
	public void testCliques() {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		assertEquals(1, bn.getCliques().size());
		assertEquals(8, bn.getCliques().get(0).size());
		bn.getNode("JohnCalls").removeInputNode("Alarm");
		assertEquals(2, bn.getCliques().size());
		assertEquals(7, bn.getCliques().get(1).size());
		assertEquals(1, bn.getCliques().get(0).size());
		bn.getNode("Alarm").removeInputNode("Burglary");
		bn.getNode("Alarm").removeInputNode("Earthquake");
		assertEquals(4, bn.getCliques().size());
		assertEquals(2, bn.getCliques().get(3).size());
		assertEquals(4, bn.getCliques().get(2).size());
		assertEquals(1, bn.getCliques().get(1).size());
		assertEquals(1, bn.getCliques().get(0).size());
	}
}
