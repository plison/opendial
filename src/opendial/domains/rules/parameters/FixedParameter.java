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

package opendial.domains.rules.parameters;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;


/**
 * Representation of a parameter fixed to one single specific value.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class FixedParameter implements Parameter {

	// logger
	public static Logger log = new Logger("FixedParameter", Logger.Level.NORMAL);
	
	// the parameter value
	double param;
	
	/**
	 * Constructs a fixed parameter with the given value.
	 * 
	 * @param param the parameter value
	 */
	public FixedParameter(double param) {
		this.param = param;
	}
	
	/**
	 * Returns the parameter value
	 * 
	 * @return the value for the parameter
	 */
	public double getParameterValue() {
		return param;
	}
	
	
	/**
	 * Returns the parameter value, ignoring the input
	 * 
	 * @return the value for the parameter
	 */
	public double getParameterValue(Assignment input) {
		return getParameterValue();
	}
	
	
	/**
	 * Returns an empty set
	 *
	 * @return an empty set of distributions
	 */
	public Collection<String> getParameterIds() {
		return new LinkedList<String>();
	}
	
	
	/**
	 * Returns the parameter value as a string
	 *
	 * @return the string
	 */
	public String toString() {
		return""+ param;
	}
	
	
	/**
	 * Returns the hashcode for the fixed parameter
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return 2* new Double(param).hashCode();
	}
	
	
	/**
	 * Copies the fixed parameter
	 * 
	 * @return the copy
	 */
	public FixedParameter copy() {
		return new FixedParameter(param);
	}
}
