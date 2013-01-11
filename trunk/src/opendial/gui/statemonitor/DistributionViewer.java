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
import java.util.HashMap;
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
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.continuous.functions.DiscreteDensityFunction;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;

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
	
	
	public static void showDistributionViewer(ProbDistribution distrib) throws DialException {
		String variableName = getVariableName(distrib);
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

		final String variableName = getVariableName(distrib);
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
				
		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(new JLabel("        "), BorderLayout.NORTH);
		container.add(new JLabel("        "), BorderLayout.WEST);
		container.add(new JLabel("        "), BorderLayout.EAST);
		container.add(new JLabel("        "), BorderLayout.SOUTH);

		final String variableName = getVariableName(distrib);
		Set<Assignment> points = distrib.toDiscrete().getProbTable(new Assignment()).getRows();
		XYSeries serie = new XYSeries("density");
		for (Assignment point : points) {
			if (point.getValue(variableName) instanceof DoubleVal) {
				serie.add((double) ((DoubleVal)point.getValue(variableName)).getDouble(),distrib.getProbDensity(new Assignment(), point));
			}
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection(); 
		dataset.addSeries(serie);
		
		JFreeChart chart = ChartFactory.createXYLineChart( "P(" + variableName + ")", // chart title 
				"Value", // domain axis label 
				"Probability density", // range axis label
				dataset, // data 
				PlotOrientation.VERTICAL, // orientation
				false, // include legend 
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
		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		ChartPanel chartPanel = new ChartPanel(chart, false); 
		chartPanel.setPreferredSize(new Dimension(500, 270)); 
		
		container.add(chartPanel, BorderLayout.CENTER);

		setContentPane(container);
		
		setMinimumSize(new Dimension(500,400));
		setPreferredSize(new Dimension(500,400));
		pack();
	}
	
	private static String getVariableName(ProbDistribution distrib) throws DialException {
		Set<Assignment> values = distrib.toDiscrete().getProbTable(new Assignment()).getRows();
		if (!values.isEmpty() && values.iterator().next().getVariables().size() == 1) {
			return values.iterator().next().getVariables().iterator().next();
		}
		else {
			log.warning("problem extracting the variable name for "  + distrib + " (values " + distrib + ")");
		}
		return "defaultVar";
	}
	
	
	private SortedMap<Value,Double> getSimplifiedDistrib(DiscreteProbDistribution distrib, String variableName) throws DialException {
		Set<Assignment> assignments = distrib.toDiscrete().getProbTable(new Assignment()).getRows();
		SortedMap<Value,Double> simpleDistrib = new TreeMap<Value,Double>();
		
		for (Assignment assignment : assignments) {
			Value value = assignment.getValue(variableName);
			simpleDistrib.put(value, distrib.toDiscrete().getProb(new Assignment(), assignment));
		}
		return simpleDistrib;
	}
}
