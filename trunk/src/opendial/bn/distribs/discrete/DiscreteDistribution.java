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

package opendial.bn.distribs.discrete;


import opendial.arch.DialException;
import opendial.bn.distribs.ProbDistribution;
import opendial.datastructs.Assignment;


/**
 * Representation of a discrete probability distribution P(X1,...,Xn | Y1,...Y_m) where
 * X1,..., Xn are variables with a discrete range of values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface DiscreteDistribution extends ProbDistribution {

	/**
	 * Returns the probability P(head|condition), if any is specified.  Else,
	 * returns 0.0f.
	 * 
	 * @param condition the conditional assignment for Y1,..., Ym
	 * @param head the head assignment X1,...,Xn
	 * @return the associated probability, if one exists.
	 * @throws DialException if the probability could not be extracted
	 */
	public abstract double getProb(Assignment condition, Assignment head) throws DialException ;
	
	
	/**
	 * Returns the posterior distribution P(X1,...,Xn | condition) as a categorical table.
	 * 
	 * @param condition the conditional assignment for Y1,..., Ym
	 * @return the probability table P(X1,..., Xn) given the conditional assignment
	 */
	@Override
	public abstract CategoricalTable getPosterior(Assignment condition) throws DialException;

	
	/**
	 * Copies the distribution
	 * 
	 * @return the copy of the discrete distribution
	 */
	@Override
	public abstract DiscreteDistribution copy();
	
}
