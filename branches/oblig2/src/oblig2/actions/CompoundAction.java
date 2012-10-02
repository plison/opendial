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

package oblig2.actions;

import java.util.ArrayList;
import java.util.List;

/**
 * Compound action, composed of a set of actions to be executed
 * in parallel (for instance, a dialogue action executed in parallel
 * to a physical action).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class CompoundAction {

	// the list of basic actions included in the compound
	List<Action> basicActions;
	
	/**
	 * Constructs a new compound action, with an empty list
	 * of actions
	 */
	public CompoundAction() {
		basicActions = new ArrayList<Action>();
	}
	
	/**
	 * Adds a new action to the list
	 * 
	 * @param a the action to insert
	 */
	public void addBasicAction(Action a) {
		basicActions.add(a);
	}
	
	/**
	 * Returns the set of actions included in the compound
	 * 
	 * @return
	 */
	public List<Action> getBasicActions() {
		return basicActions;
	}
	
	/**
	 * Returns a string representation of the compound
	 *
	 * @return the string
	 */
	public String toString() {
		return basicActions.toString();
	}
	
	/**
	 * Hashcode
	 *
	 * @return hashcode
	 */
	public int hashCode() {
		return basicActions.hashCode();
	}
}
