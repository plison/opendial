// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.state.distribs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.values.ListVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.effects.BasicEffect;
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
public class OutputDistribution implements ProbDistribution {

	// logger
	public static Logger log = new Logger("OutputDistribution", Logger.Level.DEBUG);

	// output variables	
	String baseVar;

	// primes attached to the variable label
	String primes;



	/**
	 * Creates the output distribution for the output variable label
	 * 
	 * @param var the variable name
	 */
	public OutputDistribution(String var) {
		this.baseVar = var.replace("'", "");
		this.primes = var.replace(baseVar, "");
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
	}



	/**
	 * Samples a particular value for the output variable.
	 * 
	 * @param condition the values of the parent (rule) nodes
	 * @return an assignment with the output value
	 * @throws DialException if no value could be sampled
	 */
	@Override
	public Value sample(Assignment condition) throws DialException {
		CategoricalTable result = getProbDistrib(condition);
		return result.sample();
	}
	
	
	
	/**
	 * Does nothing.
	 */
	@Override
	public void pruneValues(double threshold) {
		return;
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
	public double getProb(Assignment condition, Value head) {
		CategoricalTable result = getProbDistrib(condition);
		return result.getProb(head);
	}



	/**
	 * Fills the cache with the resulting table for the given condition
	 * 
	 * @param condition the condition for which to fill the cache
	 */
	@Override
	public CategoricalTable getProbDistrib(Assignment condition) {

		// creating the table
		CategoricalTable probTable = new CategoricalTable(baseVar+primes, false);

		// combining all effects
		List<BasicEffect> fullEffects = new ArrayList<BasicEffect>();
		for (Value inputVal : condition.getValues()) {
			if (inputVal instanceof Effect) {
				fullEffects.addAll(((Effect)inputVal).getSubEffects());
			}
		}
		Effect fullEffect = new Effect(fullEffects);

		Set<Value> setValues = fullEffect.getValues(baseVar, EffectType.SET);
		Set<Value> addValues = fullEffect.getValues(baseVar, EffectType.ADD);
		Set<Value> discardValues = fullEffect.getValues(baseVar, EffectType.DISCARD);

		// case 1 : add or remove effects
		if (!addValues.isEmpty() || !discardValues.isEmpty()) {

			Value previousValue = (!setValues.isEmpty())? 
					setValues.iterator().next() : condition.getValue(baseVar) ;
			
			ListVal addVal = ValueFactory.create(addValues);
			if (previousValue instanceof ListVal) {
				addVal.addAll((ListVal)previousValue);
			} 
			else if (!previousValue.equals(ValueFactory.none())) {
				addVal.add(previousValue);
			}
			addVal.removeAll(discardValues);
			probTable.addRow(addVal, 1.0);
		}
		
		// case 2 (most common): classical set operations
		else if (!setValues.isEmpty()) {
			for (Value v : setValues) {
				probTable.addRow(v, (1.0 / setValues.size()));
			}		
		}
		
		// case 3: set to none value
		else {
			probTable.addRow(ValueFactory.none(), 1.0);
		}

		return probTable;
	}

	/**
	 * Returns the probability table associated with the condition
	 * 
	 * @param condition the conditional assignment
	 * @return the resulting probability table
	 */
	@Override
	public MarginalDistribution getPosterior(Assignment condition)  {
		return new MarginalDistribution(this, condition);
	}



	/**
	 * Returns the possible outputs values given the input range in the parent nodes
	 * (probability rule nodes and previous version of the variable)
	 * 
	 * @param range the range of values for the parents
	 * @return the possible values for the output
	 */
	@Override
	public Set<Value> getValues(ValueRange range) {

		Set<Value> values = new HashSet<Value>();

		loop: for (String var : range.getVariables()) {
			for (Value val : range.getValues(var)) {
				if (val instanceof Effect) {

					Set<EffectType> types = ((Effect)val).getEffectTypes(baseVar);
					if (types.contains(EffectType.ADD) || types.contains(EffectType.DISCARD)) {
						for (Assignment condition : range.linearise()) {
							values.addAll(getProbDistrib(condition).getValues());
						}
						break loop;
					}

					Set<Value> setValues = ((Effect)val).getValues(baseVar, EffectType.SET);		
					values.addAll(setValues);
					if (setValues.isEmpty()) {
						values.add(ValueFactory.none());
					}
				}
				else if (var.equals(baseVar)) {
					values.add(val);
				}
			}
		}	

		if (values.isEmpty()) {
			values.add(ValueFactory.none());
		}
		return values;

	}


	/**
	 * Returns a singleton set with the label of the output
	 * 
	 * @return the singleton set with the output label
	 */
	@Override
	public String getVariable() {
		return baseVar+primes;
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}



	/**
	 * Returns a copy of the distribution
	 */
	@Override
	public OutputDistribution copy() {
		return new OutputDistribution(baseVar + primes);
	}



	/**
	 * Returns "(output)".
	 */
	@Override
	public String toString() {
		return "(output)";
	}




}
