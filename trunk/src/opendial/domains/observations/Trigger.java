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

package opendial.domains.observations;

import java.util.List;
import java.util.Map;

import opendial.inputs.Observation;

/**
 * Generic representation for an observation trigger, defined in a parametrised
 * form (a main content + an optional number of parameters/slots).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Trigger<T> {

	/**
	 * Returns the content of the trigger
	 * 
	 * @return the content of the trigger
	 */
	public abstract T getContent();

	
	/**
	 * Returns the list of slots for the trigger
	 * 
	 * @return list of slots
	 */
	public abstract List<String> getSlots();

	
	public float getProb(Observation obs);


	/**
	 * 
	 * @param t2
	 * @return
	 */
	public abstract boolean subsumes(Trigger t2);
	
	public Map<String,Map<String,Float>> fillSlots (Observation obs) ;
}
