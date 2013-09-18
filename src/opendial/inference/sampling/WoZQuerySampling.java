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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.datastructs.Intervals;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.inference.datastructs.WeightedSample;
import opendial.inference.queries.UtilQuery;

public class WoZQuerySampling extends AbstractQuerySampling {

	public static final double MAX = 50;
	
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

		Map<Assignment,Double> averages = getAverages();
		
		if (averages.size() == 1) {
			for (WeightedSample sample : samples) {
				table.put(sample, sample.getWeight());
			}
			return new Intervals<WeightedSample>(table);	
		}
		
		Assignment bestNotGold = new Assignment();
		for (Assignment a : averages.keySet()) {
			if (!a.equals(goldAction) && (bestNotGold.isEmpty() || averages.get(a) > averages.get(bestNotGold))) {
				bestNotGold = a;
			}
		}
		log.debug("Utility averages : " + (new UtilityTable(averages)).prettyPrint().replace("\n", ", "));
		log.debug(" ==> gold action = " + goldAction);
		
		synchronized(samples) {
			
			for (WeightedSample sample : samples) {
				double weight = sample.getWeight();
				if (sample.getUtility() < -15 || sample.getUtility() > 25) {
					weight = 0;
				}
				Assignment action = sample.getSample().getTrimmed(goldAction.getVariables());
				if (action.equals(goldAction)  && sample.getUtility() <= averages.get(bestNotGold)) {
					double distance = averages.get(bestNotGold) - sample.getUtility();
					double distance2 = Math.max(MAX/4, distance);
						weight *= Math.abs(MAX - distance2) / MAX;
				}
				else if (!action.equals(goldAction) && sample.getUtility() >= averages.get(goldAction)) {
					double distance = sample.getUtility() - averages.get(goldAction);
					double distance2 = Math.max(MAX/4, distance);
					double factor = (goldAction.isDefault())? 1.0 : 1.0;
					weight *= Math.abs(MAX - (factor * distance2 / averages.size() )) / MAX;
				}
				
				table.put(sample, weight);
			}
		}

		return new Intervals<WeightedSample>(table);	
	}
	
	
	private Map<Assignment,Double> getAverages() {
		
		Map<Assignment,Double> averages = new HashMap<Assignment,Double>();
		
		synchronized(samples) {
			
			for (WeightedSample sample : samples) {
				Assignment action = sample.getSample().getTrimmed(goldAction.getVariables());
				if (!averages.containsKey(action)) {
					averages.put(action, 0.0);
				}
				averages.put(action, averages.get(action) + sample.getUtility());
			}
		}
		
		for (Assignment a : averages.keySet()) {
			averages.put(a, averages.get(a) * averages.size() / samples.size());
		}
		if (!averages.containsKey(goldAction)) {
			averages.put(goldAction, 10.0);
		}
		
		return averages;
	}
	

	
	public SimpleEmpiricalDistribution getResults() {
		return distrib;
	}
	
	
	
}

