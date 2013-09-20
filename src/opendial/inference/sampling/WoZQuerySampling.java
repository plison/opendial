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
import java.util.Iterator;
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
import opendial.utils.DistanceUtils;

public class WoZQuerySampling extends AbstractQuerySampling {

	public static double FACTOR = 0.5;
	
	public static double MIN = -20;
	public static double MAX = 30;
	public static double NONE_FACTOR = 1.0;
	
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

		List<AssignmentWithUtil> averages = getAverages();
		
		if (averages.size() == 1) {
			for (WeightedSample sample : samples) {
				table.put(sample, sample.getWeight());
			}
			return new Intervals<WeightedSample>(table);	
		}
		
		log.debug("Utility averages : " + averages.toString().replace("\n", ", "));
		log.debug(" ==> gold action = " + goldAction);
		
		double factor = (goldAction.isDefault()) ? FACTOR * NONE_FACTOR : FACTOR;
		synchronized(samples) {
			
			for (WeightedSample sample : samples) {
				double weight = sample.getWeight();
				
				if (sample.getUtility() < MIN || sample.getUtility() > MAX) {
					weight = 0;
				}
				
				int position = getRanking(sample, averages);
				if (position != -1) {
					weight *= factor * Math.pow(1-factor, position);
				}
								
				table.put(sample, weight);
			}
		}

		return new Intervals<WeightedSample>(table);	
	}
	
	
	private int getRanking(WeightedSample sample, List<AssignmentWithUtil> averages) {
		
		List<AssignmentWithUtil> copy = new ArrayList<AssignmentWithUtil>(averages);
		copy.add(new AssignmentWithUtil(sample.getSample().getTrimmed(goldAction.getVariables()), sample.getUtility()));
		Collections.sort(copy);
		Collections.reverse(copy);
		for (int i = 0 ; i < copy.size() ; i++) {
			if (copy.get(i).getAssignment().equals(goldAction)) {
				return i;
			}
		}
		
	//	log.warning("could not find ranked position for the goldAction " + goldAction + " in " + averages);
		return -1;
	}
	
	
	
	private List<AssignmentWithUtil> getAverages() {
		
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
		
		List<AssignmentWithUtil> sortedAverage = new ArrayList<AssignmentWithUtil>();
		for (Assignment a : averages.keySet()) {
			AssignmentWithUtil a2 = new AssignmentWithUtil(a, averages.get(a) / samples.size());
			sortedAverage.add(a2);
		}
		
		Collections.sort(sortedAverage);
		Collections.reverse(sortedAverage);

		return sortedAverage;
	}
	

	
	public SimpleEmpiricalDistribution getResults() {
		return distrib;
	}
	
	
	
	private final class AssignmentWithUtil implements Comparable<AssignmentWithUtil> {

		Assignment a;
		double util;
		
		public AssignmentWithUtil(Assignment a, double util) {
			this.a = a;
			this.util = util;
		}
		
		public Assignment getAssignment() {
			return a;
		}
		
		public double getUtil() {
			return util;
		}
		
		@Override
		public int compareTo(AssignmentWithUtil otherA) {
			return (int)(1000*(util - otherA.getUtil()));
		}
		
		public String toString() {
			return "U("+a+")="+DistanceUtils.shorten(util);
		}
		
	}
	
}

