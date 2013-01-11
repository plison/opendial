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

package opendial.inference.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.empirical.DepEmpiricalDistribution;
import opendial.bn.distribs.empirical.EmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.inference.datastructs.DistributionCouple;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.Query;


/**
 * Thread for an inference query based on importance sampling, used to reduce a Bayesian Network
 * into a subset of its variables.
 * 
 * Once the thread has finished running, a notify message is sent and the results can then be 
 * returned via the getResults() method.
 * 
 * <p>The inference is itself based on several threads, each of which collects samples used
 * to determine the distribution.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ReductionQuerySampling extends AbstractQuerySampling {


	// the result (is null until the query is finished)
	BNetwork results;


	/**
	 * Creates a new sampling query with the given arguments
	 * 
	 * @param network the Bayesian network
	 * @param query the query to answer
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public ReductionQuerySampling(Query query,int nbSamples, long maxSamplingTime) {
		super(query, nbSamples, maxSamplingTime);
	}


	/**
	 * Compiles the results into probability/utility distribution
	 * @throws DialException 
	 */
	@Override
	protected void compileResults() {

		try {
			results = query.getNetwork().getReducedCopy(query.getQueryVars());

			reweightSamples();

			for (ChanceNode n : results.getChanceNodes()) {
				EmpiricalDistribution eDistrib = getNodeDistribution(n);
				n.setDistrib(eDistrib);
			}
			for (UtilityNode n : results.getUtilityNodes()) {
				UtilityTable eDistrib = getUtilityDistribution(n);
				n.setDistrib(eDistrib);
			}
		}
		catch (DialException e) {
			log.warning("cannot compile sampling results: " + e.toString());
			results = new BNetwork();
		}
		
		/** 
		EmpiricalDistribution empiricalDistrib = new EmpiricalDistribution();
		UtilityTable empiricalUtilityDistrib = new UtilityTable();

		while (empiricalDistrib.getSamples().size() < samples.size()) {
			try {
				WeightedSample sample = intervals.sample();
				Assignment trimmedSample = sample.getSample().getTrimmed(query.getQueryVars());
				empiricalDistrib.addSample(trimmedSample);
				empiricalUtilityDistrib.addUtility(trimmedSample, sample.getUtility());
			} 
			catch (DialException e) {
				log.warning("error compiling the results: " + e.toString());
			}
		} */

	}
	
	
	private void reweightSamples() throws DialException {
		Map<WeightedSample,Double> table = new HashMap<WeightedSample,Double>();
		synchronized(samples) {
			for (WeightedSample sample : samples) {
				double relativeWeight = sample.getWeight()/totalWeight;
				table.put(sample, relativeWeight);
			}
		}
		Intervals<WeightedSample> intervals = new Intervals<WeightedSample>(table);
		Stack<WeightedSample> reweightedSamples = new Stack<WeightedSample>();
		
		for (int i = 0 ; i < samples.size(); i++) {
			WeightedSample sample = intervals.sample();
			reweightedSamples.add(sample);
		}
		samples = reweightedSamples;
	}

	
	private EmpiricalDistribution getNodeDistribution(ChanceNode node) {

		EmpiricalDistribution eDistrib;
		if (node.getInputNodes().isEmpty()) {
			eDistrib = new EmpiricalDistribution();
		}
		else {
			eDistrib = new DepEmpiricalDistribution(Arrays.asList(node.getId()), node.getInputNodeIds());
		}

		Set<String> trimmedVariables = new HashSet<String>(Arrays.asList(node.getId()));
		trimmedVariables.addAll(node.getInputNodeIds());

		for (WeightedSample a : samples) {
			Assignment trimmedSample = a.getSample().getTrimmed(trimmedVariables);
			eDistrib.addSample(trimmedSample);
		}
		return eDistrib;
	}
	
	
	private UtilityTable getUtilityDistribution (UtilityNode node) {
		
		UtilityTable utilityTable = new UtilityTable();
		
		Set<String> trimmedVariables = new HashSet<String>(Arrays.asList(node.getId()));
		trimmedVariables.addAll(node.getInputNodeIds());

		for (WeightedSample a : samples) {
			Assignment trimmedSample = a.getSample().getTrimmed(trimmedVariables);
			utilityTable.addUtility(trimmedSample, a.getUtility());
		}
		return utilityTable;
	}

	
	/**
	 * Returns the compiled results, if they are available. Else, returns
	 * null.
	 * 
	 * @return the distributions
	 */
	public BNetwork getResults() {
		return results;
	}


	@Override
	public boolean isTerminated() {
		return (results != null);
	}



}
