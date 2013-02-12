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

package opendial.bn.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.utils.StringUtils;

/**
 * Decision node whose action values are not explicitly specified, but are provided
 * by the utility nodes attached to it, via the getRelevantActions() method.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DerivedActionNode extends ActionNode {

	// logger
	public static Logger log = new Logger("DerivedActionNode", Logger.Level.DEBUG);

	Set<Value> valueCache;

	Map<Assignment, Intervals<Value>> relevantActionsCache;

	/**
	 * Creates a new derived action node, with the given node identifier
	 * 
	 * @param nodeId the node identifier
	 */
	public DerivedActionNode(String nodeId) {
		super(nodeId);
		relevantActionsCache = new HashMap<Assignment, Intervals<Value>>();
	}


	@Override
	public void addOutputNode_internal(BNode node) {
		super.addOutputNode_internal(node);
		valueCache = null;
	}


	public void addValue(Value value) {
		log.info("functionality not available, decision node is derived");
	} 

	public void addValues(Set<Value> values) {
		log.info("functionality not available, decision node is derived");
	} 

	public void removeValue(Value value) {
		log.info("functionality not available, decision node is derived");
	} 

	public void removeValues(Set<Object> values) {
		log.info("functionality not available, decision node is derived");
	} 


	/**
	 * Returns the factor matrix for the action node.  The matrix lists the possible
	 * actions for the node, along with a uniform probability distribution over
	 * its values
	 *
	 * @return the factor matrix corresponding to the node
	 */
	@Override
	public Map<Assignment,Double> getFactor() {
		Map<Assignment,Double> factor = new HashMap<Assignment,Double>();
		for (Value value : getValues()) {
			factor.put(new Assignment(nodeId, value), 1.0 / (getValues().size()));
		}
		return factor;
	}

	/**
	 * Returns a probability uniformly distributed on the alternative values.
	 * @param actionValue
	 * @return 1/|values|
	 */
	public double getProb(Value actionValue) {
		Map<Assignment,Double> factor = getFactor();
		if (factor.containsKey(new Assignment(nodeId, actionValue))) {
			return factor.get(new Assignment(nodeId, actionValue));
		}
		return 0.0;
	}


	// ===================================
	//  GETTERS
	// ===================================



	/**
	 * Returns the list of values currently listed in the node
	 * 
	 * @return the list of values
	 */
	public Set<Value> getValues() {
		if (valueCache == null) {
			buildValueCache();
		}
		return valueCache;
	}


	/**
	 * Returns a sample point for the action, assuming a uniform distribution
	 * over the action values
	 * 
	 * @return
	 */
	public Value sample() { 	
		int index = sampler.nextInt(getValues().size());
		return new ArrayList<Value>(getValues()).get(index);
	}


	/**
	 * Returns a sample point for the action, assuming a uniform distribution
	 * over the action values.  The input assignment might constrain the
	 * set of relevant actions considered for sampling.
	 * 
	 * @param sample
	 * @return
	 */
	public Value sample(Assignment input) {

		if (!relevantActionsCache.containsKey(input)) {
			buildRelevantActionsCache(input);
		}

		Intervals<Value> allActions = relevantActionsCache.get(input);
		if (allActions == null) {
			log.debug("action is " + nodeId + " input is " +input);
			log.debug("relevant actions " + relevantActionsCache.keySet());
		}
		if (!allActions.isEmpty()) {
			try {
				return allActions.sample();
			}
			catch (DialException e) {
				log.warning("action sampling problem: " + e);
				return ValueFactory.none();
			}
		}
		else {
			//	log.warning("cannot sample for action " + nodeId + " (no relevant actions found)");
			return ValueFactory.none();
		}
	}


	// ===================================
	//  UTILITIES
	// ===================================


	/**
	 * Copies the action node
	 * 
	 * @return the copy of the node
	 */
	@Override
	public ActionNode copy() {
		ActionNode nodeCopy = new DerivedActionNode(nodeId);
		return nodeCopy;
	}


	/**
	 * Returns a pretty print representation of the node, which enumerates
	 * the action values
	 * 
	 * @return the pretty print representation
	 */
	@Override
	public String prettyPrint() {
		return getValues().toString() + "\n";
	}


	/**
	 * Returns a string representation of the node, which states the node identifier
	 * followed by the action values
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return nodeId + ": " + getValues().toString();
	}


	/**
	 * Returns the hashcode corresponding to the action node
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode() + getValues().hashCode();
	}


	/**
	 * Builds the cache of possible action values
	 */
	private synchronized void buildValueCache() {
		Set<Value> valueCacheTemp = new HashSet<Value>();
	//	String formattedId = StringUtils.removeSpecifiers(nodeId);
		for (BNode output : getOutputNodes()) {
			if (output instanceof UtilityNode) {
				Set<Assignment> relevantActions = ((UtilityNode)output).getRelevantActions();
				for (Assignment relevantAction : relevantActions) {
					if (relevantAction.containsVar(nodeId)) {
						valueCacheTemp.add(relevantAction.getValue(nodeId));
					}
					else {
						valueCacheTemp.add(ValueFactory.none());						
					}
				}
			}
		}
		// seems necessary for complex cases with multiple actions
		valueCacheTemp.add(ValueFactory.none());
		valueCache = valueCacheTemp;
	}

	
	private synchronized void buildRelevantActionsCache(Assignment input) {
	//	String formattedId = StringUtils.removeSpecifiers(nodeId);
	//	Assignment input2 = input.removeSpecifiers();
		Map<Value, Double> frequencies = new HashMap<Value,Double>();
		for (BNode output : getOutputNodes()) {
			if (output instanceof UtilityNode) {
				Set<Assignment> possibleActions = ((UtilityNode)output).getRelevantActions();	
				Set<Assignment> compatibleActions = new HashSet<Assignment>();
				for (Assignment action : possibleActions) {
					if (action.consistentWith(input)) {
						compatibleActions.add(action);
					}
				}
				for (Assignment compatibleAction: compatibleActions) {
					if (compatibleAction.containsVar(nodeId)) {
						Value val = compatibleAction.getValue(nodeId);
						if (!frequencies.containsKey(val)) {
							frequencies.put(val, 0.0);
						}
						frequencies.put(val, frequencies.get(val)+1.0);
					}
	
				}
			}
		}
		frequencies.put(ValueFactory.none(), 1.0);
		double total = 0.0;
		for (Value val : frequencies.keySet()) {
			total += frequencies.get(val);
		}
		for (Value val : new HashSet<Value>(frequencies.keySet())) {
			frequencies.put(val, frequencies.get(val)/total);
		}
		relevantActionsCache.put(new Assignment(input), new Intervals<Value>(frequencies));
	}
}
