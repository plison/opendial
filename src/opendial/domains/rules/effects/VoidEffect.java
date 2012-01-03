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

/**
 * Representation of a void effect (which does nothing)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VoidEffect implements Effect {

	
	/**
	 * Returns a string representation of the effect
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "Void";
	}
	
	
	/**
	 * Compare the effect to the given one
	 * 
	 * @param e the effect to compare with the current object
	 * @return 0 if equals, -1 otherwise
	 */
	@Override
	public int compareTo(Effect e) {
		if (e instanceof VoidEffect) {
			return 0;
		}
		return -1;
	}

	
	@Override
	public boolean equals(Object o) {
		return (o instanceof VoidEffect);
	}
	
	
	@Override
	public int hashCode() {
		return 103459;
	}

}
