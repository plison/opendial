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

package opendial.gui.stateviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JDialog;
import javax.swing.JLabel;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution.DistribType;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

	public DistributionViewer(final IndependentProbDistribution distrib, final StateViewer viewer) {
		super(viewer.tab.getMainFrame().getFrame(),Dialog.ModalityType.MODELESS);
		setTitle("Distribution Viewer");
		update(distrib);
		
		addWindowListener( new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent e) {
            	super.windowClosing(e);
                viewer.shownDistribs.remove(distrib.getHeadVariables());
            }

        });
	}

	
	private ChartPanel generatePanel(CategoricalTable distrib) {
		final String variableName = distrib.getHeadVariables().toString().replace("[", "").replace("]", "");

		DefaultCategoryDataset dataset = new DefaultCategoryDataset(); 

		for (Assignment row : distrib.getRows()) {
			String val = row.getValues().toString().replace("[", "").replace("]", "");
			dataset.addValue(distrib.getProb(row), "", val.toString());
		}

		JFreeChart chart = ChartFactory.createBarChart("Probability distribution P(" + variableName + ")", // chart title 
				"Value", // domain axis label 
				"Probability", // range axis label
				dataset, // data 
				PlotOrientation.VERTICAL, // orientation
				false, // include legend 
				true, // tooltips
				false); // URLs

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		BarRenderer renderer = (BarRenderer) plot.getRenderer(); 
		renderer.setToolTipGenerator(new CategoryToolTipGenerator()
		{ @Override
		public String generateToolTip(CategoryDataset data, int series, int category) {
			return "P("+variableName + "=" + data.getColumnKeys().get(category) + ") = "
					+ data.getValue(series, category); 
		} });

		renderer.setBarPainter(new StandardBarPainter());
		renderer.setDrawBarOutline(false);
		renderer.setSeriesPaint(0, new Color(5,100,30)); 

		return new ChartPanel(chart, true, true, true, true, true); 

	}

	
	private ChartPanel generatePanel(ContinuousDistribution distrib) throws DialException {

		final String variableName = distrib.getHeadVariables().toString().replace("[", "").replace("]", "");

		List<Series> series = new ArrayList<Series>();
		for (int i = 0 ; i < distrib.getFunction().getDimensionality() ; i++) {
			series.add(new Series("dimension " + i));
		}
		
		Map<Double[],Double> points = distrib.getFunction().discretise(500);
		for (Double[] point : points.keySet()) {
			for (int k = 0 ; k < point.length ; k++) {
				series.get(k).add(point[k].doubleValue(), distrib.getFunction().getDensity(point));
			}
		}

		CombinedDomainXYPlot combined = new CombinedDomainXYPlot(new NumberAxis("Value"));
		for (Series serie : series) {
			serie.smoothen();
			
			JFreeChart chart = ChartFactory.createXYLineChart("", // chart title 
					"Value", // domain axis label 
					"Density", // range axis label
					new XYSeriesCollection(serie), // data 
					PlotOrientation.VERTICAL, // orientation
					(distrib.getFunction().getDimensionality() > 1), // include legend 
					true, // tooltips? 
					false); // URLs?

			XYPlot plot = (XYPlot) chart.getPlot();
			combined.add(plot);
			plot.setBackgroundPaint(Color.white); plot.setRangeGridlinePaint(Color.white);			
		}		
		return new ChartPanel(new JFreeChart("Probability distribution P(" + variableName + ")", JFreeChart.DEFAULT_TITLE_FONT, combined, true), false); 
	}

	

	public void update(IndependentProbDistribution distrib) {
		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(new JLabel("        "), BorderLayout.NORTH);
		container.add(new JLabel("        "), BorderLayout.WEST);
		container.add(new JLabel("        "), BorderLayout.EAST);
		container.add(new JLabel("        "), BorderLayout.SOUTH);

		try {
			if (distrib.getPreferredType() == DistribType.CONTINUOUS) {
				container.add(generatePanel(distrib.toContinuous()), BorderLayout.CENTER);
			}
			else {
				container.add(generatePanel(distrib.toDiscrete()), BorderLayout.CENTER);
			}
		}
		catch (DialException e) {
			log.warning("could not generate distribution viewer: " + e);
		}
		setContentPane(container);
		if (getSize().height == 0 || getSize().width == 0) {
			pack();
			setLocation(new Random().nextInt(500), (new Random()).nextInt(500));
			setVisible(true);
		}
		else {
			validate();
		}
	}

	
	
	class Series extends XYSeries {
		
		static final int WINDOW = 10;
		
		public Series(String key) {
			super(key);
		}
			
		public void smoothen() {
			List<XYDataItem> newList = new ArrayList<XYDataItem>();
			for (int i = 0 ; i < data.size() ; i++) {
				double newProb = 0.0;
				double totalWeight = 0.0;
				for (int j = Math.max(0, i-WINDOW) ; j < Math.min(data.size(), i+WINDOW); j++) {
					double weight = 1.0 / (Math.abs(i-j)+1);
					newProb += weight * getDataItem(j).getYValue();
					totalWeight += weight;
				}
				newProb = newProb / totalWeight;
				newList.add(new XYDataItem(getDataItem(i).getXValue(), newProb));
			}
			data = newList;
		}
	}



}
