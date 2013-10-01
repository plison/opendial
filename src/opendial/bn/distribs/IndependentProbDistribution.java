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

package opendial.bn.distribs;


import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Generic probability distribution P(X1,...,Xn) without conditional variables.
 * The probability distribution may be continuous or discrete.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public interface IndependentProbDistribution extends ProbDistribution {
	
	/**
	 * Returns a sample from the probability distribution
	 * 
	 * @return the sample value
	 * @throws DialException if no value could be sampled
	 */
	public Assignment sample() throws DialException;

	/**
	 * Returns a discrete representation of the distribution as a categorical table.
	 * 
	 * @return the distribution in the format of a categorical table
	 * @throws DialException if the distribution could not be converted to a discrete form
	 */
	@Override
	public CategoricalTable toDiscrete();
	
	
	/**
	 * Returns a continuous representation of the distribution.
	 * 
	 * @return the distribution in a continuous form
	 * @throws DialException if the distribution could not be converted to a continuous form
	 */
	public ContinuousDistribution toContinuous() throws DialException;
	
	/**
	 * Returns a copy of the distribution.
	 */
	@Override
	public IndependentProbDistribution copy();
	
	
	/**
	 * Returns a set of possible values for the distribution.  If the distribution is continuous,
	 * assumes a discretised representation of the distribution.
	 * 
	 * @return the possible values for the distribution
	 */
	public abstract Set<Assignment> getPossibleValues();

}

