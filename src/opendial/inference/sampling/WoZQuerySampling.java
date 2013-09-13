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

import java.util.HashMap;
import java.util.Map;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.UtilQuery;

public class WoZQuerySampling extends AbstractQuerySampling {

	public static final double MIN_UTIL = -20;
	
	// logger
	public static Logger log = new Logger("WoZQuerySampling",
			Logger.Level.DEBUG);

	Assignment goldAction;
	SimpleEmpiricalDistribution distrib;
	

	public WoZQuerySampling(UtilQuery query, Assignment goldAction, int nbSamples, long maxSamplingTime) {
		super(query, nbSamples, maxSamplingTime);
		this.goldAction = goldAction;
	}

	@Override
	protected void compileResults() {
		try {
			Intervals<WeightedSample> intervals = resample();
			distrib = new SimpleEmpiricalDistribution();
			
			for (int i = 0 ; i < samples.size() ; i++) {
				WeightedSample sample = intervals.sample();
				distrib.addSample(sample.getSample().getTrimmed(query.getQueryVars()));
			}
			
		}
		catch (DialException e) {
			log.warning("sampling problem: " + e);
		}
	}


	private Intervals<WeightedSample> resample() throws DialException {

		Map<WeightedSample,Double> table = new HashMap<WeightedSample,Double>();

		synchronized(samples) {
			for (WeightedSample sample : samples) {
				double weight = sample.getWeight();
				if (sample.getSample().contains(goldAction)) {
					 weight *= (sample.getUtility() - MIN_UTIL);
				}
				else {
					weight *= 1/(sample.getUtility()-MIN_UTIL);
				}
				table.put(sample, weight);
			}
		}

		return new Intervals<WeightedSample>(table);	
	}

	
	public SimpleEmpiricalDistribution getResults() {
		return distrib;
	}
	
}

