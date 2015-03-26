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

package opendial.modules.core;


import java.util.List;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.state.DialogueState;
 
/**
 * Functionality to import a previously recorded dialogue in the dialogue system.  The 
 * import essentially "replays" the previous interaction, including all state update
 * operations.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-16 17:34:31 #$
 */
public class DialogueImporter extends Thread {

	// logger
	public static Logger log = new Logger("DialogueImporter", Logger.Level.DEBUG);

	
	DialogueSystem system;
	List<DialogueState> turns;

	/**
	 * Creates a new dialogue importer attached to a particular dialogue system, and
	 * with an ordered list of turns (encoded by their dialogue state).
	 * 
	 * @param system the dialogue system
	 * @param turns the sequence of turns
	 */
	public DialogueImporter(DialogueSystem system, List<DialogueState> turns) {
		this.system = system;
		this.turns = turns;
	}
	
	/**
	 * Runs the import operation.
	 */
	@Override
	public void run() {
		system.attachModule(WizardLearner.class);
		for (final DialogueState turn : turns) {
			try {
				while (system.isPaused() || !system.getModule(DialogueRecorder.class).isRunning()) {
					try { Thread.sleep(100); } catch (Exception e) { }
				}
				system.addContent(turn.copy()); 
			} 
			catch (DialException e) {	
				log.warning("could not add content: " + e);	
			}
		}
		system.detachModule(WizardLearner.class);
	}


}

