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

package opendial.inference.bn;

import java.util.HashMap;
import java.util.Map;

import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNetwork {

	static Logger log = new Logger("BNetwork", Logger.Level.DEBUG);
	
	Map<String,BNode<?>> nodes;
	
	public static boolean autoCompletion = true;

	public BNetwork () {
		nodes = new HashMap<String, BNode<?>>();
	}
	
	
	public BNode<?> getNode(String nodeId) {
		return nodes.get(nodeId);
	}
	
	public void addNode(BNode<?> node) {
		if (autoCompletion) {
			node.completeProbabilityTable();
		}
		checkCorrectness(node);
		nodes.put(node.getId(), node);
	}

	public void checkCorrectness(BNode<?> node) {
		for (Assignment a : node.getAllPossibleAssignments()) {
			if (!node.hasProb(a)) {
				log.warning("probability for " + node.getId() + " not defined for assignment: " + a);
			}
		}
		
		for (Assignment a : node.getCondAssignments()) {
			float total = 0.0f;
			
			for (Object val : node.getValues()) {
				Assignment a2 = new Assignment(a, node.getId(), val);
				total += node.getProb(a2);
			}
			
			if(total< 0.999f || total > 1.0f) {
				log.warning("total probability for " + node.getId() + " not correctly normalised: "
						+ total + " for conditial assignment " + a);
				for (Object val : node.getValues()) {
					Assignment a2 = new Assignment(a, node.getId(), val);
					log.debug("P(" + a2 + ") = " + node.getProb(a2));
				}
				log.debug("-----");
			}
		}

	}


}
