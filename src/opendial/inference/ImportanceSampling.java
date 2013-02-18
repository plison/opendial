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

package opendial.inference;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.inference.datastructs.DistributionCouple;
import opendial.inference.datastructs.DoubleFactor;
import opendial.inference.queries.Query;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;
import opendial.inference.sampling.BasicQuerySampling;
import opendial.inference.sampling.ReductionQuerySampling;

/**
 * Inference algorithm for Bayesian networks based on normalised importance sampling 
 * (likelihood weighting).  The algorithm relies on the selection of samples from the
 * network, which are weighted according to the specified evidence.
 * 
 * TODO: make the sampling stop after a specific amount of time, instead of number of
 * samples.
 * 
 * <p>The algorithm is multi-threaded, to allow for the selection of several samples in 
 * parallel. The default number of samples is specified as an argument to the 
 * constructor.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ImportanceSampling extends AbstractInference implements InferenceAlgorithm {

	// logger
	public static Logger log = new Logger("ImportanceSampling", Logger.Level.DEBUG);

	// actual number of samples for the algorithm
	int nbSamples = Settings.getInstance().nbSamples;
	
	// maximum sampling time (in milliseconds)
	long maxSamplingTime = Settings.getInstance().maximumSamplingTime;
	
	
	/**
	 * Creates a new sampling algorithm with the default number of samples
	 * and maximum sampling time from ConfigurationSettings
	 */
	public ImportanceSampling() {	}
	
	
	/**
	 *  Creates a new sampling algorithm with the provided number of samples
	 *  
	 * @param nbSamples the number of samples to collect
	 */
	public ImportanceSampling(int nbSamples) {
		this.nbSamples = nbSamples;
	}
	
	
	/**
	 *  Creates a new sampling algorithm with the provided number of samples
	 *  and maximum sampling time
	 *  
	 * @param nbSamples the number of samples to collect
	 * @param maxSamplingTime the maximum sampling time (in milliseconds)
	 */
	public ImportanceSampling(int nbSamples, long maxSamplingTime) {
		this.nbSamples = nbSamples;
		this.maxSamplingTime = maxSamplingTime;
	}
	
	/**
	 * Changes the number of samples required per inference
	 * 
	 * @param nbSamples the new number of samples
	 */
	public void setNbOfSamples(int nbSamples) {
		this.nbSamples = nbSamples;
	}
	
	/**
	 * Sets the maximum sampling time (in milliseconds) per inference
	 * 
	 * @param maxSamplingTime the maximum sampling time, in milliseconds
	 */
	public void setMaximumSamplingTime(long maxSamplingTime) {
		this.maxSamplingTime = maxSamplingTime;
	}


	/**
	 * Performs an inference query on the network for the given query variables and the 
	 * evidence.  A set of nodes to ignore can also be given.
	 *
	 * @param query the full query
	 * @return the probability and utility distributions
	 */
	@Override
	protected DistributionCouple queryJoint(Query query) {
		
		// creates a new query thread
		BasicQuerySampling isquery = new BasicQuerySampling(query, nbSamples, maxSamplingTime);
		Thread t = new Thread(isquery);
		
		// waits for the results to be compiled
		synchronized (isquery) {
			t.start();
			while (isquery.getResults() == null) {
				try { isquery.wait();  }
				catch (InterruptedException e) {}
			}
		}
		return isquery.getResults();
	}


	@Override
	public BNetwork reduceNetwork(ReductionQuery query) throws DialException {

	//	log.debug("reduction query " + query + " on network " + query.getNetwork().getNodeIds());
		
		// creating the reduced copy
		BNetwork reduced = query.getNetwork().getReducedCopy(query.getQueryVars(), query.getNodesToIsolate());

		Set<String> identicalNodes = query.getNetwork().getIdenticalNodes(reduced, query.getEvidence());
		for (String nodeId : identicalNodes) {
			ChanceNode originalNode = query.getNetwork().getChanceNode(nodeId);
			Collection<BNode> inputNodesInReduced = reduced.getNode(nodeId).getInputNodes();
			Collection<BNode> outputNodesInReduced = reduced.getNode(nodeId).getOutputNodes();
			reduced.replaceNode(originalNode.copy());
			reduced.getNode(nodeId).addInputNodes(inputNodesInReduced);
			reduced.getNode(nodeId).addOutputNodes(outputNodesInReduced);
			query.removeQueryVar(nodeId);
		}  
				
		// creates a new query thread
		ReductionQuerySampling isquery = new ReductionQuerySampling(query, reduced, nbSamples, maxSamplingTime);
		Thread t = new Thread(isquery);

		// waits for the results to be compiled
		synchronized (isquery) {
			t.start();
			while (isquery.getResults() == null) {
				try { isquery.wait();  }
				catch (InterruptedException e) {}
			}
		}
		BNetwork result = isquery.getResults();
		return result;
	}



	public int getNbOfSamples() {
		return nbSamples;
	}



}
