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

package opendial.bn;

import opendial.arch.Logger;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.DerivedActionNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.NetworkExamples;
 
/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * 
 * @version $Date:: 2012-06-11 18:14:37 #$
 *
 */
public class BNetworkStructureTest {

	public static Logger log = new Logger("BNetworkTest", Logger.Level.DEBUG);

	
	@Test
	public void buildBasicNetwork() throws DialException {
			
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		
		assertEquals(8, bn.getNodes().size());
		
		assertEquals(3, bn.getNode("Burglary").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getInputNodes().size());
		assertEquals(2, bn.getNode("Util1").getInputNodes().size());
		
		assertEquals(2, bn.getNode("Burglary").getValues().size());
		assertEquals(2, bn.getNode("Alarm").getValues().size());
		assertEquals(2, bn.getNode("MaryCalls").getValues().size());

		assertTrue(bn.getNode("Burglary").getValues().contains(ValueFactory.create(true)));

		assertEquals(0.001f, bn.getChanceNode("Burglary").
				getProb(ValueFactory.create(true)), 0.0001f);
		assertEquals(0.95f, bn.getChanceNode("Alarm").
				getProb(new Assignment(Arrays.asList("Burglary", "Earthquake")), ValueFactory.create(true)), 0.0001f);
		assertEquals(0.9f, bn.getChanceNode("JohnCalls").getProb(new Assignment("Alarm"), ValueFactory.create(true)), 0.0001f);
		
		assertTrue(bn.getChanceNode("MaryCalls").getDistrib().isWellFormed());
		
		assertEquals(3, bn.getActionNode("Action").getValues().size());
		assertEquals(-10f, bn.getUtilityNode("Util2").getUtility(new Assignment(new Assignment("Burglary"), "Action",
				ValueFactory.create("DoNothing"))), 0.0001f);
	}
	
