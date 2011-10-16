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

package opendial.inference.distribs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNode;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * Generic class for a discrete probability distribution, which consists of a set 
 * of head variables h_1...h_n, and a set of dependent variables d_1...d_n.  The 
 * distribution is then defined over P(h_1...h_n | d_1...d_n)
 * 
 * <p>The class provides methods for adding, retrieving and manipulating the distribution
 * variables and their associated values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-10-15 23:46:18 #$
 *
 */
public abstract class GenericDistribution implements Distribution {

	// logger
	static Logger log = new Logger("GenericDistribution", Logger.Level.DEBUG);

	// head variables and their values
	protected Map<String,Set<Object>> heads;
	
	// dependent variables and their values
	protected Map<String,Set<Object>> deps;

	
	/**
	 * Creates a new generic distribution, with empty sets of variables
	 */
	public GenericDistribution() {
		this.heads = new HashMap<String,Set<Object>>();
		this.deps = new HashMap<String,Set<Object>>();;
	}
	
	
	/**
	 * Creates a new generic distribution, with the head variable being
	 * the variable from the Bayesian node, and the dependent variables
	 * being the variables defined for its input nodes.
	 * 
	 * @param node the Bayesian node
	 */
	public GenericDistribution(BNode node) {
		this();
		heads.put(node.getId(), node.getValues());
		for (BNode inputNode: node.getInputNodes()) {
			deps.put(inputNode.getId(), inputNode.getValues());
		}
	}
	
	// ===================================
	// SETTERS
	// ===================================

	
	/**
	 * Adds a new head variable to the distribution
	 * 
	 * @param headLabel the variable label 
	 * @param values the variable values
	 */
	public void addHeadVariable(String headLabel, Collection<Object> values) {
		if (heads.containsKey(headLabel)) {
			heads.get(headLabel).addAll(values);
		}
		else {
			heads.put(headLabel, new HashSet<Object>(values));
		}
	}
	
	
	
	/**
	 * Adds a new dependent variable to the distribution
	 * 
	 * @param depLabel the variable label
	 * @param values the variable values
	 */
	public void addDependentVariable(String depLabel, Set<Object> values) {
		if (deps.containsKey(depLabel)) {
			deps.get(depLabel).addAll(values);
		}
		else {
			deps.put(depLabel, values);
		}
	}
	
	
	// ===================================
	// GETTERS
	// ===================================

	
	/**
	 * Returns the list of possible assignments for the combination of
	 * dependent variables d_1...d_n in the distribution
	 * 
	 * @return the possible assignments for the dependent variables
	 */
	public List<Assignment> getConditions() {
		return InferenceUtils.getAllCombinations(deps);
	}
	
	
	/**
	 * Returns the list of possible assignments for the combination of
	 * head variables h_1... h_n in the distribution
	 * 
	 * @return the possible assignments for the head variables
	 */
	public List<Assignment> getHeads() {
		return InferenceUtils.getAllCombinations(heads);
	}
	
	
	/**
	 * Returns the list of possible assignment for all (head + dependent)
	 * variables in the distribution
	 * 
	 * @return the possible assignments for all variables
	 */
	public List<Assignment> getAllPossibleAssignments() {
		Map<String, Set<Object>> all = new HashMap<String, Set<Object>>();
		all.putAll(heads);
		all.putAll(deps);
		return InferenceUtils.getAllCombinations(all);
	}
	
	
	/**
	 * Returns true if the assignment is valid, i.e. if it only uses
	 * variables and values which have been declared in the head and
	 * dependent variables of the distribution
	 * 
	 * @param assign the assignment
	 * @return true if valid assignment, false otherwise
	 */
	protected boolean isValid (Assignment assign) {
		boolean result = true;
		for (String basicAss : assign.getVariables()) {
			Object value = assign.getValue(basicAss);
			if (heads.containsKey(basicAss)) {
				result = (heads.get(basicAss).contains(value));
			}
			else if (deps.containsKey(basicAss)) {
				result = (deps.get(basicAss).contains(value));
			}
			else {
				log.warning("assignment contains variable " + basicAss + " which is not declared in the table");
			}
		}
		
		if (!result) {
			log.warning("warning, assignment not valid for probability table: " + assign);
		}
		return result;
	}
	
	

}
