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

import java.util.Collection;

import opendial.arch.DialException;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

/**
 * Generic interface for probabilistic inference algorithms.  Queries can be 
 * expressed as P(X1...Xn|Y1=y1,...Yn=yn), where X1...Xn are the query variables,
 * and Y1=y1..Yn=yn are the evidences.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface InferenceAlgorithm {

	
	/**
	 * Computes the probability distribution for the query variables given the 
	 * provided evidence, all specified in the query
	 * 
	 * @param query the full query
	 */
	public ProbDistribution queryProb (ProbQuery query) throws DialException;
	
		
	/**
	 * Computes the utility distribution for the action variables, given the provided
	 * evidence.
	 * 
	 * @param query the full query
	 */
	public UtilityTable queryUtility (UtilQuery query) throws DialException;

	/**
	 * Reduces the Bayesian network to a subset of its variables
	 * 
	 * @param reductionQuery the reduction query
	 * @return the reduced query
	 */
	public BNetwork reduceNetwork(ReductionQuery reductionQuery) throws DialException;
	
	
}