	@Test
	public void testCopy() throws DialException {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		BNetwork bn2 = bn.copy();
		
		ChanceNode b = bn.getChanceNode("Burglary");
		b.addProb(ValueFactory.create(true), 0.2f);
		b.addProb(ValueFactory.create(false), 0.8f);
		UtilityNode value =bn.getUtilityNode("Util1");
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action", ValueFactory.create("DoNothing")), -20.0f);
		
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

		assertTrue(bn2.getNode("Burglary").getValues().contains(ValueFactory.create(true)));

		assertEquals(0.001f, bn2.getChanceNode("Burglary").
				getProb(ValueFactory.create(true)), 0.0001f);
		assertEquals(0.95f, bn2.getChanceNode("Alarm").
				getProb(new Assignment(Arrays.asList("Burglary", "Earthquake")), ValueFactory.create(true)), 0.0001f);
		assertEquals(0.9f, bn2.getChanceNode("JohnCalls").getProb(new Assignment("Alarm"), ValueFactory.create(true)), 0.0001f);
		
		assertTrue(bn2.getChanceNode("MaryCalls").getDistrib().isWellFormed());
		
		assertEquals(3, bn2.getActionNode("Action").getValues().size());
		assertEquals(-10f, bn2.getUtilityNode("Util2").getUtility(new Assignment(new Assignment("Burglary"), "Action", ValueFactory.create("DoNothing"))), 0.0001f);		
	}
	
	
	@Test
	public void structureTest() throws DialException {
		
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
	public void distributionTest() throws DialException {
		
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		ChanceNode e = bn.getChanceNode("Earthquake");
		assertTrue(e.getDistrib().isWellFormed());
		e.removeProb(ValueFactory.create(false));
		DiscreteProbabilityTable.log.setLevel(Logger.Level.NONE);
	//	assertFalse(e.getDistribution().isWellFormed());
		e.addProb(ValueFactory.create(false), 0.1f);
	//	assertFalse(e.getDistribution().isWellFormed());
		e.removeProb(ValueFactory.create(true));
	//	assertFalse(e.getDistribution().isWellFormed());
		e.addProb(ValueFactory.create(true), 0.2f);
		e.addProb(ValueFactory.create(false), 0.8f);
		assertTrue(e.getDistrib().isWellFormed());
	
		ChanceNode a = bn.getChanceNode("Alarm");
		assertTrue(a.getDistrib().isWellFormed());
		a.removeProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),ValueFactory.create(true));
	//	assertFalse(a.getDistribution().isWellFormed());
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),ValueFactory.create(true), 0.4f);
	//	assertFalse(a.getDistribution().isWellFormed());
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),ValueFactory.create(true), 0.29f);
		assertTrue(a.getDistrib().isWellFormed());

		DiscreteProbabilityTable.log.setLevel(Logger.Level.NORMAL);

		UtilityTable.log.setLevel(Logger.Level.NONE);
		UtilityNode v = bn.getUtilityNode("Util1");
		assertTrue(v.getDistribution().isWellFormed());
		v.removeUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("CallPolice")));
		assertFalse(v.getDistribution().isWellFormed());
		v.addUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("CallPolice")), 100f);
		assertTrue(v.getDistribution().isWellFormed());
		UtilityTable.log.setLevel(Logger.Level.NORMAL);		
	}
	
	@Test
	public void removalTest() throws DialException {
		
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
	public void idTestChange() throws DialException {
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
	public void copyIdChange() throws DialException {
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
	public void tableExpansion() throws DialException {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		ChanceNode node = new ChanceNode("HouseSize");
		node.addProb(ValueFactory.create("Small"), 0.7f);
		node.addProb(ValueFactory.create("Big"), 0.2f);
		node.addProb(ValueFactory.create("None"), 0.1f);
		bn.addNode(node);
		bn.getNode("Burglary").addInputNode(node);
		assertEquals(0.001f, bn.getChanceNode("Burglary").getProb(new Assignment("HouseSize", "Small"), ValueFactory.create(true)), 0.0001f);
		assertEquals(0.001f, bn.getChanceNode("Burglary").getProb(new Assignment("HouseSize", "Big"), ValueFactory.create(true)), 0.0001f);
		bn.getNode("Alarm").addInputNode(node);
		assertEquals(0.95f, bn.getChanceNode("Alarm").
				getProb(new Assignment(Arrays.asList("Burglary", "Earthquake")), ValueFactory.create(true)), 0.0001f);
		assertEquals(0.95f, bn.getChanceNode("Alarm").
				getProb(new Assignment(new Assignment(Arrays.asList("Burglary", "Earthquake")), 
						"HouseSize", ValueFactory.create("None")), ValueFactory.create(true)), 0.0001f);
	}
	
	/** @Test
	public void marginalisation() throws DialException {
		BNetwork bn = CommonTestUtils.constructBasicNetwork();
		ChanceNode node = bn.getChanceNode("Alarm");
		assertEquals(node.getProb(ValueFactory.create(true)), (new VariableElimination()).queryProb(bn, Arrays.asList("Alarm"), 
				new Assignment()).getDiscreteDistrib().getProb(new Assignment("Alarm", true)), 0.0001);
	} */
	
	
	@Test
	public void defaultValue() throws DialException {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		ChanceNode node = bn.getChanceNode("Burglary");
		node.addProb(ValueFactory.create(false), 0.8);
		assertEquals(node.getProb(ValueFactory.none()), 0.199, 0.0001);
		node.removeProb(ValueFactory.create(false));
		assertEquals(node.getProb(ValueFactory.none()), 0.999, 0.0001);
		assertTrue(node.hasProb(new Assignment(), ValueFactory.none()));
	node.addProb(ValueFactory.create(false), 0.999);
		assertEquals(node.getProb(ValueFactory.none()), 0.0, 0.000001);
		assertFalse(node.hasProb(new Assignment(), ValueFactory.none()));
	}
	
	@Test
	public void sortedNodes() throws DialException {
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
	public void derivedActionNodes () throws DialException {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		BNetwork bn2 = NetworkExamples.constructBasicNetwork3();
		assertTrue(bn2.getActionNode("Action") instanceof DerivedActionNode);
		assertEquals(2, bn2.getUtilityNode("Util1").getRelevantActions().size());
		assertEquals(2, bn2.getUtilityNode("Util2").getRelevantActions().size());
		assertEquals(bn.getActionNode("Action").getValues(), bn2.getActionNode("Action").getValues());
	}
}
