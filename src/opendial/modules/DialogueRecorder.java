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


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

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

public class DialogueRecorder implements StateListener {

	// logger
	public static Logger log = new Logger("DialogueRecorder", Logger.Level.DEBUG);


	public static final String RECORD_FILE =  "experiments//tacl2013//WoZData-" +
			(new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss")).format(new Date()) + ".xml";

	DialogueSystem system;

	long referenceStamp = 0;
	
	
	@Override
	public void update(DialogueState state) {
		
		if (state.isUpdated("a_u", referenceStamp)) {
			Assignment noAction = new Assignment("a_m", "None");
			recordTrainingData(state, noAction);
			referenceStamp = System.currentTimeMillis();
		}
		
	}


	public static void removeLastSample() {
		try {
			Document doc;
			if (new File(RECORD_FILE).exists()) {
				doc = XMLUtils.getXMLDocument(RECORD_FILE);
			}
			else {
				doc = XMLUtils.newXMLDocument();
			}
			Node rootNode = XMLUtils.getMainNode(doc);
			if (rootNode.getNodeName().equals("samples") && rootNode.getChildNodes().getLength() > 1) {
				Node lastNode = rootNode.getChildNodes().item(rootNode.getChildNodes().getLength()-2);
				rootNode.removeChild(lastNode);
			}
			else {
				log.warning("root node is ill-formatted: " + rootNode.getNodeName() + " or first value is null");
			}
			XMLUtils.writeXMLDocument(doc, RECORD_FILE);
		} catch (DialException e) {
			log.warning("cannot record training data : " + e.toString());
		}
	}
	
	
	public static void recordTrainingData(DialogueState state, Assignment action) {
		try {

			Document doc;
			if (new File(RECORD_FILE).exists()) {
				doc = XMLUtils.getXMLDocument(RECORD_FILE);
			}
			else {
				doc = XMLUtils.newXMLDocument();
				Node rootNode = doc.createElement("samples");
				doc.appendChild(rootNode);
			}

			Node rootNode = XMLUtils.getMainNode(doc);
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
			XMLUtils.writeXMLDocument(doc, RECORD_FILE);
		} catch (DialException e) {
			log.warning("cannot record training data : " + e.toString());
		}
	}
}

