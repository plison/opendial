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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.domains.actions.Action;
import opendial.domains.actions.VerbalAction;
import opendial.domains.observations.Observation;
import opendial.domains.observations.UtteranceObservation;
import opendial.domains.types.ActionType;
import opendial.domains.types.EntityType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.FixedVariableType;
import opendial.domains.types.StandardType;
import opendial.domains.types.ObservationType;
import opendial.domains.types.StandardType;
import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.ComplexValue;
import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDeclarationsReader {

	static Logger log = new Logger("XMLDeclarationsReader", Logger.Level.NORMAL);
	

	/**
	 * Extracts the entity type declarations from the XML document
	 * 
	 * @param doc the XML document
	 * @return the list of entity types which have been declared
	 * @throws DialException if the document is ill-formatted
	 */
	public List<StandardType> getTypes(Document doc) throws DialException {

		List<StandardType> entityTypes = new LinkedList<StandardType>();
		Map<StandardType,List<String>> unlinkedFeatures = new HashMap<StandardType,List<String>>();

		Node mainNode = XMLDomainReader.getMainNode(doc,"declarations");
		NodeList midList = mainNode.getChildNodes();

		for (int i = 0 ; i < midList.getLength() ; i++) {
			Node node = midList.item(i);


			if (node.hasAttributes() && node.getAttributes().getNamedItem("name") != null) {
				String name = node.getAttributes().getNamedItem("name").getNodeValue();

				if (node.getNodeName().equals("entity")) {
					EntityType type = new EntityType(name);
					List<Value> values = extractTypeValues (node);
					type.addEntityValues(values);
					List<String> refFeatures = extractReferencedFeatures(node);
					unlinkedFeatures.put(type, refFeatures);
					List<FeatureType> features = extractContentFeatures(node);
					((StandardType)type).addFeatures(features);
					entityTypes.add(type);
				}
				else if (node.getNodeName().equals("variable")) {
					FixedVariableType type = new FixedVariableType(name);
					List<Value> values = extractTypeValues (node);
					type.addFixedVariableValues(values);
					List<String> refFeatures = extractReferencedFeatures(node);
					unlinkedFeatures.put(type, refFeatures);
					List<FeatureType> features = extractContentFeatures(node);
					((StandardType)type).addFeatures(features);
					entityTypes.add(type);
				}
				else if (node.getNodeName().equals("feature")) {
					FeatureType type = new FeatureType(name);
					List<Value> values = extractTypeValues (node);
					type.addFeatureValues(values);
					List<String> refFeatures = extractReferencedFeatures(node);
					unlinkedFeatures.put(type, refFeatures);
					List<FeatureType> features = extractContentFeatures(node);
					((StandardType)type).addFeatures(features);
					entityTypes.add(type);
				}
				else if (node.getNodeName().equals("observation")) {
					ObservationType type = getObservation(node);
					entityTypes.add(type);
				}
				else if (node.getNodeName().equals("action")) {
					ActionType type = getAction(node);
					entityTypes.add(type);
				}
				else {
					throw new DialException("declaration type not recognised");
				}
			}
			else if (!node.getNodeName().equals("#text") && (!node.getNodeName().equals("#comment"))){
				log.debug("node name: " + node.getNodeName());
				throw new DialException("name attribute not provided");
			}
		}

		linkFeatures(entityTypes, unlinkedFeatures);

		return entityTypes;
	}

	private void linkFeatures(List<StandardType> types, 
			Map<StandardType,List<String>> unlinkedFeatures) throws DialException {

		for (StandardType type : unlinkedFeatures.keySet()) {
			for (String featureName : unlinkedFeatures.get(type)) {
				boolean foundAssociatedType = false;
				for (StandardType type2: types) {
					if (featureName.equals(type2.getName()) && type instanceof StandardType && type2 instanceof FeatureType) {
						type.addFeature((FeatureType)type2);
						foundAssociatedType = true;
					}
				}
				if (!foundAssociatedType) {
					throw new DialException("feature " + featureName + " is not declared in the domain");
				}
			}
		}
	}

	/**
	 * Extract values from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted values
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<Value> extractTypeValues(Node node) throws DialException {
		
		NodeList contentList = node.getChildNodes();

		List<Value> values = new LinkedList<Value>();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node valueNode = contentList.item(i);
			if (valueNode.getNodeName().equals("value")) {
				values.add(new BasicValue(valueNode.getTextContent()));
			}
			else if (valueNode.getNodeName().equals("range")) {
				values.add(new RangeValue(valueNode.getTextContent()));
			}
			else if (valueNode.getNodeName().equals("complexvalue")) {
				NodeList subValueNodes = valueNode.getChildNodes();
				
				String valueLabel="";

				for (int j = 0 ; j < subValueNodes.getLength() ; j++) {
					if (subValueNodes.item(j).getNodeName().equals("label")) {
						valueLabel = subValueNodes.item(j).getTextContent();
					}
				}
				
				List<FeatureType> features = extractContentFeatures(valueNode);
				
				ComplexValue complexVal = new ComplexValue(valueLabel);
				complexVal.addFeatures(features);
				values.add(complexVal);
				
			}
 		}
		return values;
	}


	/**
	 * Extracts referenced features from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<String> extractReferencedFeatures(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<String> features = new LinkedList<String>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("feature")) {

				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("ref") != null) {
					String featName = subnode.getAttributes().getNamedItem("ref").getNodeValue();
					features.add(featName);			
				}
			}
		}
		return features;
	}

	
	/**
	 * Extracts features from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<FeatureType> extractContentFeatures(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<FeatureType> features = new LinkedList<FeatureType>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node featNode = contentList.item(j);
			if (featNode.getNodeName().equals("feature")) {

				if (featNode.hasAttributes() && featNode.getAttributes().getNamedItem("name") != null) {
					String featName = featNode.getAttributes().getNamedItem("name").getNodeValue();
					FeatureType newFeat = new FeatureType(featName);
					
					List<Value> basicValues = extractTypeValues(featNode);
					newFeat.addFeatureValues(basicValues);
					features.add(newFeat);
				}
				else {
					throw new DialException("\"feature\" tag must have a reference or a content");
				}
			}
		}
		return features;
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
	private ObservationType getObservation(Node obsNode) throws DialException {

		ObservationType obs;

		if (obsNode.hasAttributes() && obsNode.getAttributes().getNamedItem("name") != null && 
				obsNode.getAttributes().getNamedItem("type")!= null &&
				obsNode.getAttributes().getNamedItem("content")!= null) {

			String name = obsNode.getAttributes().getNamedItem("name").getNodeValue();
			String type = obsNode.getAttributes().getNamedItem("type").getNodeValue();
			String content = obsNode.getAttributes().getNamedItem("content").getNodeValue();


			Observation trigger ;
			if (type.equals("utterance") ) {
				trigger = new UtteranceObservation(content);
				obs = new ObservationType(name, trigger);
			}
			else {
				throw new DialException("type " + type + " currently not supported");
			}			
		}
		else {
			throw new DialException("observation type not correctly specified (missing attributes)");
		}

		return obs;
	}



	/**
	 * 
	 * @param refDoc
	 * @return
	 * @throws DialException 
	 */
	private ActionType getAction(Node actionNode) throws DialException {

		ActionType action; 
		if (actionNode.hasAttributes() && actionNode.getAttributes().getNamedItem("name") != null) {

			String actionName = actionNode.getAttributes().getNamedItem("name").getNodeValue();
			action = new ActionType(actionName);

			List<Action> values = getActionValues(actionNode);
			action.addActionValues(values);
		}
		else {
			throw new DialException("action must have a \"name\" attribute");
		}


		return action;
	}


	private List<Action> getActionValues(Node topNode) {

		List<Action> values = new LinkedList<Action>();

		NodeList valueList = topNode.getChildNodes();
		for (int j = 0 ; j < valueList.getLength(); j++) {

			Node valueNode = valueList.item(j);

			if (valueNode.getNodeName().equals("value") && 
					valueNode.hasAttributes() && valueNode.getAttributes().getNamedItem("label") != null && 
					valueNode.getAttributes().getNamedItem("type")!= null &&
					valueNode.getAttributes().getNamedItem("content")!= null) {					
				String label = valueNode.getAttributes().getNamedItem("label").getNodeValue();
				String type = valueNode.getAttributes().getNamedItem("type").getNodeValue();
				String content = valueNode.getAttributes().getNamedItem("content").getNodeValue();

				Action option;
				if (type.equals("verbal")) {
					option = new VerbalAction(label, content);
					values.add(option);
				}
			}
		}
		return values;
	}


}
