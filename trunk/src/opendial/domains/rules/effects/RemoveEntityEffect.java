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

package opendial.domains.rules.effects;

import opendial.domains.rules.variables.PointerVariable;
import opendial.domains.rules.variables.TypedVariable;

/**
 * Representation of an effect which removes a given entity from the dialogue state
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RemoveEntityEffect implements Effect {

	// static Logger log = new Logger("RemoveEntityEffect", Logger.Level.NORMAL);

	// the variable to remove
	PointerVariable var;
	
	/**
	 * Creates a new "entity removal" effect, for the entity referenced by the
	 * variable
	 * 
	 * @param var the variable reference
	 */
	public RemoveEntityEffect(PointerVariable var) {
		this.var = var;
	}
	
	
	/**
	 * Returns the variable in which the effect is anchored
	 * 
	 * @return the variable
	 */
	public PointerVariable getVariable() {
		return var;
	}


	/**
	 * Returns a string representation of the effect
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "[RemoveEntity: " + var + "]";
	}
	
	
	/**
	 * Compare the effect to the given one
	 * 
	 * @param e the effect to compare with the current object
	 * @return 0 if equals, -1 otherwise
	 */
	@Override
	public int compareTo(Effect e) {
		if (e instanceof RemoveEntityEffect) {
			if (((RemoveEntityEffect)e).getVariable().equals(var)) {
				return 0;
			}
			else {
				return -1;
			}
		}
		return -1;
	}
	
	
	/**
	 * Compare the two objects for equality
	 *
	 * @param o the object to compare
	 * @return true if the two objects are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof RemoveEntityEffect) {
			return var.equals(((RemoveEntityEffect)o).getVariable());
		}
		return false;
	}
	
	

	public int hashCode() {
		return var.hashCode();
	}
		
}
