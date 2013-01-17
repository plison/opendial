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
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.NodeIdChangeListener;
import opendial.bn.nodes.UtilityNode;
import opendial.modules.SynchronousModule;
import opendial.utils.StringUtils;


/**
 * Graph rendering component for the Bayesian Network.  The component is based on
 * the JUNG library for easy layout of the graphs.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
@SuppressWarnings("serial")
public class DialogueStateViewer extends VisualizationViewer<String,Integer> implements NodeIdChangeListener {

	// logger
	public static Logger log = new Logger("DialogueStateViewer", Logger.Level.DEBUG);

	// connection to the top tab including the graph viewer
	// (necessary to write information to the logging area)
	StateMonitorTab tab;
	
	// the current state to display
	DialogueState currentState;
	
	Map<String,String> nodeIdChanges;


	/**
	 * Creates a new graph viewer, connected to the component given as
	 * argument.  The viewer initially displays an empty graph.
	 * 
	 * @param tab the state viewer component
	 */
	public DialogueStateViewer(StateMonitorTab tab) {
		super(getGraphLayout(new DialogueState())); 
	
		this.tab = tab;
/**
		// rotating the graph by 90 degrees
		MutableTransformer modelTransformer =
			getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
		modelTransformer.rotate(Math.PI / 2.0, 
				getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, new Point(400, 200))); */

		// scaling it by 60%
		final ScalingControl scaler = new CrossoverScalingControl();
	    scaler.scale(this, 0.6f, getCenter()); 

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
		graphMouse.add(new DialogueStatePopup(this));
		setGraphMouse(graphMouse);
		
		nodeIdChanges = new HashMap<String,String>();
	}


	/**
	 * Creates a new DAG-based graph layout for the given Bayesian Network.
	 * The nodes are identified by a string label, and the edges by a number.
	 * 
	 * @param bn the Bayesian network
	 * @return the generated layout
	 */
	private static Layout<String,Integer> getGraphLayout(DialogueState ds) {
		Forest<String, Integer> f = new DelegateForest<String,Integer>();

		BNetwork bn = ds.getNetwork();
		// adding the nodes and edges
		int counter = 0;
		for (BNode node: bn.getNodes()) {
			String nodeName = getVerticeId(node);
			f.addVertex(nodeName);
			for (BNode inputNode : node.getInputNodes()) {
				if (bn.getNode(inputNode.getId()) != null) {
					String inputNodeName =  getVerticeId(inputNode);
					f.addEdge(counter, inputNodeName, nodeName);
					counter++;
				}
			}
		}

		Layout<String,Integer> layout = null;

		// creating the DAG layout
		layout = new SpringLayout2<String,Integer>(f); 
		((SpringLayout2<String,Integer>)layout).setStretch(0.1);
		((SpringLayout2<String,Integer>)layout).setRepulsionRange(100);
		
		layout.setSize(new Dimension(500,500));
		
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
	BNode getBNode(String verticeID) {
		String nodeId = verticeID.replace("util---", "").replace("action---", "");
		if (currentState.getNetwork().hasNode(nodeId)) {
			return currentState.getNetwork().getNode(nodeId);
		}
		else if (nodeIdChanges.containsKey(nodeId)){
			return getBNode(nodeIdChanges.get(nodeId));
		}
		
		log.warning("node corresponding to " + verticeID + " not found");
		return null;
	}





	/**
	 * Shows the given Bayesian network in the viewer
	 * 
	 * @param state the Bayesian Network to display
	 */
	public void showBayesianNetwork(DialogueState state) {
		currentState = state;	
		for (BNode n : state.getNetwork().getNodes()) {
			n.addNodeIdChangeListener(this);
		}
		Layout<String,Integer> layout = getGraphLayout(state);
		setGraphLayout(layout);
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
        	int dy = -horizontal;
            modelTransformer.translate(dx, dy);
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
	public StateMonitorTab getStateMonitorTab() {
		return tab;
	}

	

	/**
	 * Returns the Bayesian network currently displayed in the viewer
	 * 
	 * @return the Bayesian Network
	 */
	public DialogueState getDialogueState() {
		return currentState;
	}
	
	
	public void modifyNodeId(String oldNodeId, String newNodeId) {
		nodeIdChanges.put(oldNodeId, newNodeId);
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
			String prettyPrintNode = getBNode(nodeId2).prettyPrint();
			String htmlDistrib = "<html><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + 
			prettyPrintNode.replace("\n", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "<br></html>";
			htmlDistrib = htmlDistrib.replace("P(", "<b>P(</b>").replace("):=", "<b>):=</b>");
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
				JLabel jlabel = new JLabel("<html>"+StringUtils.getHtmlRendering(node.getId())+"</html>");
				jlabel.setFont(new Font("Arial bold", Font.PLAIN, 24));
				return jlabel;
			} 		
			return null;
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
			else if (getDialogueState().getEvidence().containsVar(node.getId())) {
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
			if (node instanceof ChanceNode) {
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
				log.warning("unknown case");
				return null;
			}
		}
	}

	
		
}
