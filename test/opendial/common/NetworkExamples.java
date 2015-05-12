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

package opendial.common;

import java.util.logging.*;

import java.util.Arrays;

import opendial.bn.BNetwork;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.densityfunctions.UniformDensityFunction;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NetworkExamples {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static BNetwork constructBasicNetwork() {
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
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),
				ValueFactory.create(true), 0.95f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "Earthquake")),
				ValueFactory.create(false), 0.05f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")),
				ValueFactory.create(true), 0.95f);
		a.addProb(new Assignment(Arrays.asList("Burglary", "!Earthquake")),
				ValueFactory.create(false), 0.05f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),
				ValueFactory.create(true), 0.29f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "Earthquake")),
				ValueFactory.create(false), 0.71f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),
				ValueFactory.create(true), 0.001f);
		a.addProb(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),
				ValueFactory.create(false), 0.999f);
		bn.addNode(a);

		ChanceNode mc = new ChanceNode("MaryCalls");
		mc.addInputNode(a);
		mc.addProb(new Assignment("Alarm"), ValueFactory.create(true), 0.7f);
		mc.addProb(new Assignment("Alarm"), ValueFactory.create(false), 0.3f);
		mc.addProb(new Assignment("!Alarm"), ValueFactory.create(true), 0.01f);
		mc.addProb(new Assignment("!Alarm"), ValueFactory.create(false), 0.99f);
		bn.addNode(mc);

		ChanceNode jc = new ChanceNode("JohnCalls");
		jc.addInputNode(a);
		jc.addProb(new Assignment("Alarm"), ValueFactory.create(true), 0.9f);
		jc.addProb(new Assignment("Alarm"), ValueFactory.create(false), 0.1f);
		jc.addProb(new Assignment("!Alarm"), ValueFactory.create(true), 0.05f);
		jc.addProb(new Assignment("!Alarm"), ValueFactory.create(false), 0.95f);
		bn.addNode(jc);

		ActionNode action = new ActionNode("Action");
		action.addValue(ValueFactory.create("CallPolice"));
		action.addValue(ValueFactory.create("DoNothing"));
		bn.addNode(action);

		UtilityNode value = new UtilityNode("Util1");
		value.addInputNode(b);
		value.addInputNode(action);
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("CallPolice")), -0.5f);
		value.addUtility(new Assignment(new Assignment("Burglary", false), "Action",
				ValueFactory.create("CallPolice")), -1.0f);
		value.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("DoNothing")), 0.0f);
		value.addUtility(new Assignment(new Assignment("Burglary", false), "Action",
				ValueFactory.create("DoNothing")), 0.0f);
		bn.addNode(value);

		UtilityNode value2 = new UtilityNode("Util2");
		value2.addInputNode(b);
		value2.addInputNode(action);
		value2.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("CallPolice")), 0.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", false),
				"Action", ValueFactory.create("CallPolice")), 0.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("DoNothing")), -10.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", false),
				"Action", ValueFactory.create("DoNothing")), 0.5f);
		bn.addNode(value2);

		return bn;
	}

	public static BNetwork constructBasicNetwork2() {
		BNetwork network = constructBasicNetwork();
		network.getChanceNode("Burglary").addProb(ValueFactory.create(true), 0.1f);
		network.getChanceNode("Burglary").addProb(ValueFactory.create(false), 0.9f);
		network.getChanceNode("Earthquake").addProb(ValueFactory.create(true), 0.2f);
		network.getChanceNode("Earthquake")
				.addProb(ValueFactory.create(false), 0.8f);
		return network;
	}

	public static BNetwork constructBasicNetwork3() {
		BNetwork network = constructBasicNetwork();
		network.removeNode("Action");
		ActionNode ddn = new ActionNode("Action");
		network.getUtilityNode("Util1").addInputNode(ddn);
		network.getUtilityNode("Util1").removeUtility(
				new Assignment(new Assignment("Burglary", false), new Assignment(
						"Action", "DoNothing")));
		network.getUtilityNode("Util2").addInputNode(ddn);
		network.addNode(ddn);
		return network;
	}

	public static BNetwork constructBasicNetwork4() {
		BNetwork network = constructBasicNetwork();
		ChanceNode node = new ChanceNode("gaussian");
		node.setDistrib(new ContinuousDistribution("gaussian",
				new UniformDensityFunction(-2, 3)));
		network.addNode(node);
		return network;
	}

	public static BNetwork constructIWSDSNetwork() {

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
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment(
				"i_u", "ki")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment(
				"i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"), new Assignment(
				"i_u", "co")), -5);

		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment(
				"i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment(
				"i_u", "of")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "of"), new Assignment(
				"i_u", "co")), -5);

		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment(
				"i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment(
				"i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"), new Assignment(
				"i_u", "co")), 3);

		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment(
				"i_u", "ki")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment(
				"i_u", "of")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"), new Assignment(
				"i_u", "co")), -0.5);
		net.addNode(r);

		return net;
	}
}
