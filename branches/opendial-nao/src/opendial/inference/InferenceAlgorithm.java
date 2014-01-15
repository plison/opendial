// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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
import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

/**
 * Generic interface for probabilistic inference algorithms. Three distinct types of queries
 * are possible: <ul>
 * <li> probability queries of the form P(X1,...,Xn)
 * <li> utility queries of the form U(X1,...,Xn)
 * <li> reduction queries where a Bayesian network is reduced to a subet of variables X1,...,Xn
 * </ul>
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
	 * @return the resulting probability distribution
	 */
	public IndependentProbDistribution queryProb (ProbQuery query) throws DialException;
	
	
	/**
	 * Computes the utility distribution for the action variables, given the provided
	 * evidence.
	 * 
	 * @param query the full query
	 * @return the resulting utility table
	 */
	public UtilityTable queryUtil (UtilQuery query) throws DialException;

	
	/**
	 * Estimates the probability distribution on a reduced subset of variables
	 * 
	 * @param reductionQuery the reduction query
	 * @return the reduced network
	 */
	public BNetwork reduce(ReductionQuery reductionQuery) throws DialException;
	
	
}
