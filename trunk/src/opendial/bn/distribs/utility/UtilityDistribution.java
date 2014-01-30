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

package opendial.bn.distribs.utility;


import opendial.datastructs.Assignment;

/**
 * Generic interface for an utility distribution (also called value distribution),
 * mapping every assignment X1, ..., Xn to a scalar utility U(X1, ...., Xn).  
 * 
 * <p>Typically, at least one of these X1, ..., Xn variables consist of a decision variable.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface UtilityDistribution {

	/**
	 * Returns the utility associated with the specific assignment of values for
	 * the input nodes.  If none exists, returns 0.0f.
	 * 
	 * @param input the value assignment for the input chance nodes
	 * @return the associated utility
	 */
	public double getUtil(Assignment input);
	
	/**
	 * Checks that the utility distribution is well-formed (all assignments are covered)
	 * 
	 * @return true is the distribution is well-formed, false otherwise
	 */
	
	public boolean isWellFormed();
	
	/**
	 * Creates a copy of the utility distribution
	 * 
	 * @return the copy
	 */
	public UtilityDistribution copy();


	/**
	 * Changes the variable label
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	public void modifyVarId(String oldId, String newId);
	
	
	
}
