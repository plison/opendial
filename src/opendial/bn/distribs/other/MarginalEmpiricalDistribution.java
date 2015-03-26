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

package opendial.bn.distribs.other;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.ConditionalCategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;

/**
 * Distribution defined "empirically" in terms of a set of samples on the relevant 
 * variables.  The distribution is derived as a marginal distribution on a subset 
 * of head and conditional variables.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 *
 */
public class MarginalEmpiricalDistribution implements ProbDistribution {

	// logger
	public static Logger log = new Logger("MarginalEmpiricalDistribution", Logger.Level.DEBUG);

	// list of samples for the empirical distribution
	EmpiricalDistribution empirical;

	// cache for the discrete and continuous distributions
	DiscreteDistribution discreteCache;
	ContinuousDistribution continuousCache;

	// the head variables
	Set<String> headVars;

	// the conditionalVars variables
	Set<String> condVars;

	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================


	/**
	 * Constructs a new empirical distribution, initially with a empty set of
	 * samples
	 */
	public MarginalEmpiricalDistribution(Collection<String> headVars, 
			Collection<String> condVars, EmpiricalDistribution empirical) {
		this.headVars = new HashSet<String>(headVars);
		this.condVars = new HashSet<String>(condVars);
		this.empirical = empirical;
	}



	/**
	 * Constructs a new empirical distribution, initially with a empty set of
	 * samples
	 */
	public MarginalEmpiricalDistribution(Collection<String> headVars, 
			EmpiricalDistribution empirical) {
		this.headVars = new HashSet<String>(headVars);
		this.condVars = new HashSet<String>();
		this.empirical = empirical;
	}



	/**
	 * Prunes the values of the underlying empirical distribution.
	 */
	@Override
	public void pruneValues(double threshold) {
		empirical.pruneValues(threshold);
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @return the selected sample
	 * @throws DialException 
	 */
	public Assignment sample() throws DialException {
		Assignment fullSample =  getFullSample();
		return fullSample.getTrimmed(headVars);
	}





	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment 
	 * @return the selected sample
	 * @throws DialException 
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		if (condition.isEmpty()) {
			return sample();
		}
		log.warning("sampling marginal distribution: is that necessary??");

		Assignment a = new Assignment();
		int nbLoops = 0;
		while (!a.contains(condition) && nbLoops < empirical.size()) {
			a = sample();
			nbLoops++;
		}
		if (nbLoops > 100) {
			log.warning("high number of loops in sampling from complex " +
					"probability distribution P("+ headVars + "| " + condVars+")");
		}
		if (nbLoops >= empirical.size()) {
			log.warning("could not find sample of correct condition : " + condition);
			log.debug("discrete table: " + toDiscrete());
		}
		return a.getTrimmed(headVars);
	}


	/**
	 * Returns the full sample for the distribution (without trimming the assignment
	 * to only retain the head variables).
	 * 
	 * @return the full sample
	 * @throws DialException
	 */
	public Assignment getFullSample() throws DialException {
		return empirical.sample();
	}


	/**
	 * Returns true is the collection of samples is not empty
	 */
	@Override
	public boolean isWellFormed() {
		return empirical.isWellFormed();
	}


	/**
	 * Returns the head variables for the distribution.
	 */
	@Override
	public Set<String> getHeadVariables() {
		return new HashSet<String>(headVars);
	}


	/**
	 * Returns the collection of samples.
	 * 
	 * @return the collection of samples
	 */
	public Collection<Assignment> getSamples() {
		return empirical.getSamples();
	}



	/**
	 * Returns the posterior distribution associated with the empirical distribution given
	 * the conditional assignment provided as argument.  This posterior distribution
	 * is either discrete or continuous (depending on the preferred format for the
	 * head variables).
	 */
	@Override
	public IndependentProbDistribution getPosterior(Assignment condition)
			throws DialException {
		if (getPreferredType() == DistribType.CONTINUOUS) {
			return toContinuous().getPosterior(condition);
		}
		else {
			return toDiscrete().getPosterior(condition);
		}
	}


	@Override
	public ProbDistribution getPartialPosterior (Assignment condition) {
		Set<String> newHeadVars = new HashSet<String>(headVars);
		newHeadVars.removeAll(condition.getVariables());
		Set<String> newCondVars = new HashSet<String>(condVars);
		newCondVars.removeAll(condition.getVariables());
		MarginalEmpiricalDistribution newDistrib = 
				new MarginalEmpiricalDistribution(newHeadVars, newCondVars, empirical);	
		return newDistrib;
	}

	public int size() {
		return empirical.size();
	}


