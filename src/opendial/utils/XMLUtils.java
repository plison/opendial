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

import java.util.logging.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.BNetwork;
import opendial.readers.XMLStateReader;

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
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLUtils {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Opens the XML document referenced by the filename, and returns it
	 * 
	 * @param filename the filename
	 * @return the XML document
	 */
	public static Document getXMLDocument(String filename) {
		InputStream is = null;
		if (new File(filename).exists()) {
			try {
				is = new FileInputStream(filename);
			}
			catch (IOException e) {
				log.warning(e.getMessage());
				throw new RuntimeException(e.getMessage());
			}
		}
		else {
			is = XMLUtils.class
					.getResourceAsStream("/" + filename.replace("//", "/"));
			if (is == null) {
				throw new RuntimeException("Resource cannot be found: " + filename);
			}
		}

		return getXMLDocument(new InputSource(new InputStreamReader(is)));
	}

	/**
	 * Opens the XML document referenced by the input source, and returns it
	 * 
	 * @param is the input source
	 * @return the XML document
	 */
	public static Document getXMLDocument(InputSource is) {
		log.fine("parsing file: " + is);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.parse(is);
			// log.info("XML parsing of file: " + filename + " successful!");
			return doc;
		}
		catch (SAXException e) {
			// log.warning("Reading aborted: \n" + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		catch (IOException e) {
			log.warning(e.getMessage());
			throw new RuntimeException(e.getMessage());
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
			DOMImplementationRegistry registry =
					DOMImplementationRegistry.newInstance();
			DOMImplementationLS lsImpl =
					(DOMImplementationLS) registry.getDOMImplementation("LS");
			LSSerializer serializer = lsImpl.createLSSerializer();
			return serializer.writeToString(node);
		}
		catch (Exception e) {
			log.fine("could not serialise XML node: " + e);
			return "";
		}
	}

	/**
	 * Creates a new XML, empty document
	 * 
	 * @return the empty XML document
	 */
	public static Document newXMLDocument() {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.newDocument();
			return doc;
		}

		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			throw new RuntimeException("cannot create XML file");
		}
	}

	/**
	 * Writes the XML document to the particular file specified as argument
	 * 
	 * @param doc the document
	 * @param filename the path to the file in which to write the XML data writing
	 *            operation fails
	 */
	public static void writeXMLDocument(Document doc, String filename) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filename));
			transformer.transform(source, result);
			log.fine("writing operation to " + filename + " successful!");
		}
		catch (TransformerConfigurationException e) {
			log.warning(e.getMessage());
		}
		catch (TransformerException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * Writes the XML document as a raw string
	 * 
	 * @param doc the document
	 */
	public static String writeXMLString(Document doc) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			return writer.getBuffer().toString();
		}
		catch (TransformerConfigurationException e) {
			log.warning(e.getMessage());
		}
		catch (TransformerException e) {
			log.warning(e.getMessage());
		}
		return "";
	}

	public static Document loadXMLFromString(String xml)
			throws ParserConfigurationException, SAXException, IOException {
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
	 */
	public static Node getMainNode(Document doc) {
		for (int i = 0; i < doc.getChildNodes().getLength(); i++) {
			Node node = doc.getChildNodes().item(i);
			if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				return node;
			}
		}
		throw new RuntimeException("main node in XML file could not be retrieved");
	}

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
		catch (RuntimeException e) {
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
	public static Properties extractMapping(Node mainNode) {

		Properties settings = new Properties();

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0; j < firstElements.getLength(); j++) {

			Node node = firstElements.item(j);

			if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				String propName = node.getNodeName().trim();
				settings.put(propName, node.getTextContent());
			}
		}

		return settings;
	}

	/**
	 * Validates a XML document containing a specification of a dialogue domain.
	 * Returns true if the XML document is valid, false otherwise.
	 * 
	 * @param dialSpecs the domain file
	 * @param schemaFile the schema file
	 * @return true if document is validated, false otherwise when parsing XML
	 */
	public static boolean validateXML(String dialSpecs, String schemaFile) {

		log.fine("Checking the validation of file " + dialSpecs
				+ " against XML schema " + schemaFile + "...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			SchemaFactory schema =
					SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			factory.setSchema(
					schema.newSchema(new Source[] { new StreamSource(schemaFile) }));
			DocumentBuilder builder = factory.newDocumentBuilder();

			try {
				builder.setErrorHandler(new XMLErrorHandler());
				Document doc = builder.parse(new InputSource(dialSpecs));
				log.fine("XML parsing of file: " + dialSpecs + " successful!");

				// extracting included files, and validating them as well
				String rootpath =
						dialSpecs.substring(0, dialSpecs.lastIndexOf("//") + 1);
				Vector<String> includedFiles = extractIncludedFiles(doc);
				for (String file : includedFiles) {
					boolean validation = validateXML(rootpath + file, schemaFile);
					if (!validation) {
						return false;
					}
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
			return true;
		}
		catch (SAXException e) {
			log.warning("Validation aborted: \n" + e.getMessage());
			return false;
		}
		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			return false;
		}
	}

	/**
	 * Extract included filenames in the XML document, assuming that filenames are
	 * provided with the attribute "href".
	 * 
	 * @param xmlDocument the XML document
	 * @return the filenames to include
	 */
	private static Vector<String> extractIncludedFiles(Document xmlDocument) {

		Vector<String> includedFiles = new Vector<String>();

		NodeList top = xmlDocument.getChildNodes();
		for (int i = 0; i < top.getLength(); i++) {
			Node topNode = top.item(i);
			NodeList firstElements = topNode.getChildNodes();
			for (int j = 0; j < firstElements.getLength(); j++) {
				Node midNode = firstElements.item(j);
				for (int k = 0; k < midNode.getChildNodes().getLength(); k++) {
					Node node = midNode.getChildNodes().item(k);
					if (node.hasAttributes()
							&& node.getAttributes().getNamedItem("href") != null) {
						String fileName = node.getAttributes().getNamedItem("href")
								.getNodeValue();
						includedFiles.add(fileName);
					}
				}

			}
		}
		return includedFiles;
	}

	/**
	 * Imports a dialogue state or prior parameter distributions.
	 * 
	 * @param system the dialogue system
	 * @param file the file that contains the state or parameter content
	 * @param tag the expected top XML tag. into the system
	 */
	public static void importContent(DialogueSystem system, String file,
			String tag) {
		if (tag.equals("parameters")) {
			BNetwork parameters = XMLStateReader.extractBayesianNetwork(file, tag);
			for (String oldParam : system.getState().getParameterIds()) {
				if (!parameters.hasChanceNode(oldParam)) {
					parameters.addNode(system.getState().getChanceNode(oldParam));
				}
			}
			system.getState().setParameters(parameters);
		}
		else {
			BNetwork state = XMLStateReader.extractBayesianNetwork(file, tag);
			system.addContent(new DialogueState(state));
		}
	}

	/**
	 * Exports a dialogue state or prior parameter distributions.
	 * 
	 * @param system the dialogue system
	 * @param file the file in which to write the state or parameter content
	 * @param tag the expected top XML tag. from the system
	 */
	public static void exportContent(DialogueSystem system, String file,
			String tag) {
		Document doc = XMLUtils.newXMLDocument();

		Set<String> parameterIds =
				new HashSet<String>(system.getState().getParameterIds());
		Set<String> otherVarsIds =
				new HashSet<String>(system.getState().getChanceNodeIds());
		otherVarsIds.removeAll(parameterIds);
		Set<String> variables =
				(tag.equals("parameters")) ? parameterIds : otherVarsIds;
		Node paramXML = system.getState().generateXML(doc, variables);
		doc.renameNode(paramXML, null, tag);
		doc.appendChild(paramXML);
		XMLUtils.writeXMLDocument(doc, file);
	}

	/**
	 * Returns true if the node has some actual content (other than a comment or an
	 * empty text).
	 * 
	 * @param node the XML node
	 * @return true if the node contains information, false otherwise
	 */
	public static boolean hasContent(Node node) {
		if (node.getNodeName().equals("#comment")) {
			return false;
		}
		else if (node.getNodeName().equals("#text")) {
			return !node.getNodeValue().trim().isEmpty();
		}
		return true;
	}
}

/**
 * Small error handler for XML syntax errors.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
final class XMLErrorHandler extends DefaultHandler {

	final static Logger log = Logger.getLogger("OpenDial");

	@Override
	public void error(SAXParseException e) throws SAXParseException {
		log.warning("Parsing error: " + e.getMessage());
		throw e;
	}

	@Override
	public void warning(SAXParseException e) {
		log.warning("Parsing problem: " + e.getMessage());
	}

	@Override
	public void fatalError(SAXParseException e) {
		log.severe("Fatal error: " + e.getMessage());
	}

}
