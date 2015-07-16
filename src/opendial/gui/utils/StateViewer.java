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

package opendial.gui.utils;

import java.util.logging.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.domains.rules.distribs.AnchoredRule;
import opendial.gui.StateMonitorTab;
import opendial.utils.StringUtils;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * Graph rendering component for the Bayesian Network. The component is based on the
 * JUNG library for easy layout of the graphs.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
@SuppressWarnings("serial")
public class StateViewer extends VisualizationViewer<String, Integer> {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// connection to the top tab including the graph viewer
	// (necessary to write information to the logging area)
	StateMonitorTab tab;

	// the current state to display
	DialogueState currentState;

	// whether the viewer is currently being updated
	volatile boolean isUpdating = false;

	// shown distribution charts
	Map<String, DistributionViewer> shownDistribs;

	/**
	 * Creates a new graph viewer, connected to the component given as argument. The
	 * viewer initially displays an empty graph.
	 * 
	 * @param tab the state viewer component
	 */
	public StateViewer(StateMonitorTab tab) {
		super(new StaticLayout<String, Integer>(
				new DelegateForest<String, Integer>()));
		this.tab = tab;

		// scaling it by 60%
		final ScalingControl scaler = new CrossoverScalingControl();
		scaler.scale(this, 0.7f, getCenter());

		// setting various renderers and element transformers
		setBackground(Color.white);
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
		getRenderContext()
				.setVertexShapeTransformer(new CustomVertexShapeRenderer());
		getRenderContext()
				.setVertexFillPaintTransformer(new CustomVertexColourRenderer());
		getRenderContext().setVertexLabelRenderer(new CustomVertexLabelRenderer());
		getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		setVertexToolTipTransformer(new CustomToolTipTransformer());
		ToolTipManager.sharedInstance().setDismissDelay(1000000000);

		// connects the graph to a custom mouse listener (for selecting nodes)
		DefaultModalGraphMouse<String, Integer> graphMouse =
				new DefaultModalGraphMouse<String, Integer>();
		graphMouse.setMode(Mode.PICKING);

		graphMouse.add(new PopupHandler(this));
		setGraphMouse(graphMouse);

		shownDistribs = new HashMap<String, DistributionViewer>();
	}

