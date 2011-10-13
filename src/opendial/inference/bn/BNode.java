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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.distribs.GenericDistribution;
import opendial.inference.bn.distribs.ProbabilityTable;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNode {

	static Logger log = new Logger("BNode", Logger.Level.NORMAL);

	String id;

	Set<Object> values;

	Map<String, BNode> inputNodes ;
	Map<String, BNode> outputNodes;

	GenericDistribution table;
	
	
	public BNode(String id) {
		this.id = id;
		values = new HashSet<Object>();
		
		inputNodes = new HashMap<String, BNode>();
		outputNodes = new HashMap<String, BNode>();
		
		table = new ProbabilityTable();
	
	}


	public BNode(Set<Object> values) {
		this.values = values;
		
	}

	public void addConditionalNode(BNode node) {
		inputNodes.put(node.getId(), node);
		table.addDepValues(node.getId(), node.getValues());
		node.addOutputNode(this);
	}

	
	protected void addOutputNode(BNode node) {
		outputNodes.put(node.getId(), node);
	}
	
	
	
	public void addRow(Assignment assignment, float prob) {
		
		if (table instanceof ProbabilityTable) {
			((ProbabilityTable)table).addRow(assignment, prob);		
		}
		else {
			log.warning("distribution is not defined as a CPD, cannot add rows");
		}
	}
 

	/**
	 * 
	 * @param val
	 * @return
	 */
	public boolean hasValue(Object val) {
		return values.contains(val);
	}

	
	public void addValue(Object val) {
		values.add(val);
		table.addHeadValue(id, val);
		for (String nodeId : outputNodes.keySet()) {
			BNode oNode = outputNodes.get(nodeId);
			oNode.getDistribution().addDepValues(id, val);
		}
	}


	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public Set<Object> getValues() {
		return values;
	}

	public List<Assignment> getAllPossibleAssignments() {
		Map<String,Set<Object>> allValues = getAllValues();
		return InferenceUtils.getAllCombinations(allValues);
	}
	
	
	
	/**
	 * 
	 * @return
	 */
	public List<BNode> getConditionalNodes() {
		return new ArrayList<BNode>(inputNodes.values());
	}

	
	public List<String> getConditionalNodeIds() {
		return new ArrayList<String>(inputNodes.keySet());
	}
	
	/**
	 * 
	 * @param a
	 * @return
	 */
	public boolean hasProb(Assignment a) {
		return table.hasDefinedProb(a);
	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public float getProb(Assignment a) {
		return table.getProb(a);
	}
	


	/**
	 * 
	 * @return
	 */
	public GenericDistribution getDistribution() {
		return table;
	}


	/**
	 * 
	 * @param bValues
	 */
	public void addValues(List<Object> vals) {
		for (Object val: vals) {
			addValue(val);
		}
	}


	/**
	 * 
	 * @return
	 */
	public Set<String> getVariables() {
		Set<String> vars = new HashSet<String>();
		vars.add(id);
		vars.addAll(inputNodes.keySet());
		return vars;
	}


	/**
	 * 
	 * @return
	 */
	public Map<String, Set<Object>> getAllValues() {
		Map<String,Set<Object>> allValues = new HashMap<String,Set<Object>>();
		allValues.put(id, values);
		for (BNode n : inputNodes.values()) {
			allValues.put(n.getId(), n.getValues());
		}
		return allValues;
	}


	/**
	 * 
	 * @return
	 */
	public List<BNode> getOutputNodes() {
		return new ArrayList<BNode>(outputNodes.values());
	}

}
