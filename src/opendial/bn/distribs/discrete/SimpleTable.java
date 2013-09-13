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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.DiscreteDensityFunction;
import opendial.bn.distribs.continuous.functions.MultiDiscreteDensityFunction;
import opendial.bn.distribs.continuous.functions.ProductKernelDensityFunction;
import opendial.bn.distribs.datastructs.EntryComparator;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.values.NoneVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.bn.values.VectorVal;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.DistanceUtils;

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
			addRow(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
	}


	/**
	 * Adds a new row to the probability table, assuming no conditional assignment.
	 * If the table already contains a probability, it is erased.
	 * 
	 * @param head the assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public synchronized void addRow (Assignment head, double prob) {

		if (prob < 0.0f || prob > 1.02f) {
			log.warning("probability is not well-formed: " + prob);
			return;
		}

		headVars.addAll(head.getVariables());

		table.put(head,prob);

		double totalProb = countTotalProb();
		if (totalProb < 0.98) {
			table.put(Assignment.createDefault(headVars), 1.0 - totalProb);
		}
		else {
			table.remove(Assignment.createDefault(headVars));
		}
	}



	/**
	 * Increments the probability specified in the table for the given head 
	 * assignment.  If none exists, simply assign the probability.
	 * 
	 * @param head the head assignment
	 * @param prob the probability increment
	 */
	public synchronized void incrementRow(Assignment head, double prob) {
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
	public synchronized void addRows(Map<Assignment,Double> heads) {
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
	public synchronized void removeRow(Assignment head) {

		table.remove(head);

		double totalProb = countTotalProb();
		if (totalProb < 0.99999) {
			table.put(Assignment.createDefault(headVars), 1.0 - totalProb);
		}

	}


	/**
	 * Modifies the distribution table by replace the old variable identifier
	 * by the new one
	 * 
	 * @param oldVarId
	 * @param newVarId
	 */
	public synchronized void modifyVarId(String oldVarId, String newVarId) {
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
		
		if (headVars.contains(oldVarId)) {
			headVars.remove(oldVarId);
			headVars.add(newVarId);
		}

		table = newTable;
		resetIntervals();
	}


	private synchronized void resetIntervals() {
		if (table.isEmpty()) {
			log.warning("creating intervals for an empty table");
		}
		try {
		intervals = new Intervals<Assignment>(table);
		}
		catch (DialException e) {
			log.warning("could not reset intervals: " + e);
		}
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

		else {
		//	log.debug("exact value cannot be found in table, must use proximity: " + trimmedHead);
			Assignment closest = DistanceUtils.getClosestElement(table.keySet(), trimmedHead);
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

		while (intervals == null) {
			resetIntervals();
		}
		if (intervals.isEmpty()) {
			log.warning("interval is empty, table: " + table);
			return new Assignment();
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
	public ContinuousProbDistribution toContinuous() throws DialException {
		if (headVars.size() == 1 && !table.keySet().isEmpty()) {
			String headVar = headVars.iterator().next();
			if (getTypicalValueType(headVar).equals(DoubleVal.class)) {
				return extractUnivariateDistribution(headVar);
			}
			if (getTypicalValueType(headVar).equals(VectorVal.class)) {
				return extractMultivariateDistribution(headVar);
			}

		}
		throw new DialException("Distribution could not be converted to a continuous distribution: " + headVars);
	}
	
	
	private Class<? extends Value> getTypicalValueType(String headVar) {
		for (Assignment a : table.keySet()) {
			if (!(a.getValue(headVar) instanceof NoneVal)) {
				return a.getValue(headVar).getClass();
			}
		}
		return NoneVal.class;
	}

	
	
	private UnivariateDistribution extractUnivariateDistribution(String headVar) throws DialException {
		Map<Double,Double> values = new HashMap<Double,Double>();		
		double total = countTotalProb();		
		for (Assignment a : table.keySet()) {
			if (a.getValue(headVar) instanceof DoubleVal) {
				DoubleVal val = (DoubleVal)a.getValue(headVar);
				values.put(val.getDouble(), table.get(a)/total);	
			}
			else if (!(a.getValue(headVar) instanceof NoneVal)) {
				throw new DialException("conversion error: " + a.getValue(headVar));
			}
		}
		DiscreteDensityFunction function = new DiscreteDensityFunction(values);
		return new UnivariateDistribution(headVar, function);
	}
	
	
	private MultivariateDistribution extractMultivariateDistribution(String headVar) throws DialException {
	/**	Map<Double[],Double> values = new HashMap<Double[],Double>();		
		double total = countTotalProb();		
		for (Assignment a : table.keySet()) {
			if (a.getValue(headVar) instanceof VectorVal) {
				VectorVal val = (VectorVal)a.getValue(headVar);
				values.put(val.getArray(), table.get(a)/total);	
			}
			else if (!(a.getValue(headVar) instanceof NoneVal)) {
				throw new DialException("conversion error: " + a.getValue(headVar));
			}
		}
		MultiDiscreteDensityFunction function = new MultiDiscreteDensityFunction(values);
		return new MultivariateDistribution(headVar, function); */
		
		List<Double[]> samples = new ArrayList<Double[]>(Settings.getInstance().nbSamples);
		for (int i = 0 ; i < Settings.getInstance().nbSamples ;i++) {
			Value val = sample().getValue(headVar);
			if (val instanceof VectorVal) {
				samples.add(((VectorVal)val).getArray());
			}
		}
		ProductKernelDensityFunction pkde = new ProductKernelDensityFunction(samples);
		pkde.setAsBounded(true);
		return new MultivariateDistribution(headVar, pkde);
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
	
	
	/**
	 * Returns true if the table is empty (or contains only a default
	 * assignment), false otherwise
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		if (table.isEmpty()) {
			return true;
		}
		else return (table.size() == 1 && table.keySet().iterator().next().
				equals(Assignment.createDefault(headVars)));
	}

	
	public SimpleTable getNBest(int nbest)  {
		if (nbest < 1) {
			log.warning("nbest should be superior to 1");
		}
		List<Map.Entry<Assignment,Double>> entries = 
				new ArrayList<Map.Entry<Assignment,Double>>(table.entrySet());
		
		Collections.sort(entries, new EntryComparator());
		Collections.reverse(entries);
				
		SimpleTable nbestTable = new SimpleTable();
		int incr = 0;
		for (Map.Entry<Assignment, Double> entry : entries) {
			if (incr < nbest) {
				nbestTable.addRow(entry.getKey(), entry.getValue());
			}
			incr++;
		}
		return nbestTable;
	}
	
	
	public SimpleTable getAboveThreshold(double threshold) {
		if (threshold < 0 || threshold > 1) {
			log.warning("invalid threshold: " + threshold);
		}
		SimpleTable nbestTable = new SimpleTable();
		for (Assignment a : table.keySet()) {
			if (table.get(a) >= threshold) {
				nbestTable.addRow(a, table.get(a));
			}
		}
		return nbestTable;
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
		double totalProb = countTotalProb() + getProb(Assignment.createDefault(headVars));
		if (totalProb < 0.975f || totalProb > 1.025f) {
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
			double prob = DistanceUtils.shorten(table.get(head));
			str += "P("+head + "):=" + prob + "\n";
		}

		return (str.length() > 0)? str.substring(0, str.length()-1) : str;
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
	public synchronized SimpleTable copy() {
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
		Assignment defaultA = Assignment.createDefault(headVars);
		for (Assignment head : table.keySet()) {
			if (!defaultA.equals(head)) {
				totalProb += table.get(head);
			}
		}
		return totalProb;
	}


}
