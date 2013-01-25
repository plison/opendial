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

package opendial.planning;


import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.inference.ImportanceSampling;
import opendial.inference.queries.ProbQuery;
import opendial.state.DialogueState;

public class InteractionModel {

	// logger
	public static Logger log = new Logger("InteractionModel", Logger.Level.DEBUG);
	
		
	public SampledObservation sampleObservation(DialogueState state) throws DialException {
		
		Set<String> predictionNodes = new HashSet<String>();
		for (String nodeId: state.getNetwork().getChanceNodeIds()) {
			if (nodeId.contains("^p")) {
				predictionNodes.add(nodeId);
			}
		}
		
		ProbQuery query = new ProbQuery(state, predictionNodes);
		ImportanceSampling sampling = new ImportanceSampling(1, 200);
		
		ProbDistribution distrib = sampling.queryProb(query);
		Assignment samplePrediction = distrib.sample(new Assignment());
		Assignment sampleObservation = new Assignment();
		for (String var : samplePrediction.getVariables()) {
			sampleObservation.addPair(var.replace("^p", ""), samplePrediction.getValue(var));
		}
		return new SampledObservation(sampleObservation);

	}




}

