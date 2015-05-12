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

import java.util.List;
import java.util.logging.Logger;

import opendial.DialogueState;
import opendial.DialogueSystem;

/**
 * Functionality to import a previously recorded dialogue in the dialogue system. The
 * import essentially "replays" the previous interaction, including all state update
 * operations.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class DialogueImporter extends Thread {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;
	List<DialogueState> turns;
	boolean wizardOfOzMode = false;

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
	 * Sets whether the import should consider the system actions as "expert"
	 * Wizard-of-Oz actions to imitate.
	 * 
	 * @param isWizardOfOz whether the system actions are wizard-of-Oz examples
	 */
	public void setWizardOfOzMode(boolean isWizardOfOz) {
		wizardOfOzMode = isWizardOfOz;
	}

	/**
	 * Runs the import operation.
	 */
	@Override
	public void run() {

		if (wizardOfOzMode) {
			system.attachModule(WizardLearner.class);
			for (final DialogueState turn : turns) {
				addTurn(turn);
			}
		}
		else {
			system.detachModule(ForwardPlanner.class);
			for (final DialogueState turn : turns) {
				addTurn(turn);
				system.getState().removeNodes(system.getState().getActionNodeIds());
				system.getState().removeNodes(system.getState().getUtilityNodeIds());
			}
			system.attachModule(ForwardPlanner.class);
		}
	}

	private void addTurn(DialogueState turn) {
		try {
			while (system.isPaused()
					|| !system.getModule(DialogueRecorder.class).isRunning()) {
				try {
					Thread.sleep(100);
				}
				catch (Exception e) {
				}
			}
			system.addContent(turn.copy());
		}
		catch (RuntimeException e) {
			log.warning("could not add content: " + e);
		}
	}

}
