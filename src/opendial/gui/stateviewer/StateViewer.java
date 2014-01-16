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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.gui.StateViewerTab;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.state.DialogueState;
import opendial.state.nodes.ProbabilityRuleNode;
import opendial.utils.StringUtils;


/**
 * Graph rendering component for the Bayesian Network.  The component is based on
 * the JUNG library for easy layout of the graphs.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class StateViewer extends VisualizationViewer<String,Integer> {

	// logger
	public static Logger log = new Logger("StateViewer", Logger.Level.DEBUG);

	// connection to the top tab including the graph viewer
	// (necessary to write information to the logging area)
	StateViewerTab tab;

	// the current state to display
	DialogueState currentState;

	// whether the viewer is currently being updated
	boolean isUpdating = false;
	
	// shown distribution charts
	Map<Collection<String>, DistributionViewer> shownDistribs;
	

	/**
	 * Creates a new graph viewer, connected to the component given as
	 * argument.  The viewer initially displays an empty graph.
	 * 
	 * @param tab the state viewer component
	 */
	public StateViewer(StateViewerTab tab) {
		super(getGraphLayout(new DialogueState(), tab.showParameters())); 
		this.tab = tab;

		// scaling it by 60%
		final ScalingControl scaler = new CrossoverScalingControl();
		scaler.scale(this, 0.7f, getCenter()); 

		// setting various renderers and element transformers
		setBackground(Color.white);
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
		getRenderContext().setVertexShapeTransformer(new CustomVertexShapeRenderer());
		getRenderContext().setVertexFillPaintTransformer(new CustomVertexColourRenderer());
		getRenderContext().setVertexLabelRenderer(new CustomVertexLabelRenderer());
		getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		setVertexToolTipTransformer(new CustomToolTipTransformer());


		// connects the graph to a custom mouse listener (for selecting nodes)
		DefaultModalGraphMouse<String,Integer> graphMouse = new DefaultModalGraphMouse<String,Integer>();
		graphMouse.setMode(Mode.PICKING);
		graphMouse.add(new PopupHandler(this));
		setGraphMouse(graphMouse);
		
		shownDistribs = new HashMap<Collection<String>,DistributionViewer>();

	}


	/**
	 * Creates a new DAG-based graph layout for the given Bayesian Network.
	 * The nodes are identified by a string label, and the edges by a number.
	 * 
	 * @param bn the Bayesian network
	 * @return the generated layout
	 */
	private static Layout<String,Integer> getGraphLayout(DialogueState ds, boolean showParameters) {
		Forest<String, Integer> f = new DelegateForest<String,Integer>();

		// adding the nodes and edges
		int counter = 0;
		try {
		for (BNode node: new ArrayList<BNode>(ds.getNodes())) {
			if (showParameters || !ds.getParameterIds().contains(node.getId())) {
			String nodeName = getVerticeId(node);

			f.addVertex(nodeName);
			for (BNode inputNode : new ArrayList<BNode>(node.getInputNodes())) {
				if (ds.getNode(inputNode.getId()) != null) {
					String inputNodeName =  getVerticeId(inputNode);
					f.addEdge(counter, inputNodeName, nodeName);
					counter++;
				}
			}
			}
		}
		}
		catch (ConcurrentModificationException e) {
			return getGraphLayout(ds, showParameters);
		}
		
		KKLayout<String,Integer> layout  = new KKLayout<String,Integer>(f); 
		layout.setLengthFactor(1.5);
		layout.setMaxIterations(100);

		layout.setSize(new Dimension(600,600));

		return layout;
	}


	/**
	 * Returns the graph identifier associated with the node
	 * 
	 * @param node the node
	 * @return the corresponding graph identifier
	 */
	private static String getVerticeId (BNode node) {
		String nodeName = node.getId();
		if (node instanceof UtilityNode) {
			nodeName = "util---" + node.getId();
		}
		else if (node instanceof ActionNode) {
			nodeName = "action---" + node.getId();
		}
		return nodeName;
	}


	/**
	 * Returns the node associated with the graph identifier
	 * (inverse operation of getGraphId)
	 * 
	 * @param verticeID the vertice identifier
	 * @return the node in the Bayesian Network, if any
	 */
	private BNode getBNode(String verticeID) {
		String nodeId = verticeID.replace("util---", "").replace("action---", "");
		if (currentState != null && currentState.hasNode(nodeId)) {
			return currentState.getNode(nodeId);
		}
		//		log.warning("node corresponding to " + verticeID + " not found");
		return null;
	}





	/**
	 * Shows the given Bayesian network in the viewer
	 * 
	 * @param state the Bayesian Network to display
	 */
	public void showBayesianNetwork(DialogueState state) {
		currentState = state;	
		if (!isUpdating) {
			new Thread(new Runnable() { 
				public void run() { 
					isUpdating = true;
					synchronized (currentState) {
						Layout<String,Integer> layout = getGraphLayout(currentState, tab.showParameters());
						setGraphLayout(layout);
						updateDistributions();
						isUpdating = false;
					}
				}
			}).start();
		}
		repaint();
	} 


	/**
	 * Zoom in on the graph by a factor 1.1
	 * 
	 */
	public void zoomIn() {
		final ScalingControl scaler = new CrossoverScalingControl();
		scaler.scale(this, 1.1f, getCenter());
	}

	/**
	 * Zoom out on the graph by a factor 1/1.1
	 */
	public void zoomOut() {
		final ScalingControl scaler = new CrossoverScalingControl();
		scaler.scale(this, 1.0f/1.1f, getCenter());
	}

	/**
	 * Translates the graph using the given offset
	 * 
	 * @param horizontal horizontal offset
	 * @param vertical vertical offset
	 */
	public void translate(int horizontal, int vertical) {
		MutableTransformer modelTransformer =
				getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
		try {       
			int dx = -vertical;
			int dy = horizontal;
			modelTransformer.translate(dy, dx);
		} catch(RuntimeException ex) {
			throw ex;
		}
	}

	/**
	 * Wraps the graph viewer in a scroll panel, and returns it
	 * 
	 * @return the scroll panel wrapping the graph viewer
	 */
	public GraphZoomScrollPane wrapWithScrollPane () {
		return new GraphZoomScrollPane (this);
	}


	/**
	 * Returns the state viewer tab which contains the viewer
	 * 
	 * @return the state viewer tab
	 */
	public StateViewerTab getStateMonitorTab() {
		return tab;
	}



	/**
	 * Returns the Bayesian network currently displayed in the viewer
	 * 
	 * @return the Bayesian Network
	 */
	public DialogueState getState() {
		return currentState;
	}
	
	
	public void showDistribution(Collection<String> queryVars) {
		if (!shownDistribs.containsKey(queryVars)) {
			IndependentProbDistribution distrib = currentState.queryProb(queryVars);
			DistributionViewer viewer = new DistributionViewer(distrib, this);
			shownDistribs.put(distrib.getHeadVariables(), viewer);
		}
	}
	
	public void updateDistributions() {
		for (Collection<String> queryVars : shownDistribs.keySet()) {
			IndependentProbDistribution distrib = currentState.queryProb(queryVars);
			shownDistribs.get(queryVars).update(distrib);
		}
	}



	/**
	 * Tooltip transformer showing the pretty print information available in
	 * the original Bayesian node.  The information is shown when the mouse
	 * cursor hovers over the node.
	 *
	 */
	final class CustomToolTipTransformer implements Transformer<String,String> {


		@Override
		public String transform(String nodeGraphId) {
			String nodeId2 = getBNode(nodeGraphId).getId();
			String prettyPrintNode = getBNode(nodeId2).toString();
			String htmlDistrib = "<html><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + 
					prettyPrintNode.replace("\n", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
							+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "<br></html>";
			htmlDistrib = htmlDistrib.replace("if", "<b>if</b>").replace("then", "<b>then</b>");
			return StringUtils.getHtmlRendering(htmlDistrib);
		}

	}

	/**
	 * Renderer for the node labels
	 *
	 */
	final class CustomVertexLabelRenderer implements VertexLabelRenderer {


		@Override
		public <T> Component getVertexLabelRendererComponent(JComponent arg0,
				Object arg1, Font arg2, boolean arg3, T arg4) {
			if (arg4 instanceof String) {
				BNode node = getBNode((String)arg4);
				if (node!=null) {
					JLabel jlabel = new JLabel("<html>"+StringUtils.getHtmlRendering(node.getId())+"</html>");
					jlabel.setFont(new Font("Arial bold", Font.PLAIN, 24));
					return jlabel;
				}
			} 		
			return new JLabel();
		}
	}

	final class CustomVertexColourRenderer implements Transformer<String,Paint> {


		@Override
		public Paint transform(String arg0) {
			BNode node = getBNode(arg0);
			boolean isPicked = getPickedVertexState().getPicked().contains(arg0);
			if (isPicked) {
				return new Color(255,204,0);
			}
			else if (node instanceof UtilityNode) {
				return new Color(0,128,108);
			}
			else if (node instanceof ActionNode) {
				return new Color(0,100,155);
			}
			else if (node != null && getState().getEvidence().containsVar(node.getId())) {
				return Color.darkGray;
			}
			else {
				return new Color(179,0,45);
			}
		}

	}

	/**
	 * Renderer for the node shapes
	 *
	 */
	final class CustomVertexShapeRenderer implements Transformer<String, Shape> {

		public Shape transform(String arg0) {
			BNode node = getBNode(arg0);
			if (node instanceof ProbabilityRuleNode) {
				return (Shape) new Ellipse2D.Double(-5.0,-5.0,20.0,20.0);
			}
			else if (node instanceof ChanceNode) {
				return (Shape) new Ellipse2D.Double(-15.0,-15.0,30.0,30.0);
			}
			else if (node instanceof UtilityNode) {
				GeneralPath p0 = new GeneralPath();
				p0.moveTo(0.0f, -20);
				p0.lineTo(20, 0.0f);
				p0.lineTo(0.0f, 20);
				p0.lineTo(-20, 0.0f);
				p0.closePath();
				return (Shape) p0;
			}
			else if (node instanceof ActionNode) {
				return (Shape) new Rectangle2D.Double(-15.0,-15.0,30.0,30.0);
			}
			else {
				return (Shape) new Ellipse2D.Double(-15.0,-15.0,30.0,30.0);
			}
		}
	}



}
