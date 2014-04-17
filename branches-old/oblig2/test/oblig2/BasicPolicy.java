
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

package oblig2;

import oblig2.actions.Action;
import oblig2.actions.DialogueAction;
import oblig2.actions.VoidAction;
import oblig2.state.DialogueState;
import oblig2.state.WorldState;

/**
 * Example of a basic dialogue policy which answers any input X
 * by the response "you said X".
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicPolicy implements DialoguePolicy {

	/**
	 * Answers any input X with "you said X"
	 * 
	 * @param u_u N-best list of utterances
	 * @return dialogue action with the answer "you said X"
	 */
	@Override
	public Action processInput(NBest u_u, DialogueState dstate, WorldState wstate) {
		if (!u_u.getHypotheses().isEmpty()) {
		return new DialogueAction("you said: " + u_u.getHypotheses().get(0).getString());
		}
		else {
			return new VoidAction();
		}
	}


}
