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
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ConditionalTable;
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

		CategoricalTable.Builder builder = new CategoricalTable.Builder("Burglary");
		builder.addRow(ValueFactory.create(true), 0.001f);
		builder.addRow(ValueFactory.create(false), 0.999f);
		ChanceNode b = new ChanceNode("Burglary", builder.build());
		bn.addNode(b);

		builder = new CategoricalTable.Builder("Earthquake");
		builder.addRow(ValueFactory.create(true), 0.002f);
		builder.addRow(ValueFactory.create(false), 0.998f);
		ChanceNode e = new ChanceNode("Earthquake", builder.build());
		bn.addNode(e);

		ConditionalTable.Builder builder2 = new ConditionalTable.Builder("Alarm");
		builder2.addRow(new Assignment(Arrays.asList("Burglary", "Earthquake")),
				ValueFactory.create(true), 0.95f);
		builder2.addRow(new Assignment(Arrays.asList("Burglary", "Earthquake")),
				ValueFactory.create(false), 0.05f);
		builder2.addRow(new Assignment(Arrays.asList("Burglary", "!Earthquake")),
				ValueFactory.create(true), 0.95f);
		builder2.addRow(new Assignment(Arrays.asList("Burglary", "!Earthquake")),
				ValueFactory.create(false), 0.05f);
		builder2.addRow(new Assignment(Arrays.asList("!Burglary", "Earthquake")),
				ValueFactory.create(true), 0.29f);
		builder2.addRow(new Assignment(Arrays.asList("!Burglary", "Earthquake")),
				ValueFactory.create(false), 0.71f);
		builder2.addRow(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),
				ValueFactory.create(true), 0.001f);
		builder2.addRow(new Assignment(Arrays.asList("!Burglary", "!Earthquake")),
				ValueFactory.create(false), 0.999f);
		ChanceNode a = new ChanceNode("Alarm", builder2.build());
		a.addInputNode(b);
		a.addInputNode(e);
		bn.addNode(a);

		builder2 = new ConditionalTable.Builder("MaryCalls");
		builder2.addRow(new Assignment("Alarm"), ValueFactory.create(true), 0.7f);
		builder2.addRow(new Assignment("Alarm"), ValueFactory.create(false), 0.3f);
		builder2.addRow(new Assignment("!Alarm"), ValueFactory.create(true), 0.01f);
		builder2.addRow(new Assignment("!Alarm"), ValueFactory.create(false), 0.99f);
		ChanceNode mc = new ChanceNode("MaryCalls", builder2.build());
		mc.addInputNode(a);
		bn.addNode(mc);

		builder2 = new ConditionalTable.Builder("JohnCalls");
		builder2.addRow(new Assignment("Alarm"), ValueFactory.create(true), 0.9f);
		builder2.addRow(new Assignment("Alarm"), ValueFactory.create(false), 0.1f);
		builder2.addRow(new Assignment("!Alarm"), ValueFactory.create(true), 0.05f);
		builder2.addRow(new Assignment("!Alarm"), ValueFactory.create(false), 0.95f);
		ChanceNode jc = new ChanceNode("JohnCalls", builder2.build());
		jc.addInputNode(a);
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
		value2.addUtility(new Assignment(new Assignment("Burglary", false), "Action",
				ValueFactory.create("CallPolice")), 0.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", true), "Action",
				ValueFactory.create("DoNothing")), -10.0f);
		value2.addUtility(new Assignment(new Assignment("Burglary", false), "Action",
				ValueFactory.create("DoNothing")), 0.5f);
		bn.addNode(value2);

		return bn;
	}

	public static BNetwork constructBasicNetwork2() {
		BNetwork network = constructBasicNetwork();
		CategoricalTable.Builder builder = new CategoricalTable.Builder("Burglary");
		builder.addRow(ValueFactory.create(true), 0.1f);
		builder.addRow(ValueFactory.create(false), 0.9f);
		network.getChanceNode("Burglary").setDistrib(builder.build());
		builder = new CategoricalTable.Builder("Earthquake");
		builder.addRow(ValueFactory.create(true), 0.2f);
		builder.addRow(ValueFactory.create(false), 0.8f);
		network.getChanceNode("Earthquake").setDistrib(builder.build());

		return network;
	}

	public static BNetwork constructBasicNetwork3() {
		BNetwork network = constructBasicNetwork();
		network.removeNode("Action");
		ActionNode ddn = new ActionNode("Action");
		network.getUtilityNode("Util1").addInputNode(ddn);
		network.getUtilityNode("Util1")
				.removeUtility(new Assignment(new Assignment("Burglary", false),
						new Assignment("Action", "DoNothing")));
		network.getUtilityNode("Util2").addInputNode(ddn);
		network.addNode(ddn);
		return network;
	}

	public static BNetwork constructBasicNetwork4() {
		BNetwork network = constructBasicNetwork();
		ChanceNode node =
				new ChanceNode("gaussian", new ContinuousDistribution("gaussian",
						new UniformDensityFunction(-2, 3)));
		network.addNode(node);
		return network;
	}

	public static BNetwork constructIWSDSNetwork() {

		BNetwork net = new BNetwork();
		CategoricalTable.Builder builder = new CategoricalTable.Builder("i_u");
		builder.addRow(ValueFactory.create("ki"), 0.4);
		builder.addRow(ValueFactory.create("of"), 0.3);
		builder.addRow(ValueFactory.create("co"), 0.3);
		ChanceNode i_u = new ChanceNode("i_u", builder.build());
		net.addNode(i_u);

		ConditionalTable.Builder builder2 = new ConditionalTable.Builder("a_u");
		builder2.addRow(new Assignment("i_u", "ki"), ValueFactory.create("ki"), 0.9);
		builder2.addRow(new Assignment("i_u", "ki"), ValueFactory.create("null"),
				0.1);
		builder2.addRow(new Assignment("i_u", "of"), ValueFactory.create("of"), 0.9);
		builder2.addRow(new Assignment("i_u", "of"), ValueFactory.create("null"),
				0.1);
		builder2.addRow(new Assignment("i_u", "co"), ValueFactory.create("co"), 0.9);
		builder2.addRow(new Assignment("i_u", "co"), ValueFactory.create("null"),
				0.1);
		ChanceNode a_u = new ChanceNode("a_u", builder2.build());
		a_u.addInputNode(i_u);
		net.addNode(a_u);

		builder2 = new ConditionalTable.Builder("a_u");
		builder2.addRow(new Assignment("a_u", "ki"), ValueFactory.create("true"),
				0.0);
		builder2.addRow(new Assignment("a_u", "ki"), ValueFactory.create("false"),
				1.0);
		builder2.addRow(new Assignment("a_u", "of"), ValueFactory.create("true"),
				0.6);
		builder2.addRow(new Assignment("a_u", "of"), ValueFactory.create("false"),
				0.4);
		builder2.addRow(new Assignment("a_u", "co"), ValueFactory.create("true"),
				0.15);
		builder2.addRow(new Assignment("a_u", "co"), ValueFactory.create("false"),
				0.85);
		builder2.addRow(new Assignment("a_u", "null"), ValueFactory.create("true"),
				0.25);
		builder2.addRow(new Assignment("a_u", "null"), ValueFactory.create("false"),
				0.75);
		ChanceNode o = new ChanceNode("o", builder2.build());
		o.addInputNode(a_u);
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
		r.addUtility(new Assignment(new Assignment("a_m", "ki"),
				new Assignment("i_u", "ki")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"),
				new Assignment("i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "ki"),
				new Assignment("i_u", "co")), -5);

		r.addUtility(new Assignment(new Assignment("a_m", "of"),
				new Assignment("i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "of"),
				new Assignment("i_u", "of")), 3);
		r.addUtility(new Assignment(new Assignment("a_m", "of"),
				new Assignment("i_u", "co")), -5);

		r.addUtility(new Assignment(new Assignment("a_m", "co"),
				new Assignment("i_u", "ki")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"),
				new Assignment("i_u", "of")), -5);
		r.addUtility(new Assignment(new Assignment("a_m", "co"),
				new Assignment("i_u", "co")), 3);

		r.addUtility(new Assignment(new Assignment("a_m", "rep"),
				new Assignment("i_u", "ki")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"),
				new Assignment("i_u", "of")), -0.5);
		r.addUtility(new Assignment(new Assignment("a_m", "rep"),
				new Assignment("i_u", "co")), -0.5);
		net.addNode(r);

		return net;
	}
}
