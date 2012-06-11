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

package opendial.gui.stateviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
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
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ValueNode;
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
public class GraphViewer extends VisualizationViewer<String,Integer>  {

	// logger
	public static Logger log = new Logger("BNetworkViewer", Logger.Level.DEBUG);

	// connection to the top component including the graph viewer
	// (necessary to write information to the logging area)
	StateViewerComponent topComponent;
	
	// the current network to display
	BNetwork currentNetwork;


	/**
	 * Creates a new graph viewer, connected to the component given as
	 * argument.  The viewer initially displays an empty graph.
	 * 
	 * @param topComponent the state viewer component
	 */
	public GraphViewer(StateViewerComponent topComponent) {
		super(getGraphLayout(new BNetwork())); 
	
		this.topComponent = topComponent;

		// rotating the graph by 90 degrees
		MutableTransformer modelTransformer =
			getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
		modelTransformer.rotate(Math.PI / 2.0, 
				getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, new Point(120,180)));

		// scaling it by 60%
		final ScalingControl scaler = new CrossoverScalingControl();
	    scaler.scale(this, 0.6f, getCenter());

	    // setting various renderers and element transformers
		setBackground(Color.white);
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
		getRenderContext().setVertexShapeTransformer(new CustomVertexShapeRenderer());
		getRenderContext().setVertexLabelRenderer(new CustomVertexLabelRenderer());
		getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		setVertexToolTipTransformer(new CustomToolTipTransformer());
		

		// connects the graph to a custom mouse listener (for selecting nodes)
		DefaultModalGraphMouse<String,Integer> graphMouse = new DefaultModalGraphMouse<String,Integer>();
		graphMouse.setMode(Mode.PICKING);
		graphMouse.add(new CustomPopupGraphMousePlugin());
		setGraphMouse(graphMouse);
	}


	/**
	 * Creates a new DAG-based graph layout for the given Bayesian Network.
	 * The nodes are identified by a string label, and the edges by a number.
	 * 
	 * @param bn the Bayesian network
	 * @return the generated layout
	 */
	private static Layout<String,Integer> getGraphLayout(BNetwork bn) {
		Forest<String, Integer> f = new DelegateForest<String,Integer>();

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
		layout = new DAGLayout<String,Integer>(f); 
		((DAGLayout<String,Integer>)layout).setStretch(10);

		layout.setSize(new Dimension(400,400));

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
		if (node instanceof ValueNode) {
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
	 * @param graphNodeId the graph identifier
	 * @return the node in the Bayesian Network, if any
	 */
	private BNode getBNode(String graphNodeId) {
		String nodeId = graphNodeId.replace("util---", "").replace("action---", "");
		if (currentNetwork.hasNode(nodeId)) {
			return currentNetwork.getNode(nodeId);
		}
		log.warning("node corresponding to " + graphNodeId + " not found");
		return null;
	}




	/**
	 * Shows the given Bayesian network in the viewer
	 * 
	 * @param network the Bayesian Network to display
	 */
	public void showBayesianNetwork(BNetwork network) {
		currentNetwork = network;
		Layout<String,Integer> layout = getGraphLayout(network);
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
	
	
	private GraphViewer getViewer() { return this;  }



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
			else if (node instanceof ValueNode) {
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


	/**
	 * Mouse plugin to handle the right-click popup after selecting nodes
	 * 
	 * TODO: add use of evidence and value distribution
	 */
	protected class CustomPopupGraphMousePlugin extends AbstractPopupGraphMousePlugin 
	implements MouseListener {

	    public CustomPopupGraphMousePlugin() {
	        this(MouseEvent.BUTTON3_MASK);
	    }
	    public CustomPopupGraphMousePlugin(int modifiers) {
	        super(modifiers);
	    }

	    /**
	     * Creates the popup with relevant action items
	     *
	     * @param e the mouse event
	     */
	    protected void handlePopup(MouseEvent e) {
	    	super.mouseClicked(e);			
			Set<String> pickedVertices = pickedVertexState.getPicked();
			JPopupMenu popup = new JPopupMenu();
			if (suitedForInference(pickedVertices)) {
				popup.add(new InferenceAction("Compute probability distribution", 
						pickedVertices, new Assignment()));
			}
			if (popup.getComponentCount() == 0 && !pickedVertices.isEmpty()) {
				popup.add(new JLabel("  No action available for the selected node(s)  "));
			}
			if (popup.getComponentCount() > 0) {
				popup.show(getViewer(), e.getX(), e.getY());
			}
	    }
	    
	    /**
	     * Determines whether the inference action is appropriate in this setting
	     * (selection must consist of chance nodes, of size between 1 and 3.
	     * 
	     * @param graphIds the identifier for the vertices
	     * @return whether the inference action is appropriate
	     */
	    private boolean suitedForInference(Set<String> graphIds) {
	    	if (graphIds.size() == 0 || graphIds.size() > 4) {
	    		return false;
	    	}
	    	for (String graphId : graphIds) {
	    		BNode node = getBNode(graphId);
	    		if (!(node instanceof ChanceNode)) {
	    			return false;
	    		}
	    	}
	    	return true;
	    }
	}
	
	
	/**
	 * Representation of an inference action triggered by the user via the popup
	 * menu.  An inference action is defined via a set of query variables, plus some
	 * optional evidence.
	 * 
	 *
	 */
	final class InferenceAction extends AbstractAction {

		Set<String> queryVariables;
		Assignment evidence;
		
		public InferenceAction(String name, Set<String> queryVariables, Assignment evidence) {
			super(name);
			this.queryVariables = queryVariables;
			this.evidence = evidence;
		}
		/**
		 *
		 * @param arg0
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			topComponent.writeToLogArea("computing " + StringUtils.getProbabilityString(queryVariables, evidence));
		}
		
	}

						
		//    Distribution distrib = VariableElimination.query(network, vertex.replace("util-", ""));
		//   String str = StringUtils.getHtmlRendering(distrib.toString().replace("\n", "\n<br>"));
		//   logArea.setText("<html><font face=\"helvetica\">"+ str + "</font></html>");
				
}
