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

package opendial.inference;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.inference.algorithms.NaiveInference;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNInferenceTest {

	static Logger log = new Logger("BNInferenceTest", Logger.Level.DEBUG);
	
	
	public BNetwork constructBayesianNetwork() throws DialException {
		BNetwork bn = new BNetwork();
		
		List<Object> bValues = Arrays.asList((Object)Boolean.TRUE, Boolean.FALSE);
		
		BNode b = new BNode("Burglary");
		b.addValues(bValues);
		b.addRow(new Assignment("Burglary"), 0.001f);
		bn.addNode(b);
		
		BNode e = new BNode("Earthquake");
		e.addValues(bValues);
		e.addRow(new Assignment("Earthquake"), 0.002f);
		bn.addNode(e);

		BNode a = new BNode("Alarm");
		a.addValues(bValues);
		a.addConditionalNode(b);
		a.addConditionalNode(e);
		a.addRow(new Assignment(Arrays.asList("Burglary", "Earthquake", "Alarm")), 0.95f);
		a.addRow(new Assignment(Arrays.asList("Burglary", "!Earthquake", "Alarm")), 0.94f);
		a.addRow(new Assignment(Arrays.asList("!Burglary", "Earthquake", "Alarm")), 0.29f);
		a.addRow(new Assignment(Arrays.asList("!Burglary", "!Earthquake", "Alarm")), 0.001f);
		bn.addNode(a);

		BNode mc = new BNode("MaryCalls");
		mc.addValues(bValues);
		mc.addConditionalNode(a);
		mc.addRow(new Assignment(Arrays.asList("Alarm", "MaryCalls")), 0.7f);
		mc.addRow(new Assignment(Arrays.asList("!Alarm", "MaryCalls")), 0.01f);
		bn.addNode(mc);
		
		BNode jc = new BNode("JohnCalls");
		jc.addValues(bValues);
		jc.addConditionalNode(a);
		jc.addRow(new Assignment(Arrays.asList("Alarm", "JohnCalls")), 0.9f);
		jc.addRow(new Assignment(Arrays.asList("!Alarm", "JohnCalls")), 0.05f);
		bn.addNode(jc);
		
		return bn;
	}
	
	
	@Test
	public void bayesianNetworkTest1() throws DialException {
		
		BNetwork bn = constructBayesianNetwork();
		
		Map<Assignment,Float> fullJoint = NaiveInference.getFullJoint(bn);

		assertEquals(0.000628f, fullJoint.get(new Assignment(
				Arrays.asList("JohnCalls", "MaryCalls", "Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		assertEquals(0.9367428f, fullJoint.get(new Assignment(
				Arrays.asList("!JohnCalls", "!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		Map<Assignment,Float> query = NaiveInference.query(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		assertEquals(0.7158281f, query.get(new Assignment("Burglary", Boolean.FALSE)), 0.0001f);
		assertEquals(0.28417188f, query.get(new Assignment("Burglary", Boolean.TRUE)), 0.0001f);
		
	}
}
