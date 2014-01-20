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

package opendial.bn.distribs.other;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.ConditionalCategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.utils.CombinatoricsUtils;

/**
 * Conditional probability distribution represented as a probability table.  The table
 * expresses a generic distribution of type P(X1...Xn|Y1...Yn), where X1...Xn is called
 * the "head" part of the distribution, and Y1...Yn the conditional part.
 * 
 * <p>This class represent a generic conditional distribution in which the distribution for
 * the head variables X1,...,Xn can be represented using arbitrary distributions of type T.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ConditionalDistribution<T extends IndependentProbDistribution> implements ProbDistribution {

	// logger
	public static Logger log = new Logger("ConditionalDistribution", Logger.Level.DEBUG);

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
	public ConditionalDistribution() {
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
	@Override
	public void modifyVariableId(String oldVarId, String newVarId) {
	//	log.debug("changing var id from " + oldVarId + " --> " + newVarId);
		HashMap<Assignment,T> newTable = new HashMap<Assignment,T>();

		for (Assignment condition : table.keySet()) {
			Assignment newCondition = condition.copy();
			if (condition.containsVar(oldVarId)) {
				Value condVal = newCondition.removePair(oldVarId);
				newCondition.addPair(newVarId, condVal);
			}
			table.get(condition).modifyVariableId(oldVarId, newVarId);
			newTable.put(newCondition, table.get(condition));
		}
		
		if (conditionalVars.contains(oldVarId)) {
			conditionalVars.remove(oldVarId);
			conditionalVars.add(newVarId);
		}
		
		table = newTable;
	}

	
	/**
	 * Adds a new continuous probability distribution associated with the given
	 * conditional assignment
	 * 
	 * @param condition the conditional assignment
	 * @param distrib the distribution (in a continuous, function-based representation)
	 */
	public void addDistrib (Assignment condition, T distrib) {
		table.put(condition, distrib);
		conditionalVars.addAll(condition.getVariables());
	}

	
	@Override
	public void pruneValues(double threshold) {
		for (Assignment condition : table.keySet()) {
			table.get(condition).pruneValues(threshold);
		}
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

		log.debug("could not find the corresponding condition for " + condition + 
				" (vars: " + conditionalVars + ", nb of rows: " + table.size() + ")");

		Assignment defaultA = new Assignment();

		return defaultA;
				
	}
	
	
	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>();
		for (IndependentProbDistribution subdistrib : table.values()) {
			headVars.addAll(subdistrib.getHeadVariables());
		}
		return headVars;
	}
	

	/**
	 * Returns the probability of the head assignment given the conditional assignment.
	 * The method assumes that the posterior distribution has a discrete form.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 * @throws DialException if the probability could not be extracted
	 */
	public double getProb(Assignment condition, Assignment head) throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed).toDiscrete().getProb(head);
		}
		else {
			log.warning("could not find the corresponding condition for " + condition + ")");
			return 0.0;
		}
	}

	

	/**
	 * Returns the posterior distribution P(X1,...Xn) given the condition provided
	 * in the argument.  The type of the distribution is T.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getPosterior(Assignment condition) throws DialException {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}
		else {
			log.warning("could not find the corresponding condition for " + condition);
			if (this instanceof ConditionalCategoricalTable) {
				return (T) new CategoricalTable(Assignment.createDefault(getHeadVariables()));
			}
			return table.get(table.keySet().iterator().next());
		}
	}
	
	
	@Override
	public ProbDistribution getPartialPosterior (Assignment condition) {
		Assignment trimmed = condition.getTrimmed(conditionalVars);
		if (table.containsKey(trimmed)) {
			return table.get(trimmed);
		}
		
		ConditionalDistribution<T> newDistrib = new ConditionalDistribution<T>();
		for (Assignment a : table.keySet()) {
			if (a.consistentWith(condition)) {
				Assignment remaining = a.getTrimmedInverse(condition.getVariables());
				if (!newDistrib.table.containsKey(remaining)) {
					newDistrib.addDistrib(remaining, table.get(a));
				}
				else {
					log.warning("inconsistent results for partial posterior");
				}
			}
		}
		return newDistrib;
	}
	
	
	/**
	 * Returns all possible values specified in the table.  The input values are here
	 * ignored (for efficiency reasons), so the method simply extracts all possible head rows
	 * in the table.
	 * 
	 * @param range the input values (is ignored)
	 * @return the possible values for the head variables.
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		Set<Assignment> headRows = new HashSet<Assignment>();
		for (Assignment condition : table.keySet()) {
			headRows.addAll(table.get(condition).getPossibleValues());
		}
		return headRows;
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

	

	/**
	 * Returns a copy of the probability table
	 * 
	 * @return the copy
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConditionalDistribution<T> copy() {
		ConditionalDistribution<T> newTable = new ConditionalDistribution<T>();
		for (Assignment condition : table.keySet()) {
			newTable.addDistrib(condition, (T)table.get(condition).copy());
		}
		return newTable;
	}

	
	/**
	 * Returns a pretty print of the distribution
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		String s = "";
		for (Assignment condition : table.keySet()) {
			String distribString = table.get(condition).toString();
			Pattern p = Pattern.compile("PDF\\((.)*\\)=");
			Matcher m = p.matcher(distribString);
			while (m.find()) {
				String toreplace = m.group();
				distribString = distribString.replace(toreplace, 
						toreplace.substring(0,toreplace.length()-2) + "|" + condition + ")=");
			}
			s += distribString + "\n";
		}
		return s;
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
		
		if (getHeadVariables().isEmpty()) {
			log.warning("no head variables for distribution " + toString());
			return false;
		}
		// checks that all possible conditional assignments are covered in the table
		Map<String,Set<Value>> possibleCondPairs = 
			CombinatoricsUtils.extractPossiblePairs(table.keySet());
		
		if (CombinatoricsUtils.getEstimatedNbCombinations(possibleCondPairs) < 100) {
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
		}

		for (Assignment condition : table.keySet()) {
			if (!table.get(condition).isWellFormed()) {
				log.debug(table.get(condition) + " is ill-formed");
				return false;
			}
		}

		return true;
	}


	/**
	 * Returns the preferred distribution format for the distribution (discrete or continuous).
	 */
	@Override
	public DistribType getPreferredType() {
		if (!table.keySet().isEmpty()) {
			Assignment cond = table.keySet().iterator().next();
			return table.get(cond).getPreferredType();
		}
		return DistribType.DISCRETE;
	}


	/**
	 * Returns a discrete representation of the distribution.
	 */
	@Override
	public DiscreteDistribution toDiscrete() throws DialException {
		if (this instanceof DiscreteDistribution) {
			return (DiscreteDistribution)this;
		}
		ConditionalCategoricalTable newT = new ConditionalCategoricalTable();
		for (Assignment cond : table.keySet()) {
			CategoricalTable distrib = table.get(cond).toDiscrete();
			newT.addRows(cond, distrib);
		}
		return newT;
	}


}
