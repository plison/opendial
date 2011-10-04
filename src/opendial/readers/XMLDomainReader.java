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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.actions.Action;
import opendial.domains.actions.VerbalAction;
import opendial.domains.observations.Observation;
import opendial.domains.observations.UtteranceObservation;
import opendial.domains.rules.Rule;
import opendial.domains.types.ActionType;
import opendial.domains.types.EntityType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.FixedVariableType;
import opendial.domains.types.StandardType;
import opendial.domains.types.ObservationType;
import opendial.domains.types.StandardType;
import opendial.state.DialogueState;
import opendial.state.StateEntity;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainReader {

	static Logger log = new Logger("XMLDomainReader", Logger.Level.DEBUG);

	// the dialogue domain which is currently extracted
	Domain domain;

	// the root path for the XML specification files
	String rootpath;


	public XMLDomainReader() {

	}

	// ===================================
	// CORE METHODS
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

		// extract the XML document
		Document doc = getXMLDocument(topDomainFile);

		// determine the root path 
		rootpath = topDomainFile.substring(0, topDomainFile.lastIndexOf("//")+1);

		// create a new, empty domain
		domain = new Domain("");

		Node mainNode = getMainNode(doc, "domain");

		if (mainNode.hasAttributes() && 
				mainNode.getAttributes().getNamedItem("name") != null) {
			domain.setName(mainNode.getAttributes().getNamedItem("name").getNodeValue());
		}

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0 ; j < firstElements.getLength() ; j++) {
			Node node = firstElements.item(j);

			if (node.getNodeName().equals("declarations")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				XMLDeclarationsReader declReader = new XMLDeclarationsReader();
				List<StandardType> types = declReader.getTypes(refDoc);
				domain.addTypes(types);
			}

			else if (node.getNodeName().equals("initialstate")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				XMLInitialStateReader isReader = new XMLInitialStateReader();
				DialogueState initialState = isReader.getInitialState(refDoc,domain);
				domain.addInitialState(initialState);
			}
			else if (node.getNodeName().equals("model")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				Model model = getModel(refDoc);
				domain.addModel(model);
			}
		}

		return domain;
	}



	// ===================================
	//  INITIAL STATE METHODS
	// ===================================


	// ===================================
	//  MODEL CONSTRUCTION METHODS
	// ===================================


	/**
	 * 
	 * @param refDoc
	 * @return
	 * @throws DialException 
	 */
	private Model getModel(Document doc) throws DialException {

		Model model;

		Node mainNode = getMainNode(doc, "model");

		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("type")!=null) {

			// get the type of the model
			Model.Type type = getModelType(mainNode);
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


	private Model.Type getModelType(Node topNode) throws DialException {
		String type = topNode.getAttributes().getNamedItem("type").getNodeValue();
		if (type.equals("userRealisation")) {
			return Model.Type.USER_REALISATION;
		}
		else if (type.equals("userPrediction")) {
			return Model.Type.USER_PREDICTION;
		}
		else if (type.equals("userTransition")) {
			return Model.Type.USER_TRANSITION;
		}	
		else if (type.equals("systemRealisation")) {
			return Model.Type.SYSTEM_REALISATION;
		}
		else if (type.equals("systemActionValue")) {
			return Model.Type.SYSTEM_ACTIONVALUE;
		}
		else if (type.equals("systemTransition")) {
			return Model.Type.SYSTEM_TRANSITION;
		}
		else {
			throw new DialException("model type is not accepted");
		}
	}



	// ===================================
	//  ENTITY TYPE DECLARATION METHODS
	// ===================================


	// ===================================
	//  UTILITY METHODS
	// ===================================



	public static Document getXMLDocument (String filename) throws DialException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			Document doc = builder.parse(new InputSource(filename));
			log.debug("XML parsing of file: " + filename + " successful!");
			return doc;
		}
		catch (SAXException e) {
			log.warning("Reading aborted: \n" + e.getMessage());
		} 
		catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
		} 
		catch (IOException e) {
			log.warning(e.getMessage());
			throw new DialException(e.getMessage());
		}
		return null;
	}



	/**
	 * 
	 * @param node
	 * @return
	 * @throws IOException 
	 * @throws DialException 
	 */
	public static String getReference(Node node) throws DialException {

		if (node.hasAttributes() && node.getAttributes().getNamedItem("file") != null) {
			String filename = node.getAttributes().getNamedItem("file").getNodeValue();
			return filename;
		}
		else {
			throw new DialException("Not file attribute in which to extract the reference");
		}
	}


	public static Node getMainNode (Document doc, String topTag) throws DialException {

		NodeList topList = doc.getChildNodes();
		if (topList.getLength() == 1 && topList.item(0).getNodeName().equals(topTag)) {
			Node topNode = topList.item(0);
			return topNode;
		}
		else if (topList.getLength() == 0) {
			throw new DialException("Document is empty");
		}
		else  {
			throw new DialException("Document contains other tags than \"" + topTag + "\": " + topList.item(0).getNodeName());
		}
	}

}
