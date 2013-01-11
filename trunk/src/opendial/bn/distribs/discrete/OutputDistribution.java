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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Output;


/**
 * HERE, THE SAMPLING IS TERRIBLY INEFFICIENT!
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class OutputDistribution implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("OutputDistribution", Logger.Level.DEBUG);

	// output variables	
	String outputVar;
	
	// primes attached to the variable label
	String primes;

	// cache for the probability distribution
	Map<Assignment, SimpleTable> cache;



	public OutputDistribution(String var) {
		this.outputVar = var.replace("'", "");
		this.primes = var.replace(outputVar, "");
		cache = new HashMap<Assignment,SimpleTable>();
	}

	/**
	 *
	 * @return
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	/**
	 *
	 * @return
	 */
	@Override
	public OutputDistribution copy() {
		return new OutputDistribution(outputVar);
	}

	/**
	 *
	 * @return
	 */
	@Override
	public String prettyPrint() {
		return "output distrib";
	}

	/**
	 *
	 * @param oldId
	 * @param newId
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		if ((outputVar+primes).equals(oldId)) {
			this.outputVar = newId.replace("'", "");
			this.primes = newId.replace(outputVar, "");
		}
		cache.clear();
	}

	/**
	 *
	 * @param condition
	 * @return
	 * @throws DialException
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {	
		synchronized(cache) {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}	
		return cache.get(condition).sample();
		}
	}

	/**
	 * Returns itself.
	 * 
	 * @return itself
	 */
	@Override
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}

	
	/**
	 * Returns a continuous version of the output distribution (based on a conversion
	 * to a discrete probability table).
	 * 
	 * <p>NB: the method works based on the values that have already been cached.
	 *
	 * @return the continuous distribution.
	 */
	@Override
	public ContinuousProbDistribution toContinuous() {
		DiscreteProbabilityTable probTable = new DiscreteProbabilityTable();
		for (Assignment condition : cache.keySet()) {
			probTable.addRows(condition, cache.get(condition));
		}
		return probTable.toContinuous();
	}

	/**
	 * Returns the probability associated with the given conditional and head
	 * assignments.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}	
		return cache.get(condition).getProb(head);
	}

	
	/**
	 * Returns true if a probability table is defined for the given conditional 
	 * and head assignments, and false otherwise
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return true if the probability is defined, false otherwise
	 */
	@Override
	public boolean hasProb(Assignment condition, Assignment head) {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}	
		return cache.get(condition).hasProb(head);
	}



	/**
	 * Returns the probability table associated with the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the resulting probability table
	 */
	@Override
	public SimpleTable getProbTable(Assignment condition)  {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}	
		return cache.get(condition);
	}


	/**
	 * Fills the cache with the resulting table for the given condition
	 * 
	 * @param condition the condition for which to fill the cache
	 */
	private synchronized void fillCacheForCondition(Assignment condition) {

		// creating the table
		SimpleTable probTable = new SimpleTable();

		// extracting the previous value for the variable (if any)
		Value previousValue = ValueFactory.none();
		String previousLabel = getPreviousLabel();
		if (condition.containsVar(previousLabel)) {
			previousValue = condition.getValue(previousLabel);
		}
		
		// extracting the information from the outputs
		Set<Value> setValues = new HashSet<Value>();
		Set<Value> addValues = new HashSet<Value>();
		Set<Value> discardValues = new HashSet<Value>();

		for (Value inputVal : condition.getValues()) {
			if (inputVal instanceof Output) {
				Output output = (Output)inputVal;
				Value setValue = output.getSetValue(outputVar);
				if (!setValue.equals(ValueFactory.none())) {
					setValues.add(output.getSetValue(outputVar));
				}
				addValues.addAll(output.getValuesToAdd(outputVar));
				discardValues.addAll(output.getValuesToDiscard(outputVar));
				if (output.mustBeCleared(outputVar)) {
					previousValue = ValueFactory.none();
				}
			}
		}
		setValues.removeAll(discardValues);

		// if the output is made from set values, create the table with
		// a uniform probability on each alternative
		if (!setValues.isEmpty()) {
			for (Value v : setValues) {
				probTable.addRow(new Assignment(outputVar+primes, v), 1.0 / setValues.size());
			}
		}
		// if the output is made from add values, concatenate the results
		else if (!addValues.isEmpty()) {
			SetVal addValue = ValueFactory.create(addValues);
			if (previousValue instanceof SetVal) {
				addValue.addAll((SetVal)previousValue);
			}
			probTable.addRow(new Assignment(outputVar+primes, addValue), 1.0);
		}
		
		// else, let the value reflect the previous one (or none)
		else {
			probTable.addRow(new Assignment(outputVar+primes, previousValue), 1.0);
		} 
		
		cache.put(new Assignment(condition), probTable);
	}
	
	
	private String getPreviousLabel() {
		if (!primes.isEmpty()) {
			return outputVar+primes.substring(0, primes.length()-1);
		}
		else {
			return outputVar+"^o";
		}
	}

}
