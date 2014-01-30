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

package opendial.state.distribs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;


/**
 * Representation of an output distribution (see Pierre Lison's PhD thesis, page 70
 * for details), which is a reflection of the combination of effects specified in the 
 * parent rules.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class OutputDistribution implements DiscreteDistribution {

	// logger
	public static Logger log = new Logger("OutputDistribution", Logger.Level.DEBUG);

	// output variables	
	String baseVar;

	// primes attached to the variable label
	String primes;

	// cache for the probability distribution
	Map<Assignment, CategoricalTable> cache;


	/**
	 * Creates the output distribution for the output variable label
	 * @param var
	 */
	public OutputDistribution(String var) {
		this.baseVar = var.replace("'", "");
		this.primes = var.replace(baseVar, "");
		cache = new HashMap<Assignment,CategoricalTable>();
	}


	/**
	 * Modifies the label of the output variable.
	 * 
	 * @param oldId the old label
	 * @param newId the new label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if ((baseVar+primes).equals(oldId)) {
			this.baseVar = newId.replace("'", "");
			this.primes = newId.replace(baseVar, "");
		}
		cache.clear();
	}



	/**
	 * Samples a particular value for the output variable.
	 * 
	 * @param condition the values of the parent (rule) nodes
	 * @return an assignment with the output value
	 * @throws DialException if no value could be sampled
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
	 * Returns the probability table associated with the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the resulting probability table
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition)  {
		if (!cache.containsKey(condition)) {
			fillCacheForCondition(condition);
		}	
		return cache.get(condition);
	}


	/**
	 * Returns the probability table associated with the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the resulting probability table
	 */
	@Override
	public CategoricalTable getPartialPosterior(Assignment condition)  {	
		return getPosterior(condition);
	}


	/**
	 * Returns the possible outputs values given the input range in the parent nodes
	 * (probability rule nodes and previous version of the variable)
	 * 
	 * @param range the range of values for the parents
	 * @return the possible values for the output
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {

		// check whether the parents contain add or discard operations
		boolean containsAddOrDiscard = false;
		for (String var : range.getVariables()) {
			for (Value val : range.getValues(var)) {
				if (val instanceof Effect) {
					if (!((Effect)val).getValues(baseVar, EffectType.ADD).isEmpty()
							|| !((Effect)val).getValues(baseVar, EffectType.DISCARD).isEmpty()) {
						containsAddOrDiscard = true;
					}
				}
			}
		}

		Set<Assignment> result = new HashSet<Assignment>();

		// if the parents do not contain add or discard, the extraction of values can be 
		// performed efficiently
		if (!containsAddOrDiscard) {
			for (String var : range.getVariables()) {
				for (Value val : range.getValues(var)) {
					if (val instanceof Effect) {
						Set<Value> setValues = ((Effect)val).getValues(baseVar, EffectType.SET);
						for (Value v : setValues) {
							result.add(new Assignment(baseVar+primes, v));
						}
						if (setValues.isEmpty()) {
							result.add(new Assignment(baseVar+primes, ValueFactory.none()));
						}
					}
					else if (var.equals(baseVar + ((!primes.isEmpty())? primes.substring(0, primes.length()-1): ""))) {
						result.add(new Assignment(baseVar+primes, val));
					}
				}
			}	
			if (result.isEmpty()) {
				result.add(new Assignment(baseVar+primes, ValueFactory.none()));
			}
		}

		// else, we generate all possible conditions
		else {
			Set<Assignment> conditions = range.linearise();
			for (Assignment condition : conditions) {
				CategoricalTable table = getPosterior(condition);
				result.addAll(table.getRows());
			}
		}

		return result;
	}


	/**
	 * Returns a singleton set with the label of the output
	 * 
	 * @return the singleton set with the output label
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>(Arrays.asList(baseVar+primes));
		return headVars;
	}



	/**
	 * Returns discrete.
	 */
	@Override
	public DistribType getPreferredType() {
		return DistribType.DISCRETE;
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	/**
	 * Returns itself.
	 */
	@Override
	public DiscreteDistribution toDiscrete() throws DialException {
		return this;
	}




	/**
	 * Returns a copy of the distribution
	 */
	@Override
	public OutputDistribution copy() {
		return new OutputDistribution(baseVar + primes);
	}



	/**
	 * Does nothing.
	 */
	@Override
	public void pruneValues(double threshold) {
		return;
	}

	/**
	 * Returns "(output)".
	 */
	@Override
	public String toString() {
		return "(output)";
	}


	/**
	 * Fills the cache with the resulting table for the given condition
	 * 
	 * @param condition the condition for which to fill the cache
	 */
	private void fillCacheForCondition(Assignment condition) {

		// creating the table
		CategoricalTable probTable = new CategoricalTable();

		// combining all effects
		Effect combinedEffect = new Effect();
		for (Value inputVal : condition.getValues()) {
			if (inputVal instanceof Effect) {
				combinedEffect.addSubEffects(((Effect)inputVal).getSubEffects());
			}
		}

		Set<Value> setValues = combinedEffect.getValues(baseVar, EffectType.SET);
		Set<Value> addValues = combinedEffect.getValues(baseVar, EffectType.ADD);
		Set<Value> discardValues = combinedEffect.getValues(baseVar, EffectType.DISCARD);

		Value previousValue = (!combinedEffect.getClearVariables().contains(baseVar))?
				condition.getValue(baseVar) : ValueFactory.none();

		// case 1: at least one effect is a classical set operation
		if (!setValues.isEmpty()) {
			for (Value v : setValues) {
				probTable.addRow(new Assignment(baseVar+primes, v), (1.0 / setValues.size()));
			}		
		}

		// case 2: operations on set (add / removal)
		else if (!addValues.isEmpty() || !discardValues.isEmpty()) {

			SetVal addVal = ValueFactory.create(addValues);
			if (previousValue instanceof SetVal) {
				addVal.addAll((SetVal)previousValue);
			} 
			else if (!previousValue.equals(ValueFactory.none())) {
				addVal.add(previousValue);
			}
			addVal.removeAll(discardValues);
			probTable.addRow(new Assignment(baseVar+primes, addVal), 1.0);
		}
		
		// case 3: backtrack to previous value
		else {
			probTable.addRow(new Assignment(baseVar+primes, previousValue), 1.0);
		}


		cache.put(new Assignment(condition), probTable);
	}



}
