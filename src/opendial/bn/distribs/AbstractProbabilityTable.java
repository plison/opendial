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

package opendial.bn.distribs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbabilityTable;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.DistanceUtils;

/**
 * Traditional probability distribution represented as a probability table.  The table
 * expresses a generic distribution of type P(X1...Xn|Y1...Yn), where X1...Xn is called
 * the "head" part of the distribution, and Y1...Yn the conditional part.
 * 
 * <p>This abstract class represents the common methods for two tables: 
 * DiscreteProbabilityTable, where X1...Xn are discrete random variables, and 
 * ContinuousProbabilityTable, where they are continuous.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public abstract class AbstractProbabilityTable<T extends ProbDistribution> implements ProbDistribution {

	// logger
	public static Logger log = new Logger("AbstractProbabilityTable", Logger.Level.DEBUG);

	// conditional variables
	protected Set<String> conditionalVars;
	
	// the probability table
	protected HashMap<Assignment,T> table;

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================


	/**
	 * Constructs a new probability table, with no values
	 */
	public AbstractProbabilityTable() {
		table = new HashMap<Assignment,T>();
		conditionalVars = new HashSet<String>();
	}


	/**
	 * Modifies the distribution table by replace the old variable identifier
	 * by the new one
	 * 
	 * @param oldVarId the old variable label
	 * @param newVarId the new variable label
	 */
	public void modifyVarId(String oldVarId, String newVarId) {
	//	log.debug("changing var id from " + oldVarId + " --> " + newVarId);
		HashMap<Assignment,T> newTable = new HashMap<Assignment,T>();

		for (Assignment condition : table.keySet()) {
			Assignment newCondition = condition.copy();
			if (condition.containsVar(oldVarId)) {
				Value condVal = newCondition.removePair(oldVarId);
				newCondition.addPair(newVarId, condVal);
			}
			table.get(condition).modifyVarId(oldVarId, newVarId);
			newTable.put(newCondition, table.get(condition));
		}
		
		if (conditionalVars.contains(oldVarId)) {
			conditionalVars.remove(oldVarId);
			conditionalVars.add(newVarId);
		}
		
		table = newTable;
	}


	// ===================================
	//  GETTERS
	// ===================================


	
	/**
	 * Sample a head assignment from the distribution P(head|condition), given the
	 * condition.  If no assignment can be sampled (due to e.g. an ill-formed 
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 * @throws DialException 
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {

		Assignment trimmed = condition.getTrimmed(conditionalVars);
		
		if (table.containsKey(trimmed)) {
			return table.get(trimmed).sample(new Assignment());
		}
		else  {
			Assignment closest = DistanceUtils.getClosestElement(table.keySet(), trimmed);
			if (!closest.isEmpty()) {
				return table.get(trimmed).sample(new Assignment());
			}	
		}
		log.debug("could not find the corresponding condition for " + condition + 
				" (vars: " + conditionalVars + ", nb of rows: " + table.size() + ")");
		return new Assignment();
	}
	

	// ===================================
	//  UTILITIES
	// ===================================

	/**
	 * Returns the hashcode for the table.
	 */
	@Override
	public int hashCode() {
		return table.hashCode();
	}


	public void fillConditionalHoles() {
				Map<String,Set<Value>> possibleCondPairs = 
					CombinatoricsUtils.extractPossiblePairs(table.keySet());
				
				Set<Assignment> possibleCondAssignments = 
					CombinatoricsUtils.getAllCombinations(possibleCondPairs);
				possibleCondAssignments.remove(new Assignment());
				
				for (Assignment a: possibleCondAssignments) {
					if (!table.containsKey(a)) {
						
					}
				}
	}

	/**
	 * Returns true if the probability table is well-formed.  The method checks that all 
	 * possible assignments for the condition and head parts are covered in the table, 
	 * and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		
		// checks that all possible conditional assignments are covered in the table
		Map<String,Set<Value>> possibleCondPairs = 
			CombinatoricsUtils.extractPossiblePairs(table.keySet());
		
		// Note that here, we only check on the possible assignments in the distribution itself
		// but a better way to do it would be to have it on the actual values given by the input nodes
		// but would require the distribution to have some access to it.
		Set<Assignment> possibleCondAssignments = 
			CombinatoricsUtils.getAllCombinations(possibleCondPairs);
		possibleCondAssignments.remove(new Assignment());
		if (possibleCondAssignments.size() != table.keySet().size() && possibleCondAssignments.size() > 1) {
			log.warning("number of possible conditional assignments: " 
					+ possibleCondAssignments.size() 
					+ ", but number of actual conditional assignments: " 
					+ table.keySet().size());
			log.debug("possible conditional assignments: " + possibleCondAssignments);
			log.debug("actual assignments: "+ table.keySet());
			return false;
		}

		for (Assignment condition : table.keySet()) {
			if (!table.get(condition).isWellFormed()) {
				log.debug(table.get(condition) + " is ill-formed");
				return false;
			}
		}

		return true;
	}



}
