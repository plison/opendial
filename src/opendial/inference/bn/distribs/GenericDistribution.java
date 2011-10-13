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

package opendial.inference.bn.distribs;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.inference.bn.Assignment;
import opendial.utils.InferenceUtils;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class GenericDistribution {

	static Logger log = new Logger("ProbabilityTable", Logger.Level.DEBUG);

	
	protected SortedMap<String,Set<Object>> headValues;
	protected SortedMap<String,Set<Object>> depValues;

	public GenericDistribution() {
		this.headValues = new TreeMap<String,Set<Object>>();
		this.depValues = new TreeMap<String,Set<Object>>();;
	}
	
	
	// ===================================
	// HEAD AND DEPENDENT VALUES
	// ===================================

	
	public void addHeadValues(String headLabel, Set<Object> values) {
		if (headValues.containsKey(headLabel)) {
			headValues.get(headLabel).addAll(values);
		}
		else {
			headValues.put(headLabel, values);
		}
	}
	
	public void addHeadValue(String headLabel, Object value) {
		if (headValues.containsKey(headLabel)) {
			headValues.get(headLabel).add(value);
		}
		else {
			Set<Object> newSet = new HashSet<Object>();
			newSet.add(value);
			headValues.put(headLabel, newSet);
		}
	}
	
	
	public void addDepValues(String depLabel, Set<Object> values) {
		if (depValues.containsKey(depLabel)) {
			depValues.get(depLabel).addAll(values);
		}
		else {
			depValues.put(depLabel, values);
		}
	}
	
	public void addDepValues(String depLabel, Object value) {
		if (depValues.containsKey(depLabel)) {
			depValues.get(depLabel).add(value);
		}
		else {
			Set<Object> newSet = new HashSet<Object>();
			newSet.add(value);
			depValues.put(depLabel, newSet);
		}
	}
	
	
	// ===================================
	// ABSTRACT METHODS
	// ===================================

	
	public abstract boolean hasDefinedProb(Assignment assign);
	
	public abstract float getProb(Assignment assign);
	
	public abstract Set<Assignment> getDefinedAssignments();
	
	public abstract boolean isWellFormed() ;
	
	
	public void completeProbabilityTable() {
		
	}
	
	// ===================================
	// ASSIGNMENT METHODS
	// ===================================

	
	
	public boolean isValidAssignment(Assignment assign) {
		for (String basicAss : assign.getPairs().keySet()) {
			Object value = assign.getPairs().get(basicAss);
			if (headValues.containsKey(basicAss)) {
				return headValues.get(basicAss).contains(value);
			}
			else if (depValues.containsKey(basicAss)) {
				return depValues.get(basicAss).contains(value);
			}
			else {
				log.warning("assignment contains variable " + basicAss + " which is not declared in the table");
			}
		}
		return true;
	}
	
	
	
	public List<Assignment> getConditionalAssignments() {
		List<Assignment> result = new LinkedList<Assignment>();
		for (Assignment assignment : getDefinedAssignments()) {
			Assignment reducedAss = assignment.copy();
			reducedAss.removePairs(headValues.keySet());
			result.add(reducedAss);
		}
		return result;
	}

	
	
	protected List<Assignment> getUncoveredAssignments(Assignment condAss) {
		List<Assignment> uncovered = new LinkedList<Assignment>();
		
		List<Assignment> valueCombinations = InferenceUtils.getAllCombinations(headValues);
		for (Assignment comb : valueCombinations) {
			if (!hasDefinedProb(new Assignment(condAss, comb))) {
				uncovered.add(comb);
			}
		}
		return uncovered;
	}


	protected float getTotalProbability(Assignment condAss) {
		float total = 0.0f;
		for (Assignment a : getDefinedAssignments()) {
			if (a.contains(condAss)) {
				total += getProb(a);
			}
		}
		return total;
	}

}
