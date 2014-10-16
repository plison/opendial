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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.MarginalDistribution;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;


/**
 * Representation of an equivalence distribution (see dissertation p. 78 for details) with 
 * two possible values: true or false. The distribution is essentially defined as:
 * 
 * <p>	P(eq=true | X, X^p) = 1 when X = X^p and != None
 *                          = NONE_PROB  when X = None or X^p = None
 *                          = 0 otherwise.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class EquivalenceDistribution implements ProbDistribution {

	// logger
	public static Logger log = new Logger("EquivalenceDistribution", Logger.Level.DEBUG);

	// the variable label
	String baseVar;

	// sampler
	Random sampler;

	// probability of the equivalence variable when X or X^p have a None value.
	public static double NONE_PROB = 0.02;


	/**
	 * Create a new equivalence node for the given variable.
	 * 
	 * @param variable the variable label
	 */
	public EquivalenceDistribution(String variable) {
		this.baseVar = variable;
		sampler = new Random();	
	}



	/**
	 * Does nothing
	 */
	@Override
	public void pruneValues(double threshold) {
		return;
	}

	/**
	 * Copies the distributioin
	 */
	@Override
	public EquivalenceDistribution copy() {
		return new EquivalenceDistribution(baseVar);
	}


	/**
	 * Returns a string representation of the distribution
	 */
	@Override
	public String toString() {
		String str= "Equivalence(" + baseVar + ", " + baseVar+"^p)";
		return str;
	}


	/**
	 * Replaces occurrences of the old variable identifier oldId with the new identifier
	 * newId.
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (baseVar.equals(oldId)) {
			baseVar = newId.replace("'", "");
		}
	}


	/**
	 * Generates a sample from the distribution given the conditional assignment.
	 */
	@Override
	public Value sample(Assignment condition) throws DialException {
		double prob = getProb(condition);

		if (sampler.nextDouble() < prob) {
			return ValueFactory.create(true);
		}
		else {
			return  ValueFactory.create(false);
		}
	}


	/**
	 * Returns the identifier for the equivalence distribution
	 * 
	 * @return a singleton set with the equality identifier
	 */
	@Override
	public String getVariable() {
		return "=_" + baseVar;
	}


	/**
	 * Returns the probability of P(head | condition). 
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability 
	 */
	@Override
	public double getProb(Assignment condition, Value head) {

		try {
			double prob = getProb(condition);
			if (head instanceof BooleanVal) {
				boolean val = ((BooleanVal)head).getBoolean();
				if (val) { 
					return prob;
				}
				else {
					return 1 - prob;
				}
			}
			log.warning("cannot extract prob for P(" + head + "|" + condition +")");
		} 
		catch (DialException e) { log.warning(e.toString()); } 
		return 0.0;
	}


	/**
	 * Returns a new equivalence distribution with the conditional assignment as
	 * fixed input.
	 */
	@Override
	public ProbDistribution getPosterior(Assignment condition) throws DialException {
		return new MarginalDistribution(this, condition);
	}

	
	/**
	 * Returns the categorical table associated with the conditional assignment.
	 * 
	 * @param condition the conditional assignment
	 * @return the corresponding categorical table on the true and false values
	 * @throws DialException if the table could not be extracted for the condition
	 */
	@Override
	public CategoricalTable getProbDistrib(Assignment condition) throws DialException {
		double positiveProb = getProb(condition);
		CategoricalTable table = new CategoricalTable(getVariable());
		table.addRow(true, positiveProb);
		table.addRow(false, 1- positiveProb);
		return table;
	}

	/**
	 * Returns a set of two assignments: one with the value true, and one with the value
	 * false.
	 * 
	 * @param range the set of possible input values (is ignored)
	 * @return the set with the two possible assignments
	 */
	@Override
	public Set<Value> getValues(ValueRange range) {
		Set<Value> vals = new HashSet<Value>();
		vals.add(ValueFactory.create(true));
		vals.add(ValueFactory.create(false));
		return vals;
	}


	/**
	 * Returns the probability of eq=true given the condition
	 * @param condition the conditional assignment
	 * @return the probability of eq=true
	 * @throws DialException if the distribution is ill-formed
	 */
	private double getProb(Assignment condition) throws DialException {

		Value[] coupledValues = getCoupledValues(condition);

		if (coupledValues[0].equals(ValueFactory.none()) || 
				coupledValues[1].equals(ValueFactory.none())) {	
			return NONE_PROB;
		}
		else if (coupledValues[0].equals(coupledValues[1])) {
			return 1.0;
		}
		else{
			return 0.0;		
		}			

	}


	/**
	 * Returns true.
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	

	private Value[] getCoupledValues(Assignment initialInput) throws DialException {

		Value[] coupledValues = new Value[2];
		for (String inputVar : initialInput.getVariables()) {
			if (inputVar.equals(baseVar+"^p")) {
				coupledValues[0] = initialInput.getValue(inputVar);
			}
			else if (inputVar.equals(baseVar+"'")) {
				coupledValues[1] = initialInput.getValue(inputVar);				
			}
			else if (inputVar.equals(baseVar) && !inputVar.contains(baseVar+"'")) {
				coupledValues[1] = initialInput.getValue(inputVar);								
			}
		}
		if (coupledValues[0]==null || coupledValues[1]==null) {
			throw new DialException("equivalence distribution with variable " + 
					baseVar + " cannot handle condition " + initialInput);
		}
		return coupledValues;
	}


}
