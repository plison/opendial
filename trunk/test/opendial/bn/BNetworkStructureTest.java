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
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbabilityTable;
import opendial.bn.distribs.ValueDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ValueNode;
import opendial.gui.DialogueMonitor;
import opendial.gui.stateviewer.StateViewerComponent;
 
/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * 
 * @version $Date::                      $
 *
 */
public class BNetworkStructureTest {

	public static Logger log = new Logger("BNetworkTest", Logger.Level.DEBUG);
	
	public static BNetwork constructBasicNetwork() throws DialException {
	BNetwork bn = new BNetwork();
		
		ChanceNode b = new ChanceNode("Burglary");
		b.addProb((true), 0.001f);
		b.addProb((false), 0.999f);
		bn.addNode(b);
		
		ChanceNode e = new ChanceNode("Earthquake");
		e.addProb((true), 0.002f);
		e.addProb((false), 0.998f);
		bn.addNode(e);

		ChanceNode a = new ChanceNode("Alarm");
		a.addRelation(b);
		a.addRelation(e);
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),(true), 0.95f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),(false),0.05f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")), (true),0.94f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")),(false),0.06f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),(true),0.29f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),(false),0.71f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),(true),0.001f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),(false),0.999f);
		bn.addNode(a);
				
		ChanceNode mc = new ChanceNode("MaryCalls");
		mc.addRelation(a);
		mc.addProb(new Assignment("Alarm"),(true),0.7f);
		mc.addProb(new Assignment("Alarm"),(false),0.3f);
		mc.addProb(new Assignment("!Alarm"),(true),0.01f);
		mc.addProb(new Assignment("!Alarm"),(false),0.99f);
		bn.addNode(mc);
		
		ChanceNode jc = new ChanceNode("JohnCalls");
		jc.addRelation(a);
		jc.addProb(new Assignment("Alarm"),(true),0.9f);
		jc.addProb(new Assignment("Alarm"),(false),0.1f);
		jc.addProb(new Assignment("!Alarm"),(true),0.05f);
		jc.addProb(new Assignment("!Alarm"),(false),0.95f);
		bn.addNode(jc);
		
		ActionNode action = new ActionNode("Action");
		action.addValue("CallPolice");
		action.addValue("DoNothing");
		bn.addNode(action);
		
		ValueNode value = new ValueNode("Value");
		value.addRelation(b);
		value.addRelation(action);
		value.addValue(new Assignment(new Assignment("Burglary", true), "Action", "CallPolice"), -1.0f);
		value.addValue(new Assignment(new Assignment("Burglary", false), "Action", "CallPolice"), -1.0f);
		value.addValue(new Assignment(new Assignment("Burglary", true), "Action", "DoNothing"), -10.0f);
		value.addValue(new Assignment(new Assignment("Burglary", false), "Action", "DoNothing"), 0.5f);
		bn.addNode(value);
		
		return bn;
	}
	
	
	@Test
	public void buildBasicNetwork() throws DialException {
			
		BNetwork bn = constructBasicNetwork();
		
		assertEquals(7, bn.getNodes().size());
		
		assertEquals(2, bn.getNode("Burglary").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getInputNodes().size());
		assertEquals(2, bn.getNode("Value").getInputNodes().size());
		
		assertEquals(2, bn.getNode("Burglary").getValues().size());
		assertEquals(2, bn.getNode("Alarm").getValues().size());
		assertEquals(2, bn.getNode("MaryCalls").getValues().size());

		assertTrue(bn.getNode("Burglary").getValues().contains(true));

		assertEquals(0.001f, bn.getChanceNode("Burglary").
				getProb(true), 0.0001f);
		assertEquals(0.95f, bn.getChanceNode("Alarm").
				getProb(new Assignment(Arrays.asList("Burglary", "Earthquake")), true), 0.0001f);
		assertEquals(0.9f, bn.getChanceNode("JohnCalls").getProb(new Assignment("Alarm"), true), 0.0001f);
		
		assertTrue(bn.getChanceNode("MaryCalls").getDistribution().isWellFormed());
		
		assertEquals(2, bn.getActionNode("Action").getValues().size());
		assertEquals(-10f, bn.getValueNode("Value").getValue(new Assignment(new Assignment("Burglary"), "Action", "DoNothing")), 0.0001f);
	}
	
	@Test
	public void testCopy() throws DialException {
		BNetwork bn = constructBasicNetwork();
		BNetwork bn2 = bn.copy();
		
		ChanceNode b = bn.getChanceNode("Burglary");
		b.addProb((true), 0.2f);
		b.addProb((false), 0.8f);
		ValueNode value =bn.getValueNode("Value");
		value.addValue(new Assignment(new Assignment("Burglary", true), "Action", "DoNothing"), -20.0f);
		
		assertEquals(2, bn.getNode("Burglary").getOutputNodes().size());
		assertEquals(2, bn2.getNode("Burglary").getOutputNodes().size());
		
		assertEquals(2, bn.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn2.getNode("Alarm").getOutputNodes().size());
		assertEquals(2, bn.getNode("Alarm").getInputNodes().size());
		assertEquals(2, bn2.getNode("Alarm").getInputNodes().size());
		
		assertEquals(2, bn.getNode("Value").getInputNodes().size());
		assertEquals(2, bn2.getNode("Value").getInputNodes().size());
		
		assertEquals(2, bn2.getNode("Burglary").getValues().size());
		assertEquals(2, bn2.getNode("Alarm").getValues().size());
		assertEquals(2, bn2.getNode("MaryCalls").getValues().size());

		assertTrue(bn2.getNode("Burglary").getValues().contains(true));

		assertEquals(0.001f, bn2.getChanceNode("Burglary").
				getProb(true), 0.0001f);
		assertEquals(0.95f, bn2.getChanceNode("Alarm").
				getProb(new Assignment(Arrays.asList("Burglary", "Earthquake")), true), 0.0001f);
		assertEquals(0.9f, bn2.getChanceNode("JohnCalls").getProb(new Assignment("Alarm"), true), 0.0001f);
		
		assertTrue(bn2.getChanceNode("MaryCalls").getDistribution().isWellFormed());
		
		assertEquals(2, bn2.getActionNode("Action").getValues().size());
		assertEquals(-10f, bn2.getValueNode("Value").getValue(new Assignment(new Assignment("Burglary"), "Action", "DoNothing")), 0.0001f);		
	}
	
	
	@Test
	public void structureTest() throws DialException {
		
		BNetwork bn = constructBasicNetwork();
		assertEquals(4, bn.getNode("Burglary").getDescendantIds().size());
		assertTrue(bn.getNode("Burglary").getDescendantIds().contains("Alarm"));
		assertTrue(bn.getNode("Burglary").getDescendantIds().contains("MaryCalls"));
		
		assertEquals(3, bn.getNode("MaryCalls").getAncestorIds().size());
		assertTrue(bn.getNode("MaryCalls").getAncestorIds().contains("Alarm"));
		assertTrue(bn.getNode("MaryCalls").getAncestorIds().contains("Earthquake"));
		assertEquals(0, bn.getNode("MaryCalls").getDescendantIds().size());
		
		assertEquals(2, bn.getNode("Value").getAncestorIds().size());
		assertTrue(bn.getNode("Value").getAncestorIds().contains("Action"));
		assertEquals(0, bn.getNode("Value").getDescendantIds().size());
		
		assertEquals(0, bn.getNode("Action").getAncestorIds().size());
		assertTrue(bn.getNode("Action").getDescendantIds().contains("Value"));
		assertEquals(1, bn.getNode("Action").getDescendantIds().size());
	}
	
	@Test
	public void distributionTest() throws DialException {
		
		BNetwork bn = constructBasicNetwork();
		ChanceNode e = bn.getChanceNode("Earthquake");
		assertTrue(e.getDistribution().isWellFormed());
		e.removeProb(false);
		ProbabilityTable.log.setLevel(Logger.Level.NONE);
		assertFalse(e.getDistribution().isWellFormed());
		e.addProb(false, 0.1f);
		assertFalse(e.getDistribution().isWellFormed());
		e.removeProb(true);
		assertFalse(e.getDistribution().isWellFormed());
		e.addProb(true, 0.2f);
		e.addProb(false, 0.8f);
		assertTrue(e.getDistribution().isWellFormed());
	
		ChanceNode a = bn.getChanceNode("Alarm");
		assertTrue(a.getDistribution().isWellFormed());
		a.removeProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),(true));
		assertFalse(a.getDistribution().isWellFormed());
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),(true), 0.4f);
		assertFalse(a.getDistribution().isWellFormed());
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),(true), 0.29f);
		assertTrue(a.getDistribution().isWellFormed());

		ProbabilityTable.log.setLevel(Logger.Level.NORMAL);

		ValueDistribution.log.setLevel(Logger.Level.NONE);
		ValueNode v = bn.getValueNode("Value");
		assertTrue(v.getDistribution().isWellFormed());
		v.removeValue(new Assignment(new Assignment("Burglary", false), "Action", "CallPolice"));
		assertFalse(v.getDistribution().isWellFormed());
		v.addValue(new Assignment(new Assignment("Burglary", false), "Action", "CallPolice"), 100f);
		assertTrue(v.getDistribution().isWellFormed());
		ValueDistribution.log.setLevel(Logger.Level.NORMAL);		
	}
	
	
	public static void main(String[] args) throws DialException {
		
		BNetwork bn = constructBasicNetwork();
		
		DialogueMonitor monitor = DialogueMonitor.getSingletonInstance();
		StateViewerComponent viewer = new StateViewerComponent();
		monitor.addComponent(viewer);
		viewer.recordNetwork(bn.copy(), "not current");
		viewer.updateCurrentNetwork(bn);
	}
}
