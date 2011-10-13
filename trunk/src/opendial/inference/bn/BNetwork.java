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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.utils.InferenceUtils;
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
	
	Map<String,BNode> nodes;
	
	public static boolean autoCompletion = true;

	public BNetwork () {
		nodes = new HashMap<String, BNode>();
	}
	
	
	public BNode getNode(String nodeId) {
		return nodes.get(nodeId);
	}
	
	public void addNode(BNode node) throws DialException {
		if (autoCompletion) {
			node.getDistribution().completeProbabilityTable();
		}
		
		if (!node.getDistribution().isWellFormed()) {
			throw new DialException("Probability table for node " + node.getId() + " is not well-formed");
		}
		nodes.put(node.getId(), node);
	}
	
	
	public List<BNode> getNodes() {
		return new ArrayList<BNode>(nodes.values());
	}


}
