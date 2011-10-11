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

import java.util.Arrays;

import org.junit.Test;

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
	
	@Test
	public void constructBayesianNetwork() {
		BNetwork bn = new BNetwork();
		
		BNode<Boolean> b = new BNode<Boolean>("Burglary");
		b.addValue(Boolean.TRUE);
		b.addValue(Boolean.FALSE);
		b.addRow(new Assignment("Burglary"), 0.001f);
		bn.addNode(b);
		
		BNode<Boolean> e = new BNode<Boolean>("Earthquake");
		e.addValue(Boolean.TRUE);
		e.addValue(Boolean.FALSE);
		e.addRow(new Assignment("Earthquake"), 0.002f);
		bn.addNode(e);

		BNode<Boolean> a = new BNode<Boolean>("Alarm");
		a.addValue(Boolean.TRUE);
		a.addValue(Boolean.FALSE);
		a.addConditionalNode(b);
		a.addConditionalNode(e);
		a.addRow(new Assignment(Arrays.asList("Burglary", "Earthquake", "Alarm")), 0.95f);
		a.addRow(new Assignment(Arrays.asList("Burglary", "!Earthquake", "Alarm")), 0.94f);
		a.addRow(new Assignment(Arrays.asList("!Burglary", "Earthquake", "Alarm")), 0.29f);
		a.addRow(new Assignment(Arrays.asList("!Burglary", "!Earthquake", "Alarm")), 0.001f);
		bn.addNode(a);

		BNode<Boolean> mc = new BNode<Boolean>("MaryCalls");
		mc.addValue(Boolean.TRUE);
		mc.addValue(Boolean.FALSE);
		mc.addConditionalNode(a);
		mc.addRow(new Assignment(Arrays.asList("Alarm", "MaryCalls")), 0.7f);
		mc.addRow(new Assignment(Arrays.asList("!Alarm", "MaryCalls")), 0.01f);
		bn.addNode(mc);
		
		BNode<Boolean> jc = new BNode<Boolean>("JohnCalls");
		jc.addValue(Boolean.TRUE);
		jc.addValue(Boolean.FALSE);
		jc.addConditionalNode(a);
		jc.addRow(new Assignment(Arrays.asList("Alarm", "JohnCalls")), 0.9f);
		jc.addRow(new Assignment(Arrays.asList("!Alarm", "JohnCalls")), 0.05f);
		bn.addNode(jc);
		
	}
}
