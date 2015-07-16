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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.logging.Logger;

import opendial.Settings;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.densityfunctions.DirichletDensityFunction;
import opendial.bn.distribs.densityfunctions.GaussianDensityFunction;
import opendial.bn.distribs.densityfunctions.KernelDensityFunction;
import opendial.bn.distribs.densityfunctions.UniformDensityFunction;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.inference.exact.VariableElimination;
import opendial.utils.MathUtils;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class DistributionsTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void testSimpleDistrib() {
		CategoricalTable.Builder builder = new CategoricalTable.Builder("var1");
		builder.addRow("val1", 0.7);
		assertFalse(builder.isWellFormed());
		builder.addRow("val2", 0.3);
		assertTrue(builder.isWellFormed());
		IndependentDistribution table = builder.build();
		assertEquals(table.getProb("val1"), 0.7, 0.001);
		assertEquals(table.getProb("val1"), 0.7, 0.001);
		MultivariateTable.Builder table2 = new MultivariateTable.Builder();
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val2"),
				0.9);
		// assertFalse(table2.isWellFormed());
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val1"),
				0.1);
		assertTrue(table2.isWellFormed());
		assertEquals(
				table2.build().getProb(new Assignment(new Assignment("var1", "val1"),
						new Assignment("var2", "val3"))),
				0.1, 0.001);
	}

	@Test
	public void testMaths() {
		assertEquals(4.0, MathUtils.getVolume(2, 1), 0.001);
		assertEquals(Math.PI * 4, MathUtils.getVolume(2, 2), 0.001);
		assertEquals(4.0 / 3.0 * Math.PI * 8, MathUtils.getVolume(2, 3), 0.001);
		assertEquals(Math.pow(Math.PI, 2) / 2 * 81, MathUtils.getVolume(3, 4),
				0.001);
	}

	@Test
	public void testConversion1Distrib() {

		CategoricalTable.Builder builder = new CategoricalTable.Builder("var1");
		builder.addRow(1.5, 0.7);
		builder.addRow(2.0, 0.1);
		builder.addRow(-3.0, 0.2);
		assertTrue(builder.isWellFormed());
		IndependentDistribution table = builder.build();
		assertEquals(table.getProb("2.0"), 0.1, 0.001);
		ContinuousDistribution continuous = table.toContinuous();
		assertEquals(continuous.getProbDensity(2.0), 0.2, 0.001);
		assertEquals(continuous.getProbDensity(2.1), 0.2, 0.001);
		assertEquals(continuous.getCumulativeProb(-3.1), 0.0, 0.001);
		assertEquals(continuous.getCumulativeProb(1.6), 0.9, 0.001);
		CategoricalTable table2 = continuous.toDiscrete();
		assertEquals(table2.getValues().size(), 3);
		assertEquals(table2.getProb(2.0), 0.1, 0.05);

		double sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += ((DoubleVal) continuous.sample()).getDouble();
		}
		assertEquals(sum / 10000.0, 0.65, 0.1);

	}

	@Test
	public void testContinuous() {

		ContinuousDistribution distrib =
				new ContinuousDistribution("X", new UniformDensityFunction(-2, 4));
		assertEquals(1 / 6.0f, distrib.getProbDensity(1.0), 0.0001f);
		assertEquals(1 / 6.0f, distrib.getProbDensity(4.0), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(-3.0), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(6.0), 0.0001f);
		assertEquals(0.01, distrib.toDiscrete().getProb(0.5), 0.01);
		assertEquals(0.01, distrib.toDiscrete().getProb(4), 0.01);
		double totalProb = 0.0f;
		for (Value a : distrib.toDiscrete().getPosterior(new Assignment())
				.getValues()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.03);
	}

	@Test
	public void testGaussian() {

		ContinuousDistribution distrib = new ContinuousDistribution("X",
				new GaussianDensityFunction(1.0, 3.0));
		assertEquals(0.23032, distrib.getProbDensity(1.0), 0.001f);
		assertEquals(0.016f, distrib.getProbDensity(-3.0f), 0.01f);
		assertEquals(0.00357f, distrib.getProbDensity(6.0f), 0.01f);
		assertEquals(0.06290, distrib.toDiscrete().getProb(1.0), 0.01f);
		assertEquals(0.060615, distrib.toDiscrete().getProb(0.5f), 0.01f);
		assertEquals(0.014486, distrib.toDiscrete().getProb(4), 0.01f);
		double totalProb = 0.0f;
		for (Value a : distrib.toDiscrete().getPosterior(new Assignment())
				.getValues()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.05);
		assertEquals(distrib.getFunction().getMean()[0], 1.0, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 3.0, 0.01);

		double[][] samples = new double[20000][];
		for (int i = 0; i < 20000; i++) {
			double[] val =
					new double[] { ((DoubleVal) distrib.sample()).getDouble() };
			samples[i] = val;
		}
		GaussianDensityFunction estimated = new GaussianDensityFunction(samples);
		assertEquals(estimated.getMean()[0], distrib.getFunction().getMean()[0],
				0.05);
		assertEquals(estimated.getVariance()[0],
				distrib.getFunction().getVariance()[0], 0.1);
	}

	@Test
	public void testDiscrete() {

		CategoricalTable.Builder builder = new CategoricalTable.Builder("A");
		builder.addRow(1, 0.6);
		builder.addRow(2.5, 0.3);
		IndependentDistribution table = builder.build();
		assertEquals(0.3, table.getProb(2.5), 0.0001f);
		assertEquals(0.6, table.getProb(1.0), 0.0001f);
		ContinuousDistribution distrib = table.toContinuous();
		assertEquals(0.2, distrib.getProbDensity(2.5), 0.01);
		assertEquals(0.4, distrib.getProbDensity(1), 0.001);
		assertEquals(0, distrib.getProbDensity(-2), 0.001f);
		assertEquals(0.4, distrib.getProbDensity(0.9), 0.001f);
		assertEquals(0.4, distrib.getProbDensity(1.2), 0.0001f);
		assertEquals(0.2, distrib.getProbDensity(2.2), 0.001f);
		assertEquals(0.2, distrib.getProbDensity(2.7), 0.001f);
		assertEquals(0, distrib.getProbDensity(5), 0.0001f);
		assertEquals(0, distrib.getCumulativeProb(0.5), 0.0001f);
		assertEquals(0.6, distrib.getCumulativeProb(1.1), 0.0001f);
		assertEquals(0.6, distrib.getCumulativeProb(2.4), 0.0001f);
		assertEquals(0.9, distrib.getCumulativeProb(2.5), 0.0001f);
		assertEquals(0.9, distrib.getCumulativeProb(2.6), 0.0001f);

		assertEquals(distrib.getFunction().getMean()[0], 1.35, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 0.47, 0.01);
	}

	@Test
	public void testUniformDistrib() {
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2",
				new UniformDensityFunction(-2, 3.0));
		assertEquals(continuous2.getProbDensity(1.2), 1 / 5.0, 0.001);
		// assertEquals(continuous2.getCumulativeProb(new Assignment("var2",
		// 2)), 4/5.0, 0.001);
		assertEquals(continuous2.toDiscrete().getValues().size(),
				Settings.discretisationBuckets);
		assertEquals(continuous2.getProb(ValueFactory.create(1.2)), 0.01, 0.01);

		double sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += ((DoubleVal) continuous2.sample()).getDouble();
		}
		assertEquals(sum / 10000.0, 0.5, 0.1);

		assertEquals(continuous2.getFunction().getMean()[0], 0.5, 0.01);
		assertEquals(continuous2.getFunction().getVariance()[0], 2.08, 0.01);

	}

	@Test
	public void testGaussianDistrib() {
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2",
				new GaussianDensityFunction(2.0, 3.0));
		assertEquals(continuous2.getProbDensity(1.2), 0.2070, 0.001);
		assertEquals(continuous2.getProbDensity(2.0), 0.23033, 0.001);
		assertEquals(continuous2.getCumulativeProb(2), 0.5, 0.001);
		assertEquals(continuous2.getCumulativeProb(3), 0.7181, 0.001);
		assertTrue(continuous2.toDiscrete().getValues()
				.size() > Settings.discretisationBuckets / 2);
		assertTrue(continuous2.toDiscrete().getValues()
				.size() <= Settings.discretisationBuckets);
		assertEquals(continuous2.toDiscrete().getProb(2), 0.06205, 0.01);

		double sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += ((DoubleVal) continuous2.sample()).getDouble();
		}
		assertEquals(sum / 10000.0, 2.0, 0.1);
	}

	@Test
	public void testKernelDistrib() throws InterruptedException, RuntimeException {
		KernelDensityFunction kds =
				new KernelDensityFunction(Arrays.asList(new double[] { 0.1 },
						new double[] { -1.5 }, new double[] { 0.6 },
						new double[] { 1.3 }, new double[] { 1.3 }));

		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", kds);

		assertEquals(continuous2.getProbDensity(-2.0), 0.086, 0.001);
		assertEquals(continuous2.getProbDensity(0.6), 0.32, 0.01);
		assertEquals(continuous2.getProbDensity(1.3), 0.30, 0.01);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(-1.6)), 0.0,
				0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(-1.4)), 0.2,
				0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.29)), 0.6,
				0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.3)), 1.0,
				0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.31)), 1.0,
				0.001);
		double sum = 0;
		for (int i = 0; i < 20000; i++) {
			sum += ((DoubleVal) continuous2.sample()).getDouble();
		}
		assertEquals(sum / 20000.0, 0.424, 0.1);
		// DistributionViewer.showDistributionViewer(continuous2);
		// Thread.sleep(300000000);
		assertEquals(continuous2.toDiscrete().getProb(-1.5), 0.2, 0.03);

		assertEquals(continuous2.getFunction().getMean()[0], 0.36, 0.01);
		assertEquals(continuous2.getFunction().getVariance()[0], 1.07, 0.01);
	}

	@Test
	public void testEmpiricalDistrib() {

		CategoricalTable.Builder st = new CategoricalTable.Builder("var1");
		st.addRow("val1", 0.6);
		st.addRow("val2", 0.4);

		ConditionalTable.Builder builder = new ConditionalTable.Builder("var2");
		builder.addRow(new Assignment("var1", "val1"), "val1", 0.9);
		builder.addRow(new Assignment("var1", "val1"), "val2", 0.1);
		builder.addRow(new Assignment("var1", "val2"), "val1", 0.2);
		builder.addRow(new Assignment("var1", "val2"), "val2", 0.8);

		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1", st.build());

		bn.addNode(var1);

		ChanceNode var2 = new ChanceNode("var2", builder.build());
		var2.addInputNode(var1);
		bn.addNode(var2);

		SamplingAlgorithm sampling = new SamplingAlgorithm(2000, 500);

		IndependentDistribution distrib =
				sampling.queryProb(bn, "var2", new Assignment("var1", "val1"));

		assertEquals(distrib.getProb("val1"), 0.9, 0.05);
		assertEquals(distrib.getProb("val2"), 0.1, 0.05);

		IndependentDistribution distrib2 = sampling.queryProb(bn, "var2");

		assertEquals(distrib2.getProb("val1"), 0.62, 0.05);
		assertEquals(distrib2.getProb("val2"), 0.38, 0.05);

	}

	@Test
	public void empiricalDistribContinuous() {
		ContinuousDistribution continuous = new ContinuousDistribution("var1",
				new UniformDensityFunction(-1, 3));

		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1", continuous);
		bn.addNode(var1);

		SamplingAlgorithm sampling = new SamplingAlgorithm(2000, 200);

		IndependentDistribution distrib2 = sampling.queryProb(bn, "var1");
		assertEquals(distrib2.getPosterior(new Assignment()).getValues().size(),
				Settings.discretisationBuckets, 2);
		assertEquals(0, distrib2.toContinuous().getCumulativeProb(-1.1), 0.001);
		assertEquals(0.5, distrib2.toContinuous().getCumulativeProb(1), 0.06);
		assertEquals(1.0, distrib2.toContinuous().getCumulativeProb(3.1), 0.00);

		assertEquals(continuous.getProbDensity(-2),
				distrib2.toContinuous().getProbDensity(-2), 0.1);
		assertEquals(continuous.getProbDensity(-0.5),
				distrib2.toContinuous().getProbDensity(-0.5), 0.1);
		assertEquals(continuous.getProbDensity(1.8),
				distrib2.toContinuous().getProbDensity(1.8), 0.1);
		assertEquals(continuous.getProbDensity(3.2),
				distrib2.toContinuous().getProbDensity(3.2), 0.1);
	}

	@Test
	public void testDepEmpiricalDistribContinuous() throws InterruptedException {
		BNetwork bn = new BNetwork();
		CategoricalTable.Builder builder = new CategoricalTable.Builder("var1");
		builder.addRow(ValueFactory.create("one"), 0.7);
		builder.addRow(ValueFactory.create("two"), 0.3);
		ChanceNode var1 = new ChanceNode("var1", builder.build());
		bn.addNode(var1);

		ContinuousDistribution continuous = new ContinuousDistribution("var2",
				new UniformDensityFunction(-1, 3));
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2",
				new GaussianDensityFunction(3.0, 10.0));
		ConditionalTable table = new ConditionalTable("var2");
		table.addDistrib(new Assignment("var1", "one"), continuous);
		table.addDistrib(new Assignment("var1", "two"), continuous2);
		ChanceNode var2 = new ChanceNode("var2", table);
		var2.addInputNode(var1);
		bn.addNode(var2);

		InferenceChecks inference = new InferenceChecks();
		inference.checkCDF(bn, "var2", -1.5, 0.021);
		inference.checkCDF(bn, "var2", 0, 0.22);
		inference.checkCDF(bn, "var2", 2, 0.632);
		inference.checkCDF(bn, "var2", 8, 0.98);

		/**
		 * ProbDistribution distrib = (new ImportanceSampling()).queryProb(query);
		 * DistributionViewer.showDistributionViewer(distrib);
		 * Thread.sleep(300000000);
		 */
	}

	@Test
	public void testDirichlet() throws InterruptedException, RuntimeException {

		int oldDiscretisationSettings = Settings.discretisationBuckets;
		Settings.discretisationBuckets = 250;

		double[] alphas = new double[2];
		alphas[0] = 40.0;
		alphas[1] = 80.0;
		DirichletDensityFunction dirichlet = new DirichletDensityFunction(alphas);
		ContinuousDistribution distrib = new ContinuousDistribution("x", dirichlet);
		assertTrue(distrib.sample() instanceof ArrayVal);
		assertEquals(2, ((ArrayVal) distrib.sample()).getVector().size());
		assertEquals(0.33, ((ArrayVal) distrib.sample()).getVector().get(0), 0.15);

		assertEquals(8.0,
				distrib.getProbDensity(new ArrayVal(Arrays.asList(0.333, 0.666))),
				0.5);

		ChanceNode n = new ChanceNode("x", distrib);
		BNetwork network = new BNetwork();
		network.addNode(n);

		IndependentDistribution table =
				(new VariableElimination()).queryProb(network, "x");
		double sum = 0;
		for (Value a : table.getValues()) {
			if (((ArrayVal) a).getVector().get(0) < 0.33333) {
				sum += table.getProb(a);
			}
		}
		assertEquals(0.5, sum, 0.1);

		IndependentDistribution conversion1 =
				(new VariableElimination()).queryProb(network, "x");

		assertTrue(Math
				.abs(conversion1.getPosterior(new Assignment()).getValues().size()
						- Settings.discretisationBuckets) < 10);
		assertEquals(0.02, conversion1.getPosterior(new Assignment())
				.getProb(ValueFactory.create("[0.3333,0.6666]")), 0.05);

		IndependentDistribution conversion3 =
				(new SamplingAlgorithm(4000, 1000)).queryProb(network, "x");

		// new DistributionViewer(conversion3);
		// Thread.sleep(3000000);

		assertEquals(9.0, conversion3.toContinuous()
				.getProbDensity(ValueFactory.create("[0.3333,0.6666]")), 1.5);

		assertEquals(distrib.getFunction().getMean()[0], 0.333333, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 0.002, 0.01);

		assertEquals(conversion3.toContinuous().getFunction().getMean()[0], 0.333333,
				0.05);
		assertEquals(conversion3.toContinuous().getFunction().getVariance()[0],
				0.002, 0.05);

		Settings.discretisationBuckets = oldDiscretisationSettings;
	}

	@Test
	public void testKernelDistrib2() throws InterruptedException {

		KernelDensityFunction mkds =
				new KernelDensityFunction(Arrays.asList(new double[] { 0.1 },
						new double[] { -1.5 }, new double[] { 0.6 },
						new double[] { 1.3 }, new double[] { 1.3 }));

		ContinuousDistribution continuous2 =
				new ContinuousDistribution("var2", mkds);

		assertEquals(continuous2.getProbDensity(new double[] { -2.0 }), 0.086,
				0.001);
		assertEquals(continuous2.getProbDensity(new double[] { 0.6 }), 0.32, 0.01);
		assertEquals(continuous2.getProbDensity(new double[] { 1.3 }), 0.30, 0.01);
		double sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += ((DoubleVal) continuous2.sample()).getDouble();
		}
		assertEquals(sum / 10000.0, 0.424, 0.15);

		assertEquals(continuous2.toDiscrete().getProb(-1.5), 0.2, 0.1);

	}

	@Test
	public void nbestTest() {

		CategoricalTable.Builder builder = new CategoricalTable.Builder("test");
		builder.addRow("bla", 0.5);
		builder.addRow("blo", 0.1);
		IndependentDistribution table = builder.build();
		for (int i = 0; i < 10; i++) {
			assertEquals(table.getBest().toString(), "bla");
		}

		MultivariateTable.Builder builder2 = new MultivariateTable.Builder();
		builder2.addRow(new Assignment("test", "bla"), 0.5);
		builder2.addRow(new Assignment("test", "blo"), 0.1);
		MultivariateTable table2 = builder2.build();
		for (int i = 0; i < 10; i++) {
			assertEquals(table2.getBest().getValue("test").toString(), "bla");
		}
	}

}
