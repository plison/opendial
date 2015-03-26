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

package opendial.bn.distribs.incremental;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Intervals;
import opendial.datastructs.ValueRange;

/**
 * Representation of an incremental distribution -- that is, an distribution that is
 * built up of a connected graph of smaller units that can be incrementally inserted or 
 * modified at runtime.
 * 
 * <p>The incremental distribution is therefore composed of a set of IncrementalUnit 
 * objects connected with one another (where each connection is associated with a particular
 * probability). The values of the distribution correspond to the possible "paths" through
 * the graph, starting from units without any predecessors and ending with units without any
 * successors.  The probability of each path is defined as the product of the probabilities 
 * for each connection traversed by the path.
 * 
 * <p>The incremental distribution can be explicitly marked as "committed", which means that 
 * its content will not change anymore. 
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-16 17:34:31 #$
 */
public class IncrementalDistribution implements IndependentProbDistribution, DiscreteDistribution {

	// logger
	public static Logger log = new Logger("IncrementalDistribution", Logger.Level.DEBUG);

	// variable for the distribution
	String variable;
	
	// set of incremental units, indexed by their identifiers
	Map<Long,IncrementalUnit> units;	

	// whether the distribution is committed or not
	boolean committed = false;

	// equivalent N-best list for the distribution (cached)
	CategoricalTable nbests;

	

	// ===================================
	//  INCREMENTAL CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new incremental distribution, with a first incremental unit
	 * 
	 * @param firstUnit the first unit
	 */
	public IncrementalDistribution(IncrementalUnit firstUnit) {
		units = new HashMap<Long,IncrementalUnit>();
		this.variable = firstUnit.getVariable();
		addUnit(firstUnit);
	}

	
	/**
	 * Creates a new incremental distribution, with a set of incremental units
	 * 
	 * @param firstUnits the first incremental units
	 * @throws DialException if the collection is empty
	 */
	public IncrementalDistribution(Collection<IncrementalUnit> firstUnits) throws DialException {
		if (firstUnits.isEmpty()) {
			throw new DialException("incremental distribution must have at least one unit");
		}
		units = new HashMap<Long,IncrementalUnit>();
		this.variable = firstUnits.iterator().next().getVariable();
		addUnits(firstUnits);
	}


	/**
	 * Adds a new unit to the distribution
	 * 
	 * @param unit the unit to add
	 */
	public void addUnit(IncrementalUnit unit) {
		if (committed) {
			log.warning("distribution is already committed");
			return;
		}
		if (!unit.getVariable().equals(variable)) {
			log.warning(unit.getVariable() + " != " + variable);
		}
		units.put(unit.id, unit);
		nbests = null;
	}

	
	/**
	 * Adds a new collection of incremental units to the distribution
	 * 
	 * @param units the units to add
	 */
	public void addUnits(Collection<IncrementalUnit> units) {
		for (IncrementalUnit unit : units) {
			addUnit(unit);
		}
	}

	/**
	 * Sets the distribution as committed
	 * 
	 * @param committed true if the distribution should be committed, false otherwise
	 */
	public void setAsCommitted(boolean committed) {
		this.committed = committed;
	}

	

	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns whether if the distribution is committed or not
	 * 
	 * @return true if the distribution is committed, and false otherwise
	 */
	public boolean isCommitted() {
		return committed;
	}


	/**
	 * Returns a singleton with the variable label
	 */
	@Override
	public Collection<String> getHeadVariables() {
		return Arrays.asList(variable);
	}


	/**
	 * Samples a possible path through the distribution
	 * 
	 * @return the sampled assignment
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}

	
	/**
	 * Returns the N-Best list corresponding to the expansion of all possible
	 * paths for the distribution.
	 * 
	 * @return the categorical table corresponding to the N-Best list
	 */
	@Override
	public CategoricalTable toDiscrete() {
		if (nbests != null) {
			return nbests;
		}
		nbests = new CategoricalTable();

		for (Long start : getStartUnits()) {
			Map<Value,Double> paths = getPaths(start);
			for (Value path : paths.keySet()) {
				nbests.addRow(new Assignment(variable, path), paths.get(path));
			}
		}
		return nbests;
	}
	

