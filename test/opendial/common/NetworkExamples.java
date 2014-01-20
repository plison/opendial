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

package opendial.common;

import java.util.Arrays;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NetworkExamples {
 
	// logger
	public static Logger log = new Logger("CommonTestUtils",
			Logger.Level.NORMAL);
	
	
	
	public static BNetwork constructBasicNetwork() throws DialException {
	BNetwork bn = new BNetwork();
		
	ChanceNode b = new ChanceNode("Burglary");
		b.addProb(ValueFactory.create(true), 0.001f);
		b.addProb(ValueFactory.create(false), 0.999f);
		bn.addNode(b);
		
		ChanceNode e = new ChanceNode("Earthquake");
		e.addProb(ValueFactory.create(true), 0.002f);
		e.addProb(ValueFactory.create(false), 0.998f);
		bn.addNode(e);

		ChanceNode a = new ChanceNode("Alarm");
		a.addInputNode(b);
		a.addInputNode(e);
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),ValueFactory.create(true), 0.95f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),ValueFactory.create(false),0.05f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")), ValueFactory.create(true),0.95f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")),ValueFactory.create(false),0.05f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),ValueFactory.create(true),0.29f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),ValueFactory.create(false),0.71f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),ValueFactory.create(true),0.001f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),ValueFactory.create(false),0.999f);
		bn.addNode(a);
				
		ChanceNode mc = new ChanceNode("MaryCalls");
		mc.addInputNode(a);
		mc.addProb(new Assignment("Alarm"),ValueFactory.create(true),0.7f);
		mc.addProb(new Assignment("Alarm"),ValueFactory.create(false),0.3f);
		mc.addProb(new Assignment("!Alarm"),ValueFactory.create(true),0.01f);
		mc.addProb(new Assignment("!Alarm"),ValueFactory.create(false),0.99f);
		bn.addNode(mc);
		
		ChanceNode jc = new ChanceNode("JohnCalls");
		jc.addInputNode(a);
		jc.addProb(new Assignment("Alarm"),ValueFactory.create(true),0.9f);
		jc.addProb(new Assignment("Alarm"),ValueFactory.create(false),0.1f);
		jc.addProb(new Assignment("!Alarm"),ValueFactory.create(true),0.05f);
		jc.addProb(new Assignment("!Alarm"),ValueFactory.create(false),0.95f);
		bn.addNode(jc);
		
		ActionNode action = new ActionNode("Action");
		action.addValue(ValueFactory.create("CallPolice"));
		action.addValue(ValueFactory.create("DoNothing"));
		bn.addNode(action);
		
		UtilityNode value = new UtilityNode("Util1");
		value.addInputNode(b);
		value.addInputNode(action);
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action", ValueFactory.create("CallPolice")), -0.5f);
		value.addUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("CallPolice")), -1.0f);
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action", ValueFactory.create("DoNothing")), 0.0f);
		value.addUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("DoNothing")), 0.0f);
		bn.addNode(value);
		
		UtilityNode value2 = new UtilityNode("Util2");
		value2.addInputNode(b);
		value2.addInputNode(action);
		value2.addUtility(new Assignment(new Assignment("Burglary", true), "Action", ValueFactory.create("CallPolice")), 0.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("CallPolice")), 0.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", true), "Action", ValueFactory.create("DoNothing")), -10.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", false), "Action", ValueFactory.create("DoNothing")), 0.5f);
		bn.addNode(value2);
		
		return bn;
	}
	
	public static BNetwork constructBasicNetwork2() throws DialException {
		BNetwork network = constructBasicNetwork();
		network.getChanceNode("Burglary").addProb(ValueFactory.create(true), 0.1f);
		network.getChanceNode("Burglary").addProb(ValueFactory.create(false), 0.9f);
		network.getChanceNode("Earthquake").addProb(ValueFactory.create(true), 0.2f);
		network.getChanceNode("Earthquake").addProb(ValueFactory.create(false), 0.8f);
		return network;
	}
	
	
	
	public static BNetwork constructBasicNetwork3() throws DialException {
		BNetwork network = constructBasicNetwork();
		network.removeNode("Action");
		ActionNode ddn = new ActionNode("Action");
		network.getUtilityNode("Util1").addInputNode(ddn);
		network.getUtilityNode("Util1").removeUtility(new Assignment(new Assignment("Burglary", false), new Assignment("Action", "DoNothing")));
		network.getUtilityNode("Util2").addInputNode(ddn);
		network.addNode(ddn);
		return network;
	}
	

	public static BNetwork constructBasicNetwork4() throws DialException {
		BNetwork network = constructBasicNetwork();
		ChanceNode node = new ChanceNode("gaussian");
		node.setDistrib(new ContinuousDistribution("gaussian", new UniformDensityFunction(-2,3)));
		network.addNode(node);
		return network;
	}
	
	
	public static BNetwork constructIWSDSNetwork() throws DialException {
		
		BNetwork net = new BNetwork();
		
		ChanceNode i_u = new ChanceNode("i_u");
		i_u.addProb(ValueFactory.create("ki"), 0.4);
		i_u.addProb(ValueFactory.create("of"), 0.3);
		i_u.addProb(ValueFactory.create("co"), 0.3);
		net.addNode(i_u);
		
		ChanceNode a_u = new ChanceNode("a_u");
		a_u.addInputNode(i_u);
		a_u.addProb(new Assignment("i_u", "ki"), ValueFactory.create("ki"), 0.9);
		a_u.addProb(new Assignment("i_u", "ki"), ValueFactory.create("null"), 0.1);
		a_u.addProb(new Assignment("i_u", "of"), ValueFactory.create("of"), 0.9);
		a_u.addProb(new Assignment("i_u", "of"), ValueFactory.create("null"), 0.1);
		a_u.addProb(new Assignment("i_u", "co"), ValueFactory.create("co"), 0.9);
		a_u.addProb(new Assignment("i_u", "co"), ValueFactory.create("null"), 0.1);
		net.addNode(a_u);
		
		ChanceNode o = new ChanceNode("o");
		o.addInputNode(a_u);
		o.addProb(new Assignment("a_u", "ki"), ValueFactory.create("true"), 0.0);
		o.addProb(new Assignment("a_u", "ki"), ValueFactory.create("false"), 1.0);
		o.addProb(new Assignment("a_u", "of"), ValueFactory.create("true"), 0.6);
		o.addProb(new Assignment("a_u", "of"), ValueFactory.create("false"), 0.4);
		o.addProb(new Assignment("a_u", "co"), ValueFactory.create("true"), 0.15);
		o.addProb(new Assignment("a_u", "co"), ValueFactory.create("false"), 0.85);
		o.addProb(new Assignment("a_u", "null"), ValueFactory.create("true"), 0.25);
		o.addProb(new Assignment("a_u", "null"), ValueFactory.create("false"), 0.75);
		net.addNode(o);
		
		ActionNode a_m = new ActionNode("a_m");
		a_m.addValue(ValueFactory.create("ki"));
		a_m.addValue(ValueFactory.create("of"));
		a_m.addValue(ValueFactory.create("co"));
		a_m.addValue(ValueFactory.create("rep"));
		net.addNode(a_m);
		
		UtilityNode r = new UtilityNode("r");
		r.addInputNode(a_m);
		r.addInputNode(i_u);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment("i_u", "ki")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment("i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment("i_u", "co")), -5);
		
		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment("i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment("i_u", "of")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment("i_u", "co")), -5);
		
		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment("i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment("i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment("i_u", "co")), 3);

		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment("i_u", "ki")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment("i_u", "of")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment("i_u", "co")), -0.5);
		net.addNode(r);
		
		return net;
	}
}
