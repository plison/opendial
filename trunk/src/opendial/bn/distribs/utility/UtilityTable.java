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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Estimate;
import opendial.bn.values.Value;
import opendial.planning.ValuedAction;
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
	public static Logger log = new Logger("UtilityTable", Logger.Level.DEBUG);

	// mapping between assignments and estimates of the utility value
	Map<Assignment,Estimate> table;

	// the variables of the table
	Set<String> tableVars;
	
	
	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================


	/**
	 * Creates a new, empty empirical utility table
	 */
	public UtilityTable() {
		table = new HashMap<Assignment,Estimate>();
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
			table.put(sample, new Estimate(utility));
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
		table.put(input,new Estimate(utility));
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
			return table.get(input).getValue();
		}
		else {
			Assignment trimmedInput = input.getTrimmed(tableVars);
			if (table.containsKey(trimmedInput)) {
				return table.get(trimmedInput).getValue();
			}
			return 0.0f;
		}
	}



	/**
	 * Returns true if the table defines an utility for the specified input,
	 * and false otherwise
	 * 
	 * @param input the input assignment
	 * @return true if an utility is defined, false otherwise
	 */
	public boolean hasUtility(Assignment input) {
		return table.containsKey(input);
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
	 * Creates a table with a subset of the utility values, namely the nbest highest
	 * ones.  
	 * 
	 * @param nbest the number of values to keep in the filtered table
	 * @return the table of values, of size nbest
	 * @throws DialException if nbest is < 1
	 */
	public List<ValuedAction> getFilteredTable(int nbest) throws DialException {
		if (nbest < 1) {
			throw new DialException("nbest must be >= 1");
		}
		List<ValuedAction> couples = new ArrayList<ValuedAction>(table.size());
		for (Assignment a : table.keySet()) {
			couples.add(new ValuedAction(a, table.get(a).getValue()));
		}
		Collections.sort(couples);
		int fromIndex = (couples.size() >= nbest)? couples.size()-nbest : 0;
		return couples.subList(fromIndex, couples.size());
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
		Map<Assignment,Estimate> utilities2 = new HashMap<Assignment,Estimate>();
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


}
