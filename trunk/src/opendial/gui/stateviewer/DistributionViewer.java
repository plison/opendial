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

package opendial.gui.stateviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JLabel;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.densityfunctions.DensityFunction;
import opendial.bn.distribs.densityfunctions.KernelDensityFunction;
import opendial.bn.values.Value;
import opendial.state.DialogueState;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * GUI window displaying a (discrete or continuous) distribution as a chart. The  graphical
 * layout of the chart is based on JFreeChart.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
@SuppressWarnings({ "deprecation", "serial" })
public class DistributionViewer extends JDialog {

	// logger
	public static Logger log = new Logger("DistributionViewer", Logger.Level.DEBUG);

	String queryVar;
	IndependentProbDistribution lastDistrib;
	
	/**
	 * Constructs a new viewer for the given distribution, connected to the state viewer component.
	 * 
	 * @param currentState the current dialogue state
	 * @param queryVar the variable to display
	 * @param viewer the state viewer component
	 */
	public DistributionViewer(final DialogueState currentState, final String queryVar, final StateViewer viewer) {
		super(viewer.tab.getMainFrame().getFrame(),Dialog.ModalityType.MODELESS);
		setTitle("Distribution Viewer");
		this.queryVar = queryVar;
		update(currentState);

		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				viewer.shownDistribs.remove(queryVar);
			}

		});
	}



	/**
	 * Constructs or update the current viewer with the distribution.
	 * 
	 * @param currentState the updated dialogue state
	 */
	protected void update(DialogueState currentState) {

		if (!currentState.hasChanceNode(queryVar)) {
			return;
		}
		else if (lastDistrib != null && this.lastDistrib.equals(currentState.getChanceNode(queryVar).getDistrib())) {
			return;
		}
		this.lastDistrib = currentState.queryProb(queryVar);
		
		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(new JLabel("        "), BorderLayout.NORTH);
		container.add(new JLabel("        "), BorderLayout.WEST);
		container.add(new JLabel("        "), BorderLayout.EAST);
		container.add(new JLabel("        "), BorderLayout.SOUTH);

		try {
			IndependentProbDistribution indepDistrib = currentState.queryProb(queryVar);
			if (indepDistrib instanceof ContinuousDistribution) {
				container.add(generatePanel(indepDistrib.toContinuous()), BorderLayout.CENTER);				
			}
			else {
				container.add(generatePanel(indepDistrib.toDiscrete()), BorderLayout.CENTER);
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


	/**
	 * Generates a chart panel for the categorical table.
	 * 
	 * @param distrib the categorical table
	 * @return the constructed chart panel
	 */
	private ChartPanel generatePanel(CategoricalTable distrib) {
		final String variableName = distrib.getVariable();
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset(); 

		for (Value val : distrib.getValues()) {
			dataset.addValue(distrib.getProb(val), "", val.toString());
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
		renderer.setToolTipGenerator((d,s,c) -> {
			return "P("+variableName + "=" + d.getColumnKeys().get(c) + ") = "+ d.getValue(s, c); 
		});

		renderer.setBarPainter(new StandardBarPainter());
		renderer.setDrawBarOutline(false);
		renderer.setSeriesPaint(0, new Color(5,100,30)); 

		return new ChartPanel(chart, true, true, true, true, true); 

	}


	/**
	 * Constructs a chart panel for the continuous distribution.
	 * 
	 * @param distrib the continuous distribution
	 * @return the generated chart panel
	 * @throws DialException if the distribution could not be displayed
	 */
	private ChartPanel generatePanel(ContinuousDistribution distrib) throws DialException {

		final String variableName = distrib.getVariable();
		
		List<Series> series = extractSeries(distrib.getFunction());

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
		
		return new ChartPanel(new JFreeChart("Probability distribution P(" + variableName + ")", 
				JFreeChart.DEFAULT_TITLE_FONT, combined, true), false); 
	}
	
	
	
	private List<Series >extractSeries(DensityFunction function) throws DialException {

		List<Series> series =new ArrayList<Series>();

		for (int i = 0 ; i < function.getDimensionality() ; i++) {
			series.add(new Series("dimension " + i));
		}
		
		if (function instanceof KernelDensityFunction) {
			((KernelDensityFunction)function).multiplyBandwidth(5);
			for (int i = 0 ; i < 200 ; i++) {
				Double[] point1 = function.sample();
				double density1 = function.getDensity(point1);
				Double[] point2 = function.sample();
				double density2 = function.getDensity(point2);
				Double[] midrange = new Double[point1.length];
				for (int d = 0 ; d < point1.length ; d++) {
					midrange[d] = (point1[d] + point2[d])/2.0;
				}
				double density_mid = function.getDensity(midrange);
				for (int d = 0 ; d < point1.length ; d++) {
					series.get(d).add(point1[d].doubleValue(), density1);		
					series.get(d).add(point2[d].doubleValue(), density2);		
					series.get(d).add(midrange[d].doubleValue(), density_mid);		
				}
			}
			((KernelDensityFunction)function).multiplyBandwidth(0.2);
		}
		else {
			Set<Double[]> points = function.discretise(200).keySet();
			for (Double[] point : points) {
				double density = function.getDensity(point);
				for (int d = 0 ; d < point.length ; d++) {
					series.get(d).add(point[d].doubleValue(), density);
				}
			}
		}
		

		return series;
	}



	/**
	 * Series of "smoothed" XYDataItem for continuous distributions.
	 */
	class Series extends XYSeries {

		static final int WINDOW = 2;

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
