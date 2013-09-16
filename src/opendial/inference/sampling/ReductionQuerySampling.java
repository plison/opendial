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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.empirical.ComplexEmpiricalDistribution;
import opendial.bn.distribs.empirical.EmpiricalDistribution;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;
import opendial.inference.ImportanceSampling;
import opendial.inference.datastructs.DistributionCouple;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.ReductionQuery;


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

	// logger
	public static Logger log = new Logger("ReductionQuerySampling", Logger.Level.DEBUG);

	
	// the result (is null until the query is finished)
	BNetwork reduced;
	
	boolean isFinished = false;


	/**
	 * Creates a new sampling query with the given arguments
	 * 
	 * @param network the Bayesian network
	 * @param query the query to answer
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 * @throws DialException 
	 */
	public ReductionQuerySampling(ReductionQuery query, int nbSamples, long maxSamplingTime) throws DialException {
		super(query, nbSamples, maxSamplingTime);
		this.reduced = query.getReducedCopy();
	}


	/**
	 * Compiles the results into probability/utility distribution
	 * @throws DialException 
	 */
	@Override
	protected void compileResults() {
		
		try {
			reweightSamples();	
			processClusters();
		}
		catch (DialException e) {
			log.warning("cannot compile sampling results: " + e.toString());
		}
		
		isFinished = true;
	}
	
	private void processClusters() throws DialException {
		
		List<Set<String>> clusters = reduced.getClusters();
		for (Set<String> cluster : clusters) {
			cluster.retainAll(query.getQueryVars());
			if (!cluster.isEmpty()) {
			List<Assignment> trimmedSamples = new ArrayList<Assignment>();
			for (WeightedSample a : samples) {
				trimmedSamples.add(a.getSample().getTrimmed(cluster));
			}
			
			if (trimmedSamples.isEmpty()) {
				log.warning("cannot estimate " + cluster + " (no relevant samples)");
				log.debug("query was: " + query + " and total nb. of samples " + samples.size());
			}
			
			SimpleEmpiricalDistribution distrib = new SimpleEmpiricalDistribution(trimmedSamples);
			if (cluster.size() == 1) {
				ChanceNode node = reduced.getChanceNode(cluster.iterator().next());
				node.setDistrib(distrib);
			}
			else {
				for (String var : cluster) {
					ChanceNode node = reduced.getChanceNode(var);
					ComplexEmpiricalDistribution cdistrib = new ComplexEmpiricalDistribution
							(Arrays.asList(var), node.getInputNodeIds(), distrib);
					node.setDistrib(cdistrib);
				}
			}
			}
		}
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
	
	

	
	/**
	 * Returns the compiled results, if they are available. Else, returns
	 * null.
	 * 
	 * @return the distributions
	 */
	public BNetwork getResults() {
		if (isFinished) {
		return reduced;
		}
		return null;
	}


}
