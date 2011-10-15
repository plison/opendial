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

import java.awt.Dimension;

import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
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

	static Logger log = new Logger("NetworkVisualisation", Logger.Level.NORMAL);
	
	
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
		BasicVisualizationServer<String,Integer> vv =
		new BasicVisualizationServer<String,Integer>(layout); 
		vv.setPreferredSize(new Dimension(500,600)); 

		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		//Sets the viewing area size
		JFrame frame = new JFrame("Simple Graph View"); 

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.getContentPane().add(vv); 
		frame.pack();
		frame.setVisible(true);
	}
}
