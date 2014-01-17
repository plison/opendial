// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.modules;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.state.DialogueState;


/**
 * Representation of a system module.  In order to be easily loaded into the system,
 * modules should have a constructor with a unique parameter, the DialogueSystem instance
 * to which it should be connected.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Module {


	/**
	 * Starts the module. 
	 * 
	 * @throws DialException if the initialisation fails
	 */
	public void start() throws DialException;

	/**
	 * Triggers the module after a state update
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the set of updated variables
	 */
	public void trigger(DialogueState state, Collection<String> updatedVars);

	/**
	 * Pauses the current module
	 * 
	 * @param toPause whether to pause or resume the module
	 */
	public void pause(boolean toPause);

	
	/**
	 * Returns true if the module is running (i.e. started and not paused), and
	 * false otherwise
	 * 
	 * @return whether the module is running or not
	 */
	public boolean isRunning();


	class MissingParameterException extends DialException {

		List<String> missingParams;

		public MissingParameterException(String missingParam) {
			super("could not start module due to missing parameter : " + missingParam);
			this.missingParams = Arrays.asList(missingParam);
		}

		public MissingParameterException(List<String> missingParams) {
			super("could not start module due to missing parameters : " + missingParams);
			this.missingParams = missingParams;
		}

		public List<String> getMissingParameters() {
			return missingParams;
		}
	}

}
