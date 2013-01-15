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

package opendial.bn.distribs;

import java.util.Collection;

import opendial.arch.DialException;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface ProbDistribution  {

	
	/**
	 * Checks that the probability distribution is well-formed (all assignments are covered,
	 * and the probabilities add up to 1.0f)
	 * 
	 * @return true is the distribution is well-formed, false otherwise
	 */
	public boolean isWellFormed();
	
	
	/**
	 * Creates a copy of the probability distribution
	 * 
	 * @return the copy
	 */
	public ProbDistribution copy();


	/**
	 * Returns a pretty print representation of the distribution
	 * 
	 * @return the pretty print for the distribution
	 */
	public String prettyPrint();
	
	
	/**
	 * Changes a variable label in the distribution
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	public void modifyVarId(String oldId, String newId);
	
	/**
	 * Sample a head assignment from the distribution P(head), given the
	 * condition.  If no assignment can be sampled (due to e.g. an ill-formed 
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 * @throws DialException if no sample point can be drawn
	 */
	public Assignment sample(Assignment condition) throws DialException;
	
	
	/**
	 * Returns the discrete form of the distribution.  If the current distribution
	 * has a continuous range, the returned distribution will be a discretised conversion
	 * of the current one.
	 * 
	 * @return the discretised form of the distribution
	 */
	public DiscreteProbDistribution toDiscrete();
	
	
	/**
	 * Returns the continuous form of the distribution.  If the current distribution
	 * has a discrete range, the returned distribution will be a continuous distribution
	 * using dirac delta functions in the density distributions.
	 * 
	 * @return the continuous form of the distribution
	 * @throws DialException 
	 */
	public ContinuousProbDistribution toContinuous() throws DialException;
	
	
	
	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	public Collection<String> getHeadVariables();
}
