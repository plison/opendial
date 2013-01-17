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

package opendial.bn.distribs.discrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.FunctionBasedDistribution;
import opendial.bn.distribs.continuous.functions.DiscreteDensityFunction;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.values.NoneVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.utils.CombinatoricsUtils;

/**
 * Traditional probability distribution represented as a probability table. 
 * A simple table assumes that the distribution is (1) discrete and (2) does 
 * not contain any conditional variables, and can therefore be expressed as
 * P(X1,... X,n).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
public class SimpleTable implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("SimpleTable", Logger.Level.DEBUG);

	// the head variables
	Set<String> headVars;

	// the probability table
	Map<Assignment,Double> table;

	// probability intervals (used for binary search in sampling)
	Intervals<Assignment> intervals;

	// sampler
	Random sampler;
	
	// if search for a "close" row in the table, minimum distance
	public static final double MIN_PROXIMITY_DISTANCE = 0.1;

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================


	/**
	 * Constructs a new probability table, with no values
	 */
	public SimpleTable() {
		table = new HashMap<Assignment,Double>(5);
		headVars = new HashSet<String>();
		sampler = new Random();
	}

	/**
	 * Constructs a new probability table with a mapping between head
	 * variable assignments and probability values.  The construction
	 * assumes that the distribution does not have any conditional
	 * variables.
	 * 
	 * @param headTable the mapping to fill the table
	 */
	public SimpleTable(Map<Assignment,Double> headTable) {
		this();
		double totalProb = 0.0;
		for (Assignment a : headTable.keySet()) {
			addRow(a, headTable.get(a));
			totalProb += headTable.get(a);
		}
		if (totalProb < 0.99999) {
			addRow(getDefaultAssign(), 1.0 - totalProb);
		}
	}


	/**
	 * Adds a new row to the probability table, assuming no conditional assignment.
	 * If the table already contains a probability, it is erased.
	 * 
	 * @param head the assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public void addRow (Assignment head, double prob) {

		if (prob < 0.0f || prob > 1.03f) {
			log.warning("probability is not well-formed: " + prob);
			return;
		}

		headVars.addAll(head.getVariables());

		table.put(head,prob);

		double totalProb = countTotalProb();
		if (totalProb < 0.975) {
			table.put(getDefaultAssign(), 1.0 - totalProb);
		}
		else {
			table.remove(getDefaultAssign());
		}
	}



	/**
	 * Increments the probability specified in the table for the given head 
	 * assignment.  If none exists, simply assign the probability.
	 * 
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public void incrementRow(Assignment head, double prob) {
		if (table.containsKey(head)) {
			addRow(head, table.get(head) + prob);
		}
		else {
			addRow(head, prob);
		}
	}

	/**
	 * Add a new set of rows to the probability table, given the conditional 
	 * assignment and the mappings (head assignment, probability value).
	 * 
	 * @param condition the conditional assignment for Y1...Yn
	 * @param heads the mappings (head assignment, probability value)
	 */
	public void addRows(Map<Assignment,Double> heads) {
		for (Assignment head : heads.keySet()) {
			addRow(head, heads.get(head));
		}
	}

	/**
	 * Removes a row from the table, given the condition and the head assignments.
	 * 
	 * @param condition conditional assignment
	 * @param head head assignment
	 */
	public void removeRow(Assignment head) {

		table.remove(head);

		double totalProb = countTotalProb();
		if (totalProb < 0.99999) {
			table.put(getDefaultAssign(), 1.0 - totalProb);
		}

	}


	/**
	 * Modifies the distribution table by replace the old variable identifier
	 * by the new one
	 * 
	 * @param oldVarId
	 * @param newVarId
	 */
	public void modifyVarId(String oldVarId, String newVarId) {
		//	log.debug("changing var id from " + oldVarId + " --> " + newVarId);
		Map<Assignment,Double> newTable = new HashMap<Assignment,Double>();

		for (Assignment head : table.keySet()) {
			Assignment newHead = head.copy();
			if (head.containsVar(oldVarId)) {
				Value condVal = newHead.removePair(oldVarId);
				newHead.addPair(newVarId, condVal);
			}
			newTable.put(newHead, table.get(head));
		}

		headVars.remove(oldVarId);
		headVars.add(newVarId);

		table = newTable;
		resetIntervals();
	}


	private synchronized void resetIntervals() {
		intervals = new Intervals<Assignment>(table);
	}


	// ===================================
	//  GETTERS
	// ===================================



	/**
	 * Samples the probability distribution
	 * 
	 * @param condition the conditional assignment (ignored here)
	 * @return the sample (variable,value) pair
	 * @throws DialException 
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}


	/**
	 * Returns the probability of the head assignment given the condition
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the probability
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {
		return getProb(head);
	}


	/**
	 * Returns true if there exists a probability for P(head|condition)
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return true if the probability is defined, false otherwise
	 */
	@Override
	public boolean hasProb(Assignment condition, Assignment head) {
		// TODO Auto-generated method stub
		return false;
	}


	/**
	 * Returns itself
	 * 
	 * @param condition ignored
	 * @return itself
	 * @throws DialException
	 */
	@Override
	public SimpleTable getProbTable(Assignment condition) throws DialException {
		return this;
	}


	/**
	 * Returns the rows of the table
	 * 
	 * @return the table rows
	 */
	public Set<Assignment> getRows() {
		return table.keySet();
	}


	/**
	 * Returns the probability P(head).  If the specified distribution contains
	 * conditional variables, they are marginalised.
	 * 
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	public double getProb(Assignment head) {
		if (headVars.isEmpty() && !head.isEmpty()) {
			return 0.0;
		}
		Assignment trimmedHead = head.getTrimmed(headVars);
		if (table.containsKey(trimmedHead)) {
			return table.get(trimmedHead);
		}

		else if (trimmedHead.size() == 1 && trimmedHead.getEntrySet().iterator().next().getValue() instanceof DoubleVal){
			//	log.debug("exact value cannot be found in table, must use proximity: " + trimmedHead);
			Assignment closest = getClosestRow(trimmedHead);
			if (!closest.isEmpty()) {
				return table.get(closest);
			}
			else{
		//		log.debug("closest row could not be found, table size " + table.size());
			}
		}
		return 0.0f;
	}




	/**
	 * Returns whether the distribution has a well-defined probability for the
	 * given assignment (assuming no conditional variables)
	 * 
	 * @param head the head assignment
	 * @return true if the distribution defines a probability for the value, and
	 *         false otherwise
	 */
	public boolean hasProb(Assignment head) {
		return table.containsKey(head.getTrimmed(headVars));
	}


	/**
	 * Sample a head assignment from the distribution P(head|condition), given the
	 * condition.  If no assignment can be sampled (due to e.g. an ill-formed 
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 * @throws DialException 
	 */
	public Assignment sample() throws DialException {

		if (intervals == null) {
			resetIntervals();
		}

		return intervals.sample();
	}


	/**
	 * Returns the continuous probability distribution equivalent to the
	 * current table
	 * 
	 * @return the continuous equivalent for the distribution
	 * @throws DialException 
	 */
	@Override
	public FunctionBasedDistribution toContinuous() throws DialException {
		if (headVars.size() == 1 && !table.keySet().isEmpty()) {
			String headVar = headVars.iterator().next();
			Map<Double,Double> values = new HashMap<Double,Double>();		
			double total = countTotalProb();		
			for (Assignment a : table.keySet()) {
				if (a.getValue(headVar) instanceof DoubleVal) {
					DoubleVal val = (DoubleVal)a.getValue(headVar);
					values.put(val.getDouble(), table.get(a)/total);	
				}
				else if (!(a.getValue(headVar) instanceof NoneVal)) {
					throw new DialException("Distribution could not be converted to a continuous distribution");
				}
			}
			DiscreteDensityFunction function = new DiscreteDensityFunction(values);
			return new FunctionBasedDistribution(headVar, function);
		}
		throw new DialException("Distribution could not be converted to a continuous distribution");
	}


	/**
	 * Returns the head values for the table
	 * 
	 * @return
	 */
	public Set<Assignment> getHeadValues() {
		return table.keySet();
	}


	/**
	 * Returns itself
	 * 
	 * @return itself
	 */
	@Override
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}


	
	/**
	 * Returns the set of variable labels used in the table
	 * 
	 * @return the variable labels in the table
	 */
	@Override
	public Collection<String> getHeadVariables() {
		return new HashSet<String>(headVars);
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
	 * Returns true if the probability table is well-formed.  The method checks that all 
	 * possible assignments for the condition and head parts are covered in the table, 
	 * and that the probabilities add up to 1.0f.
	 * 
	 * @return true if the table is well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		// checks that all possible conditional assignments are covered in the table
		Map<String,Set<Value>> possibleHeadPairs = 
				CombinatoricsUtils.extractPossiblePairs(table.keySet());
		Set<Assignment> possibleHeadAssignments = 
				CombinatoricsUtils.getAllCombinations(possibleHeadPairs);
		if (possibleHeadAssignments.size() != table.keySet().size() && possibleHeadAssignments.size() > 1) {
			log.warning("number of possible head assignments: " 
					+ possibleHeadAssignments.size() 
					+ ", but number of actual head assignments: " 
					+ table.keySet().size());
			log.debug("possible head assignments: " + possibleHeadAssignments);
			log.debug("actual assignments: "+ table.keySet());
			return false;
		}

		// checks that the total probability is roughly equal to 1.0f
		double totalProb = countTotalProb() + getProb(getDefaultAssign());
		if (totalProb < 0.9999f || totalProb > 1.0001f) {
			log.debug("total probability is " + totalProb);
			return false;
		}

		return true;
	}



	/**
	 * Returns a string representation of the probability table
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Assignment head: table.keySet()) {
			double prob = Math.round(table.get(head)*10000.0)/10000.0;
			str += "P("+head + "):=" + prob + "\n";
		}

		return str;
	}



	/**
	 * Returns a pretty print representation of the table, which is identical 
	 * to the toString() representation if the total number of lines is < 20.
	 * 
	 * <p>Else, returns the message "(probability table too big to be shown)".
	 * 
	 * @return the pretty print representation of the table
	 */
	@Override
	public String prettyPrint() {
		if (table.size() < 20) {
			return toString();
		}
		else {
			return "(probability table too big to be shown)";
		}
	}



	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public SimpleTable copy() {
		SimpleTable tableCopy = new SimpleTable();
		for (Assignment head : table.keySet()) {
			tableCopy.addRow(head.copy(), table.get(head));
		}
		return tableCopy;
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================



	/**
	 * Returns the total accumulated probability for the distribution P(.|condition)
	 * 
	 * @param condition the conditional assignment
	 * @return the total probability
	 */
	private double countTotalProb() {
		double totalProb = 0.0f;
		Assignment defaultA = getDefaultAssign();
		for (Assignment head : table.keySet()) {
			if (!defaultA.equals(head)) {
				totalProb += table.get(head);
			}
		}
		return totalProb;
	}


	/**
	 * Returns the default assignment
	 * 
	 * @return the default assignment
	 */
	private Assignment getDefaultAssign() {
		Assignment defaultA = new Assignment();
		for (String var : headVars) {
			defaultA.addPair(var, ValueFactory.none());
		}
		return defaultA;
	}


	/**
	 * Returns the closest row for the given value.  This method only works
	 * if the head values are defined as doubles.
	 * 
	 * @param head the head assignment
	 * @return the closest row, if any.  Else, an empty assignment
	 */
	private Assignment getClosestRow(Assignment head) {

		String variable = head.getVariables().iterator().next();
		Value value = head.getValue(variable);

		Assignment closestHead = new Assignment();
		double minDistance = MIN_PROXIMITY_DISTANCE;

		for (Assignment possHead : table.keySet()) {
			Value curVal = possHead.getValue(head.getVariables().iterator().next());

			double curDistance = Double.MAX_VALUE;
			if (value instanceof DoubleVal && curVal instanceof DoubleVal) {
				curDistance = Math.abs(((DoubleVal)value).getDouble() - ((DoubleVal)curVal).getDouble()) ;
			}
			if (curDistance < minDistance) {
				closestHead = possHead;
				minDistance = curDistance;
			}
		}
		return closestHead;
	}

}
