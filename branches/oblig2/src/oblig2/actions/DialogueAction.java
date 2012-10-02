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
 * A Dialogue action, represented by an utterance to synthesise.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueAction implements Action {
		
		// the utterance
		String utterance;
		
		/**
		 * Constructs a new dialogue action with the given utterance
		 * 
		 * @param utterance the utterance
		 */
		public DialogueAction(String utterance) {
			this.utterance = utterance;
		}
		
		/**
		 * Returns the utterance contained in the action
		 * 
		 * @return the utterance
		 */
		public String getUtterance() {
			return utterance;
		}
		
		/**
		 * Returns a string representation of the action
		 *
		 * @return the utterance itself
		 */
		public String toString() {
			return utterance;
		}
		
		/**
		 * Computes the hashcode for the action
		 *
		 * @return hashcode
		 */
		public int hashCode() {
			return utterance.hashCode();
		}
}
