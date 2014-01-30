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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
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
public class EquivalenceDistribution implements DiscreteDistribution {

	// logger
	public static Logger log = new Logger("EquivalenceDistribution", Logger.Level.DEBUG);

	// the variable label
	String variable;

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
		this.variable = variable;
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
		return new EquivalenceDistribution(variable);
	}

	
	/**
	 * Returns a string representation of the distribution
	 */
	@Override
	public String toString() {
		return "Equivalence(" + variable + ", " + variable+"^p)";
	}

	
	/**
	 * Replaces occurrences of the old variable identifier oldId with the new identifier
	 * newId.
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
	}

	
	/**
	 * Generates a sample from the distribution given the conditional assignment.
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		double prob = getProb(condition);

		if (sampler.nextDouble() < prob) {
			return new Assignment(createLabel(), true);
		}
		else {
			return new Assignment(createLabel(), false);
		}
	}


	/**
	 * Returns the identifier for the equivalence distribution
	 * 
	 * @return a singleton set with the equality identifier
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>(Arrays.asList(createLabel()));
		return headVars;
	}


	/**
	 * Returns the probability of P(head | condition). 
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the resulting probability 
	 */
	@Override
	public double getProb(Assignment condition, Assignment head) {
		try {
			double prob = getProb(condition);
			if (head.containsVar(createLabel()) && 
					head.getValue(createLabel()) instanceof BooleanVal) {
				boolean val = ((BooleanVal) head.getValue(createLabel())).getBoolean();
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
	 * Creates a categorical table for the posterior distribution P(head) given the
	 * conditional assignment in the argument.
	 * 
	 * @param condition the conditional assignment
	 * @return the posterior distribution
	 */
	@Override
	public CategoricalTable getPosterior(Assignment condition) throws DialException {
		double prob = getProb(condition);
		CategoricalTable table = new CategoricalTable();
		if (prob > 0) {
			table.addRow(new Assignment(createLabel(), true), prob);
		}
		if (prob < 1) {
			table.addRow(new Assignment(createLabel(), false), 1 - prob);
		}
		return table;
	}
	
	@Override
	public CategoricalTable getPartialPosterior(Assignment condition) throws DialException {
		return getPosterior(condition);
	}
	
	
	/**
	 * Returns a set of two assignments: one with the value true, and one with the value
	 * false.
	 * 
	 * @param range the set of possible input values (is ignored)
	 * @return the set with the two possible assignments
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		Set<Assignment> vals = new HashSet<Assignment>();
		vals.add(new Assignment(createLabel(), ValueFactory.create(true)));
		vals.add(new Assignment(createLabel(), ValueFactory.create(false)));
		return vals;
	}

	/**
	 * Returns the probability of eq=true given the condition
	 * @param condition the conditional assignment
	 * @return the probability of eq=true
	 * @throws DialException if the distribution is ill-formed
	 */
	private double getProb(Assignment condition) throws DialException {

	
		Assignment trimmed = condition.getTrimmed(variable+"^p");
		if (condition.containsVar(variable+"'")) {
			trimmed.addPair(variable+"'", condition.getValue(variable+"'"));
		}
		else if (condition.containsVar(variable)) {
			trimmed.addPair(variable, condition.getValue(variable));
		}
		if (trimmed.size() == 2) {	
			List<Value> valList = new ArrayList<Value>(trimmed.getValues());

			if (valList.get(0).equals(ValueFactory.none()) || 
					valList.get(1).equals(ValueFactory.none())) {	
				return NONE_PROB;
			}
			else {
				return (valList.get(0).equals(valList.get(1)))? 1.0 : 0.0;		
			}			
		}
		else {
			throw new DialException("equivalence distribution with variable " + 
					variable + " cannot handle condition " + trimmed);
		}
	}


	/**
	 * Returns DISCRETE
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
	 * Returns itself
	 */
	@Override
	public DiscreteDistribution toDiscrete() throws DialException {
		return this;
	}



	/**
	 * Create a label for the equivalence variable
	 * 
	 * @return the label
	 */
	private String createLabel() {
		return "=_" + variable;
	}

}
