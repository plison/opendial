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
import java.util.LinkedList;
import java.util.List;

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
import opendial.domains.EntityType;
import opendial.domains.Model;
import opendial.domains.actions.Action;
import opendial.domains.actions.VerbalAction;
import opendial.domains.observations.Observation;
import opendial.domains.observations.StringObservation;
import opendial.domains.rules.Rule;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-09-30 18:13:25 #$
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
	public Domain extractDomain(String topDomainFile) throws IOException, DialException {

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
				List<EntityType> entityTypes = getEntityTypes(refDoc);
				domain.addEntityTypes(entityTypes);
			}

			else if (node.getNodeName().equals("initialstate")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				DialogueState initialState = getInitialState(refDoc);
				domain.addInitialState(initialState);
			}
			else if (node.getNodeName().equals("model")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				Model model = getModel(refDoc);
				domain.addModel(model);
			}
			else if (node.getNodeName().equals("observations")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				List<Observation> observations = getObservations(refDoc);
				domain.addObservations(observations);
			}
			else if (node.getNodeName().equals("actions")) {
				String fileReference = getReference(node);
				Document refDoc = getXMLDocument(rootpath+fileReference);
				List<Action> actions = getActions(refDoc);
				domain.addActions(actions);
			}
		}


		return domain;
	}



	// ===================================
	//  INITIAL STATE METHODS
	// ===================================


	/**
	 * TODO: implement this method
	 * @param refDoc
	 * @return
	 */
	private DialogueState getInitialState(Document refDoc) {
		return new DialogueState();
	}



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
					Rule rule = ruleReader.getRule(node);
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

	/**
	 * Extracts the entity type declarations from the XML document
	 * 
	 * @param doc the XML document
	 * @return the list of entity types which have been declared
	 * @throws DialException if the document is ill-formatted
	 */
	private List<EntityType> getEntityTypes(Document doc) throws DialException {

		List<EntityType> entityTypes = new LinkedList<EntityType>();

		Node mainNode = getMainNode(doc,"declarations");
		NodeList midList = mainNode.getChildNodes();

		for (int i = 0 ; i < midList.getLength() ; i++) {
			Node node = midList.item(i);
			if (node.getNodeName().equals("entitytype")) {

				if (node.hasAttributes() && node.getAttributes().getNamedItem("name") != null) {
					String name = node.getAttributes().getNamedItem("name").getNodeValue();

					EntityType type = new EntityType(name);
					List<String> values = extractValues (node);
					type.addValues(values);

					List<EntityType> features = extractFeatures(node);
					type.addFeatures(features);

					entityTypes.add(type);
				}
				else {
					throw new DialException("name attribute not provided");
				}
			}
		}

		return entityTypes;
	}


	/**
	 * Extract values from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted values
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<String> extractValues(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<String> values = new LinkedList<String>();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node subnode = contentList.item(i);
			if (subnode.getNodeName().equals("value")) {
				values.add(subnode.getTextContent());
				log.debug("adding value: " + subnode.getTextContent());
			}
		}
		return values;
	}


	/**
	 * Extracts features from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<EntityType> extractFeatures(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<EntityType> types = new LinkedList<EntityType>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("feature")) {

				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("name") != null) {
					String featName = subnode.getAttributes().getNamedItem("name").getNodeValue();
					EntityType featType = new EntityType(featName);

					List<String> values = extractValues (subnode);
					featType.addValues(values);

					List<EntityType> features = extractFeatures(subnode);
					featType.addFeatures(features);

					types.add(featType);

				}
				else {
					throw new DialException("\"feature\" tag must have a name");
				}
			}
		}
		return types;
	}


	// ===================================
	//  OBSERVATION AND ACTION METHODS
	// ===================================


	/**
	 * 
	 * @param refDoc
	 * @return
	 * @throws DialException 
	 */
	private List<Observation> getObservations(Document doc) throws DialException {
		
		List<Observation> observations = new LinkedList<Observation>();

		Node mainNode = getMainNode(doc, "observations");
		NodeList actionList = mainNode.getChildNodes();
		for (int j = 0 ; j < actionList.getLength() ; j++) {

			Node subnode = actionList.item(j);
			if (subnode.getNodeName().equals("observation")) {
				
				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("name") != null && 
						subnode.getAttributes().getNamedItem("type")!= null &&
						subnode.getAttributes().getNamedItem("content")!= null) {
					
					String value = subnode.getAttributes().getNamedItem("name").getNodeValue();
					String type = subnode.getAttributes().getNamedItem("type").getNodeValue();
					String content = subnode.getAttributes().getNamedItem("content").getNodeValue();
					
					Observation obs ;
					if (type.equals("substring") || type.equals("string")) {
						obs = new StringObservation(value,content);
					}
					else {
						throw new DialException("type " + type + " currently not supported");
					}
					observations.add(obs);
					
				}
			}
		}

		return observations;
	}



	/**
	 * TODO: use the action variable spec a_m?
	 * 
	 * @param refDoc
	 * @return
	 * @throws DialException 
	 */
	private List<Action> getActions(Document doc) throws DialException {

		List<Action> actions = new LinkedList<Action>();

		Node mainNode = getMainNode(doc, "actions");
		NodeList actionList = mainNode.getChildNodes();
		for (int j = 0 ; j < actionList.getLength() ; j++) {

			Node subnode = actionList.item(j);
			if (subnode.getNodeName().equals("action")) {
				
				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("value") != null && 
						subnode.getAttributes().getNamedItem("type")!= null &&
						subnode.getAttributes().getNamedItem("content")!= null) {
					
					String value = subnode.getAttributes().getNamedItem("value").getNodeValue();
					String type = subnode.getAttributes().getNamedItem("type").getNodeValue();
					String content = subnode.getAttributes().getNamedItem("content").getNodeValue();
					
					Action action ;
					if (type.equals("string")) {
						action = new VerbalAction(value,content);
					}
					else {
						throw new DialException("type " + type + " currently not supported");
					}
					actions.add(action);
					
				}
			}
		}

		return actions;
	}




	// ===================================
	//  UTILITY METHODS
	// ===================================



	private static Document getXMLDocument (String filename) throws IOException {

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
		} catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
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
	private static String getReference(Node node) throws IOException, DialException {

		if (node.hasAttributes() && node.getAttributes().getNamedItem("file") != null) {
			String filename = node.getAttributes().getNamedItem("file").getNodeValue();
			return filename;
		}
		else {
			throw new DialException("Not file attribute in which to extract the reference");
		}
	}


	private static Node getMainNode (Document doc, String topTag) throws DialException {

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
