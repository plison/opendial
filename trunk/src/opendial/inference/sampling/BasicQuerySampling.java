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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.values.ValueFactory;
import opendial.inference.datastructs.DistributionCouple;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.Query;


/**
 * Thread for an inference query based on importance sampling.  Once the thread has finished
 * running, a notify message is sent and the results can then be returned via the getResults() 
 * method.
 * 
 * <p>The inference is itself based on several threads, each of which collects samples used
 * to determine the distribution.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicQuerySampling extends AbstractQuerySampling {


	// the result (is null until the query is finished)
	DistributionCouple results;


	/**
	 * Creates a new sampling query with the given arguments
	 * 
	 * @param network the Bayesian network
	 * @param query the query to answer
	 * @param maxSamplingTime maximum sampling time (in milliseconds)
	 */
	public BasicQuerySampling(Query query,int nbSamples, long maxSamplingTime) {
		super(query, nbSamples, maxSamplingTime);
	}


	/**
	 * Compiles the results into probability/utility distribution
	 */
	@Override
	protected void compileResults() {
		Map<WeightedSample,Double> table = new HashMap<WeightedSample,Double>();
					
		synchronized(samples) {
		for (WeightedSample sample : samples) {
			double relativeWeight = sample.getWeight()/totalWeight;
			table.put(sample, relativeWeight);
		}
		}
		
		try {
		Intervals<WeightedSample> intervals = new Intervals<WeightedSample>(table);
	
		SimpleEmpiricalDistribution empiricalDistrib = new SimpleEmpiricalDistribution();
		UtilityTable utilityTable = new UtilityTable();
		while (empiricalDistrib.getSize() < samples.size()) {
				WeightedSample sample = intervals.sample();
				Assignment trimmedSample = sample.getSample().getTrimmed(query.getQueryVars());
				empiricalDistrib.addSample(trimmedSample);
				 
				utilityTable.incrementUtil(trimmedSample, sample.getUtility());
			} 
		results = new DistributionCouple(empiricalDistrib, utilityTable);
		}
		catch (DialException e) {
			log.warning("error compiling the results: " + e.toString());
		}
	}


	/**
	 * Returns the compiled results, if they are available. Else, returns
	 * null.
	 * 
	 * @return the distributions
	 */
	public DistributionCouple getResults() {
		return results;
	}

	
}
