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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Rule;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML reader for dialogue domains.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainReader {

	public static Logger log = new Logger("XMLDomainReader", Logger.Level.DEBUG);

	// ===================================
	// TOP DOMAIN
	// ===================================

	/**
	 * Extract a dialogue domain from the XML specification
	 * 
	 * @param topDomainFile the filename of the top XML file
	 * @return the extracted dialogue domain
	 * @throws IOException if the file cannot be found/opened
	 * @throws DialException if a format error occurs
	 */
	public static Domain extractDomain(String topDomainFile) throws DialException {
		
		// create a new, empty domain
		Domain domain = new Domain();
	

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(topDomainFile);

		Node mainNode = XMLUtils.getMainNode(doc);

		// determine the root path and filename
		File f = new File(topDomainFile);
		String rootpath = f.getParent();		

		if (mainNode.hasAttributes() && 
				mainNode.getAttributes().getNamedItem("name") != null) {
			domain.setName(mainNode.getAttributes().getNamedItem("name").getNodeValue());
		}
		else {
			domain.setName(topDomainFile.replace("//", "/"));
		}

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0 ; j < firstElements.getLength() ; j++) {
			
			Node node = firstElements.item(j);	
			domain = extractPartialDomain(node, domain, rootpath);
		}
		return domain;
	}
	
	 
	
	/**
	 * Extracts a partially specified domain from the XML node and add its content
	 * to the dialogue domain.
	 * 
	 * @param mainNode main XML node
	 * @param domain dialogue domain
	 * @param rootpath rooth path (necessary to handle references)
	 * @return the augmented dialogue domain
	 * @throws DialException
	 */
	private static Domain extractPartialDomain (Node mainNode, Domain domain, 
			String rootpath) throws DialException {

			// extracting rule-based probabilistic model
			if (mainNode.getNodeName().equals("settings")) {
						Properties settings = XMLSettingsReader.extractMapping(mainNode);
						domain.getSettings().fillSettings(settings);
				}
					
			// extracting initial state
			else if (mainNode.getNodeName().equals("initialstate")) {
				BNetwork state = XMLStateReader.getBayesianNetwork(mainNode);
				domain.setInitialState(new DialogueState(state));
		//		log.debug(state);
			}
			
			// extracting rule-based probabilistic model
			else if (mainNode.getNodeName().equals("model")) {
				Model model = createModel(mainNode);
		//		log.debug(model);
				domain.addModel(model);
			}
			
			// extracting parameters
				else if (mainNode.getNodeName().equals("parameters")) {
						BNetwork parameters = XMLStateReader.getBayesianNetwork(mainNode);
						domain.setParameters(parameters);
				}
			
			// extracting imported references
			else if (mainNode.getNodeName().equals("import") && mainNode.hasAttributes() && 
					mainNode.getAttributes().getNamedItem("href") != null) {
				
				String fileName = mainNode.getAttributes().getNamedItem("href").getNodeValue();	
				Document subdoc = XMLUtils.getXMLDocument(rootpath+ File.separator + fileName);
				domain = extractPartialDomain(XMLUtils.getMainNode(subdoc), domain, rootpath);		
			}
			
		
		return domain;
	}
	
	
	/**
	 * Given an XML node, extracts the rule-based model that corresponds to it.
	 * 
	 * @param topNode the XML node
	 * @return the corresponding model
	 * @throws DialException if the specification is ill-defined
	 */
	private static Model createModel(Node topNode) throws DialException {
		Model model = new Model();
		for (int i = 0 ; i < topNode.getChildNodes().getLength() ; i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("rule")) {
				Rule rule = XMLRuleReader.getRule(node);
				model.addRule(rule);
			}
		}
		
		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("trigger")!= null) {
			String[] triggerArray = topNode.getAttributes().getNamedItem("trigger").getNodeValue().split(",");
			model.addTriggers(Arrays.asList(triggerArray));			
		}
		else {
			throw new DialException("each model must specify a variable trigger:" + XMLUtils.serialise(topNode));
		}
		
		if (topNode.getAttributes().getNamedItem("id")!= null) {
			String id = topNode.getAttributes().getNamedItem("id").getNodeValue();
			model.setId(id);
		}
		

		return model;
	}



}
