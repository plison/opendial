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

package opendial.bn.distribs.discrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.continuous.functions.DiscreteDensityFunction;
import opendial.bn.values.NoneVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.bn.values.ArrayVal;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.datastructs.ValueRange;
import opendial.utils.CombinatoricsUtils;
import opendial.utils.DistanceUtils;
import opendial.utils.InferenceUtils;
import opendial.utils.StringUtils;


/**
 * Simple probability distribution represented as a probability table. The table 
 * assumes that the distribution is  discrete and does not contain any conditional 
 * variables, and can therefore be expressed as P(X1,..., Xn).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class CategoricalTable implements DiscreteDistribution, IndependentProbDistribution {

	// logger
	public static Logger log = new Logger("CategoricalTable", Logger.Level.DEBUG);

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
	public CategoricalTable() {
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
	public CategoricalTable(Map<Assignment,Double> headTable) {
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
	 * Create a categorical table with a unique value with probability 1.0.
	 * 
	 * @param uniqueValue the unique value for the table
	 */
	public CategoricalTable(Assignment uniqueValue) {
		this();
		addRow(uniqueValue, 1.0);
	}

	/**
	 * Adds a new row to the probability table, assuming no conditional assignment.
	 * If the table already contains a probability, it is erased.
	 * 
	 * @param head the assignment for X1...Xn
	 * @param prob the associated probability
	 */
	public void addRow (Assignment head, double prob) {

		if (prob < 0.0f || prob > 1.02f) {
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
	public void incrementRow(Assignment head, double prob) {
		if (table.containsKey(head)) {
			if (head.equals(Assignment.createDefault(headVars))) {
				return;
			} 
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
			table.put(Assignment.createDefault(headVars), 1.0 - totalProb);
		}

	}


	/**
	 * Modifies the distribution table by replace the old variable identifier
	 * by the new one
	 * 
	 * @param oldVarId the old identifier
	 * @param newVarId the new identifier
	 */
	public void modifyVariableId(String oldVarId, String newVarId) {
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


	/**
	 * Resets the interval caches associated with the table.
	 */
	private void resetIntervals() {
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
	
	
	/**
	 * Prunes all table values that have a probability lower than the threshold.
	 * 
	 * @param the threshold
	 */
	public void pruneValues(double threshold) {
		Map<Assignment,Double> newTable = new HashMap<Assignment,Double>();
		for (Assignment row : table.keySet()) {
			double prob = table.get(row);
			if (prob >= threshold) {
				newTable.put(row, prob);
			}
		}
		table = newTable;
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the rows of the table
	 * 
	 * @return the table rows
	 */
	public Set<Assignment> getRows() {
		return table.keySet();
	}


	/**
	 * Returns the probability P(head). 
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
		
		// if the distribution has continuous values, search for the closest element
		else if (getPreferredType() != DistribType.DISCRETE && !trimmedHead.isDefault()) {
			try {
				Assignment closest = DistanceUtils.getClosestElement(getRows(), trimmedHead);
				return getProb(closest);
			}
			catch (DialException e) {
				log.warning("could not locate nearest point in distribution: " + e);
			}

		}

		return 0.0f;
	}
	
	
	/**
	 * Returns the marginal probability of the partial assignment.
	 * 
	 * @param head the partial assignment
	 * @return the marginal probability
	 */
	public double getMarginalProb(Assignment head) {
		Assignment trimmedHead = head.getTrimmed(headVars);
		double totalProb = 0.0;
		for (Assignment row : getRows()) {
			if (row.contains(trimmedHead)) {
				totalProb += getProb(row);
			}
		}
		return totalProb;
	}
	
	
	/**
	 * Returns the marginal distribution for a particular variable
	 * 
	 * @param head the partial assignment
	 * @return the marginal probability
	 */
	public CategoricalTable getMarginalTable(String var) {
		CategoricalTable marginalTable = new CategoricalTable();
		for (Assignment row : getRows()) {
			Value val = row.getValue(var);
			marginalTable.incrementRow(new Assignment(var, val), table.get(row));
			
		}
		return marginalTable;
	}
	
	
	/**
	 * Returns the probability for P(head), ignoring the conditional assignment.
	 * 
	 * @param condition conditional assignment (ignored)
	 * @param head head assignment
	 * @return the probability
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {
		return getProb(head);
	}


	/**
	 * returns true if the table contains a probability for the given assignment
	 * 
	 * @param head the assignment
	 * @return true if the table contains a row for the assignment, false otherwise
	 */
	public boolean hasProb(Assignment head) {
		Assignment trimmedHead = head.getTrimmed(headVars);
		return table.containsKey(trimmedHead);
	}


	/**
	 * Samples from the distribution.  The conditional assignment is ignored.
	 * 
	 * @return the sampled assignment.
	 * @throws DialException if no assignment could be sampled
	 */
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}


	/**
	 * Sample an assignment from the distribution. If no assignment can be sampled 
	 * (due to e.g. an ill-formed distribution), returns an empty assignment.
	 * 
	 * @return the sampled assignment
	 * @throws DialException if no assignment could be sampled
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
	 * @throws DialException if the distribution could not be converted
	 */
	@Override
	public ContinuousDistribution toContinuous() throws DialException {

		if (getPreferredType() == DistribType.CONTINUOUS) {
			String headVar = headVars.iterator().next();
			Map<Double[],Double> points = new HashMap<Double[],Double>();
			for (Assignment a : getRows()) {
				Value v = a.getValue(headVar);
				if (v instanceof ArrayVal) {
					points.put(((ArrayVal)v).getArray(), getProb(a));
				}
				else if (v instanceof DoubleVal) {
					points.put(new Double[]{((DoubleVal)v).getDouble()}, getProb(a));
				}
			}
			DiscreteDensityFunction fun = new DiscreteDensityFunction(points);
			return new ContinuousDistribution(headVar, fun);
		}
		
		throw new DialException("Distribution could not be converted to a " +
				"continuous distribution: " + headVars);
	}


	/**
	 * Returns itself.
	 * 
	 * @return the distribution
	 */
	@Override
	public CategoricalTable toDiscrete() {
		return this;
	}


	/**
	 * Returns itself.
	 * 
	 * @return the distribution
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition) {
		return this;
	}
 
	 

	/**
	 * Returns the distribution.
	 */
	@Override
	public CategoricalTable getPartialPosterior(Assignment condition) {
		return this;
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


	/**
	 * Returns a subset of the N values in the table with the highest probability.
	 * 
	 * @param nbest the number of values to select
	 * @return the distribution with the subset of values
	 */
	public CategoricalTable getNBest(int nbest)  {

		Map<Assignment,Double> filteredTable = InferenceUtils.getNBest(table, nbest);
		return new CategoricalTable(filteredTable);
	}


	/**
	 * Returns a distribution where all assignments whose probability falls below the 
	 * threshold are pruned out.
	 * 
	 * @param threshold the probability threshold
	 * @return the distribution with the subset of values
	 */
	public CategoricalTable getAboveThreshold(double threshold) {
		if (threshold < 0 || threshold > 1) {
			log.warning("invalid threshold: " + threshold);
		}
		CategoricalTable nbestTable = new CategoricalTable();
		for (Assignment a : table.keySet()) {
			if (table.get(a) >= threshold) {
				nbestTable.addRow(a, table.get(a));
			}
		}
		return nbestTable;
	}
	
	

	public Assignment getBest() {
		if (table.size() > 0) {
		CategoricalTable nbest = getNBest(1);
		return nbest.getRows().iterator().next();
		}
		else {
			log.warning("table is empty, cannot extract best value");
			return new Assignment();
		}
	}
	
	
	/**
	 * Returns the rows of the table.
	 * 
	 * @param range possible input values (is ignored)
	 * @return the table rows
	 */
	public Set<Assignment> getValues(ValueRange range) {
		return getPossibleValues();
	}
	
	
	/**
	 * Returns the rows of the table.
	 * 
	 * @return the table rows
	 */
	public Set<Assignment> getPossibleValues() {
		return getRows();
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
	/**	Map<String,Set<Value>> possibleHeadPairs = 
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
		} */

		// checks that the total probability is roughly equal to 1.0f
		double totalProb = countTotalProb() + getProb(Assignment.createDefault(headVars));
		if (totalProb < 0.9f || totalProb > 1.1f) {
			log.debug("total probability is " + totalProb);
			return false;
		}

		return true;
	}



	/**
	 * Returns a string representation of the probability table
	 */
	@Override
	public String toString() {

		Map<Assignment,Double> sortedTable = InferenceUtils.getNBest(table, table.size());

		String str = "";		
		for (Entry<Assignment,Double> entry : sortedTable.entrySet()) {
			String prob = StringUtils.getShortForm(entry.getValue());
			str += "P("+entry.getKey() + "):=" + prob + "\n";
		}

		return (str.length() > 0)? str.substring(0, str.length()-1) : str;
	}



	/**
	 * Returns a copy of the probability table
	 *
	 * @return the copy of the table
	 */
	@Override
	public CategoricalTable copy() {
		CategoricalTable tableCopy = new CategoricalTable();
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



	/**
	 * Returns the preferred distribution format for the content of the distribution
	 * 
	 * @return DISCRETE or CONTINUOUS
	 */
	@Override
	public DistribType getPreferredType() {
		if (headVars.size() == 1 && !table.keySet().isEmpty()) {
			String headVar = headVars.iterator().next();
			for (Assignment a : getRows()) {
				if (!(a.getValue(headVar) instanceof ArrayVal)
						&& !(a.getValue(headVar) instanceof DoubleVal) & 
						!(a.getValue(headVar) instanceof NoneVal)) {
					return DistribType.DISCRETE;
				}
			}
			if (getRows().size() > 1) {
				return DistribType.CONTINUOUS;
			}
		}
		return DistribType.DISCRETE;
	}

	@Override
	public Node generateXML(Document doc) throws DialException {
		if (headVars.size() != 1) {
			throw new DialException("XML representation can only be "
					+ "generated for table with one single head variable");
		}
		Element var = doc.createElement("variable");

		Attr id = doc.createAttribute("id");
		id.setValue(headVars.iterator().next().replace("'", ""));
		var.setAttributeNode(id);

		for (Assignment a : table.keySet()) {
			Element valueNode = doc.createElement("value");
			if (table.get(a) < 0.99) {
			Attr prob = doc.createAttribute("prob");
			prob.setValue(""+StringUtils.getShortForm(table.get(a)));
			valueNode.setAttributeNode(prob);
			}
			valueNode.setTextContent(""+a.getValue(headVars.iterator().next()));
			var.appendChild(valueNode);	
		}
		return var;
	}

}
