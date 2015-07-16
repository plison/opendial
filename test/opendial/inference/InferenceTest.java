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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.EmpiricalDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.densityfunctions.GaussianDensityFunction;
import opendial.bn.distribs.densityfunctions.UniformDensityFunction;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
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
public class InferenceTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static void main(String[] args) {
		BNetwork bn = NetworkExamples.constructBasicNetwork();
		log.info("" + (new VariableElimination()).queryProb(bn,
				Arrays.asList("Burglary"),
				new Assignment(new Assignment("JohnCalls", true),
						new Assignment("MaryCalls", false))));
	}

	@Test
	public void testNetwork1() {

		BNetwork bn = NetworkExamples.constructBasicNetwork();
		Map<Assignment, Double> fullJoint = NaiveInference.getFullJoint(bn, false);

		assertEquals(
				0.000628f, fullJoint.get(new Assignment(Arrays.asList("JohnCalls",
						"MaryCalls", "Alarm", "!Burglary", "!Earthquake"))),
				0.000001f);

		assertEquals(0.9367428f,
				fullJoint.get(new Assignment(Arrays.asList("!JohnCalls",
						"!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))),
				0.000001f);

		NaiveInference naive = new NaiveInference();

		MultivariateDistribution query =
				naive.queryProb(bn, Arrays.asList("Burglary"),
						new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		assertEquals(0.71367f, query.getProb(new Assignment("Burglary", false)),
				0.0001f);
		assertEquals(0.286323, query.getProb(new Assignment("Burglary", true)),
				0.0001f);

		MultivariateDistribution query2 =
				naive.queryProb(bn, Arrays.asList("Alarm", "Burglary"),
						new Assignment(Arrays.asList("Alarm", "MaryCalls")));

		assertEquals(0.623974,
				query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))),
				0.001f);

	}

	@Test
	public void testNetwork1bis() {

		BNetwork bn = NetworkExamples.constructBasicNetwork2();
		Map<Assignment, Double> fullJoint = NaiveInference.getFullJoint(bn, false);

		assertEquals(
				0.000453599f, fullJoint.get(new Assignment(Arrays.asList("JohnCalls",
						"MaryCalls", "Alarm", "!Burglary", "!Earthquake"))),
				0.000001f);

		assertEquals(0.6764828f,
				fullJoint.get(new Assignment(Arrays.asList("!JohnCalls",
						"!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))),
				0.000001f);

		NaiveInference naive = new NaiveInference();

		MultivariateDistribution query =
				naive.queryProb(bn, Arrays.asList("Burglary"),
						new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		assertEquals(0.360657, query.getProb(new Assignment("Burglary", false)),
				0.0001f);
		assertEquals(0.639343, query.getProb(new Assignment("Burglary", true)),
				0.0001f);

		MultivariateDistribution query2 =
				naive.queryProb(bn, Arrays.asList("Alarm", "Burglary"),
						new Assignment(Arrays.asList("Alarm", "MaryCalls")));

		assertEquals(0.3577609,
				query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))),
				0.001f);

	}

	@Test
	public void testNetwork2() {

		VariableElimination ve = new VariableElimination();
		BNetwork bn = NetworkExamples.constructBasicNetwork();

		MultivariateDistribution distrib =
				ve.queryProb(bn, Arrays.asList("Burglary"),
						new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		assertEquals(0.713676, distrib.getProb(new Assignment("Burglary", false)),
				0.0001f);
		assertEquals(0.286323, distrib.getProb(new Assignment("Burglary", true)),
				0.0001f);

		MultivariateDistribution query2 =
				ve.queryProb(bn, Arrays.asList("Alarm", "Burglary"),
						new Assignment(Arrays.asList("Alarm", "MaryCalls")));

		assertEquals(0.623974,
				query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))),
				0.001f);
	}

	@Test
	public void testNetwork2bis() {

		VariableElimination ve = new VariableElimination();
		BNetwork bn = NetworkExamples.constructBasicNetwork2();

		MultivariateDistribution query = ve.queryProb(bn, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		assertEquals(0.360657, query.getProb(new Assignment("Burglary", false)),
				0.0001f);
		assertEquals(0.63934, query.getProb(new Assignment("Burglary", true)),
				0.0001f);

		MultivariateDistribution query2 =
				ve.queryProb(bn, Arrays.asList("Alarm", "Burglary"),
						new Assignment(Arrays.asList("Alarm", "MaryCalls")));

		assertEquals(0.3577609,
				query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))),
				0.001f);
	}

	@Test
	public void testNetwork3bis() {

		SamplingAlgorithm is = new SamplingAlgorithm(5000, 300);
		BNetwork bn = NetworkExamples.constructBasicNetwork2();

		MultivariateDistribution query = is.queryProb(bn, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));

		assertEquals(0.362607f, query.getProb(new Assignment("Burglary", false)),
				0.06f);
		assertEquals(0.637392, query.getProb(new Assignment("Burglary", true)),
				0.06f);

		MultivariateDistribution query2 =
				is.queryProb(bn, Arrays.asList("Alarm", "Burglary"),
						new Assignment(Arrays.asList("Alarm", "MaryCalls")));

		assertEquals(0.35970f,
				query2.getProb(new Assignment(Arrays.asList("Alarm", "!Burglary"))),
				0.05f);
	}

	@Test
	public void testNetworkUtil() {
		BNetwork network = NetworkExamples.constructBasicNetwork2();

		VariableElimination ve = new VariableElimination();
		NaiveInference naive = new NaiveInference();
		SamplingAlgorithm is = new SamplingAlgorithm(4000, 300);

		assertEquals(-0.680, ve
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, naive
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "CallPolice")), 0.001);
		assertEquals(-0.680, is
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "CallPolice")), 0.5);
		assertEquals(-6.213, ve
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, naive
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "DoNothing")), 0.001);
		assertEquals(-6.213, is
				.queryUtil(network, Arrays.asList("Action"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Action", "DoNothing")), 1.5);

		assertEquals(-0.1667, ve
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.1667, naive
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("!Burglary")), 0.001);
		assertEquals(-0.25, is
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("!Burglary")), 0.5);
		assertEquals(-3.5, ve
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Burglary")), 0.001);
		assertEquals(-3.5, naive
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Burglary")), 0.001);
		assertEquals(-3.5, is
				.queryUtil(network, Arrays.asList("Burglary"),
						new Assignment(new Assignment("JohnCalls"),
								new Assignment("MaryCalls")))
				.getUtil(new Assignment("Burglary")), 1.0);

	}

	@Test
	public void testSwitching() {
		int oldFactor = SwitchingAlgorithm.MAX_BRANCHING_FACTOR;
		SwitchingAlgorithm.MAX_BRANCHING_FACTOR = 4;
		BNetwork network = NetworkExamples.constructBasicNetwork2();

		MultivariateDistribution distrib = (new SwitchingAlgorithm()).queryProb(
				network, Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		assertTrue(distrib instanceof MultivariateTable);

		CategoricalTable.Builder builder = new CategoricalTable.Builder("n1");
		builder.addRow(ValueFactory.create("aha"), 1.0);
		ChanceNode n1 = new ChanceNode("n1", builder.build());
		network.addNode(n1);
		builder = new CategoricalTable.Builder("n2");
		builder.addRow(ValueFactory.create("oho"), 0.7);
		ChanceNode n2 = new ChanceNode("n2", builder.build());
		network.addNode(n2);
		builder = new CategoricalTable.Builder("n3");
		builder.addRow(ValueFactory.create("ihi"), 0.7);
		ChanceNode n3 = new ChanceNode("n3", builder.build());
		network.addNode(n3);
		network.getNode("Alarm").addInputNode(n1);
		network.getNode("Alarm").addInputNode(n2);
		network.getNode("Alarm").addInputNode(n3);

		distrib = (new SwitchingAlgorithm()).queryProb(network,
				Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		assertEquals(EmpiricalDistribution.class, distrib.getClass());

		network.removeNode(n1.getId());
		network.removeNode(n2.getId());

		distrib = (new SwitchingAlgorithm()).queryProb(network,
				Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		assertTrue(distrib instanceof MultivariateTable);

		n1 = new ChanceNode("n1",
				new ContinuousDistribution("n1", new UniformDensityFunction(-2, 2)));
		n2 = new ChanceNode("n2", new ContinuousDistribution("n2",
				new GaussianDensityFunction(-1.0, 3.0)));
		network.addNode(n1);
		network.addNode(n2);
		network.getNode("Earthquake").addInputNode(n1);
		network.getNode("Earthquake").addInputNode(n2);

		distrib = (new SwitchingAlgorithm().queryProb(network,
				Arrays.asList("Burglary"),
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls"))));
		assertTrue(distrib instanceof EmpiricalDistribution);

		SwitchingAlgorithm.MAX_BRANCHING_FACTOR = oldFactor;
	}

	/**
	 * @Test public void specialUtilQueryTest() {
	 * 
	 *       BNetwork network = new BNetwork(); ChanceNode n = new ChanceNode("s");
	 *       n.addProb(ValueFactory.create("val1"), 0.3);
	 *       n.addProb(ValueFactory.create("val2"), 0.7); network.addNode(n);
	 * 
	 *       UtilityNode u = new UtilityNode("u"); u.addUtility(new Assignment("s",
	 *       "val1"), +2); u.addUtility(new Assignment("s", "val2"), -1);
	 *       u.addInputNode(n); network.addNode(u);
	 * 
	 *       UtilQuery query = new UtilQuery(network, new LinkedList<String>());
	 *       InferenceChecks inference = new InferenceChecks();
	 *       inference.checkUtil(query, new Assignment(), -0.1); }
	 */

}
