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


import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.Query;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

public class SwitchingAlgorithm implements InferenceAlgorithm {

	// logger
	public static Logger log = new Logger("SwitchingAlgorithm",
			Logger.Level.NORMAL);

	@Override
	public ProbDistribution queryProb(ProbQuery query) throws DialException {
		return selectBestAlgorithm(query).queryProb(query);
	}

	@Override
	public UtilityTable queryUtility(UtilQuery query) throws DialException {
		return selectBestAlgorithm(query).queryUtility(query);
	}

	@Override
	public BNetwork reduceNetwork(ReductionQuery query) throws DialException {
		return selectBestAlgorithm(query).reduceNetwork(query);
	}
	
	
	private InferenceAlgorithm selectBestAlgorithm (Query query) {
			
		int branchingFactor = 0;
		int nbContinuous = 0;
		for (BNode node : query.getNetwork().getNodes()) {
			if (node.getInputNodeIds().size() > branchingFactor) {
				branchingFactor = node.getInputNodeIds().size();
			}
			if (node instanceof ChanceNode && node.getInputNodeIds().isEmpty()) {
				if (isContinuous(((ChanceNode)node).getDistrib())) {
					nbContinuous++;
				}
			}
		}
		
		if (nbContinuous > 1 || branchingFactor > 3 || query.getQueryVars().size() > 2) {
			return new ImportanceSampling();
		}
		else {
			return new VariableElimination();
		}
	}
	
	
	private boolean isContinuous(ProbDistribution distrib) {
		if (distrib instanceof ContinuousProbDistribution) {
			return true;
		}
		try {
			distrib.toContinuous();
			return true;
		}
		catch (DialException e) {
			return false;
		}
	}

}

