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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Distribution defined "empirically" in terms of a set of samples on the relevant 
 * variables.  This distribution can then be explicitly converted into a table 
 * or a continuous distribution (depending on the variable type).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class EmpiricalDistribution implements IndependentProbDistribution {

	// logger
	public static Logger log = new Logger("EmpiricalDistribution", Logger.Level.DEBUG);

	// list of samples for the empirical distribution
	protected List<Assignment> samples;

	// whether to use KDE for continuous distributions
	public static boolean USE_KDE = false;

	// random sampler
	Random sampler;

	// cache for the discrete and continuous distributions
	DiscreteDistribution discreteCache;
	ContinuousDistribution continuousCache;

	Set<String> variables;

	DistribType preferredType;
	boolean pruned = false;

	// ===================================
	//  CONSTRUCTION METHODS
	// ===================================

	/**
	 * Constructs an empirical distribution with an empty set of samples
	 */
	public EmpiricalDistribution() {
		this.samples = new ArrayList<Assignment>();
		this.variables = new HashSet<String>();
		sampler = new Random();
	}

	/**
	 * Constructs a new empirical distribution with a set of samples
	 * samples
	 * 
	 * @param samples the samples to add
	 */
	public EmpiricalDistribution(Collection<? extends Assignment> samples) {
		this();
		for (Assignment a : samples) {
			addSample(a);
		}
	}


	/**
	 * Adds a new sample to the distribution
	 * 
	 * @param sample the sample to add
	 */
	public void addSample(Assignment sample) {
		samples.add(sample);
		discreteCache = null;
		continuousCache = null;	
		variables.addAll(sample.getVariables());
		pruned = false;
	}


	/**
	 * Prunes all samples that contain a value whose relative frequency is below the
	 * threshold specified as argument.  DoubleVal and ArrayVal are ignored.
	 * 
	 * @param threshold the frequency threshold
	 */
	@Override
	public void pruneValues(double threshold) {
		if (pruned) {
			return;
		}
		Map<String,Map<Value, Integer>> frequencies = new HashMap<String,Map<Value,Integer>>();
		for (Assignment sample : samples) {
			for (String var : sample.getVariables()) {
				Value val = sample.getValue(var);
				if (!(val instanceof DoubleVal) && !(val instanceof ArrayVal)) {
					if (!frequencies.containsKey(var)) {
						frequencies.put(var, new HashMap<Value,Integer>());
					}
					Map<Value,Integer> valFreq = frequencies.get(var);
					if (!valFreq.containsKey(val)) {
						valFreq.put(val, 1);
					}
					else {
						valFreq.put(val, valFreq.get(val) + 1);
					}
				}
			}
		}
		List<Assignment> newSamples = new ArrayList<Assignment>();
		sampleLoop : 
			for (Assignment sample : samples) {
				for (String var : sample.getVariables()) {
					if (frequencies.containsKey(var) && 
							frequencies.get(var).get(sample.getValue(var)) < samples.size()*threshold) {
						continue sampleLoop;
					}
				}
				newSamples.add(sample);
			}
		samples = newSamples;
		discreteCache = null;
		continuousCache = null;
		pruned = true;
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
	@Override
	public Assignment sample() throws DialException {

		if (preferredType == null) {
			preferredType = getPreferredType();
		}

		if (USE_KDE && preferredType == DistribType.CONTINUOUS) {
			return toContinuous().sample();
		}

		if (!samples.isEmpty()) {
			int selection = sampler.nextInt(samples.size());
			Assignment selected = samples.get(selection);
			return selected;
		}
		else {
			log.warning("distribution has no samples");
			return new Assignment();
		}	
	}


	/**
	 * Samples from the distribution.  In this case, simply selects one
	 * arbitrary sample out of the set defining the distribution
	 * 
	 * @param condition the conditional assignment (ignored here)
	 * @return the selected sample
	 * @throws DialException 
	 */
	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return sample();
	}

	
	
	public Assignment getCompatibleSample(Assignment evidence) throws DialException {
		for (int i = 0 ; i < 10 ; i++) {
			Assignment sampled = sample();
			if (sampled.consistentWith(evidence)) {
				return sampled;
			}
		}
		Collections.shuffle(samples);
		for (Assignment sample : samples) {
			if (sample.consistentWith(evidence)) {
				return sample;
			}
		}
		throw new DialException("no sample consistent with " + evidence + " for " + toString());
	}

	/**
	 * Returns true is the collection of samples is not empty
	 */
	@Override
	public boolean isWellFormed() {
		return !samples.isEmpty();
	}


	/**
	 * Returns the head variables for the distribution.
	 */
	@Override
	public Collection<String> getHeadVariables() {
		return new HashSet<String>(variables);
	}


	/**
	 * Returns the collection of samples.
	 * 
	 * @return the collection of samples
	 */
	public Collection<Assignment> getSamples() {
		return samples;
	}



	/**
	 * Returns the posterior distribution associated with the empirical distribution given
	 * the conditional assignment provided as argument.  This posterior distribution
	 * is either discrete or continuous (depending on the preferred format for the
	 * head variables).
	 */
	@Override
	public EmpiricalDistribution getPosterior(Assignment condition)  {
		EmpiricalDistribution newDistrib = new EmpiricalDistribution();
		Set<String> conditionVars = condition.getVariables();
		for (Assignment sample : samples) {
			if (sample.consistentWith(condition)) {
				newDistrib.addSample(sample.getTrimmedInverse(conditionVars));
			}
		}
		return newDistrib;
	}
	
	
	@Override
	public EmpiricalDistribution getPartialPosterior(Assignment condition) {
		return getPosterior(condition);
	}


	public int size() {
		return samples.size();
	}


	/**
	 * Returns the possible values for the head variables of the distribution.  
	 * 
	 * @return the possible values for the head variables
	 */
	@Override
	public Set<Assignment> getValues() {
		Set<Assignment> possible = new HashSet<Assignment>();
		for (Assignment sample : samples) {
			possible.add(sample);
		}
		return possible;
	}


	/**
	 * Returns the possible values for the head variables of the distribution.  For efficiency
	 * reason, the input values are ignored, and the distribution simply outputs the head variable
	 * values present in the samples.
	 * 
	 * @param range the range of input values for the conditional variables (is ignored)
	 * @return the possible values for the head variables
	 */
	@Override
	public Set<Assignment> getValues(ValueRange range) {
		return getValues();
	}

	// ===================================
	//  CONVERSION METHODS
	// ===================================


	/**
	 * Returns a discrete representation of the empirical distribution.
	 */
	@Override
	public CategoricalTable toDiscrete() {
		return createSimpleTable(variables);
	}


	/**
	 * Returns a continuous representation of the distribution (if there is no conditional 
	 * variables in the distribution).
	 * 
	 * @return the corresponding continuous distribution.
	 */
	@Override
	public ContinuousDistribution toContinuous() throws DialException {
		if (continuousCache == null) {
			if (variables.size() != 1) {
				throw new DialException ("cannot convert distribution to continuous for P(" + variables +  ")");
			}
			String headVar = variables.iterator().next();
			continuousCache = createContinuousDistribution(headVar);
		}
		return continuousCache;
	}


	/**
	 * Creates a categorical table with the define subset of variables
	 * 
	 * @param headVars the subset of variables to include in the table
	 * @return the resulting table
	 */
	protected CategoricalTable createSimpleTable(Set<String> headVars) {

		CategoricalTable table = new CategoricalTable();

		Map<Assignment, Integer> counts = new HashMap<Assignment,Integer>();

		for (Assignment sample : samples) {
			Assignment trimmed = sample.getTrimmed(headVars);
			if (counts.containsKey(trimmed)) {
				counts.put(trimmed, counts.get(trimmed) + 1);
			}
			else {
				counts.put(trimmed,1);
			}
		}
		for (Assignment value : counts.keySet()) {
			table.addRow(value, 1.0 * counts.get(value) / samples.size());
		}
		
		if (!table.isWellFormed()) {
			log.warning("table created by discretising samples is not well-formed");
		}
		return table;
	}


	/**
	 * Creates a continuous distribution for the provided variable
	 * 
	 * @param variable the variable
	 * @return the resulting continuous distribution
	 */
	protected ContinuousDistribution createContinuousDistribution(String variable) {

		List<Double[]> values = new ArrayList<Double[]>();
		for (Assignment a : samples) {
			Value v = a.getValue(variable);
			if (v instanceof ArrayVal) {
				values.add(((ArrayVal)v).getArray());
			}
			else if (v instanceof DoubleVal) {
				values.add(new Double[]{((DoubleVal)v).getDouble()});
			}
		}

		return new ContinuousDistribution(variable, new KernelDensityFunction(values));
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
	public EmpiricalDistribution copy() {
		EmpiricalDistribution copy = new EmpiricalDistribution(samples);
		return copy;
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
		return toDiscrete().toString(); 
	}


	/**
	 * Replace a variable label by a new one
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	@Override
	public void modifyVariableId(String oldId, String newId) {

		if (variables.contains(oldId)) {
			variables.remove(oldId);
			variables.add(newId);
		}

		for (Assignment a : samples) {
			if (a.containsVar(oldId)) {
				Value v = a.removePair(oldId);
				a.addPair(newId, v);
			}
		}

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

		for (String var : getHeadVariables()) {
			Assignment a = samples.get(0);
			if (a.containsVar(var) && a.containContinuousValues()) {
				if (getHeadVariables().size() == 1) {
					return DistribType.CONTINUOUS;
				}
				else {
				return  DistribType.HYBRID;
				}
			}		
		}


		return DistribType.DISCRETE;
	}

	
	@Override
	public Node generateXML(Document document) throws DialException {
		if (getPreferredType() == DistribType.CONTINUOUS) {
			try {
				return toContinuous().generateXML(document);
			}
			catch (DialException e) {
				log.debug("could not convert distribution to a continuous format: " + e);
			}
		}
		return toDiscrete().generateXML(document); 
	}





}
