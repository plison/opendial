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

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
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
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void showBayesianNetwork (BNetwork bn) {
		// Graph<V, E> where V is the type of the vertices // and E is the type of the edges 
		Forest<String, Integer> f = new DelegateForest();
		

		int counter = 0;
		for (BNode node: bn.getNodes()) {
			f.addVertex(node.getId());
			for (BNode inputNode : node.getInputNodes()) {
				f.addEdge(counter, inputNode.getId(), node.getId());
				counter++;
			}
		}
		
		
		Layout<String, Integer> layout = new TreeLayout(f, 100, 100); 
		// layout.setSize(new Dimension(300,300)); 
		// sets the initial size of the space

		// The BasicVisualizationServer<V,E> is parameterized by the edge types 
		VisualizationViewer<String,Integer> vv =
		new VisualizationViewer<String,Integer>(layout); 
	//	vv.setPreferredSize(new Dimension(800,400)); 

		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.setVertexToolTipTransformer(new TooltipTransformer(bn));

		//Sets the viewing area size
		JFrame frame = new JFrame("Bayesian Network visualisation"); 

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.getContentPane().add(vv); 
		frame.pack();
		frame.setVisible(true);
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
		return htmlDistrib;
	}
	
}
