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

package opendial.bn.distribs.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;

/**
 * Utility table that is empirically constructed from a set of samples.  The table
 * is defined via a mapping from assignment to utility estimates.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class UtilityTable implements UtilityDistribution {

	// logger
	public static Logger log = new Logger("EmpiricalUtilityDistribution",
			Logger.Level.DEBUG);

	// mapping between assignments and estimates of the utility value
	Map<Assignment,UtilityEstimate> table;

	// the variables of the table
	Set<String> tableVars;
	
	
	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================


	/**
	 * Creates a new, empty empirical utility table
	 */
	public UtilityTable() {
		table = new HashMap<Assignment,UtilityEstimate>();
		tableVars = new HashSet<String>();
	}
	
	
	/**
	 * Constructs a new utility distribution, given the values provided as argument
	 * 
	 * @param values the values
	 */
	public UtilityTable(Map<Assignment,Double> values) {
		this();
		for (Assignment a : values.keySet()) {
			setUtility(a, values.get(a));
		}
	}


	/**
	 * Adds a new utility value to the estimated table
	 * 
	 * @param sample the sample assignment
	 * @param utility the utility value for the sample
	 */
	public void addUtility(Assignment sample, double utility) {
		if (!table.containsKey(sample)) {
			table.put(sample, new UtilityEstimate(utility));
		}
		else {
			table.get(sample).updateEstimate(utility);
		}
		tableVars.addAll(sample.getVariables());
	}

	

	/**
	 * Adds a new utility to the distribution, associated with a value assignment
	 * 
	 * @param input the value assignment for the input nodes
	 * @param utility the resulting utility
	 */
	public void setUtility(Assignment input, double utility) {
		table.put(input,new UtilityEstimate(utility));
		tableVars.addAll(input.getVariables());
	}
	

	/**
	 * Removes a utility from the utility distribution
	 * 
	 * @param input the assignment associated with the utility to be removed
	 */
	public void removeUtility(Assignment input) {
		table.remove(input);
	}
	
	// ===================================
	//  GETTERS
	// ===================================

	/**
	 * Returns the estimated utility for the given assignment
	 * 
	 * @param input the assignment
	 * @return the utility for the assignment
	 */
	@Override
	public double getUtility(Assignment input) {
		if (table.containsKey(input)) {
			return table.get(input).getCurrentEstimate();
		}
		else {
			Assignment trimmedInput = input.getTrimmed(tableVars);
			if (table.containsKey(trimmedInput)) {
				return table.get(trimmedInput).getCurrentEstimate();
			}
			return 0.0f;
		}
	}


	/**
	 * Returns the table reflecting the estimated utility values for each
	 * assignment
	 * 
	 * @return the (assignment,utility) table
	 */
	public Map<Assignment, Double> getTable() {
		Map<Assignment,Double> averageUtils = new HashMap<Assignment,Double>();
		for (Assignment a : table.keySet()) {
			averageUtils.put(a, getUtility(a));
		}
		return averageUtils;
	} 


	/**
	 * Returns the set of relevant actions for the utility table, given the
	 * value assignment provided as argument
	 * NB: copy of UtilityTable
	 * 
	 * @param input the value assignment
	 * @return the relevant actions for the assignment
	 */
	@Override
	public Set<Assignment> getRelevantActions(Assignment input) {
		Set<Assignment> relevantActions = new HashSet<Assignment>();
		for (Assignment fullKey : table.keySet()) {
			if (fullKey.consistentWith(input)) {
				Assignment otherHalf = new Assignment(fullKey);
				otherHalf.removePairs(input.getVariables());
				relevantActions.add(otherHalf);
			}
		}
		return relevantActions;
	}

	
	
	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 *
	 * @return
	 */
	@Override
	public boolean isWellFormed() {
		Map<String,Set<Value>> possiblePairs = 
			CombinatoricsUtils.extractPossiblePairs(table.keySet());
		Set<Assignment> possibleAssignments = 
			CombinatoricsUtils.getAllCombinations(possiblePairs);
		
		for (Assignment assignment : possibleAssignments) {
			if (!table.containsKey(assignment)) {
				log.warning("assignment " + assignment + " not defined in utility distribution");
				return false;
			}
		}
		return true;
	}



	/**
	 * Returns a copy of the utility table
	 * 
	 * @return the copy
	 */
	@Override
	public UtilityDistribution copy() {
		return new UtilityTable(getTable());
	}



	/**
	 *
	 * @return
	 */
	@Override
	public String prettyPrint() {
		Map<Assignment,Double> table =getTable();
		String str = "";
		for (Assignment input: table.keySet()) {
			str += "Q(" + input + "):=" + table.get(input) + "\n";
		}
		return str;
	}


	public String toString() {
		return prettyPrint();
	}



	/**
	 * Modifies a variable label with a new one
	 * 
	 * @param nodeId the old variable label
	 * @param newId the new label
	 */
	@Override
	public void modifyVarId(String nodeId, String newId) {
		Map<Assignment,UtilityEstimate> utilities2 = new HashMap<Assignment,UtilityEstimate>();
		for (Assignment a : table.keySet()) {
			Assignment b = new Assignment();
			for (String var : a.getVariables()) {
				String newVar = (var.equals(nodeId))? newId: var;
				b.addPair(newVar, a.getValue(var));
			}
			utilities2.put(b, table.get(a));
		}
		table = utilities2;
	}


	// ===================================
	//  INNER CLASS
	// ===================================

	/**
	 * Estimate of an utility value, defined by the averaged estimate itself,
	 * and the number of values that have contributed to it (in order to 
	 * correctly compute the average)
	 * 
	 *
	 * @author  Pierre Lison (plison@ifi.uio.no)
	 * @version $Date::                      $
	 *
	 */
	final class UtilityEstimate {

		// averaged estimate for the utility
		double averagedEstimate = 0.0;

		// number of values used for the average
		int nbValues = 0;

		/**
		 * Creates a new utility estimate, with a first value
		 * 
		 * @param firstValue the first value
		 */
		public UtilityEstimate(double firstValue) {
			updateEstimate(firstValue);
		}

		
		/**
		 * Updates the current estimate with a new value
		 * 
		 * @param newValue the new value
		 */
		public void updateEstimate(double newValue) {
			double prevUtil = averagedEstimate;
			nbValues++;
			averagedEstimate = prevUtil + (newValue - prevUtil) / (nbValues);
		}


		/**
		 * Returns the current (averaged) estimate for the utility
		 * 
		 * @return the estimate
		 */
		public double getCurrentEstimate() {
			return averagedEstimate;
		}
	}



}
