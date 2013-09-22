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

package opendial.modules;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

public class DialogueRecorder {

	// logger
	public static Logger log = new Logger("DialogueRecorder", Logger.Level.DEBUG);

	String recordFile;			
	
	Node rootNode;
	Document doc;	
	
	
	public DialogueRecorder(String basePath, String baseName) {
		
		this.recordFile = basePath + baseName.replace(".xml", "") + 
				(new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss")).format(new Date()) + ".xml";

		try {
		if (new File(recordFile).exists()) {
			doc = XMLUtils.getXMLDocument(recordFile);
		}
		else {
			doc = XMLUtils.newXMLDocument();
			log.debug("creating new xml file " + recordFile);
			Node rootNode = doc.createElement("samples");
			doc.appendChild(rootNode);
		}
		rootNode = XMLUtils.getMainNode(doc);
		}
		catch (DialException e) {
			log.warning("could not create file " + recordFile);
		}
	}

	

	public void removeLastSample() {
			if (rootNode.getNodeName().equals("samples") && rootNode.getChildNodes().getLength() > 1) {
				Node lastNode = rootNode.getChildNodes().item(rootNode.getChildNodes().getLength()-2);
				rootNode.removeChild(lastNode);
			}
			else {
				log.warning("root node is ill-formatted: " + rootNode.getNodeName() 
						+ "(with " + rootNode.getChildNodes().getLength() + " children)");
			}
	}
	
	
	public void recordTrainingData(DialogueState state, Assignment action) {
		try {
			if (rootNode.getNodeName().equals("samples")) {
				Element dataNode = doc.createElement("data");
				Element dataEl = state.generateXML(doc, false);
				dataNode.appendChild(dataEl);
				Element outputNode = action.generateXML(doc);
				dataNode.appendChild(outputNode);
				doc.renameNode(outputNode, null, "output") ;
		/**		doc.renameNode(dataNode, null, "a_m");
				doc.renameNode(dataNode, null, "u_m");
				doc.renameNode(dataNode, null, "u_u"); 
				doc.renameNode(dataNode, null, "floor"); */
				rootNode.appendChild(dataNode);
			}
			else {
				log.warning("root node is ill-formatted: " + rootNode.getNodeName() + " or first value is null");
			}
			XMLUtils.writeXMLDocument(doc, recordFile);
		} catch (DialException e) {
			log.warning("cannot record training data : " + e.toString());
		}
	}
	
	
	public void recordTurn(DialogueState state) {
		try {
			if (rootNode.getNodeName().equals("samples")) {
				Element dataNode = doc.createElement("userTurn");
				Element dataEl = state.generateXML(doc, false);
				dataNode.appendChild(dataEl);
				rootNode.appendChild(dataNode);
			}
			else {
				log.warning("root node is ill-formatted: " + rootNode.getNodeName() + " or first value is null");
			}

			XMLUtils.writeXMLDocument(doc, recordFile);
		} catch (DialException e) {
			log.warning("cannot record training data : " + e.toString());
		}
	}
	

	
	public void recordTurn(Assignment assign) {

		try {
			if (rootNode.getNodeName().equals("samples")) {
				Element dataNode = doc.createElement("systemTurn");
				Element dataEl = assign.generateXML(doc);
				dataNode.appendChild(dataEl);
				rootNode.appendChild(dataNode);
			}
			else {
				log.warning("root node is ill-formatted: " + rootNode.getNodeName() + " or first value is null");
			}
			XMLUtils.writeXMLDocument(doc, recordFile);
		} catch (DialException e) {
			log.warning("cannot record training data : " + e.toString());
		}
	}
	
	

	public void addComment(String comment) {
		try {
			if (rootNode.getNodeName().equals("samples")) {
			Comment com = doc.createComment(comment);
			rootNode.appendChild(com);
			}
			else {
				log.warning("could not add comment");
			}
			XMLUtils.writeXMLDocument(doc, recordFile);
		}
		catch (Exception e) {
			log.warning("could not add preamble to file " + recordFile);
		}
	}


}

