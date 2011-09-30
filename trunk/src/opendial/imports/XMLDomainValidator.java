// =================================================================                                                                   
// Copyright (C) 2009-2011 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.imports;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import opendial.utils.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML reader utility for validating the XML specification of dialogue domains.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-09-28 10:42:11 #$
 *
 */

public class XMLDomainValidator {

	static Logger log = new Logger("XMLDomainReader", Logger.Level.DEBUG);


	public static final String domainSchema = "resources//schemata//domain.xsd";


	/**
	 * Validates a XML document containing a specification of a dialogue domain.
	 * Returns true if the XML document is valid, false otherwise.
	 * 
	 * @param dialDomain the filename of the XML document
	 * @return true if document is valid, false otherwise
	 * @throws IOException if file cannot be read
	 */
	public static boolean validateXML(String dialDomain) throws IOException {

		return validateXML(dialDomain, domainSchema);
	}



	private static boolean validateXML(String dialSpecs, String schemaFile) throws IOException {

		log.debug ("Checking the validation of file " + dialSpecs + " against XML schema " + schemaFile + "...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			SchemaFactory schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			factory.setSchema(schema.newSchema(new Source[] {new StreamSource(schemaFile)}));
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.parse(new InputSource(dialSpecs));
			log.debug("XML parsing of file: " + dialSpecs + " successful!");

			// extracting included files, and validating them as well
			String rootpath = dialSpecs.substring(0, dialSpecs.lastIndexOf("//")+1);
			Vector<String> includedFiles = extractIncludedFiles(doc);
			for (String file : includedFiles) {
				boolean validation = validateXML(rootpath+file);
				if (!validation) {
					return false;
				}
			}

			return true;
		}
		catch (SAXException e) {
			log.warning("Validation aborted: \n" + e.getMessage());
			return false;
		} catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			return false;
		} 
	}


	/**
	 * Extract included filenames in the XML document
	 * 
	 * @param xmlDocument the XML document
	 * @return the filenames to include
	 */
	private static Vector<String> extractIncludedFiles(Document xmlDocument) {

		Vector<String> includedFiles = new Vector<String>();

		NodeList top = xmlDocument.getChildNodes();
		for (int i = 0 ; i < top.getLength(); i++) {
			Node topNode = top.item(i);
			NodeList firstElements = topNode.getChildNodes();
			for (int j = 0 ; j < firstElements.getLength() ; j++) {
				Node node = firstElements.item(j);
				if (node.hasAttributes() && node.getAttributes().getNamedItem("file") != null) {
					String fileName = node.getAttributes().getNamedItem("file").getNodeValue();
					includedFiles.add(fileName);
				}
			}
		}
		return includedFiles;
	}


}



/**
 * Small error handler for XML syntax errors.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-09-28 10:42:11 #$
 *
 */
final class XMLErrorHandler extends DefaultHandler {

	static Logger log = new Logger("XMLErrorHandler", Logger.Level.NORMAL);

	public void error (SAXParseException e) throws SAXParseException { 
		log.warning("Parsing error: "+e.getMessage());
		throw e;
	}

	public void warning (SAXParseException e) { 
		log.warning("Parsing problem: "+e.getMessage());
	}

	public void fatalError (SAXParseException e) { 
		log.severe("Parsing error: "+e.getMessage()); 
		log.severe("Cannot continue."); 
		System.exit(1);
	}

}
