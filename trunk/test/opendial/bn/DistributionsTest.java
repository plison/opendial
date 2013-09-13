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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;


import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;

import opendial.bn.distribs.continuous.functions.DirichletDensityFunction;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.continuous.functions.MultiDiscreteDensityFunction;
import opendial.bn.distribs.continuous.functions.ProductKernelDensityFunction;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.VectorVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.gui.statemonitor.DistributionViewer;
import opendial.inference.ImportanceSampling;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;

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
	
	
	public DistributionsTest() {
		Settings.getInstance().nbSamples = 4000;
		Settings.getInstance().maximumSamplingTime = 1000;
	}
	
	
	@Test
	public void simpleDistrib() {
		SimpleTable.log.setLevel(Level.MIN);
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("var1", "val1"), 0.7);
		assertTrue(table.isWellFormed());
		table.addRow(new Assignment("var1", "val2"), 0.3);
		assertTrue(table.isWellFormed());
		assertEquals(table.getProb(new Assignment("var1", "val1")), 0.7, 0.001);
		SimpleTable table2 = new SimpleTable();
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val2"), 0.9);
		assertFalse(table2.isWellFormed());
		table2.addRow(new Assignment(new Assignment("var2", "val3"), "var1", "val1"), 0.1);
		assertTrue(table2.isWellFormed());
		assertEquals(table2.getProb(new Assignment(new Assignment("var1", "val1"), new Assignment("var2", "val3"))), 0.1, 0.001);
	}
	
	@Test
	public void conversion1Distrib() throws DialException {
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("var1", 1.5), 0.7);
		table.addRow(new Assignment("var1", 2.0), 0.1);
		table.addRow(new Assignment("var1", -3.0), 0.2);
		assertTrue(table.isWellFormed());
		assertEquals(table.getProb(new Assignment("var1", "2.0")), 0.1, 0.001);
		UnivariateDistribution continuous = (UnivariateDistribution)table.toContinuous();
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", "2.0")), 0.02222, 0.001);
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", "2.1")), 0.0222, 0.001);
		assertEquals(continuous.getCumulativeProb(new Assignment(), new Assignment("var1", "-3.1")), 0.0, 0.001);
		assertEquals(continuous.getCumulativeProb(new Assignment(), new Assignment("var1", 1.6)), 0.9, 0.001);
		SimpleTable table2 = continuous.toDiscrete();
		assertEquals(table2.getRows().size(), 3);
		assertEquals(table2.getProb(new Assignment("var1", 2.0)), 0.1, 0.001);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous.sample().getValue("var1")).getDouble();
		}
		assertEquals(sum/10000.0, 0.65, 0.1);
		
	}
	

	@Test
	public void testContinuous() throws DialException {
		
		UnivariateDistribution distrib = new UnivariateDistribution("X", new UniformDensityFunction(-2,4));
		assertEquals(1/6.0f, distrib.getProbDensity(new Assignment("X", 1.0)), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(new Assignment("X", -3.0)), 0.0001f);
		assertEquals(0.0f, distrib.getProbDensity(new Assignment("X", 6.0)), 0.0001f);
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment("X", 0.5)), 0.0001);
		assertEquals(0.01, distrib.toDiscrete().getProb(new Assignment("X", 4)), 0.0001);
		double totalProb = 0.0f;
		for (Assignment a : distrib.toDiscrete().getProbTable(new Assignment()).getRows()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.01);
	}
	
	@Test
	public void testGaussian() throws DialException {
		
		UnivariateDistribution distrib = new UnivariateDistribution("X", new GaussianDensityFunction(1.0,3.0));
		assertEquals(0.23032, distrib.getProbDensity(new Assignment("X", 1.0)), 0.0001f);
		assertEquals(0.016f, distrib.getProbDensity(new Assignment("X", -3.0f)), 0.0001f);
		assertEquals(0.00357f, distrib.getProbDensity(new Assignment("X", 6.0f)), 0.0001f);
		assertEquals(0.03180, distrib.toDiscrete().getProb(new Assignment("X", 1.0)), 0.0001f);
		assertEquals(0.030315, distrib.toDiscrete().getProb(new Assignment("X", 0.5f)), 0.0001f);
		assertEquals(0.007786, distrib.toDiscrete().getProb(new Assignment("X", 4)), 0.001f);
		double totalProb = 0.0f;
		for (Assignment a : distrib.toDiscrete().getProbTable(new Assignment()).getRows()) {
			totalProb += distrib.toDiscrete().getProb(a);
		}
		assertEquals(1.0, totalProb, 0.03);
		assertEquals(distrib.getMean(), 1.0, 0.01);
		assertEquals(distrib.getVariance(), 3.0, 0.01);
	}
	
	
	@Test
	public void testDiscrete() throws DialException {
		
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("A", 1), 0.6);
		table.addRow(new Assignment("A", 2.5), 0.3);
		assertEquals(0.3, table.getProb(new Assignment("A", 2.5)), 0.0001f);
		assertEquals(0.6, table.getProb(new Assignment("A", 1.0)), 0.0001f);
		UnivariateDistribution distrib = (UnivariateDistribution)table.toContinuous();
		assertEquals(0.22222, distrib.getProbDensity(new Assignment("A", 2.5)), 0.0001);
		assertEquals(0.44444, distrib.getProbDensity(new Assignment("A", 1)), 0.0001);
		assertEquals(0, distrib.getProbDensity(new Assignment("A", -2)), 0.0001f);
		assertEquals(0.44444, distrib.getProbDensity(new Assignment("A", 0.9)), 0.0001f);
		assertEquals(0.44444, distrib.getProbDensity(new Assignment("A", 1.2)), 0.0001f);
		assertEquals(0.22222, distrib.getProbDensity(new Assignment("A", 2.2)), 0.0001f);
		assertEquals(0.22222, distrib.getProbDensity(new Assignment("A", 2.7)), 0.0001f);
		assertEquals(0, distrib.getProbDensity(new Assignment("A", 5)), 0.0001f);
		assertEquals(0, distrib.getCumulativeProb(new Assignment("A", 0.5)), 0.0001f);
		assertEquals(0.6666, distrib.getCumulativeProb(new Assignment("A", 1.1)), 0.0001f);
		assertEquals(0.6666, distrib.getCumulativeProb(new Assignment("A", 2.4)), 0.0001f);
		assertEquals(1.0, distrib.getCumulativeProb(new Assignment("A", 2.5)), 0.0001f);
		assertEquals(1.0, distrib.getCumulativeProb(new Assignment("A", 2.6)), 0.0001f);
		
		assertEquals(distrib.getMean(), 1.5, 0.01);
		assertEquals(distrib.getVariance(), 0.5, 0.01);
	}
	
	@Test
	public void uniformDistrib() {
		UnivariateDistribution continuous2 = new UnivariateDistribution("var2", new UniformDensityFunction(-2, 3.0));
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2", 1.2)), 1/5.0, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment(), new Assignment("var2", 2)), 4/5.0, 0.001);
		assertEquals(continuous2.toDiscrete().getHeadValues().size(),
				Settings.getInstance().nbDiscretisationBuckets);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment(), new Assignment("var2", 1.2)), 0.01, 0.001);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 0.5, 0.1);
		
		assertEquals(continuous2.getMean(), 0.5, 0.01);
		assertEquals(continuous2.getVariance(), 2.08, 0.01);
		
	}
	
	@Test
	public void gaussianDistrib() {
		UnivariateDistribution continuous2 = new UnivariateDistribution("var2", new GaussianDensityFunction(2, 3.0));
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2", 1.2)), 0.2070, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2", 2.0)), 0.23033, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment(), new Assignment("var2", 2)), 0.5, 0.001);
		assertEquals(continuous2.getCumulativeProb(new Assignment(), new Assignment("var2", 3)), 0.7181, 0.001);
		assertEquals(continuous2.toDiscrete().getHeadValues().size(),
				Settings.getInstance().nbDiscretisationBuckets);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment(), new Assignment("var2", 2)), 0.031805, 0.001);
		
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 2.0, 0.1);
	}
	
	
	@Test
	public void kernelDistrib() throws InterruptedException {
		KernelDensityFunction kds = new KernelDensityFunction(Arrays.asList(0.1, -1.5, 0.6, 1.3, 1.3));
		
		UnivariateDistribution continuous2 = new UnivariateDistribution("var2", kds);

		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",-2.0)), 0.086, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",0.6)), 0.32 , 0.01);
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",1.3)), 0.30, 0.01);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(-1.6)), 0.0, 0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(-1.4)), 0.2, 0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.29)), 0.6, 0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.3)), 1.0, 0.001);
		assertEquals(continuous2.getCumulativeProb(ValueFactory.create(1.31)), 1.0, 0.001);
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((DoubleVal)continuous2.sample().getValue("var2")).getDouble();
		}
		assertEquals(sum/10000.0, 0.424, 0.1);
	//		DistributionViewer.showDistributionViewer(continuous2);
	//	Thread.sleep(300000000); 
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", -1.5)), 0.2, 0.001);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", 1.3)), 0.4, 0.001);	
		
		assertEquals(continuous2.getMean(), 0.36, 0.01);
		assertEquals(continuous2.getVariance(), 1.07, 0.01);
	}
	
	
	@Test
	public void empiricalDistrib() throws DialException {
		
		SimpleTable st = new SimpleTable();
		st.addRow(new Assignment("var1", "val1"), 0.6);
		st.addRow(new Assignment("var1", "val2"), 0.4);
		
		DiscreteProbabilityTable table = new DiscreteProbabilityTable();
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
		
		ImportanceSampling sampling = new ImportanceSampling(2000, 500);
		DiscreteProbDistribution distrib = sampling.queryProb(new ProbQuery(bn, Arrays.asList("var2"), 
				new Assignment("var1", "val1"))).toDiscrete();
		assertEquals(distrib.getProb(new Assignment(), new Assignment("var2", "val1")), 0.9, 0.05);
		assertEquals(distrib.getProb(new Assignment(), new Assignment("var2", "val2")), 0.1, 0.05);
		
		DiscreteProbDistribution distrib2 = sampling.queryProb(new ProbQuery(bn, "var2")).toDiscrete();
	
		assertEquals(distrib2.getProb(new Assignment(), new Assignment("var2", "val1")), 0.62, 0.05);
		assertEquals(distrib2.getProb(new Assignment(), new Assignment("var2", "val2")), 0.38, 0.05);
	
	}
	

	@Test
	public void empiricalDistribContinuous() throws DialException {
		UnivariateDistribution continuous = new UnivariateDistribution("var1", new UniformDensityFunction(-1,3));
		
		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1");
		var1.setDistrib(continuous);
		bn.addNode(var1);
		
		
		ImportanceSampling sampling = new ImportanceSampling(2000, 200);
		
		ProbDistribution distrib2 = sampling.queryProb(new ProbQuery(bn, "var1"));
		assertTrue(distrib2.toDiscrete().getProbTable(new Assignment()).getRows().size() > 250);
		assertEquals(0, distrib2.toContinuous().getCumulativeProb(
				new Assignment(), new Assignment("var1", -1.1)), 0.001);
		assertEquals(0.5, distrib2.toContinuous().getCumulativeProb(
				new Assignment(), new Assignment("var1", 1)), 0.05);
		assertEquals(1.0, distrib2.toContinuous().getCumulativeProb(
				new Assignment(), new Assignment("var1", 3.1)), 0.00);
		
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", -2)), 
				distrib2.toContinuous().getProbDensity(new Assignment(), new Assignment("var1", -2)), 0.1);
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", -0.5)), 
				distrib2.toContinuous().getProbDensity(new Assignment(), new Assignment("var1", -0.5)), 0.1);		
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", 1.8)), 
				distrib2.toContinuous().getProbDensity(new Assignment(), new Assignment("var1", 1.8)), 0.1);	
		assertEquals(continuous.getProbDensity(new Assignment(), new Assignment("var1", 3.2)), 
				distrib2.toContinuous().getProbDensity(new Assignment(), new Assignment("var1", 3.2)), 0.1);	
	}
	
	
	@Test
	public void depEmpiricalDistribContinuous() throws DialException, InterruptedException {
		BNetwork bn = new BNetwork();
		ChanceNode var1 = new ChanceNode("var1");
		var1.addProb(ValueFactory.create("one"), 0.7);
		var1.addProb(ValueFactory.create("two"), 0.3);
		bn.addNode(var1);
		
		UnivariateDistribution continuous = new UnivariateDistribution("var2", new UniformDensityFunction(-1,3));
		UnivariateDistribution continuous2 = new UnivariateDistribution("var2", new GaussianDensityFunction(3,10));
		ContinuousProbabilityTable table = new ContinuousProbabilityTable();
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
		Thread.sleep(300000000); */
	}
	
	
	@Test
	public void dirichletTest() throws InterruptedException, DialException {
		

		Double[] alphas = new Double[2];
		alphas[0] = 40.0;
		alphas[1] = 80.0;
		DirichletDensityFunction dirichlet = new DirichletDensityFunction(alphas);
		MultivariateDistribution distrib = new MultivariateDistribution("x", dirichlet);
		assertTrue(distrib.sample().getValue("x") instanceof VectorVal);
		assertEquals(2, ((VectorVal)distrib.sample().getValue("x")).getVector().size());
		assertEquals(0.33, ((VectorVal)distrib.sample().getValue("x")).getVector().get(0), 0.15);
		
		assertEquals(8.0, distrib.getProbDensity(new Assignment(), new Assignment("x",
				new VectorVal(Arrays.asList(0.333, 0.666)))), 0.5);
		
		ChanceNode n = new ChanceNode("x");
		n.setDistrib(distrib);
		BNetwork network = new BNetwork();
		network.addNode(n);
		
		SimpleTable table = (SimpleTable)(new VariableElimination()).queryProb(new ProbQuery(network, "x"));
		double sum = 0;
		for (Assignment a : table.getRows()) {
			if (((VectorVal)a.getValue("x")).getVector().get(0) < 0.33333) {
			sum += table.getProb(a);
			}
		}
		assertEquals(0.5, sum, 0.08);		
		
		DiscreteProbDistribution conversion1 = (DiscreteProbDistribution)(new VariableElimination()).
				queryProb(new ProbQuery(network, "x"));
		
		assertTrue(Math.abs(conversion1.getProbTable(new Assignment()).getRows().size() - Settings.getInstance().nbDiscretisationBuckets) < 10 );
		assertEquals(0.02, conversion1.getProbTable(new Assignment()).getProb(new Assignment("x",
				ValueFactory.create("(0.3333,0.6666)"))), 0.05);
		
		SimpleEmpiricalDistribution conversion3 = (SimpleEmpiricalDistribution)(new ImportanceSampling()).
				queryProb(new ProbQuery(network, "x"));
		

	//	DistributionViewer.showDistributionViewer(conversion3);
	//	Thread.sleep(3000000);
		
		assertEquals(9.0, conversion3.toContinuous().getProbDensity(new Assignment(), 
				new Assignment("x", ValueFactory.create("(0.3333,0.6666)"))), 1.5);		

		assertEquals(distrib.getMean()[0], 0.333333, 0.01);
		assertEquals(distrib.getVariance()[0], 0.002, 0.01);
		
		assertEquals(((MultivariateDistribution)conversion3.toContinuous()).getMean()[0], 0.333333, 0.05);
		assertEquals(((MultivariateDistribution)conversion3.toContinuous()).getVariance()[0], 0.002, 0.05);

	}
	
	

	@Test
	public void kernelDistrib2() throws InterruptedException {

		ProductKernelDensityFunction mkds = new ProductKernelDensityFunction(Arrays.asList(
				new Double[]{0.1}, new Double[]{-1.5}, new Double[]{0.6}, new Double[]{1.3}, new Double[]{1.3}));
		
		MultivariateDistribution continuous2 = new MultivariateDistribution("var2", mkds);

		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",new Double[]{-2.0})), 0.086, 0.001);
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",new Double[]{0.6})), 0.32 , 0.01);
		assertEquals(continuous2.getProbDensity(new Assignment(), new Assignment("var2",new Double[]{1.3})), 0.30, 0.01);
		double sum = 0;
		for (int i = 0 ; i < 10000 ; i++) {
			sum += ((VectorVal)continuous2.sample().getValue("var2")).getVector().get(0);
		}
		assertEquals(sum/10000.0, 0.424, 0.1);

		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", new Double[]{-1.5})), 0.2, 0.1);
		assertEquals(continuous2.toDiscrete().getProb(new Assignment("var2", new Double[]{1.3})), 0.4, 0.1);	
		
	}
	
}
