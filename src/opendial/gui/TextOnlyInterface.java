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

package opendial.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

/**
 * Text-only interface to OpenDial, to use when no X11 display
 * is available.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class TextOnlyInterface implements Module {

	public static Logger log = new Logger("TextOnlyInterface", Logger.Level.DEBUG); 

	DialogueSystem system;
	boolean paused = true;
	
	/**
	 * Creates a new text-only interface.
	 * @param system
	 */
	public TextOnlyInterface(DialogueSystem system) {
		this.system = system;
	}
	
	/**
	 * Starts the interface.
	 */
	@SuppressWarnings("resource")
	@Override
	public void start() throws DialException {
		paused = false;
		log.info("Starting text-only user interface...");
		log.info("Local address: " + system.getLocalAddress());
		log.info("Press Ctrl + C to exit");
		new Thread(() -> {
			try {Thread.sleep(500);} catch (InterruptedException e) {}
			while (true) {
				System.out.println("Type new input: ");
				String input = new Scanner(System.in).nextLine();
				Map<String,Double> table = StringUtils.getTableFromInput(input);
				if (!paused && !table.isEmpty()) {
					system.addUserInput(table);
				}
			}
		}).start();
	}

	/**
	 * Updates the interface with the new content (if relevant).
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		
		for (String var : Arrays.asList(system.getSettings().userInput, 
				system.getSettings().systemOutput)) {
			if (!paused && updatedVars.contains(var) && state.hasChanceNode(var)) {
				System.out.println(getTextRendering(system.getContent(var).toDiscrete()));
			}
		}
	}
	

	/**
	 * Pauses the interface
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true if the interface is running, and false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

	
	/**
	 * Generates the text representation for the categorical table.
	 * 
	 * @param table the table
	 * @return the text rendering of the table
	 */
	private String getTextRendering(CategoricalTable table) {

		String textTable = "";
		String baseVar = table.getVariable().replace("'", "");

		if (baseVar.equals(system.getSettings().userInput)) {
			textTable += "[user]\t";
		}
		else if (baseVar.equals(system.getSettings().systemOutput)) {
			textTable += "[system]\t";
		}
		else {
			textTable += "[" + baseVar + "]\t";
		}
		for (Value value : table.getValues()) {
			if (!(value instanceof NoneVal)) {
				String content = value.toString();
				if (table.getProb(value) < 0.98) {
					content += " (" + StringUtils.getShortForm(table.getProb(value)) + ")";
				}
				textTable += content + "\n\t\t";
			}
		}
		textTable = textTable.substring(0, textTable.length() - 3);

		return textTable;		
	}
	
}
