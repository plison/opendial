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
package opendial.inference.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.state.DialogueState;

public abstract class Query {
	
	public static Logger log = new Logger("Query", Logger.Level.DEBUG);
	
	BNetwork network;
	Collection<String> queryVars;
	Assignment evidence;
	Collection<String> conditionalVars;
	
	boolean lightweight = false;

	
	protected Query (BNetwork network, Collection<String> queryVars, 
			Assignment evidence) {
		this(network, queryVars, evidence, new ArrayList<String>());
	}
	
	protected Query (BNetwork network, Collection<String> queryVars, 
			Assignment evidence, Collection<String> conditionalVars) {
		this.network = network;
		this.queryVars = new ArrayList<String>(queryVars);
		this.evidence = new Assignment(evidence);
		this.conditionalVars = new ArrayList<String>(conditionalVars);
	}

	
	public void addEvidence(Assignment evidence) {
		this.evidence.addAssignment(evidence);
	}
	

	protected static Collection<String> getCollection(String...args) {
		List<String> list = new ArrayList<String>();
		for (int i = 0 ; i < args.length ; i++) {
			list.add(args[i]);
		}
		return list;
	}


	public BNetwork getNetwork() {
		return network;
	}
	
	public Collection<String> getQueryVars() {
		return queryVars;
	}
	
	public Assignment getEvidence() {
		return evidence;
	}
	
	public Collection<String> getConditionalVars() {
		return conditionalVars;
	}
	
	public abstract Set<String> getIrrelevantNodes() ;
	
	public List<BNode> getFilteredSortedNodes() {
		List<BNode> filteredNodes = new ArrayList<BNode>();
		Set<String> irrelevantNodes = getIrrelevantNodes();
		for (BNode node : network.getSortedNodes()) {
			if (!irrelevantNodes.contains(node.getId())) {
				filteredNodes.add(node);
			}
		}
		return filteredNodes;
	}
	
	public String toString() {
		String str = "";
		for (String q : queryVars) {
			str+= q + ",";
		}
		if (!queryVars.isEmpty()) {
			str = str.substring(0, str.length()-1);
		}
		if (!evidence.isEmpty() || !conditionalVars.isEmpty()) {
			str += "|";
			if (!evidence.isEmpty()) {
				str += evidence;
				if (!conditionalVars.isEmpty()) {
					str += ",";
				}
			}
			if (!conditionalVars.isEmpty()) {
				for (String c : conditionalVars) {
					str+= c + ",";
				}
				str = str.substring(0, str.length()-1);
			}
		}
		str += "";
		return str;
	}

	
	public int hashCode() {
		return queryVars.hashCode() - network.getNodeIds().hashCode() 
				+2*evidence.hashCode() -3*conditionalVars.hashCode();
	}
	
	
	public void removeQueryVar(String queryVar) {
		queryVars.remove(queryVar);
	}
	
	public void setAsLightweight(boolean lightweight) {
		this.lightweight = lightweight;
	}
	
	
	public boolean isLightweight() {
		return lightweight;
	}
	
}
