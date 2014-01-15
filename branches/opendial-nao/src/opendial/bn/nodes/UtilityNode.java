// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a utility node (sometimes also called utility node)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class UtilityNode extends BNode {

	// logger
	public static Logger log = new Logger("UtilityNode", Logger.Level.DEBUG);

	// the utility distribution
	protected UtilityDistribution distrib;

	// ===================================
	//  NODE CONSTRUCTION
	// ===================================


	/**
	 * Creates a new utility node, with an empty utility distribution
	 * 
	 * @param nodeId the node identifier
	 */
	public UtilityNode(String nodeId) {
		super(nodeId);
		distrib = new UtilityTable();
	}

	/**
	 * Creates a new utility node, with the given utility distribution
	 * 
	 * @param nodeId the node identifier
	 * @param distrib the utility distribution
	 */
	public UtilityNode(String nodeId, UtilityDistribution distrib) {
		super(nodeId);
		this.distrib = distrib;
	}

	/**
	 * Adds a new utility to the node, valid for the given assignment on 
	 * the input nodes
	 * 
	 * @param input a value assignment on the input nodes
	 * @param value the assigned utility
	 */
	public void addUtility(Assignment input, double value) {
		if (distrib instanceof UtilityTable) {
			((UtilityTable)distrib).setUtil(input, value);
		}
		else {
			log.warning("utility distribution is not a table, cannot add value");
		}
	}

	/**
	 * Removes the utility associated with the input assignment from the node
	 * 
	 * @param input the input associated with the utility to be removed
	 */
	public void removeUtility(Assignment input) {
		if (distrib instanceof UtilityTable) {
			((UtilityTable)distrib).removeUtil(input);
		}
		else {
			log.warning("utility distribution is not a table, cannot remove value");
		}
	}



	public void setDistrib(UtilityDistribution distrib) {
		this.distrib = distrib;
	}


	@Override
	public void setId(String newId) {
		super.setId(newId);
		distrib.modifyVarId(this.nodeId, newId);
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the utility associated with the specific assignment on the
	 * input variables of the node
	 * 
	 * @param input the input assignment
	 * @return the associated utility
	 */
	public double getUtility(Assignment input) {
		return distrib.getUtil(new Assignment(input));
	}

	/**
	 * Returns an empty set (a utility node has no "value", only utilities).
	 * 
	 * @return the empty set
	 */
	@Override
	public Set<Value> getValues() {
		return new HashSet<Value>();
	}

	/**
	 * Returns the utility distribution
	 * 
	 * @return the utility distribution
	 */
	public UtilityDistribution getDistrib() {
		return distrib;
	}


	/**
	 * Returns the factor matrix associated with the utility node, which maps
	 * an assignment of input variable to a given utility.
	 *
	 * @return the factor matrix
	 */
	public Map<Assignment,Double> getFactor() {

		Map<Assignment,Double> factor = new HashMap<Assignment,Double>();

		Set<Assignment> combinations = getPossibleConditions();
		for (Assignment combination : combinations) {
			factor.put(combination, distrib.getUtil(combination));
		}
		return factor;
	}


	// ===================================
	//  UTILITIES
	// ===================================


	/**
	 * Returns a copy of the utility node. Note that only the node content is copied,
	 * not its connection with other nodes.
	 * 
	 * @return the copy
	 * @throws DialException 
	 */
	@Override
	public UtilityNode copy() throws DialException {
		UtilityNode copy = new UtilityNode(nodeId, distrib.copy());
		return copy;
	}


	/**
	 * Returns a string representation of the node, consisting of 
	 * the node identifier and its distribution
	 */
	@Override
	public String toString() {
		return nodeId ;
	}


	/**
	 * Returns the hashcode for the value, computed from the node
	 * identifier and the distribution
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return nodeId.hashCode() - distrib.hashCode();
	}



}
