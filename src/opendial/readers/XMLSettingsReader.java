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

package opendial.readers;

import java.util.Properties;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML reader for system settings.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 * 
 */
public class XMLSettingsReader {

	public static Logger log = new Logger("XMLSettingsReader",
			Logger.Level.DEBUG);

	// ===================================
	// TOP DOMAIN
	// ===================================

	
	/**
	 * Extract the settings from the XML file.
	 * 
	 * @param settingsFile the file containing the settings
	 * @return the resulting list of properties
	 */
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
	
	
	/**
	 * Extract the settings from the XML node.
	 * 
	 * @param mainNode the XML node containing the settings
	 * @return the resulting list of properties
	 */
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
