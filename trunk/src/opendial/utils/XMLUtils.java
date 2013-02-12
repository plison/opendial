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

package opendial.utils;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.readers.XMLDomainReader;

/**
 * Utility functions for manipulating XML content
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-01-03 16:02:01 #$
 *
 */
public class XMLUtils {

	// logger
	static Logger log = new Logger("XMLUtils", Logger.Level.NORMAL);


	/**
	 * Opens the XML document referenced by the filename, and returns it
	 * 
	 * @param filename the filename
	 * @return the XML document
	 * @throws DialException
	 */
	public static Document getXMLDocument (String filename) throws DialException {

		log.debug("parsing file: " + filename);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.parse(new InputSource(filename));
	//		log.info("XML parsing of file: " + filename + " successful!");
			return doc;
		}
		catch (SAXException e) {
			log.warning("Reading aborted: \n" + e.getMessage());
			throw new DialException(e.getMessage());
		} 
		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			throw new DialException(e.getMessage());
		} 
		catch (IOException e) {
			log.warning(e.getMessage());
			throw new DialException(e.getMessage());
		}
	}



	public static Document newXMLDocument() throws DialException {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.newDocument();
			return doc;
		}
		 
		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			throw new DialException("cannot create XML file");
		} 	
	}
	
	
	
	public static void writeXMLDocument(Document doc, String filename) throws DialException {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filename));
			transformer.transform(source, result);
			log.info("writing operation to " + filename + " successful!");
		} 
		catch (TransformerConfigurationException e) {
			log.warning(e.getMessage());
		} catch (TransformerException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * Returns the main node of the XML document
	 * 
	 * @param doc the XML document
	 * @param topTag the expected top tag of the document
	 * @return the main node
	 * @throws DialException if node is ill-formatted
	 */
	public static Node getMainNode (Document doc) throws DialException {
		for (int i = 0 ; i < doc.getChildNodes().getLength(); i++) {
			Node node = doc.getChildNodes().item(i);
			if (!node.getNodeName().equals("#text") && !node.getNodeName().equals("#comment")) {
				return node;
			}
		}
		throw new DialException("main node in XML file could not be retrieved");
	}



	/**
	 * Validates a XML document containing a specification of a dialogue domain.
	 * Returns true if the XML document is valid, false otherwise.
	 * 
	 * @param dialSpecs the domain file
	 * @param schemaFile the schema file
	 * @return true if document is validated, false otherwise
	 * @throws DialException if problem appears when parsing XML
	 */
	public static boolean validateXML(String dialSpecs, String schemaFile) throws DialException {

		log.debug ("Checking the validation of file " + dialSpecs + " against XML schema " + schemaFile + "...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			SchemaFactory schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			factory.setSchema(schema.newSchema(new Source[] {new StreamSource(schemaFile)}));
			DocumentBuilder builder = factory.newDocumentBuilder();

			try {
				builder.setErrorHandler(new XMLErrorHandler());
				Document doc = builder.parse(new InputSource(dialSpecs));
				log.debug("XML parsing of file: " + dialSpecs + " successful!");

				// extracting included files, and validating them as well
				String rootpath = dialSpecs.substring(0, dialSpecs.lastIndexOf("//")+1);
				Vector<String> includedFiles = extractIncludedFiles(doc);
				for (String file : includedFiles) {
					boolean validation = validateXML(rootpath+file, schemaFile);
					if (!validation) {
						return false;
					}
				}
			}
			catch (Exception e) {
				throw new DialException(e.getMessage());
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
				Node midNode = firstElements.item(j);
				for (int k = 0 ; k < midNode.getChildNodes().getLength() ; k++) {
					Node node = midNode.getChildNodes().item(k);
					if (node.hasAttributes() && node.getAttributes().getNamedItem("href") != null) {
						String fileName = node.getAttributes().getNamedItem("href").getNodeValue();
						includedFiles.add(fileName);
					}
				}
				
			}
		}
		return includedFiles;
	}




	/**
	 * Returns the probability of the value defined in the XML node
	 * (default to 1.0f is none is declared)
	 * 
	 * @param node the XML node
	 * @return the value probability
	 * @throws DialException if probability is ill-formatted
	 */
	public static float getProbability (Node node) {
	
		float prob = 1.0f;
	
		if (node.hasAttributes() && 
				node.getAttributes().getNamedItem("prob") != null) {
			String probStr = node.getAttributes().getNamedItem("prob").getNodeValue();
	
			try { prob = Float.parseFloat(probStr);	}
			catch (NumberFormatException e) {
				XMLDomainReader.log.warning("probability " + probStr +  " not valid, assuming 1.0f");
			}
		}
		return prob;
	}




	/**
	 * If the XML node contains a "href" attribute containing a filename, returns it.
	 * Else, throws an exception
	 * 
	 * @param node the XML node
	 * @return the referenced filename
	 * @throws DialException 
	 */
	public static String getReference(Node node) throws DialException {
	
		if (node.hasAttributes() && node.getAttributes().getNamedItem("href") != null) {
			String filename = node.getAttributes().getNamedItem("href").getNodeValue();
			return filename;
		}
		else {
			throw new DialException("Not file attribute in which to extract the reference");
		}
	}



	/**
	 * 
	 * @param node
	 * @return
	 */
	public static float getUtility(Node node) {

		float util = 1.0f;
	
		if (node.hasAttributes() && 
				node.getAttributes().getNamedItem("util") != null) {
			String probStr = node.getAttributes().getNamedItem("util").getNodeValue();
	
			try { util = Float.parseFloat(probStr);	}
			catch (NumberFormatException e) {
				XMLDomainReader.log.warning("probability " + probStr +  " not valid, assuming 1.0f");
			}
		}
		return util;
	}


}





/**
 * Small error handler for XML syntax errors.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-01-03 16:02:01 #$
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
