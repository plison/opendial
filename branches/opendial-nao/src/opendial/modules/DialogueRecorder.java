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


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DialogueRecorder implements Module {

	// logger
	public static Logger log = new Logger("DialogueRecorder", Logger.Level.DEBUG);

	Node rootNode;
	Document doc;
	Settings settings;

	
	public DialogueRecorder(DialogueSystem system) {
		this.settings = system.getSettings();				
	}
	
	
	@Override
	public void start() {
		try {
			doc = XMLUtils.newXMLDocument();
			doc.appendChild(doc.createElement("interaction"));
			rootNode = XMLUtils.getMainNode(doc);
		}
		catch (DialException e) {
			log.warning("could not create dialogue recorder");
		}
	}
	
	@Override
	public void pause(boolean shouldBePaused) { 	}


	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!rootNode.getNodeName().equals("interaction")) {
			log.warning("root node is ill-formatted: " 
					+ rootNode.getNodeName() + " or first value is null");
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
				Element el = state.generateXML(doc, varsToRecord);
				if (el.getChildNodes().getLength() > 0) {
				doc.renameNode(el, null, "systemTurn");
				rootNode.appendChild(el);
				}
			}
		}
		catch (DialException e) {
			log.warning("cannot record dialogue turn " + e);
		}
	}
	
	
	public void addWizardAction (Assignment action) {
			Node wizardNode =doc.createElement("wizard");
			wizardNode.setTextContent(action.toString());
			rootNode.appendChild(wizardNode);
	}

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
	

	public void writeToFile(String recordFile) {
		log.debug("recording interaction in file " + recordFile);
		try {
			XMLUtils.writeXMLDocument(doc, recordFile);
		}
		catch (DialException e) {
			log.warning("could not create file " + recordFile);
		}
	}

	public String getRecord() {
		return XMLUtils.serialise(rootNode);
	}


	@Override
	public boolean isRunning() {
		return doc != null;
	}


	



}

