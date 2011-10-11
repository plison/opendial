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

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import opendial.arch.DialConstants.ModelGroup;
import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Rule;
import opendial.domains.types.GenericType;
import opendial.state.DialogueState;
import opendial.utils.Logger;
import opendial.utils.XMLUtils;

/**
 * XML reader for a domain specification.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainReader {

	static Logger log = new Logger("XMLDomainReader", Logger.Level.NORMAL);

	// the dialogue domain which is currently extracted
	Domain domain;

	// the root path for the XML specification files
	String rootpath;

	// whether to perform XML validation before reading the file 
	// (might slow down the import operation a bit)
	public static final boolean priorValidation = false;

	// default XML schema for domains
	public static final String domainSchema = "resources//schemata//domain.xsd";


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
	public Domain extractDomain(String topDomainFile) throws DialException {

		if (priorValidation) {
			XMLUtils.validateXML(topDomainFile, domainSchema);
		}
		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(topDomainFile);

		// determine the root path 
		rootpath = topDomainFile.substring(0, topDomainFile.lastIndexOf("//")+1);
		String filename = topDomainFile.substring(topDomainFile.lastIndexOf("//")+2, topDomainFile.length());

		// create a new, empty domain
		domain = new Domain(filename);

		Node mainNode = XMLUtils.getMainNode(doc, "domain");

		if (mainNode.hasAttributes() && 
				mainNode.getAttributes().getNamedItem("name") != null) {
			domain.setName(mainNode.getAttributes().getNamedItem("name").getNodeValue());
		}

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0 ; j < firstElements.getLength() ; j++) {
			Node node = firstElements.item(j);

			// extracting type declarations
			if (node.getNodeName().equals("declarations")) {
				Document refDoc = XMLUtils.getXMLDocument(rootpath+XMLUtils.getReference(node));
				XMLDeclarationsReader declReader = new XMLDeclarationsReader();
				List<GenericType> types = declReader.getTypes(refDoc);
				domain.addTypes(types);			}

			// extracting initial state
			else if (node.getNodeName().equals("initialstate")) {
				Document refDoc = XMLUtils.getXMLDocument(rootpath+XMLUtils.getReference(node));
				XMLInitialStateReader isReader = new XMLInitialStateReader();
				DialogueState initialState = isReader.getInitialState(refDoc,domain);
				domain.addInitialState(initialState);
			}
			
			// extracting rule-based probabilistic model
			else if (node.getNodeName().equals("model")) {
				Document refDoc = XMLUtils.getXMLDocument(rootpath+XMLUtils.getReference(node));
				Model model = getModel(refDoc);
				domain.addModel(model);
			}
		}
		return domain;
	}

	
	// ===================================
	// RULE-BASED PROBABILISTIC MODEL
	// ===================================


	/**
	 * Returns the model defined in the XML document
	 * 
	 * @param refDoc the XML document
	 * @return the corresponding model
	 * @throws DialException if the model is ill-formatted
	 */
	private Model getModel(Document doc) throws DialException {

		Model model;
		
		Node mainNode = XMLUtils.getMainNode(doc, "model");
		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("type")!=null) {

			// get the model group
			ModelGroup type = getModelGroup(mainNode);
			model = new Model(type);

			// add the rules
			NodeList midList = mainNode.getChildNodes();
			for (int i = 0 ; i < midList.getLength(); i++) {
				Node node = midList.item(i);
				if (node.getNodeName().equals("rule")) {
					XMLRuleReader ruleReader = new XMLRuleReader();
					Rule rule = ruleReader.getRule(node,domain);
					model.addRule(rule);
				}
			}
			return model;
		}
		else {
			throw new DialException("model must specify a type");
		}

	}


	/**
	 * Returns the model group from the XML node
	 * 
	 * @param topNode XML node
	 * @return the corresponding model group
	 * @throws DialException if model group not valid
	 */
	private ModelGroup getModelGroup(Node topNode) throws DialException {
		String group = topNode.getAttributes().getNamedItem("type").getNodeValue();
		if (group.equals("userRealisation")) {
			return ModelGroup.USER_REALISATION;
		}
		else if (group.equals("userPrediction")) {
			return ModelGroup.USER_PREDICTION;
		}
		else if (group.equals("userTransition")) {
			return ModelGroup.USER_TRANSITION;
		}	
		else if (group.equals("systemRealisation")) {
			return ModelGroup.SYSTEM_REALISATION;
		}
		else if (group.equals("systemActionValue")) {
			return ModelGroup.SYSTEM_ACTIONVALUE;
		}
		else if (group.equals("systemTransition")) {
			return ModelGroup.SYSTEM_TRANSITION;
		}
		else {
			throw new DialException("model type is not accepted");
		}
	}


}
