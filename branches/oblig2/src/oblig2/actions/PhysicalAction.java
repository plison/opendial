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


/**
 * Representation of a physical action in our simplified world model.
 * A physical action must be one of the five basic movements:
 * {forward, backward, left, right, pick}
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public final class PhysicalAction implements Action {

	// enumeration of possible movements
	public static enum Movement {FORWARD, BACKWARD, LEFT, RIGHT, PICK, PUTDOWN}
	
	// movement for the action
	Movement mov;
	
	/**
	 * Constructs a new physical action with the movement
	 * 
	 * @param mov the movement
	 */
	public PhysicalAction(Movement mov) {
		this.mov = mov;
	}
	
	/**
	 * Returns the movement for the physical action
	 * 
	 * @return the movement
	 */
	public Movement getMovement() {
		return mov;
	}
	
	/**
	 * Returns a string representation of the physical action
	 *
	 * @return the movement as a string
	 */
	public String toString() {
		switch (mov) {
		case FORWARD: return "Forward"; 
		case BACKWARD: return "Backward";
		case LEFT: return "Left";
		case RIGHT: return "Right";
		case PICK: return "Pick";
		}
		return "";
	}
	
	
	/**
	 * Hashcode
	 *
	 * @return hashcode
	 */
	public int hashCode() {
		return mov.hashCode();
	}
}
