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

import java.util.logging.*;
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.DialogueState;
import opendial.Settings;
import opendial.bn.BNetwork;
import opendial.bn.values.Value;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Rule;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML reader for dialogue domains.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLDomainReader {

	final static Logger log = Logger.getLogger("OpenDial");

	// ===================================
	// TOP DOMAIN
	// ===================================

	/**
	 * Extract a dialogue domain from the XML specification
	 * 
	 * @param topDomainFile the filename of the top XML file
	 * @return the extracted dialogue domain
	 */

	public static Domain extractDomain(String topDomainFile) {
		return extractDomain(topDomainFile, true);

	}

	/**
	 * Extract a empty domain from the XML domain specification, only setting the
	 * source file and its possible imports. This method is used to be able to
	 * extract the source and import files in case the domain is ill-formed. You can
	 * usually safely ignore this method.
	 * 
	 * @param topDomainFile the filename of the top XML file
	 * @return the extracted dialogue domain
	 */
	public static Domain extractEmptyDomain(String topDomainFile) {
		return extractDomain(topDomainFile, false);
	}

	/**
	 * Extract a dialogue domain from the XML specification
	 * 
	 * @param topDomainFile the filename of the top XML file
	 * @param fullExtract whether to extract the full domain or only the files
	 * @return the extracted dialogue domain
	 */
	private static Domain extractDomain(String topDomainFile, boolean fullExtract) {

		// create a new, empty domain
		Domain domain = new Domain();

		// determine the root path and filename
		File f = new File(topDomainFile);
		domain.setSourceFile(f);

		// extract the XML document
		try {
			Document doc = XMLUtils.getXMLDocument(topDomainFile);

			Node mainNode = XMLUtils.getMainNode(doc);

			String rootpath = f.getParent();

			NodeList firstElements = mainNode.getChildNodes();
			for (int j = 0; j < firstElements.getLength(); j++) {

				Node node = firstElements.item(j);
				domain = extractPartialDomain(node, domain, rootpath, fullExtract);
			}
		}
		catch (RuntimeException e) {
			if (fullExtract) {
				throw e;
			}
		}
		return domain;
	}

	/**
	 * Extracts a partially specified domain from the XML node and add its content to
	 * the dialogue domain.
	 * 
	 * @param mainNode main XML node
	 * @param domain dialogue domain
	 * @param rootpath rooth path (necessary to handle references)
	 * @param fullExtract whether to extract the full domain or only the files
	 * 
	 * @return the augmented dialogue domain
	 */
	private static Domain extractPartialDomain(Node mainNode, Domain domain,
			String rootpath, boolean fullExtract) {

		// extracting rule-based probabilistic model
		if (mainNode.getNodeName().equals("domain")) {

			NodeList firstElements = mainNode.getChildNodes();
			for (int j = 0; j < firstElements.getLength(); j++) {
				Node node = firstElements.item(j);
				domain = extractPartialDomain(node, domain, rootpath, fullExtract);
			}
		}

		// extracting settings
		else if (fullExtract && mainNode.getNodeName().equals("settings")) {
			Properties settings = XMLUtils.extractMapping(mainNode);
			domain.getSettings().fillSettings(settings);
		}
		// extracting custom functions
		else if (fullExtract && mainNode.getNodeName().equals("function")
				&& mainNode.getAttributes().getNamedItem("name") != null) {
			String name =
					mainNode.getAttributes().getNamedItem("name").getNodeValue();
			String functionStr = mainNode.getTextContent().trim();
			try {
				Class<?> clazz = Class.forName(functionStr);
				@SuppressWarnings("unchecked")
				Function<List<String>, Value> f =
						(Function<List<String>, Value>) clazz.newInstance();
				domain.getSettings();
				Settings.addFunction(name, f);
			}
			catch (Exception e) {
				log.warning("cannot load function : " + e);
			}
		}

		// extracting initial state
		else if (fullExtract && mainNode.getNodeName().equals("initialstate")) {
			BNetwork state = XMLStateReader.getBayesianNetwork(mainNode);
			domain.setInitialState(new DialogueState(state));
			// log.fine(state);
		}

		// extracting rule-based probabilistic model
		else if (fullExtract && mainNode.getNodeName().equals("model")) {
			Model model = createModel(mainNode);
			// log.fine(model);
			domain.addModel(model);
		}

		// extracting parameters
		else if (fullExtract && mainNode.getNodeName().equals("parameters")) {
			BNetwork parameters = XMLStateReader.getBayesianNetwork(mainNode);
			domain.setParameters(parameters);
		}

		// extracting imported references
		else if (mainNode.getNodeName().equals("import") && mainNode.hasAttributes()
				&& mainNode.getAttributes().getNamedItem("href") != null) {

			String fileName =
					mainNode.getAttributes().getNamedItem("href").getNodeValue();
			String filepath = rootpath + File.separator + fileName;
			domain.addImportedFiles(new File(filepath));
			Document subdoc = XMLUtils.getXMLDocument(filepath);
			domain = extractPartialDomain(XMLUtils.getMainNode(subdoc), domain,
					rootpath, fullExtract);
		}
		else if (fullExtract && XMLUtils.hasContent(mainNode)) {
			if (mainNode.getNodeName().equals("#text")) {
				throw new RuntimeException("cannot insert free text in <domain>");
			}
			throw new RuntimeException(
					"Invalid tag in <domain>: " + mainNode.getNodeName());
		}

		return domain;
	}

	/**
	 * Given an XML node, extracts the rule-based model that corresponds to it.
	 * 
	 * @param topNode the XML node
	 * @return the corresponding model
	 */
	private static Model createModel(Node topNode) {
		Model model = new Model();
		for (int i = 0; i < topNode.getChildNodes().getLength(); i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("rule")) {
				Rule rule = XMLRuleReader.getRule(node);
				model.addRule(rule);
			}
			else if (XMLUtils.hasContent(node)) {
				if (node.getNodeName().equals("#text")) {
					throw new RuntimeException("cannot insert free text in <model>");
				}
				throw new RuntimeException(
						"Invalid tag in <model>: " + node.getNodeName());
			}
		}

		if (topNode.hasAttributes()
				&& topNode.getAttributes().getNamedItem("trigger") != null) {
			Pattern p = Pattern.compile("([\\w\\*\\^_\\-\\[\\]\\{\\}]+"
					+ "(?:\\([\\w\\*,\\s\\^_\\-\\[\\]\\{\\}]+\\))?)"
					+ "[\\w\\*\\^_\\-\\[\\]\\{\\}]*");
			Matcher m = p.matcher(
					topNode.getAttributes().getNamedItem("trigger").getNodeValue());
			while (m.find()) {
				model.addTrigger(m.group());
			}
		}
		else {
			throw new RuntimeException("<model> must have a 'trigger' attribute:"
					+ XMLUtils.serialise(topNode));
		}

		if (topNode.getAttributes().getNamedItem("blocking") != null) {
			boolean blocking = Boolean.parseBoolean(
					topNode.getAttributes().getNamedItem("blocking").getNodeValue());
			model.setBlocking(blocking);
		}

		if (topNode.getAttributes().getNamedItem("id") != null) {
			String id = topNode.getAttributes().getNamedItem("id").getNodeValue();
			model.setId(id);
		}

		return model;
	}

}