	/**
	 * Returns the possible values for the head variables of the distribution.  For efficiency
	 * reason, the input values are ignored, and the distribution simply outputs the head variable
	 * values present in the samples.
	 * 
	 * @return the possible values for the head variables
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		return empirical.getValues(range);
	}




	public EmpiricalDistribution getFullDistrib() {
		return empirical;
	}


	// ===================================
	//  CONVERSION METHODS
	// ===================================


	/**
	 * Returns a discrete representation of the empirical distribution.
	 */
	@Override
	public DiscreteDistribution toDiscrete() {
		if (discreteCache == null) {
			if (condVars.isEmpty()) {
				this.discreteCache = empirical.createSimpleTable(headVars);
			}
			else {
				if (condVars.toString().contains("theta")) {
					log.warning("Discretising a table with parameters! Distribution is P(" + headVars + "|" + condVars + ")");
				}
				this.discreteCache = createConditionalTable();
			}	
		}
		return discreteCache;
	}



	/**
	 * Returns a continuous representation of the distribution (if there is no conditional 
	 * variables in the distribution).
	 * 
	 * @return the corresponding continuous distribution.
	 */
	public ContinuousDistribution toContinuous() throws DialException {
		if (continuousCache == null) {
			if (!condVars.isEmpty() || headVars.size() != 1) {
				throw new DialException ("cannot convert distribution to continuous for P(" + headVars + "|" + condVars+ ")");
			}
			String headVar = headVars.iterator().next();
			return empirical.createContinuousDistribution(headVar);	
		}
		return continuousCache;
	}


	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns a copy of the distribution
	 * 
	 * @return the copy
	 */
	@Override
	public MarginalEmpiricalDistribution copy() {
		MarginalEmpiricalDistribution copy = new MarginalEmpiricalDistribution(
				headVars, condVars, empirical);
		return copy;
	}


	@Override
	public boolean equals(Object o) {
		if (o instanceof MarginalEmpiricalDistribution 
				&& ((MarginalEmpiricalDistribution)o).getFullDistrib().equals(empirical)
				&& ((MarginalEmpiricalDistribution)o).headVars.equals(headVars)
				&& ((MarginalEmpiricalDistribution)o).condVars.equals(condVars)) {
			return true;
		}
		return false;
	}

	/**
	 * Returns a pretty print representation of the distribution: here, 
	 * tries to convert it to a discrete distribution, and displays its content.
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {	
		if (getPreferredType() == DistribType.CONTINUOUS) {
			try {
				return toContinuous().toString();
			}
			catch (DialException e) {
				log.debug("could not convert distribution to a continuous format: " + e);
			}
		}
		else if (getPreferredType() == DistribType.DISCRETE) {
			return toDiscrete().toString(); 
		}
		String defStr = "sample-based distribution P(" 
				+ headVars.toString().replace("[", "").replace("]", "");
		defStr += (!condVars.isEmpty())? "|" + condVars.toString().replace("[", "").replace("]", "") + ")" : ")";
		return defStr;
	}


	/**
	 * Replace a variable label by a new one
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {

		if (headVars.contains(oldId)) {
			headVars.remove(oldId);
			headVars.add(newId);
		}
		if (condVars.contains(oldId)) {
			condVars.remove(oldId);
			condVars.add(newId);
		}

		empirical.modifyVariableId(oldId, newId);

		if (discreteCache != null) {
			discreteCache.modifyVariableId(oldId, newId);
		}
		if (continuousCache != null) {
			continuousCache.modifyVariableId(oldId, newId);
		}
	}


	/**
	 * Returns the preferred representation format for the head variables (discrete or continuous).
	 */
	@Override
	public DistribType getPreferredType() {
		try {
			for (String var : getHeadVariables()) {
				Assignment a = empirical.sample().getTrimmed(headVars);
				if (a.containsVar(var) && a.containContinuousValues()) {
					if (getHeadVariables().size() == 1) {
						return DistribType.CONTINUOUS;
					}
					else {
						return  DistribType.HYBRID;
					}
				}		
			}
		}
		catch (DialException e) {
			log.warning("problem extracting distribution type: " + e);
		}


		return DistribType.DISCRETE;
	}




	/**
	 * Creates a conditional categorical table derived from the samples.  This method 
	 * should be called in the presence of conditional variables.
	 * 
	 * @return the conditional categorical table 
	 */
	private ConditionalCategoricalTable createConditionalTable() {

		ConditionalCategoricalTable table = new ConditionalCategoricalTable();

		Map<Assignment,EmpiricalDistribution> temp = 
				new HashMap<Assignment,EmpiricalDistribution>();

		for (Assignment sample: empirical.getSamples()) {
			Assignment condition = sample.getTrimmed(condVars);
			Assignment head = sample.getTrimmed(headVars);
			if (!temp.containsKey(condition)) {
				temp.put(condition, new EmpiricalDistribution());
			}

			temp.get(condition).addSample(head);
		}

		for (Assignment condition : temp.keySet()) {
			table.addRows(condition, (temp.get(condition).toDiscrete()));
		}
		table.fillConditionalHoles();
		return table;
	}


}