	/**
	 * Returns "DISCRETE".
	 */
	@Override
	public DistribType getPreferredType() {
		return DistribType.DISCRETE;
	}

	/**
	 * Returns the current distribution
	 */
	@Override
	public ProbDistribution getPartialPosterior(Assignment condition)
			throws DialException {
		return this;
	}
	
	/**
	 * Returns the set of possible paths for the current distribution
	 * 
	 * @return the set of possible paths
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) throws DialException {
		return getValues();
	}
	
	

	/**
	 * Returns the probability P(head) in the N-best list that corresponds to the
	 * "flattened out" distribution over possible paths for the distribution
	 * 
	 * @param condition conditional assignment (ignored)
	 * @param head head assignment
	 * @return corresponding probability
	 */
	@Override
	public double getProb(Assignment condition, Assignment head)
			throws DialException {
		return toDiscrete().getProb(head);
	}

	/**
	 * Returns the N-best list for the distribution
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition)
			throws DialException {
		return toDiscrete();
	}
	
	

	/**
	 * Samples a possible path, starting from the units without predecessors and
	 * ending with the units without successors.
	 * 
	 * @return the sampled path
	 */
	@Override
	public Assignment sample() throws DialException {

		List<Long> startUnits = getStartUnits();
		long curId = startUnits.get((new Random()).nextInt(startUnits.size()));
		Value path = units.get(curId).getPayload();

		Map<Long,Double> nextUnits = getNextUnits(curId);
		while (!nextUnits.isEmpty()) {
			Map<Long, Double> weights = new HashMap<Long, Double>();
			double totalProb = 0.0;
			for (Long nextID : nextUnits.keySet()) {
				double prob = nextUnits.get(nextID);
				weights.put(nextID, prob);
				totalProb += prob;
			}
			if (totalProb < 1.0) {
				weights.put(0l, 1 - totalProb);
			}
			Intervals<Long> intervals = new Intervals<Long>(weights);
			curId = intervals.sample();
			if (curId == 0) {
				return new Assignment(variable, path);
			}
			
			// concatenate the values
			path = ValueFactory.concatenate(path, units.get(curId).payload);
			nextUnits = getNextUnits(curId);
		}
		return new Assignment(variable, path);
	}

	
	/**
	 * Throws an exception.
	 */
	@Override
	public ContinuousDistribution toContinuous() throws DialException {
		throw new DialException("cannot be converted to a continuous distribution");
	}

	/**
	 * Returns the set of possible paths for the distribution
	 */
	@Override
	public Set<Assignment> getValues() {
		return toDiscrete().getHeadValues();
	}


	// ===================================
	//  UTILITIES
	// ===================================
	
	/**
	 * Returns true if the distribution is well-formed, and false otherwise. The 
	 * distribution is well-formed if:<ul>
	 * <li>it contains no cycles
	 * <li> AND that the sum of probabilities for the connections leaving from a 
	 * particular unit are <= 1.0.
	 * 
	 * @return true if well-formed, false otherwise
	 */
	@Override
	public boolean isWellFormed() {
		Set<Long> seenIds = new HashSet<Long>();
		for (IncrementalUnit unit : units.values()) {
			if (seenIds.contains(unit.id)) {
				log.warning("cyclic dependency in the connected graph: " + unit.id);
				return false;
			}
			seenIds.add(unit.id);
			double totalProb = 0;
			Map<Long,Double> next = getNextUnits(unit.id);
			for (Long successID : next.keySet()) {
				totalProb += next.get(successID);
			}
			if (totalProb > 1.02) {
				log.debug("total probability going out of " + unit + " is " + totalProb);
				return false;
			}	
		}
		return true;
	}


