// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.modules;


import java.util.Collection;
import opendial.arch.DialException;
import opendial.state.DialogueState;


/**
 * Representation of a system module.  A module is connected to the dialogue system and
 * can read and write to its dialogue state.  It can also be paused/resumed.
 * 
 * <p>Two distinct families of modules can be distinguished: <ol>
 * <li>Asynchronous modules run independently of the dialogue system (once initiated by
 * the method start().
 * <li>Synchronous modules are triggered upon an update of the dialogue state via the method
 * trigger(state, updatedVars).  
 * </ol>
 * 
 * <p>Of course, nothing prevents in practice a module to operate both in synchronous and 
 * asynchronous mode.
 * 
 * <p> In order to make the module easy to load into the system (via e.g. the button "Load 
 * Modules" in the GUI toolbar or via the "&lt;modules&gt;" parameters in system settings), it is
 * a good idea to ensure that implement each module with a constructor with a single argument: 
 * the  DialogueSystem object to which it should be connected.  Additional arguments can in
 * this case be specified through parameters in the system settings.  When necessary parameters 
 * are missing, a MissingParameterException should be thrown.
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


}
