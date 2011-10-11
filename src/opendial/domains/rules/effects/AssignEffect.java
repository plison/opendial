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

import opendial.arch.DialException;
import opendial.domains.rules.variables.StandardVariable;
import opendial.utils.Logger;

/**
 * Representation of a basic assignment effect, which assigns a given value
 * to a single variable.
 * 
 * TODO: introduce more complex kinds of assignments? 
 * e.g. underspecified values (wildcard), constraints
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AssignEffect<T> implements Effect {

	// the logger
	static Logger log = new Logger("BasicEffect", Logger.Level.NORMAL);
	
	// the variable to be assigned
	StandardVariable var;
	
	// the value to assign
	T value;
	
	
	/**
	 * Creates a new assignment effect, composed a variable and a specific
	 * value
	 * 
	 * @param var the variable
	 * @param value the value
	 * @throws DialException if the variable type does not accept the given value
	 */
	public AssignEffect(StandardVariable var, T value) throws DialException {
		this.var = var;
		this.value = value;
		
		if (!var.getType().containsValue(value)) {
			throw new DialException("variable " + var.getType().getName() + " does not accept value " + value);
		}
	}

	/**
	 * Returns the variable
	 * 
	 * @return the variable
	 */
	public StandardVariable getVariable() {
		return var;
	}
	
	
	/**
	 * Returns the value
	 * 
	 * @return the value
	 */
	public T getValue() {
		return value;
	}
	
	
	/**
	 * Returns a string representation of the effect
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "Set " + var + " := " + value;
		return str;
	}
	

	/**
	 * Compare the effect to the given one
	 * 
	 * @param e the effect to compare with the current object
	 * @return 0 if equals, -1 otherwise
	 */
	@Override
	public int compareTo(Effect e) {
		if (e instanceof AssignEffect) {
			if (((AssignEffect<?>)e).getVariable().equals(var) && ((AssignEffect<?>)e).getValue().equals(value)) {
				return 0;
			}
			else {
				return -1;
			}
		}
		return -1;
	}
}
