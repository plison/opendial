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

package opendial.domains.values;

import opendial.arch.DialConstants.PrimitiveType;
import opendial.domains.observations.Trigger;


/**
 * Representation of an observation value in a variable type.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ObservationValue<T> extends RangeValue {

	// static Logger log = new Logger("GenericObservation", Logger.Level.NORMAL);
	
	// the observation trigger
	Trigger<T> trigger;
	
	/**
	 * Creates a new observation value, with the given trigger
	 * 
	 * @param trigger the trigger associated with the observation
	 */
	public ObservationValue(Trigger<T> trigger) {
		super(PrimitiveType.BOOLEAN);
		this.trigger = trigger;
	}
	
	
	/**
	 * Returns the trigger associated with the observation value
	 * 
	 * @return the trigger
	 */
	public Trigger<T> getTrigger() {
		return trigger;
	}

	
	/**
	 * Returns a string representation of the observation value
	 *
	 * @return the string representation
	 */
	public String toString() {
		String s = super.toString();
		s += ": " + trigger.toString();
		return s;
	}
}
