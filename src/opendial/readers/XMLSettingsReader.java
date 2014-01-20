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

package opendial.readers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.VariableElimination;
import opendial.utils.XMLUtils;

/**
 * 
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: $
 * 
 */
public class XMLSettingsReader {

	public static Logger log = new Logger("XMLSettingsReader",
			Logger.Level.DEBUG);

	// ===================================
	// TOP DOMAIN
	// ===================================

	
	public static Properties extractMapping(String settingsFile) {
		try {
			Document doc = XMLUtils.getXMLDocument(settingsFile);
			Properties mapping = extractMapping(XMLUtils.getMainNode(doc));
			return mapping;
		}
		catch (DialException e) {
			log.warning("error extracting the settings: " + e);
			return new Properties();
		}
	}

	
	static Properties extractMapping(Node mainNode) {
		
		Properties settings = new Properties();

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0; j < firstElements.getLength(); j++) {

			Node node = firstElements.item(j);

			if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")){
				settings.put(node.getNodeName().trim(), node.getTextContent());
			}
		}

		return settings;
	}
	
	
	

}
