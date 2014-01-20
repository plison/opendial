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

package opendial.bn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.arch.Settings;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.continuous.functions.DirichletDensityFunction;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.ConditionalCategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.distribs.other.ConditionalDistribution;
import opendial.bn.distribs.other.EmpiricalDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.utils.MathUtils;

import org.junit.Test;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DistributionsTest {

	// logger
	public static Logger log = new Logger("DistributionsTest",
			Logger.Level.DEBUG);
	
	
	
	@Test
	public void testSimpleDistrib() {
		CategoricalTable.log.setLevel(Level.MIN);
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("var1", "val1"), 0.7);
		assertTrue(table.isWellFormed());
		table.addRow(new Assignment("var1", "val2"), 0.3);
		assertTrue(table.isWellFormed());
		assertEquals(table.getProb(new Assignment("var1", "val1")), 0.7, 0.001);
		CategoricalTable table2 = new CategoricalTable();
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val2"), 0.9);
	//	assertFalse(table2.isWellFormed());
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val1"), 0.1);
		assertTrue(table2.isWellFormed());
		assertEquals(table2.getProb(new Assignment(new Assignment("var1", "val1"), new Assignment("var2", "val3"))), 0.1, 0.001);
	}
	
	@Test
	public void testMaths() {
		assertEquals(4.0, MathUtils.getVolume(2, 1), 0.001);
		assertEquals(Math.PI * 4, MathUtils.getVolume(2, 2), 0.001);
		assertEquals(4.0/3.0 * Math.PI * 8, MathUtils.getVolume(2, 3), 0.001);
		assertEquals(Math.pow(Math.PI,2) /2 * 81, MathUtils.getVolume(3, 4), 0.001);
	}
	
	@Test
	public void testConversion1Distrib() throws DialException {
	
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("var1", 1.5), 0.7);
		table.addRow(new Assignment("var1", 2.0), 0.1);
		table.addRow(new Assignment("var1", -3.0), 0.2);
		assertTrue(table.isWellFormed());
		assertEquals(table.getProb(new Assignment("var1", "2.0")), 0.1, 0.001);
		ContinuousDistribution continuous = table.toContinuous();
		assertEquals(continuous.getProbDensity(new Assignment("var1", "2.0")), 0.2, 0.001);
		assertEquals(continuous.getProbDensity(new Assignment("var1", "2.1")), 0.2, 0.001);
		assertEquals(continuous.getCumulativeProb(new Assignment("var1", "-3.1")), 0.0, 0.001);
		assertEquals(continuous.getCumulativeProb(new Assignment("var1", 1.6)), 0.9, 0.001);
		CategoricalTable table2 = continuous.toDiscrete();
		assertEquals(table2.getRows().size(), 3);
		assertEquals(table2.getProb(new Assignment("var1", 2.0)), 0.1, 0.05);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous.sample().getValue("var1")).getDouble();
		}
		assertEquals(sum/10000.0, 0.65, 0.1);
		
	}
	

	@Test
	public void testContinuous() throws DialException {
		
		ContinuousDistribution distrib = new ContinuousDistribution("X", new UniformDensityFunction(-2,4));
		assertEquals(1/6.0f, distrib.getProbDensity(new Assignment("X", 1.0)), 0.0001f);
		assertEquals(1/6.0f, distrib.getProbDensity(new Assignment("X", 4.0)), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(new Assignment("X", -3.0)), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(new Assignment("X", 6.0)), 0.0001f);
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment("X", 0.5)), 0.01);
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment("X", 4)), 0.01);
		double totalProb = 0.0f;
		for (Assignment a : distrib.toDiscrete().getPosterior(new Assignment()).getRows()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.03);
	}
	
	@Test
	public void testGaussian() throws DialException {
		
		ContinuousDistribution distrib = new ContinuousDistribution("X", new GaussianDensityFunction(1.0,3.0));
		assertEquals(0.23032, distrib.getProbDensity(new Assignment("X", 1.0)), 0.001f);
		assertEquals(0.016f, distrib.getProbDensity(new Assignment("X", -3.0f)), 0.01f);
		assertEquals(0.00357f, distrib.getProbDensity(new Assignment("X", 6.0f)), 0.01f);
		assertEquals(0.06290, distrib.toDiscrete().getProb(new Assignment("X", 1.0)), 0.01f);
		assertEquals(0.060615, distrib.toDiscrete().getProb(new Assignment("X", 0.5f)), 0.01f);
		assertEquals(0.014486, distrib.toDiscrete().getProb(new Assignment("X", 4)), 0.01f);
		double totalProb = 0.0f;
		for (Assignment a : distrib.toDiscrete().getPosterior(new Assignment()).getRows()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.05);
		assertEquals(distrib.getFunction().getMean()[0], 1.0, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 3.0, 0.01);
		
		List<Double[]> samples = new ArrayList<Double[]>();
		for (int i = 0 ; i < 20000 ; i++) {
			Double[] val = new Double[]{((DoubleVal)distrib.sample().getValue("X")).getDouble()};
			samples.add(val);
		}
		GaussianDensityFunction estimated = new GaussianDensityFunction(samples);
		assertEquals(estimated.getMean()[0], distrib.getFunction().getMean()[0], 0.05);
		assertEquals(estimated.getVariance()[0], distrib.getFunction().getVariance()[0], 0.08);
	}
	
	
	@Test
	public void testDiscrete() throws DialException {
		
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("A", 1), 0.6);
		table.addRow(new Assignment("A", 2.5), 0.3);
		assertEquals(0.3, table.getProb(new Assignment("A", 2.5)), 0.0001f);
		assertEquals(0.6, table.getProb(new Assignment("A", 1.0)), 0.0001f);
		ContinuousDistribution distrib = table.toContinuous();
		assertEquals(0.2 , distrib.getProbDensity(new Assignment("A", 2.5)), 0.01);
		assertEquals(0.4, distrib.getProbDensity(new Assignment("A", 1)), 0.001);
		assertEquals(0, distrib.getProbDensity(new Assignment("A", -2)), 0.001f);
		assertEquals(0.4, distrib.getProbDensity(new Assignment("A", 0.9)), 0.001f);
		assertEquals(0.4, distrib.getProbDensity(new Assignment("A", 1.2)), 0.0001f);
		assertEquals(0.2, distrib.getProbDensity(new Assignment("A", 2.2)), 0.001f);
		assertEquals(0.2, distrib.getProbDensity(new Assignment("A", 2.7)), 0.001f);
		assertEquals(0, distrib.getProbDensity(new Assignment("A", 5)), 0.0001f);
		assertEquals(0, distrib.getCumulativeProb(new Assignment("A", 0.5)), 0.0001f);
		assertEquals(0.6, distrib.getCumulativeProb(new Assignment("A", 1.1)), 0.0001f);
		assertEquals(0.6, distrib.getCumulativeProb(new Assignment("A", 2.4)), 0.0001f);
		assertEquals(0.9, distrib.getCumulativeProb(new Assignment("A", 2.5)), 0.0001f);
		assertEquals(0.9, distrib.getCumulativeProb(new Assignment("A", 2.6)), 0.0001f); 
	
		assertEquals(distrib.getFunction().getMean()[0], 1.35, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 0.47, 0.01);
	}
	
	@Test
	public void testUniformDistrib() {
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", new UniformDensityFunction(-2, 3.0));
		assertEquals(continuous2.getProbDensity(new Assignment("var2", 1.2)), 1/5.0, 0.001);
	//	assertEquals(continuous2.getCumulativeProb(new Assignment("var2", 2)), 4/5.0, 0.001);
		assertEquals(continuous2.toDiscrete().getHeadValues().size(),
				Settings.discretisationBuckets);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment(), new Assignment("var2", 1.2)), 0.01, 0.01);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 0.5, 0.1);
		
		assertEquals(continuous2.getFunction().getMean()[0], 0.5, 0.01);
		assertEquals(continuous2.getFunction().getVariance()[0], 2.08, 0.01);
		
	}
	
	@Test
	public void testGaussianDistrib() {
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", new GaussianDensityFunction(2.0, 3.0));
		assertEquals(continuous2.getProbDensity(new Assignment("var2", 1.2)), 0.2070, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment("var2", 2.0)), 0.23033, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2", 2)), 0.5, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2", 3)), 0.7181, 0.001); 
		assertTrue(continuous2.toDiscrete().getHeadValues().size()> Settings.discretisationBuckets/2);
		assertTrue(continuous2.toDiscrete().getHeadValues().size()<= Settings.discretisationBuckets);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment(), new Assignment("var2", 2)), 0.06205, 0.01);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 2.0, 0.1);
	}
	
	
	@Test
	public void testKernelDistrib() throws InterruptedException {
		KernelDensityFunction kds = new KernelDensityFunction(Arrays.asList(new Double[]{0.1}, 
				new Double[]{-1.5}, new Double[]{0.6}, new Double[]{1.3}, new Double[]{1.3}));
		
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", kds);

		assertEquals(continuous2.getProbDensity(new Assignment("var2",-2.0)), 0.086, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment("var2",0.6)), 0.32 , 0.01);
		assertEquals(continuous2.getProbDensity(new Assignment("var2",1.3)), 0.30, 0.01);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2",ValueFactory.create(-1.6))), 0.0, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2",ValueFactory.create(-1.4))), 0.2, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2",ValueFactory.create(1.29))), 0.6, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2",ValueFactory.create(1.3))), 1.0, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment("var2",ValueFactory.create(1.31))), 1.0, 0.001); 
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 0.424, 0.1);
	//		DistributionViewer.showDistributionViewer(continuous2);
	//	Thread.sleep(300000000); 
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", -1.5)), 0.2, 0.03);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", 1.3)), 0.4, 0.03);	
		
		assertEquals(continuous2.getFunction().getMean()[0], 0.36, 0.01);
		assertEquals(continuous2.getFunction().getVariance()[0], 1.07, 0.01);
	}
	
	
	@Test
	public void testEmpiricalDistrib() throws DialException {
		
		CategoricalTable st = new CategoricalTable();
		st.addRow(new Assignment("var1", "val1"), 0.6);
		st.addRow(new Assignment("var1", "val2"), 0.4);
		
		ConditionalCategoricalTable table = new ConditionalCategoricalTable();
		table.addRow(new Assignment("var1", "val1"), new Assignment("var2", "val1"), 0.9);
		table.addRow(new Assignment("var1", "val1"), new Assignment("var2", "val2"), 0.1);
		table.addRow(new Assignment("var1", "val2"), new Assignment("var2", "val1"), 0.2);
		table.addRow(new Assignment("var1", "val2"), new Assignment("var2", "val2"), 0.8);

		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1");

		var1.setDistrib(st);
		bn.addNode(var1);
		 
		ChanceNode var2 = new ChanceNode("var2");
		var2.setDistrib(table);
		var2.addInputNode(var1);
		bn.addNode(var2);
		
		LikelihoodWeighting sampling = new LikelihoodWeighting(2000, 500);

		DiscreteDistribution distrib = sampling.queryProb(new ProbQuery(bn, Arrays.asList("var2"), 
				new Assignment("var1", "val1"))).toDiscrete();

		assertEquals(distrib.getProb(new Assignment(), new Assignment("var2", "val1")), 0.9, 0.05);
		assertEquals(distrib.getProb(new Assignment(), new Assignment("var2", "val2")), 0.1, 0.05);
		
		DiscreteDistribution distrib2 = sampling.queryProb(new ProbQuery(bn, "var2")).toDiscrete();
	
		assertEquals(distrib2.getProb(new Assignment(), new Assignment("var2", "val1")), 0.62, 0.05);
		assertEquals(distrib2.getProb(new Assignment(), new Assignment("var2", "val2")), 0.38, 0.05);
	
	}
	

	@Test
	public void empiricalDistribContinuous() throws DialException {
		ContinuousDistribution continuous = new ContinuousDistribution("var1", new UniformDensityFunction(-1,3));
		
		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1");
		var1.setDistrib(continuous);
		bn.addNode(var1);
		
		
		LikelihoodWeighting sampling = new LikelihoodWeighting(2000, 200);
		
		IndependentProbDistribution distrib2 = sampling.queryProb(new ProbQuery(bn, "var1"));
		assertTrue(distrib2.toDiscrete().getPosterior(new Assignment()).getRows().size() > 250);
		assertEquals(0, distrib2.toContinuous().getCumulativeProb(new Assignment("var1", -1.1)), 0.001);
		assertEquals(0.5, distrib2.toContinuous().getCumulativeProb(new Assignment("var1", 1)), 0.05);
		assertEquals(1.0, distrib2.toContinuous().getCumulativeProb(new Assignment("var1", 3.1)), 0.00); 
		
		assertEquals(continuous.getProbDensity(new Assignment("var1", -2)), 
				distrib2.toContinuous().getProbDensity(new Assignment("var1", -2)), 0.1);
		assertEquals(continuous.getProbDensity(new Assignment("var1", -0.5)), 
				distrib2.toContinuous().getProbDensity(new Assignment("var1", -0.5)), 0.1);		
		assertEquals(continuous.getProbDensity(new Assignment("var1", 1.8)), 
				distrib2.toContinuous().getProbDensity(new Assignment("var1", 1.8)), 0.1);	
		assertEquals(continuous.getProbDensity(new Assignment("var1", 3.2)), 
				distrib2.toContinuous().getProbDensity(new Assignment("var1", 3.2)), 0.1);	
	}
	
	
	@Test
	public void testDepEmpiricalDistribContinuous() throws DialException, InterruptedException {
		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1");
		var1.addProb(ValueFactory.create("one"), 0.7);
		var1.addProb(ValueFactory.create("two"), 0.3);
		bn.addNode(var1);
		
		ContinuousDistribution continuous = new ContinuousDistribution("var2", new UniformDensityFunction(-1,3));
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", new GaussianDensityFunction(3.0,10.0));
		ConditionalDistribution<ContinuousDistribution> table = new ConditionalDistribution<ContinuousDistribution>();
		table.addDistrib(new Assignment("var1", "one"), continuous);
		table.addDistrib(new Assignment("var1", "two"), continuous2);
		ChanceNode var2 = new ChanceNode("var2");
		var2.addInputNode(var1);
		var2.setDistrib(table);
		bn.addNode(var2);
		
		ProbQuery query = new ProbQuery(bn, Arrays.asList("var2"));
		InferenceChecks inference = new InferenceChecks();
		inference.checkCDF(query, new Assignment("var2", -1.5), 0.021);
		inference.checkCDF(query, new Assignment("var2", 0), 0.22);
		inference.checkCDF(query, new Assignment("var2", 2), 0.632);
		inference.checkCDF(query, new Assignment("var2", 8), 0.98);
		
	/**	ProbDistribution distrib = (new ImportanceSampling()).queryProb(query);
		DistributionViewer.showDistributionViewer(distrib);
		Thread.sleep(300000000);  */
	} 
	
	
	@Test
	public void testDirichlet() throws InterruptedException, DialException {
		
		int oldDiscretisationSettings = Settings.discretisationBuckets;
		Settings.discretisationBuckets = 250;
		
		Double[] alphas = new Double[2];
		alphas[0] = 40.0;
		alphas[1] = 80.0;
		DirichletDensityFunction dirichlet = new DirichletDensityFunction(alphas);
		ContinuousDistribution distrib = new ContinuousDistribution("x", dirichlet);
		assertTrue(distrib.sample().getValue("x") instanceof ArrayVal);
		assertEquals(2, ((ArrayVal)distrib.sample().getValue("x")).getVector().size());
		assertEquals(0.33, ((ArrayVal)distrib.sample().getValue("x")).getVector().get(0), 0.15);
		
		assertEquals(8.0, distrib.getProbDensity(new Assignment("x",
				new ArrayVal(Arrays.asList(0.333, 0.666)))), 0.5);
		
		ChanceNode n = new ChanceNode("x");
		n.setDistrib(distrib);
		BNetwork network = new BNetwork();
		network.addNode(n);
		
		CategoricalTable table = (new VariableElimination()).queryProb(new ProbQuery(network, "x"));
		double sum = 0;
		for (Assignment a : table.getRows()) {
			if (((ArrayVal)a.getValue("x")).getVector().get(0) < 0.33333) {
			sum += table.getProb(a);
			}
		}
		assertEquals(0.5, sum, 0.1);		
		
		DiscreteDistribution conversion1 = (new VariableElimination()).
				queryProb(new ProbQuery(network, "x"));
		
		assertTrue(Math.abs(conversion1.getPosterior(new Assignment()).getRows().size()
				- Settings.discretisationBuckets) < 10 );
		assertEquals(0.02, conversion1.getPosterior(new Assignment()).getProb(new Assignment("x",
				ValueFactory.create("[0.3333,0.6666]"))), 0.05);
		
		EmpiricalDistribution conversion3 = (new LikelihoodWeighting(4000, 1000)).
				queryProb(new ProbQuery(network, "x"));
		

	//	new DistributionViewer(conversion3);
	//	Thread.sleep(3000000);
		
		assertEquals(9.0, conversion3.toContinuous().getProbDensity(
				new Assignment("x", ValueFactory.create("[0.3333,0.6666]"))), 1.5);		

		assertEquals(distrib.getFunction().getMean()[0], 0.333333, 0.01);
		assertEquals(distrib.getFunction().getVariance()[0], 0.002, 0.01);
		
		assertEquals(conversion3.toContinuous().getFunction().getMean()[0], 0.333333, 0.05);
		assertEquals(conversion3.toContinuous().getFunction().getVariance()[0], 0.002, 0.05);

		Settings.discretisationBuckets = oldDiscretisationSettings;
	}
	
	

	@Test
	public void testKernelDistrib2() throws InterruptedException {

		KernelDensityFunction mkds = new KernelDensityFunction(Arrays.asList(
				new Double[]{0.1}, new Double[]{-1.5}, new Double[]{0.6}, new Double[]{1.3}, new Double[]{1.3}));
		
		ContinuousDistribution continuous2 = new ContinuousDistribution("var2", mkds);

		assertEquals(continuous2.getProbDensity(new Assignment("var2",new Double[]{-2.0})), 0.086, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment("var2",new Double[]{0.6})), 0.32 , 0.01);
		assertEquals(continuous2.getProbDensity(new Assignment("var2",new Double[]{1.3})), 0.30, 0.01);
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 0.424, 0.1);

		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", -1.5)), 0.2, 0.1);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", 1.3)), 0.4, 0.1);	
		
	}
	
}
