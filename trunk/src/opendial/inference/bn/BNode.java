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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import opendial.inference.bn.distribs.ProbabilityTable;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNode<T> {

	static Logger log = new Logger("BNode", Logger.Level.NORMAL);

	String id;

	Set<T> values;

	Map<String, BNode<?>> condNodes ;

	ProbabilityTable table;
	


	public BNode(String id) {
		this.id = id;
		condNodes = new HashMap<String,BNode<?>>();
		values = new HashSet<T>();
		table = new ProbabilityTable();
	}

	public BNode(Set<T> values) {
		this.values = values;
	}

	public void addConditionalNode(BNode<?> node) {
		condNodes.put(node.getId(), node);
	}

	public void addRow(Assignment assignment, float prob) {
		checkValidAssignment(assignment);
		table.addRow(assignment, prob);		
	}
	
	

	public void completeProbabilityTable() {
		for (Assignment reducedAss : getCondAssignments()) {
			List<Assignment> asses = table.getIncludingAssignments (reducedAss);
			List<Object> uncovered = getUncoveredValues(asses);
			if (uncovered.size() == 1) {
				log.debug("Auto completion!!");
				Assignment lastAss = new Assignment(reducedAss, id, uncovered.get(0));
				table.addRow(lastAss, getRemainingProbability(asses));
			}
		}
	}



	public float getRemainingProbability(List<Assignment> asses) {
		float remaining = 1.0f;
		for (Assignment ass: asses) {
			remaining = remaining - table.getProb(ass);
		}
		return remaining;
	}

	
	public List<Assignment> getCondAssignments() {
		List<Assignment> result = new LinkedList<Assignment>();
		for (Assignment assignment : table.getAllAssignments()) {
			Assignment reducedAss = assignment.copy();
			reducedAss.removeAssignment(id);
			result.add(reducedAss);
		}
		return result;
	}
	
	
	private List<Object> getUncoveredValues(List<Assignment> asses) {
		List<Object> coveredVals = new LinkedList<Object>();
		for (Assignment ass : asses) {
			Object val = ass.getSubAssignment(id);
			coveredVals.add(val);
		}
		List<Object> uncoveredVals = new LinkedList<Object>();
		for (Object val : values) {
			if (!coveredVals.contains(val)) {
				uncoveredVals.add(val);
			}
		}
		return uncoveredVals;
	}
 

	public void checkValidAssignment(Assignment assignment) {
		if (assignment.getSize() != condNodes.size() +1) {
			log.warning("assignment has a different number of variables than the node: " 
					+ assignment.getSize() +  " != " + (condNodes.size() + 1));
		}
		SortedMap<String,Object> assignments = assignment.getAssignments();
		for (String key : assignments.keySet()) {
			Object val = assignments.get(key);
			
			if (id.equals(key)) {
				if (!hasValue(val)) {
					log.warning("assignement " + key + "= " + val + " uses a value not present in node");	
				}
			}
			else if (condNodes.containsKey(key)) {
				BNode<?> node = condNodes.get(key);
				if (!node.hasValue(val)) {
					log.warning("assignement " + key + "= " + val + " uses a value not present in the input node");
				}
			}
			else {
				log.warning("assignment with label " + key + " which does not exist in the node or its inputs");
			}
		}
	}


	/**
	 * 
	 * @param val
	 * @return
	 */
	private boolean hasValue(Object val) {
		return values.contains(val);
	}

	public void addValue(T val) {
		values.add(val);
	}


	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public Set<T> getValues() {
		return values;
	}

	public List<Assignment> getAllPossibleAssignments() {
		List<Assignment> assignments = new LinkedList<Assignment>();
		for (T val : values) {
			assignments.add(new Assignment(getId(), val));
		}

		for (BNode<?> inputNode : condNodes.values()) {
			for (Object val: inputNode.getValues()) {

				List<Assignment> assignments2 = new LinkedList<Assignment>(assignments);

				for (Assignment ass: assignments) {
					assignments2.add(new Assignment(ass, inputNode.getId(), val));
				}

				assignments = assignments2;
			}
		}
		return assignments;
	}
	/**
	 * 
	 * @return
	 */
	public List<BNode<?>> getConditionalNodes() {
		return new ArrayList<BNode<?>>(condNodes.values());
	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public boolean hasProb(Assignment a) {
		return table.hasProb(a);
	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public float getProb(Assignment a) {
		return table.getProb(a);
	}

}