	/**
	 * Creates a new DAG-based graph layout for the given Bayesian Network. The nodes
	 * are identified by a string label, and the edges by a number.
	 * 
	 * @param bn the Bayesian network
	 * @return the generated layout
	 */
	private Layout<String, Integer> getGraphLayout(DialogueState ds,
			boolean showParameters) {
		Forest<String, Integer> f = new DelegateForest<String, Integer>();

		// adding the nodes and edges
		int counter = 0;
		try {
			for (BNode node : new ArrayList<BNode>(ds.getNodes())) {
				if (showParameters || !ds.getParameterIds().contains(node.getId())) {
					String nodeName = getVerticeId(node);

					f.addVertex(nodeName);
					for (BNode inputNode : new ArrayList<BNode>(
							node.getInputNodes())) {
						if (ds.getNode(inputNode.getId()) != null) {
							String inputNodeName = getVerticeId(inputNode);
							f.addEdge(counter, inputNodeName, nodeName);
							counter++;
						}
					}
				}
			}

			CustomLayoutTransformer transformer = new CustomLayoutTransformer(ds);
			StaticLayout<String, Integer> layout =
					new StaticLayout<String, Integer>(f, transformer);

			layout.setSize(new Dimension(600, 600));

			return layout;
		}
		catch (ConcurrentModificationException | NullPointerException e) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e1) {
			}
			return getGraphLayout(ds, showParameters);
		}
	}

	/**
	 * Returns the graph identifier associated with the node
	 * 
	 * @param node the node
	 * @return the corresponding graph identifier
	 */
	private static String getVerticeId(BNode node) {
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
	 * Returns the node associated with the graph identifier (inverse operation of
	 * getGraphId)
	 * 
	 * @param verticeID the vertice identifier
	 * @return the node in the Bayesian Network, if any
	 */
	protected BNode getBNode(String verticeID) {
		String nodeId = getBNodeId(verticeID);
		if (currentState != null && currentState.hasNode(nodeId)) {
			return currentState.getNode(nodeId);
		}
		// log.warning("node corresponding to " + verticeID + " not found");
		return null;
	}

	/**
	 * Returns the node associated with the graph identifier (inverse operation of
	 * getGraphId)
	 * 
	 * @param verticeID the vertice identifier
	 * @return the node in the Bayesian Network, if any
	 */
	protected String getBNodeId(String verticeID) {
		return verticeID.replace("util---", "").replace("action---", "");
	}

	/**
	 * Shows the given Bayesian network in the viewer
	 * 
	 * @param state the Bayesian Network to display
	 */
	public synchronized void showBayesianNetwork(DialogueState state) {
		currentState = state;
		if (!isUpdating) {
			new Thread(() -> {
				isUpdating = true;
				if (tab.getMainFrame().getSystem().isPaused()) {
					update();
				}
				else {
					synchronized (currentState) {
						update();
					}
				}
				isUpdating = false;
			}).start();
		}
	}

	/**
	 * Updates the viewer with the current state.
	 */
	private void update() {
		Layout<String, Integer> layout =
				getGraphLayout(currentState, tab.showParameters());
		setGraphLayout(layout);
		updateDistribs();
	}

	/**
	 * Quick fix for a strange bung in JUNG
	 */
	@Override
	public void paintComponent(Graphics g) {
		try {
			super.paintComponent(g);
		}
		catch (NullPointerException e) {
			log.fine("cannot repaint state viewer, waiting for next update: ");
			e.printStackTrace();
			isUpdating = false;
			// tab.trigger(currentState, currentState.getChanceNodeIds());
		}

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
		scaler.scale(this, 1.0f / 1.1f, getCenter());
	}

	/**
	 * Translates the graph using the given offset
	 * 
	 * @param horizontal horizontal offset
	 * @param vertical vertical offset
	 */
	public void translate(int horizontal, int vertical) {
		MutableTransformer modelTransformer = getRenderContext()
				.getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
		try {
			int dx = -vertical;
			int dy = horizontal;
			modelTransformer.translate(dy, dx);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
	}

	/**
	 * Wraps the graph viewer in a scroll panel, and returns it
	 * 
	 * @return the scroll panel wrapping the graph viewer
	 */
	public GraphZoomScrollPane wrapWithScrollPane() {
		return new GraphZoomScrollPane(this);
	}

	/**
	 * Returns the state viewer tab which contains the viewer
	 * 
	 * @return the state viewer tab
	 */
	public StateMonitorTab getStateMonitorTab() {
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

	/**
	 * Displays the probability distribution(s) for the selected variables.
	 * 
	 * @param queryVar the variable to display
	 */
	public void displayDistrib(String queryVar) {
		if (!shownDistribs.containsKey(queryVar)) {
			IndependentDistribution distrib = currentState.queryProb(queryVar);
			DistributionViewer viewer =
					new DistributionViewer(currentState, queryVar, this);
			shownDistribs.put(distrib.getVariable(), viewer);
		}
	}

	/**
	 * Updates the windows displaying probability distributions.
	 */
	public void updateDistribs() {
		for (String queryVar : shownDistribs.keySet()) {
			shownDistribs.get(queryVar).update(currentState);
		}
	}

	/**
	 * Returns a pointer to the dialogue system.
	 * 
	 * @return the dialogue system
	 */
	public DialogueSystem getSystem() {
		return getStateMonitorTab().getMainFrame().getSystem();
	}

	/**
	 * Tooltip transformer showing the pretty print information available in the
	 * original Bayesian node. The information is shown when the mouse cursor hovers
	 * over the node.
	 *
	 */
	final class CustomToolTipTransformer implements Transformer<String, String> {

		@Override
		public String transform(String nodeGraphId) {
			BNode node = getBNode(nodeGraphId);
			if (node != null) {
				String prettyPrintNode = node.toString();
				String htmlDistrib =
						"<html>&nbsp;&nbsp;"
								+ prettyPrintNode.replace("\n",
										"&nbsp;&nbsp;" + "<br>&nbsp;&nbsp;")
						+ "<br></html>";
				htmlDistrib = htmlDistrib.replace("if", "<b>if</b>")
						.replace("then",
								"<b>then</b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
						.replace("else",
								"<b>else</b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
						.replace(
								"<b>else</b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>if</b>",
								"<b>else if</b>");
				return StringUtils.getHtmlRendering(htmlDistrib);
			}
			else {
				return "";
			}
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
				BNode node = getBNode((String) arg4);
				if (node != null) {
					String str = StringUtils.getHtmlRendering(node.getId());
					if (currentState.getNodeIds(AnchoredRule.class)
							.contains(node.getId())) {
						str = "<font size=\"6\" color=\"gray\">" + str + "</font>";
					}
					JLabel jlabel = new JLabel("<html>" + str + "</html>");
					jlabel.setFont(new Font("Arial bold", Font.PLAIN, 24));
					return jlabel;
				}
			}
			return new JLabel();
		}
	}

	/**
	 * Renderer for the vertice colour
	 */
	final class CustomVertexColourRenderer implements Transformer<String, Paint> {

		@Override
		public Paint transform(String arg0) {
			BNode node = getBNode(arg0);
			boolean isPicked = getPickedVertexState().getPicked().contains(arg0);
			if (isPicked) {
				return new Color(255, 204, 0);
			}
			else if (node instanceof UtilityNode) {
				return new Color(0, 128, 108);
			}
			else if (node instanceof ActionNode) {
				return new Color(0, 100, 155);
			}
			else if (node != null
					&& getState().getEvidence().containsVar(node.getId())) {
				return Color.darkGray;
			}
			else {
				return new Color(179, 0, 45);
			}
		}

	}

	/**
	 * Renderer for the node shapes
	 *
	 */
	final class CustomVertexShapeRenderer implements Transformer<String, Shape> {

		@Override
		public Shape transform(String arg0) {
			BNode node = getBNode(arg0);
			if (node instanceof ChanceNode) {
				if (((ChanceNode) node).getDistrib() instanceof AnchoredRule) {
					return new Ellipse2D.Double(-5.0, -5.0, 20.0, 20.0);
				}
				else {
					return new Ellipse2D.Double(-15.0, -15.0, 30.0, 30.0);
				}
			}
			else if (node instanceof UtilityNode) {
				GeneralPath p0 = new GeneralPath();
				p0.moveTo(0.0f, -15);
				p0.lineTo(15, 0.0f);
				p0.lineTo(0.0f, 15);
				p0.lineTo(-15, 0.0f);
				p0.closePath();
				return p0;
			}
			else if (node instanceof ActionNode) {
				return new Rectangle2D.Double(-15.0, -15.0, 30.0, 30.0);
			}
			else {
				return new Ellipse2D.Double(-15.0, -15.0, 30.0, 30.0);
			}
		}
	}

	/**
	 * Custom layout manager for the state viewer.
	 */
	final class CustomLayoutTransformer implements Transformer<String, Point2D> {

		Map<String, Point2D> positions;

		public CustomLayoutTransformer(DialogueState network) {
			positions = new HashMap<String, Point2D>();
			Point current = new Point(0, 0);

			// trying to avoid nasty concurrent modifications
			List<String> allNodes = new ArrayList<String>();
			List<String> ruleNodes = new ArrayList<String>();
			for (int i = 0; i < 3; i++) {
				try {
					allNodes.addAll(network.getNodeIds());
					ruleNodes.addAll(network.getNodeIds(AnchoredRule.class));
					break;
				}
				catch (ConcurrentModificationException | NullPointerException e) {
					try {
						Thread.sleep(50);
					}
					catch (InterruptedException e1) {
					}
				}
			}
			for (String node : allNodes) {
				if (!node.contains("'") && !node.contains("=")
						&& !ruleNodes.contains(node)) {
					positions.put(node, current);
					current = incrementPoint(current);
				}
			}
			current = new Point(current.x + 200, 0);
			for (String node : ruleNodes) {
				positions.put(node, current);
				current = incrementPoint(current);
			}

			current = new Point(current.x + 200, 0);
			for (String node : allNodes) {
				if (!positions.containsKey(node)) {
					positions.put(node, current);
					current = incrementPoint(current);
				}
			}
		}

		private Point incrementPoint(Point curPoint) {
			if (curPoint.y < 500) {
				return new Point(curPoint.x, curPoint.y + 150);
			}
			else {
				return new Point(curPoint.x + 150, 0);
			}
		}

		@Override
		public Point2D transform(String id) {
			String id2 = getBNodeId(id);
			return positions.get(id2);
		}

	}

}
