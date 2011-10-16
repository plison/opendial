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

package opendial.inference.distribs;

import java.util.Set;

import opendial.inference.bn.Assignment;

/**
 * Interface for a probability distribution over variables.  The interface does
 * not assume a particular form for the distribution (it could be encoded as a 
 * conditional probability table, or a deterministic function); the only assumption
 * made here is that the distribution is discrete.
 * 
 * TODO: provide an interface for continuous distributions as well?
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Distribution {


	/**
	 * Returns true if the distribution has a defined probability for the variable
	 * assignments given as parameter, and false otherwise.
	 * 
	 * @param assign the assignment
	 * @return true if distribution contains a probability, false otherwise
	 */
	public boolean hasProb(Assignment assign);
	
	
	/**
	 * Returns the probability associated with the assignment according to the 
	 * distribution, if such probability exists.  If no probability is defined, 
	 * returns 0.0f.
	 * 
	 * @param assign the assignment
	 * @return the probability of the assignment
	 */
	public float getProb(Assignment assign);
	
	
	/**
	 * Returns a set representing all possible assignments for the distribution.
	 * If the underlying implementation for the distribution is a look-up table,
	 * the method simply returns it.  In other cases, the table might be generated
	 * for all possible combination of assignments.
	 * 
	 * @return the table of assignments
	 */
	public Set<Assignment> getTable();
	
	
	/**
	 * Returns true if the distribution is well-formed (all possible assignments are 
	 * covered; and the sum of the probabilities for all assignments equal 1.0f). 
	 * 
	 * @return true if distribution is well-formed, false otherwise
	 */
	public boolean isWellFormed() ;
	
}
