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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.utils.XMLUtils;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Module used to systematically record all user inputs and outputs during the
 * interaction. The recordings are stored in a XML element which can be written to a
 * file at any time.
 * 
 * The module can also be used to record Wizard-of-Oz interactions.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class DialogueRecorder implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	Node rootNode;
	Document doc;
	Settings settings;

	/**
	 * Creates a new dialogue recorder for the dialogue system
	 * 
	 * @param system the dialogue system
	 */
	public DialogueRecorder(DialogueSystem system) {
		this.settings = system.getSettings();
	}

	/**
	 * Starts the recorder.
	 */
	@Override
	public void start() {
		try {
			doc = XMLUtils.newXMLDocument();
			doc.appendChild(doc.createElement("interaction"));
			rootNode = XMLUtils.getMainNode(doc);
		}
		catch (RuntimeException e) {
			log.warning("could not create dialogue recorder");
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void pause(boolean shouldBePaused) {
	}

	/**
	 * Triggers the recorder with a particular dialogue state and a set of recently
	 * updated variables. If one of the updated variables is the user input or system
	 * output, the recorder stores a new turn. Else, the module does nothing.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!rootNode.getNodeName().equals("interaction")) {
			log.warning("root node is ill-formatted: " + rootNode.getNodeName()
					+ " or first value is null");
			return;
		}
		// if the user is still speaking, do not record anything yet
		if (state.hasChanceNode(settings.userSpeech)) {
			return;
		}

		try {
			if (updatedVars.contains(settings.userInput)) {
				Set<String> varsToRecord = new HashSet<String>();
				varsToRecord.add(settings.userInput);
				varsToRecord.addAll(settings.varsToMonitor);
				Element el = state.generateXML(doc, varsToRecord);
				if (el.getChildNodes().getLength() > 0) {
					doc.renameNode(el, null, "userTurn");
					rootNode.appendChild(el);
				}
			}
			if (updatedVars.contains(settings.systemOutput)) {
				Set<String> varsToRecord = new HashSet<String>();
				varsToRecord.add(settings.systemOutput);
				varsToRecord.addAll(settings.varsToMonitor);
				if (state.hasChanceNode("a_m")) {
					varsToRecord.add("a_m");
				}
				Element el = state.generateXML(doc, varsToRecord);
				if (el.getChildNodes().getLength() > 0) {
					doc.renameNode(el, null, "systemTurn");
					rootNode.appendChild(el);
				}
			}
		}
		catch (RuntimeException e) {
			log.warning("cannot record dialogue turn " + e);
		}
	}

	/**
	 * Adds a comment in the XML recordings.
	 * 
	 * @param comment the comment to add
	 */
	public void addComment(String comment) {
		try {
			if (rootNode.getNodeName().equals("interaction")) {
				Comment com = doc.createComment(comment);
				rootNode.appendChild(com);
			}
			else {
				log.warning("could not add comment");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			log.warning("could not record preamble or comment: " + e);
		}
	}

	/**
	 * Write the recorded dialogue to a file
	 * 
	 * @param recordFile the pathname for the file
	 */
	public void writeToFile(String recordFile) {
		log.fine("recording interaction in file " + recordFile);
		try {
			XMLUtils.writeXMLDocument(doc, recordFile);
		}
		catch (RuntimeException e) {
			log.warning("could not create file " + recordFile);
		}
	}

	/**
	 * Serialises the XML recordings and returns the output.
	 * 
	 * @return the serialised XML content.
	 */
	public String getRecord() {
		return XMLUtils.serialise(rootNode);
	}

	/**
	 * Returns true if the module is running, and false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return doc != null;
	}

}
