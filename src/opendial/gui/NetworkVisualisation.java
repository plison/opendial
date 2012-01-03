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

package opendial.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.border.Border;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.utils.Logger;

/**
 * 
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NetworkVisualisation {

	static Logger log = new Logger("NetworkVisualisation", Logger.Level.DEBUG);
	
	public static boolean showOnce = true;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void showBayesianNetwork (BNetwork bn) {
		// Graph<V, E> where V is the type of the vertices // and E is the type of the edges 
		Forest<String, Integer> f = new DelegateForest();
		

		int counter = 0;
		for (BNode node: bn.getNodes()) {
			f.addVertex(node.getId());
			for (BNode inputNode : node.getInputNodes()) {
				if (bn.getNode(inputNode.getId()) != null) {
				f.addEdge(counter, inputNode.getId(), node.getId());
				counter++;
				}
			}
		}
		
		
		DAGLayout<String, Integer> layout = new DAGLayout<String,Integer>(f); 

		layout.setStretch(1);
		layout.setRepulsionRange(30);
		layout.setForceMultiplier(1);
		// sets the initial size of the space
		layout.setSize(new Dimension(300,300));
	//	layout.done();
		// The BasicVisualizationServer<V,E> is parameterized by the edge types 
		VisualizationViewer<String,Integer> vv =
		new VisualizationViewer<String,Integer>(layout, new Dimension(700,500)); 
		MutableTransformer modelTransformer =
            vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
	    modelTransformer.rotate(1 * Math.PI / 2.0, 
       		vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, new Point(100,200)));

        vv.setBackground(Color.white);

		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.setVertexToolTipTransformer(new TooltipTransformer(bn));

		//Sets the viewing area size
		JFrame frame = new JFrame("Bayesian Network visualisation"); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		Container contentPane = frame.getContentPane();
		
		contentPane.add(Box.createRigidArea(new Dimension(20,20)), BorderLayout.NORTH);
		contentPane.add(Box.createRigidArea(new Dimension(20,20)), BorderLayout.WEST);
		contentPane.add(vv, BorderLayout.CENTER); 
		contentPane.add(Box.createRigidArea(new Dimension(20,20)), BorderLayout.EAST);
		contentPane.add(Box.createRigidArea(new Dimension(20,20)), BorderLayout.SOUTH);
		frame.setLocation(new Point(400, 400));
		frame.pack();
	//	if (showOnce) {
			frame.setVisible(true);
	//		showOnce = false;
	//	}
	}
}


final class TooltipTransformer implements Transformer<String,String> {

	BNetwork bn ;
	
	public TooltipTransformer(BNetwork bn) {
		this.bn =bn;
	}
	
	/**
	 * Show the probability distribution in the tooltip
	 * 
	 * @param nodeId the node identifier
	 * @return
	 */
	@Override
	public String transform(String nodeId) {
		String distribStr = bn.getNode(nodeId).getDistribution().toString();
		String htmlDistrib = "<html><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + 
		distribStr.replace("\n", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "<br></html>";
		htmlDistrib = htmlDistrib.replace("P(", "<b>P(</b>").replace("):=", "<b>):=</b>");
		htmlDistrib = htmlDistrib.replace("if", "<b>if</b>").replace("then", "<b>then</b>");
		return htmlDistrib;
	}
	
}
