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

package opendial.inference;

import java.util.logging.*;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import opendial.bn.BNetwork;
import opendial.bn.distribs.SingleValueDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.common.NetworkExamples;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.inference.exact.NaiveInference;
import opendial.inference.exact.VariableElimination;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NetworkReductionTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	BNetwork network;

	VariableElimination ve;
	SamplingAlgorithm is;
	NaiveInference naive;
	SwitchingAlgorithm sw;

	public NetworkReductionTest() {
		network = NetworkExamples.constructBasicNetwork2();

		ve = new VariableElimination();
		is = new SamplingAlgorithm(3000, 500);
		naive = new NaiveInference();
		sw = new SwitchingAlgorithm();
	}

	@Test
	public void test1() throws InterruptedException {

		BNetwork reducedNet = ve.reduce(network,
				Arrays.asList("Burglary", "Earthquake", "MaryCalls"));
		BNetwork reducedNet2 = naive.reduce(network,
				Arrays.asList("Burglary", "Earthquake", "MaryCalls"));
		BNetwork reducedNet3 = is.reduce(network,
				Arrays.asList("Burglary", "Earthquake", "MaryCalls"));
		BNetwork reducedNet4 = sw.reduce(network,
				Arrays.asList("Burglary", "Earthquake", "MaryCalls"));

		assertEquals(3, reducedNet.getNodes().size());
		assertEquals(3, reducedNet2.getNodes().size());
		assertEquals(3, reducedNet3.getNodes().size());
		assertEquals(3, reducedNet4.getNodes().size());

		assertEquals(ve
				.queryProb(network, Arrays.asList("MaryCalls"),
						new Assignment("Burglary"))
				.getProb(new Assignment("MaryCalls")), ve
						.queryProb(reducedNet, Arrays.asList("MaryCalls"),
								new Assignment("Burglary"))
						.getProb(new Assignment("MaryCalls")),
				0.0001);

		assertEquals(ve
				.queryProb(network, Arrays.asList("MaryCalls"),
						new Assignment("Burglary"))
				.getProb(new Assignment("MaryCalls")), ve
						.queryProb(reducedNet2, Arrays.asList("MaryCalls"),
								new Assignment("Burglary"))
						.getProb(new Assignment("MaryCalls")),
				0.0001);

		assertEquals(ve
				.queryProb(network, Arrays.asList("MaryCalls"),
						new Assignment("Burglary"))
				.getProb(new Assignment("MaryCalls")), is
						.queryProb(reducedNet3, Arrays.asList("MaryCalls"),
								new Assignment("Burglary"))
						.getProb(new Assignment("MaryCalls")),
				0.15);

		assertEquals(ve
				.queryProb(network, Arrays.asList("MaryCalls"),
						new Assignment("Burglary"))
				.getProb(new Assignment("MaryCalls")), is
						.queryProb(reducedNet4, Arrays.asList("MaryCalls"),
								new Assignment("Burglary"))
						.getProb(new Assignment("MaryCalls")),
				0.15);

		assertEquals(ve
				.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Earthquake")), ve
						.queryProb(reducedNet, Arrays.asList("Earthquake"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Earthquake")),
				0.0001);
		assertEquals(ve
				.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Earthquake")), ve
						.queryProb(reducedNet2, Arrays.asList("Earthquake"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Earthquake")),
				0.0001);
		assertEquals(ve
				.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Earthquake")), is
						.queryProb(reducedNet3, Arrays.asList("Earthquake"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Earthquake")),
				0.05);

		assertEquals(ve
				.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Earthquake")), is
						.queryProb(reducedNet4, Arrays.asList("Earthquake"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Earthquake")),
				0.05);

	}

	@Test
	public void test2() throws InterruptedException {

		BNetwork reducedNet =
				ve.reduce(network, Arrays.asList("Burglary", "MaryCalls"),
						new Assignment("!Earthquake"));
		BNetwork reducedNet2 =
				naive.reduce(network, Arrays.asList("Burglary", "MaryCalls"),
						new Assignment("!Earthquake"));
		BNetwork reducedNet3 =
				is.reduce(network, Arrays.asList("Burglary", "MaryCalls"),
						new Assignment("!Earthquake"));
		BNetwork reducedNet4 =
				sw.reduce(network, Arrays.asList("Burglary", "MaryCalls"),
						new Assignment("!Earthquake"));

		assertEquals(2, reducedNet.getNodes().size());
		assertEquals(2, reducedNet2.getNodes().size());
		assertEquals(2, reducedNet3.getNodes().size());
		assertEquals(2, reducedNet4.getNodes().size());

		assertEquals(
				ve.queryProb(network, Arrays.asList("MaryCalls"),
						new Assignment("!Earthquake"))
				.getProb(new Assignment("MaryCalls")),
				ve.queryProb(reducedNet, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				0.0001);
		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				naive.queryProb(reducedNet2, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				0.0001);
		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				is.queryProb(reducedNet3, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				0.05);

		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				is.queryProb(reducedNet4, Arrays.asList("MaryCalls"))
						.getProb(new Assignment("MaryCalls")),
				0.05);

		assertEquals(ve
				.queryProb(network, Arrays.asList("Burglary"),
						new Assignment(Arrays.asList("!MaryCalls", "!Earthquake")))
				.getProb(new Assignment("Burglary")), ve
						.queryProb(reducedNet, Arrays.asList("Burglary"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Burglary")),
				0.0001);
		assertEquals(ve
				.queryProb(network, Arrays.asList("Burglary"),
						new Assignment(Arrays.asList("!MaryCalls", "!Earthquake")))
				.getProb(new Assignment("Burglary")), naive
						.queryProb(reducedNet2, Arrays.asList("Burglary"),
								new Assignment("!MaryCalls"))
								.getProb(new Assignment("Burglary")),
				0.0001);
		assertEquals(ve
				.queryProb(reducedNet, Arrays.asList("Burglary"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Burglary")), is
						.queryProb(reducedNet3, Arrays.asList("Burglary"),
								new Assignment("!MaryCalls"))
						.getProb(new Assignment("Burglary")),
				0.05);

		assertEquals(ve
				.queryProb(reducedNet, Arrays.asList("Burglary"),
						new Assignment("!MaryCalls"))
				.getProb(new Assignment("Burglary")), is
						.queryProb(reducedNet4, Arrays.asList("Burglary"),
								new Assignment("!MaryCalls"))
						.getProb(new Assignment("Burglary")),
				0.05);
	}

	@Test
	public void test3() throws InterruptedException {

		BNetwork reducedNet =
				ve.reduce(network, Arrays.asList("Burglary", "Earthquake"),
						new Assignment("JohnCalls"));
		BNetwork reducedNet2 =
				naive.reduce(network, Arrays.asList("Burglary", "Earthquake"),
						new Assignment("JohnCalls"));
		BNetwork reducedNet3 =
				is.reduce(network, Arrays.asList("Burglary", "Earthquake"),
						new Assignment("JohnCalls"));
		BNetwork reducedNet4 =
				sw.reduce(network, Arrays.asList("Burglary", "Earthquake"),
						new Assignment("JohnCalls"));

		assertEquals(2, reducedNet.getNodes().size());
		assertEquals(2, reducedNet2.getNodes().size());
		assertEquals(2, reducedNet3.getNodes().size());
		assertEquals(2, reducedNet4.getNodes().size());

		assertEquals(
				ve.queryProb(network, Arrays.asList("Burglary"),
						new Assignment("JohnCalls"))
				.getProb(new Assignment("Burglary")),
				is.queryProb(reducedNet, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				0.1);
		assertEquals(
				ve.queryProb(network, Arrays.asList("Burglary"),
						new Assignment("JohnCalls"))
				.getProb(new Assignment("Burglary")),
				naive.queryProb(reducedNet2, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				0.0001);
		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				naive.queryProb(reducedNet3, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				0.08);
		assertEquals(
				ve.queryProb(reducedNet2, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				naive.queryProb(reducedNet4, Arrays.asList("Burglary"))
						.getProb(new Assignment("Burglary")),
				0.05);

		assertEquals(
				ve.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment(Arrays.asList("JohnCalls")))
				.getProb(new Assignment("Earthquake")),
				ve.queryProb(reducedNet, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				0.0001);
		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				is.queryProb(reducedNet2, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				0.07);
		assertEquals(
				ve.queryProb(network, Arrays.asList("Earthquake"),
						new Assignment(Arrays.asList("JohnCalls")))
				.getProb(new Assignment("Earthquake")),
				naive.queryProb(reducedNet3, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				0.07);
		assertEquals(
				ve.queryProb(reducedNet, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				naive.queryProb(reducedNet4, Arrays.asList("Earthquake"))
						.getProb(new Assignment("Earthquake")),
				0.07);

	}

	@Test
	public void test5() {
		BNetwork reducedNet = ve.reduce(network, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		BNetwork reducedNet2 = is.reduce(network, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		reducedNet.addNode(network.getNode("Action").copy());
		reducedNet.addNode(network.getNode("Util1").copy());
		reducedNet.addNode(network.getNode("Util2").copy());
		reducedNet.getNode("Util1").addInputNode(reducedNet.getNode("Burglary"));
		reducedNet.getNode("Util1").addInputNode(reducedNet.getNode("Action"));
		reducedNet.getNode("Util2").addInputNode(reducedNet.getNode("Burglary"));
		reducedNet.getNode("Util2").addInputNode(reducedNet.getNode("Action"));

		UtilityTable table1 = ve.queryUtil(reducedNet, "Action");
		UtilityTable table2 = ve.queryUtil(network, Arrays.asList("Action"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		for (Assignment a : table1.getTable().keySet()) {
			assertEquals(table1.getUtil(a), table2.getUtil(a), 0.01);
		}

		reducedNet2.addNode(network.getNode("Action").copy());
		reducedNet2.addNode(network.getNode("Util1").copy());
		reducedNet2.addNode(network.getNode("Util2").copy());
		reducedNet2.getNode("Util1").addInputNode(reducedNet2.getNode("Burglary"));
		reducedNet2.getNode("Util1").addInputNode(reducedNet2.getNode("Action"));
		reducedNet2.getNode("Util2").addInputNode(reducedNet2.getNode("Burglary"));
		reducedNet2.getNode("Util2").addInputNode(reducedNet2.getNode("Action"));

		UtilityTable table3 = ve.queryUtil(reducedNet2, "Action");

		for (Assignment a : table1.getTable().keySet()) {
			assertEquals(table1.getUtil(a), table3.getUtil(a), 0.8);
		}
	}

	@Test
	public void test6() throws InterruptedException {
		BNetwork old = network.copy();

		network.getNode("Alarm").removeInputNode("Earthquake");
		network.getNode("Alarm").removeInputNode("Burglary");
		network.getChanceNode("Alarm")
				.setDistrib(new SingleValueDistribution("Alarm", "False"));

		test1();
		test2();
		test3();

		network = old;
	}

}