	/**
	 * Modifies the variable label
	 * 
	 * @param oldId the old identifier to replace
	 * @param newId the new identifier
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		for (IncrementalUnit unit : units.values()) {
			if (unit.getVariable().equals(oldId)) {
				unit.setVariable(newId);
			}
		}
		if (variable.equals(oldId)) {
			variable = newId;
		}
		nbests = null;
	}


	/**
	 * Prunes from the connected graph all units whose total incoming probability
	 * is lower than the given threshold
	 * 
	 * @param threshold the probability threshold
	 */
	@Override
	public void pruneValues(double threshold) {
		for (IncrementalUnit unit : new HashSet<IncrementalUnit>(units.values())) {
			double totalProb = 0.0;
			for (long prev : unit.getPreviousUnits()) {
				totalProb += unit.getConnectionProbability(prev);
			}
			if (totalProb < threshold) {
				units.remove(unit);
			}
		}
	}


	/**
	 * Copies the distribution
	 * 
	 * @return the copy
	 */
	@Override
	public IncrementalDistribution copy() {
		try {
			IncrementalDistribution newDistrib = new IncrementalDistribution(units.values());
			newDistrib.committed = committed;
			return newDistrib;
		}
		catch (DialException e) {
			log.debug("should not happen: " + e);
			return null;
		}
	}


	/**
	 * Generates the XML specification for the N-best list of the distribution
	 */
	@Override
	public Node generateXML(Document document) throws DialException {
		return toDiscrete().generateXML(document);
	}


	/**
	 * Returns true if the object o is an incremental distribution with the same units
	 * as the current one, and returns false otherwise.
	 * 
	 * @return true if the two objects have an identical set of units, and false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof IncrementalDistribution) {
			IncrementalDistribution wo = (IncrementalDistribution)o;
			return (wo.getHeadVariables().equals(getHeadVariables()) && units.equals(wo.units));
		}
		return false;
	}

	/**
	 * Returns the hashcode for the distribution
	 */
	@Override
	public int hashCode() {
		return units.hashCode();
	}

	/**
	 * Returns the string representation of the distribution
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return toDiscrete().toString() + " (incrementally constructed graph)";
	}



	// ===================================
	//  PRIVATE METHODS
	// ===================================
	

	/**
	 * Returns all possible paths starting from the incremental unit indexed by the
	 * identifier "startID".  Each path is associated with a particular probability value.
	 * 
	 * <p>Note that if the total probability leaving from a particular unit is lower
	 * than 1, the procedure assumes that the remaining probability mass indicates the end
	 * of the path.
	 * 
	 * @param startId the identifier for the start unit
	 * @return the resulting set of possible paths
	 */
	private Map<Value,Double> getPaths(Long startId) {
		Map<Value,Double> results = new HashMap<Value,Double>();		
		if (!units.containsKey(startId)) {
			log.warning(startId + " is not contained in the distribution");
			return results;
		}
		Value startValue = units.get(startId).getPayload();
		if (getNextUnits(startId).isEmpty()) {
			results.put(startValue, 1.0);
		}
		else {
			Map<Long,Double> nextUnits = getNextUnits(startId);
			for (Long next : nextUnits.keySet()) {
				Map<Value,Double> future = getPaths(next);
				for (Value futureVal : future.keySet()) {
					Value concatenatedValue = ValueFactory.concatenate(startValue, futureVal);
					results.put(concatenatedValue, nextUnits.get(next) * future.get(futureVal));
				}
			}
		}
		return results;
	}


	/**
	 * Returns the set of identifiers for the start units in the distribution 
	 * (that is, the units without predecessors).
	 * 
	 * @return the set of identifiers for the start units
	 */
	private List<Long> getStartUnits() {
		List<Long> start = new LinkedList<Long>();
		for (IncrementalUnit unit : units.values()) {
			if (unit.getPreviousUnits().isEmpty()) {
				start.add(unit.id);
			}
		}
		return start;
	}

	
	/**
	 * Returns the set of units that are linked from unitId.
	 * 
	 * @param unitId the incremental unit
	 * @return the set of units following unitId
	 */
	private Map<Long,Double> getNextUnits(Long unitId) {
		Map<Long,Double> next = new HashMap<Long,Double>();
		for (IncrementalUnit other : units.values()) {
			Set<Long> previous = other.getPreviousUnits();
			if (previous.contains(unitId)) {
				next.put(other.id, other.getConnectionProbability(unitId));
			}
		}
		return next;
	}


}

