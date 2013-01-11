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

package opendial.bn.distribs.discrete;

import opendial.arch.DialException;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface DiscreteProbDistribution extends ProbDistribution {

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
	public abstract double getProb(Assignment condition, Assignment head) ;
	
	
	
	/**
	 * Returns whether the distribution has a well-defined probability for the
	 * given conditional and head assignments
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return true if the distribution defines a probability for the value, and
	 *         false otherwise
	 */
	public abstract boolean hasProb(Assignment condition, Assignment head);
	

	
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
	 * @throws DialException 
	 */
	public abstract SimpleTable getProbTable(Assignment condition) throws DialException ;
	
	
	
}
