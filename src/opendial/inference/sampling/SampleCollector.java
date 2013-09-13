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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.FuzzyDistribution;
import opendial.bn.distribs.empirical.DepEmpiricalDistribution;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.Query;

/**
 * A thread responsible for collecting samples from the Bayesian network.  The 
 * thread continues constructing new samples until the terminate() method is
 * called.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SampleCollector extends Thread {

	// logger
	public static Logger log = new Logger("SampleCollector", Logger.Level.DEBUG);

	// minimum threshold for particle weights
	public static double WEIGHT_THRESHOLD = 0.00001f;

	BNetwork network;
	List<BNode> sortedNodes;
	Assignment evidence;
	AbstractQuerySampling masterSampler;

	boolean isFinished = false;


	/**
	 * Constructs a new sampling thread 
	 * 
	 * @param topSampler the reference to the master sampler, which collects all samples
	 * @param network the Bayesian network
	 * @param sortedNodes the sorted nodes
	 * @param evidence the evidence
	 */
	public SampleCollector(AbstractQuerySampling topSampler, Query query) {
		
		this.masterSampler = topSampler;
		this.network = query.getNetwork();
		this.sortedNodes = query.getFilteredSortedNodes();
		Collections.reverse(this.sortedNodes);
		this.evidence = query.getEvidence();
	}

	/**
	 * Runs the sample collection procedure
	 */
	@Override
	public void run() {

		// continue until the thread is marked as finished
		while (!isFinished) {
			try {
				WeightedSample sample = new WeightedSample();
				
		//		sample.addAssign(getComboSamples());
			
				for (Iterator<BNode> it = sortedNodes.iterator(); 
				it.hasNext() && (sample.getWeight() > WEIGHT_THRESHOLD); ) {

					BNode n = it.next();

					// if the node is an evidence node and has no input nodes
					if (n.getInputNodeIds().isEmpty() && evidence.containsVar(n.getId())) {
						sample.addPoint(n.getId(), evidence.getValue(n.getId()));
					}
					else if (n instanceof ChanceNode) {
						// if the node is a chance node and is not evidence, sample from the values
						if (!evidence.containsVar(n.getId())) {
							if (!sample.getSample().containsVar(n.getId())) {
							Value newVal = ((ChanceNode)n).sample(sample.getSample());
							sample.addPoint(n.getId(), newVal);
							}						
						}
 
						// if the node is an evidence node, recompute the weights
						else {
							Value evidenceValue = evidence.getValue(n.getId());
							double evidenceProb = 1.0;
							if (((ChanceNode)n).getDistrib() instanceof ContinuousProbDistribution) {
								Assignment trimmedInput = sample.getSample().getTrimmed(n.getInputNodeIds());
								evidenceProb = 1 + (50 * ((ContinuousProbDistribution)((ChanceNode)n).getDistrib()).
										getProbDensity(trimmedInput, evidence));
							}
							else {
								evidenceProb = ((ChanceNode)n).getProb(sample.getSample(), evidenceValue);								
							}
							sample.addLogWeight(Math.log(evidenceProb));						
							sample.addPoint(n.getId(), evidenceValue);
						}
						
					}

					// if the node is an action node
					else if (n instanceof ActionNode) {
						if (!evidence.containsVar(n.getId()) && 
								n.getInputNodeIds().isEmpty()) {
							Value newVal = ((ActionNode)n).sample(sample.getSample());
							sample.addPoint(n.getId(), newVal);
						}
						else {
							Value evidenceValue = evidence.getValue(n.getId());
							double evidenceProb = ((ActionNode)n).getProb(evidenceValue);
							sample.addLogWeight(Math.log(evidenceProb));						
							sample.addPoint(n.getId(), evidenceValue);
						}

					}

					// finally, if the node is a utility node, calculate the utility
					else if (n instanceof UtilityNode) {
						double newUtil = ((UtilityNode)n).getUtility(sample.getSample());
						sample.addUtility(newUtil);
					}
					else {
						log.debug("this is a case that we haven't predicted: " + n);
					}
				}

				// we only add the sample if the weight is larger than a given threshold
				if (sample.getWeight() > WEIGHT_THRESHOLD) {
					masterSampler.addSample(sample);
				}
				else {
			//		log.debug("discarding sample");
				}
			}
			catch (DialException e) {
				log.info("exception caught: " + e);
			}
		}
	}
	
	
	
	private Assignment getComboSamples() throws DialException {
		
		Assignment comboSample = new Assignment(evidence);
		
		List<String> already = new LinkedList<String>();
		for (BNode n : sortedNodes) {
			if (n instanceof ChanceNode && ((ChanceNode)n).getDistrib() 
					instanceof DepEmpiricalDistribution) {

				Assignment discreteCondition = comboSample.getDiscrete();
				Assignment sample = ((ChanceNode)n).getDistrib().sample(discreteCondition);
	
				comboSample.addAssignment(sample);
				already.add(n.getId());
			}
		}
	/**	if (!comboSample.isEmpty()) {
			log.debug("combo sample: " + comboSample);
		} */
		return comboSample;
	}


	/**
	 * Terminate the sampling thread
	 */
	public void terminate () {
		isFinished = true;
		interrupt();
	}
}
