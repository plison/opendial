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

package opendial.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
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

import opendial.arch.DialException;
import opendial.arch.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility functions for manipulating XML content
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
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
	 * @throws DialException if the XML document could not be read.
	 */
	public static Document getXMLDocument (String filename) throws DialException {

		log.debug("parsing file: " + filename);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			InputStreamReader streamReader = new InputStreamReader(new FileInputStream(filename));
			Document doc = builder.parse(new InputSource(streamReader));
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


	/**
	 * Serialises the XML node into a string.
	 * 
	 * @param node the XML node
	 * @return the corresponding string
	 */
	public static String serialise(Node node) {
		try {
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS lsImpl = (DOMImplementationLS)registry.getDOMImplementation("LS");
			LSSerializer serializer = lsImpl.createLSSerializer();
			return serializer.writeToString(node);
		} 
		catch (Exception e) {
			log.debug("could not serialise XML node: " + e);
			return "";
		}
	}

	/**
	 * Creates a new XML, empty document
	 * 
	 * @return the empty XML document
	 * @throws DialException if the document could not be created
	 */
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


	/**
	 * Writes the XML document to the particular file specified as argument
	 * @param doc the document
	 * @param filename the path to the file in which to write the XML data
	 * @throws DialException if the writing operation fails
	 */
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

	
	public static Document loadXMLFromString(String xml) throws ParserConfigurationException, SAXException, IOException
	{
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xml));
	    return builder.parse(is);
	}
	
	/**
	 * Returns the main node of the XML document
	 * 
	 * @param doc the XML document
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
	 * Extract included filenames in the XML document, assuming that filenames are provided
	 * with the attribute "href".
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
}


/**
 * Small error handler for XML syntax errors.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
final class XMLErrorHandler extends DefaultHandler {

	static Logger log = new Logger("XMLErrorHandler", Logger.Level.NORMAL);

	@Override
	public void error (SAXParseException e) throws SAXParseException { 
		log.warning("Parsing error: "+e.getMessage());
		throw e;
	}

	@Override
	public void warning (SAXParseException e) { 
		log.warning("Parsing problem: "+e.getMessage());
	}

	@Override
	public void fatalError (SAXParseException e) { 
		log.severe("Parsing error: "+e.getMessage()); 
		log.severe("Cannot continue."); 
		System.exit(1);
	}

}
