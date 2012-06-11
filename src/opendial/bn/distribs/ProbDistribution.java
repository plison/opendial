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

import java.util.Map;

import opendial.bn.Assignment;

/**
 * Generic interface for a probability distribution P(X1...Xn|Y1...Yn), 
 * where X1...Xn,Y1,...Yn are all random variables.  X1...Xn is called the head 
 * part of the distribution, and Y1...Yn the conditional part.  
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface ProbDistribution {
	

	/**
	 * Sample a head assignment from the distribution P(head|condition), given the
	 * condition.  If no assignment can be sampled (due to e.g. an ill-formed 
	 * distribution), returns an empty assignment.
	 * 
	 * @param condition the condition
	 * @return the sampled assignment
	 */
	public Assignment sample(Assignment condition);
	
	
	/**
	 * Returns the probability P(head|condition), if any is specified.  Else,
	 * returns 0.0f.
	 * 
	 * <p>If one head variable has a continuous range, the method returns the 
	 * probability mass for the bucket (as defined by the discretisation procedure) 
	 * in which lies the value given as argument.  
	 *
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the associated probability, if one exists.
	 */
	public float getProb(Assignment condition, Assignment head);
	
	
	
	/**
	 * Returns the probability table for the head variables, given the
	 * conditional assignment given as argument.
	 * 
	 * <p>If a head variable has a continuous range, the values defined
	 * in the table are based on a discretisation procedure which creates
	 * a sequence of buckets, each with an approximatively similar
	 * probability mass.
	 * 
	 * 
	 * @param condition the assignment for the conditional variable
	 * @return the resulting probability table
	 */
	public Map<Assignment,Float> getProbTable(Assignment condition) ;
	
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
	

}


