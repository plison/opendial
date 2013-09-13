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

package opendial.gui.statemonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JLabel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;

import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;

import opendial.bn.distribs.continuous.functions.DiscreteDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings({ "deprecation", "serial" })
public class DistributionViewer extends JDialog {

	// logger
	public static Logger log = new Logger("DistributionViewer", Logger.Level.DEBUG);

	
	static Map<String,DistributionViewer> viewers = 
		new HashMap<String,DistributionViewer>();
	
	
	
	public static void showDistributionViewer(ProbDistribution distrib) {
		if (distrib.getHeadVariables().size() != 1) {
			log.warning("distribution must have a single head variable " +
					"to be shown, but we have : " + distrib.getHeadVariables());
		}
		else {
			try {
		String variableName = distrib.getHeadVariables().iterator().next();
		if (!viewers.containsKey(variableName)) {
			
			DistributionViewer newViewer;
			if (canBeContinuous(distrib)) {
				newViewer = new DistributionViewer(distrib.toContinuous());
			}
			else {
				newViewer = new DistributionViewer(distrib.toDiscrete());
			}
			
			viewers.put(variableName, newViewer);
			newViewer.setVisible(true);
			
		}
		else {
			DistributionViewer viewer = viewers.get(variableName);
			viewer.setVisible(true);
		}
		}
			catch (DialException e) {
				log.warning("could not show distribution: " + e);
			}
		}
		
	}
	
	
	private static boolean canBeContinuous (ProbDistribution distrib) {
		if (distrib instanceof ContinuousProbDistribution) {
			return true;
		}
		else {
			try {
				distrib.toContinuous();
				return true;
			}
			catch (DialException e) {
				return false;
			}
		}
	}
	
	
	
	private DistributionViewer(DiscreteProbDistribution distrib) throws DialException {
		super(null,Dialog.ModalityType.DOCUMENT_MODAL);
		setTitle("Distribution Viewer");
		
		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(new JLabel("        "), BorderLayout.NORTH);
		container.add(new JLabel("        "), BorderLayout.WEST);
		container.add(new JLabel("        "), BorderLayout.EAST);
		container.add(new JLabel("        "), BorderLayout.SOUTH);

		final String variableName = distrib.getHeadVariables().iterator().next();
		Map<Value,Double> simpleDistrib = getSimplifiedDistrib(distrib, variableName);
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset(); 
		
		for (Value value : simpleDistrib.keySet()) {
			dataset.addValue(simpleDistrib.get(value), "", value.toString());
		}
		
		JFreeChart chart = ChartFactory.createBarChart( "P(" + variableName + ")", // chart title 
				"Value", // domain axis label 
				"Probability", // range axis label
				dataset, // data 
				PlotOrientation.VERTICAL, // orientation
				false, // include legend 
				true, // tooltips? 
				false // URLs?
		);
				
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		BarRenderer renderer = (BarRenderer) plot.getRenderer(); 
		renderer.setToolTipGenerator(new CategoryToolTipGenerator()
		{ public String generateToolTip(CategoryDataset data, int series, int category) {
			return "P("+variableName + "=" + data.getColumnKeys().get(category) + ") = "
			+ data.getValue(series, category); 
			} });
			       
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        
		renderer.setSeriesPaint(0, new Color(5,100,30)); 
		
		ChartPanel chartPanel = new ChartPanel(chart, false); 
		chartPanel.setPreferredSize(new Dimension(500, 270)); 
		
		container.add(chartPanel, BorderLayout.CENTER);

		setContentPane(container);
		
		setMinimumSize(new Dimension(500,400));
		setPreferredSize(new Dimension(500,400));
		pack();
	}
	
	

	private DistributionViewer(ContinuousProbDistribution distrib) throws DialException {
		
		super(null,Dialog.ModalityType.DOCUMENT_MODAL);
		setTitle("Distribution Viewer");
			
		if (distrib.getDimensionality() > 4) {
			throw new DialException("dimensionality is too high " + distrib.getDimensionality());
		}
				
		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(new JLabel("        "), BorderLayout.NORTH);
		container.add(new JLabel("        "), BorderLayout.WEST);
		container.add(new JLabel("        "), BorderLayout.EAST);
		container.add(new JLabel("        "), BorderLayout.SOUTH);

		String variableName = distrib.getHeadVariables().iterator().next();
		
		List<Assignment> samples = new ArrayList<Assignment>();
		for (int i = 0 ; i < 500 ; i++) {
			samples.add(distrib.sample(new Assignment()));
		}
		
		List<XYSeries> series = new ArrayList<XYSeries>(); 

		for (int i = 0 ; i < distrib.getDimensionality() ; i++) {
			series.add(new XYSeries("dimension "+i));
		}
		
		Collections.sort(samples, new AssignmentComparator());
		for (Assignment point : samples) {
			Value value = point.getValue(variableName);
			if (value instanceof DoubleVal) {
				series.get(0).add((double) ((DoubleVal)value).getDouble(),
						distrib.getProbDensity(new Assignment(), point));
			}
			
			else if (value instanceof VectorVal) {
				for (int i = 0 ; i < ((VectorVal)value).getVector().size(); i++) {
					double subval = ((VectorVal)value).getVector().get(i).doubleValue();
					series.get(i).add(subval,distrib.getProbDensity(new Assignment(), point));		
				}
			}
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection(); 
		for (XYSeries serie : series) {
			dataset.addSeries(serie);
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart( "P(" + variableName + ")", // chart title 
				"Value", // domain axis label 
				"Probability density", // range axis label
				dataset, // data 
				PlotOrientation.VERTICAL, // orientation
				(distrib.getDimensionality() > 1), // include legend 
				true, // tooltips? 
				false // URLs?
		);
				
		XYPlot plot = (XYPlot) chart.getPlot();
	/**	BarRenderer renderer = (BarRenderer) plot.getRenderer(); 
		renderer.setToolTipGenerator(new CategoryToolTipGenerator()
		{ public String generateToolTip(CategoryDataset data, int series, int category) {
			return "P("+variableName + "=" + data.getColumnKeys().get(category) + ") = "
			+ data.getValue(series, category); 
			} });
			       
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        
		renderer.setSeriesPaint(0, new Color(5,100,30)); */
		
		plot.setBackgroundPaint(Color.white); plot.setRangeGridlinePaint(Color.white);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
	//	domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		domainAxis.setAutoTickUnitSelection(true);
		
			
		ChartPanel chartPanel = new ChartPanel(chart, false); 
		chartPanel.setPreferredSize(new Dimension(500, 300)); 
		
		container.add(chartPanel);

		setContentPane(container);
		
		pack();
	}
	
	
	private SortedMap<Value,Double> getSimplifiedDistrib(DiscreteProbDistribution distrib, 
			String variableName) throws DialException {
		
		Set<Assignment> assignments = distrib.getProbTable(new Assignment()).getRows();
		SortedMap<Value,Double> simpleDistrib = new TreeMap<Value,Double>();
		for (Assignment assignment : assignments) {
			Value value = assignment.getValue(variableName);
			simpleDistrib.put(value, distrib.getProb(new Assignment(), assignment));
		}
		return simpleDistrib;
	}
	
	
	final class AssignmentComparator implements Comparator<Assignment> {
		
		public int compare(Assignment a, Assignment b) {
			if (a.getVariables().size() == 1 && a.getVariables().equals(b.getVariables())) {
				String var = a.getVariables().iterator().next();
				return a.getValue(var).compareTo(b.getValue(var));
			}
		//	log.warning("problem comparing " + a + " and " + b);
			return 0;
		}
	}
}
