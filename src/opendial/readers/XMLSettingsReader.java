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

package opendial.readers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.Settings.PlanSettings;
import opendial.bn.BNetwork;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLSettingsReader {

	public static Logger log = new Logger("XMLSettingsReader", Logger.Level.DEBUG);

	
	// ===================================
	// TOP DOMAIN
	// ===================================

	/**
	 * Extract the settings from the XML specification
	 * 
	 * @param settingsFile the filename of the XML file
	 * @return the extracted settings
	 * @throws IOException if the file cannot be found/opened
	 * @throws DialException if a format error occurs
	 */
	public static Settings extractSettings(String settingsFile) throws DialException {
		
		// create a new, empty settings
		Settings settings = new Settings();
		
		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(settingsFile);

		Node mainNode = XMLUtils.getMainNode(doc);


		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0 ; j < firstElements.getLength() ; j++) {
			
			Node node = firstElements.item(j);	
			
			// extracting initial state
			if (node.getNodeName().equals("planning")) {
				
				String variable= null;
				int horizon = 0;
				double discountFactor = 0.0;

				for (int k = 0 ; k < node.getChildNodes().getLength() ; k++) {
					
					Node subnode = node.getChildNodes().item(k);
					if (subnode.getNodeName().equalsIgnoreCase("variable")) {
						variable = subnode.getTextContent().trim();
					}
					if (subnode.getNodeName().equalsIgnoreCase("horizon")) {
						horizon = Integer.parseInt(subnode.getTextContent());
					}
					if (subnode.getNodeName().equalsIgnoreCase("discount")) {
						discountFactor = Double.parseDouble(subnode.getTextContent());
					}	
				}
				if (variable != null && horizon > 0 && discountFactor > 0.0) {
				settings.planning.addSpecific(variable, settings.new PlanSettings(horizon, discountFactor));
				}
			}
			if (node.getNodeName().equals("gui")) {
				for (int k = 0 ; k < node.getChildNodes().getLength() ; k++) {
					
					Node subnode = node.getChildNodes().item(k);
					if (subnode.getNodeName().equalsIgnoreCase("showgui")) {
						settings.gui.showGUI = Boolean.parseBoolean(subnode.getTextContent().trim());
					}
					if (subnode.getNodeName().equalsIgnoreCase("userUtterance")) {
						settings.gui.userUtteranceVar = subnode.getTextContent().trim();
					}
					if (subnode.getNodeName().equalsIgnoreCase("systemUtterance")) {
						settings.gui.systemUtteranceVar = subnode.getTextContent().trim();
					}
					if (subnode.getNodeName().equalsIgnoreCase("toMonitor")) {
						settings.gui.varsToMonitor.add(subnode.getTextContent().trim());
					}
				}
			}
			if (node.getNodeName().equals("inference")) {
				for (int k = 0 ; k < node.getChildNodes().getLength() ; k++) {			
					Node subnode = node.getChildNodes().item(k);
					if (subnode.getNodeName().equalsIgnoreCase("nbsamples")) {
						settings.nbSamples = Integer.parseInt(subnode.getTextContent().trim());
						log.debug("Number of samples to use : " + settings.nbSamples);
					}
				}
			}
			
		}
		
		return settings;
	}
	
		
}
