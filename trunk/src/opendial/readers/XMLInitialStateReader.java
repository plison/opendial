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
import opendial.domains.Domain;
import opendial.domains.types.FeatureType;
import opendial.domains.types.GenericType;
import opendial.state.ConditionalFluent;
import opendial.state.DialogueState;
import opendial.state.EntityFluent;
import opendial.state.Fluent;
import opendial.utils.Logger;
import opendial.utils.XMLUtils;

/**
 * XML reader for the initial state specification
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLInitialStateReader {

	// logger
	static Logger log = new Logger("XMLInitialStateReader", Logger.Level.DEBUG);

	// corresponding domain (assumes that the variable types are already declared)
	Domain domain;


	
	// ===================================
	//  INITIAL STATE
	// ===================================

	
	
	/**
	 * Returns the initial state from the XML document, for the given domain (where the
	 * variable types are already declared)
	 * 
	 * @param refDoc the XML document
	 * @return the corresponding dialogue state
	 * @throws DialException if XML document is ill-formatted
	 */
	public DialogueState getInitialState(Document doc, Domain domain) throws DialException {

		this.domain = domain;

		DialogueState state = new DialogueState();

		Node mainNode = XMLUtils.getMainNode(doc, "initialstate");
		NodeList mainNodeList = mainNode.getChildNodes();

		for (int i = 0; i < mainNodeList.getLength(); i++) {
			Node node = mainNodeList.item(i);
			if (node.hasAttributes() && node.getAttributes().getNamedItem("type") != null) {
				String typeStr = node.getAttributes().getNamedItem("type").getNodeValue();
				if (domain.hasType(typeStr)) {
					GenericType type = domain.getType(typeStr);

					// entities
					if (node.getNodeName().equals("entity")) {
						EntityFluent entity = new EntityFluent(type);

						entity.addValues(extractFluentValues(node));

						entity.addFeatures(extractFeatures(node, entity));

						if (node.getAttributes().getNamedItem("existsProb") != null) {
							float existsProb = Float.parseFloat(node.getAttributes().getNamedItem("existsProb").getNodeValue());
							entity.setExistenceProb(existsProb);
						}
						state.addFluent(entity);
					}
					
					// fixed variables
					else if (node.getNodeName().equals("variable")) {
						Fluent variable = new Fluent(type);

						variable.addValues(extractFluentValues(node));

						variable.addFeatures(extractFeatures(node, variable));

						state.addFluent(variable);
					}
				}
				else {
					log.debug("type not found: " + typeStr);
					throw new DialException("type " + typeStr +  " was not found in domain declarations");
				}
			}
		}
		return state;
	}


	// ===================================
	//  INITIAL STATE VALUES
	// ===================================
	
	
	/**
	 * Extract values from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted values
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private Map<String,Float> extractFluentValues(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		Map<String,Float> values = new HashMap<String,Float>();

		for (int i = 0 ; i < contentList.getLength() ; i++) {
			
			Node subnode = contentList.item(i);

			// extracting the value probability
			float prob = getValueProbability (subnode);
			
			// adding a standard value
			if (subnode.getNodeName().equals("value")) {
				String value = subnode.getTextContent();
				values.put(value,prob);
			}

			// adding a complex value
			else if (subnode.getNodeName().equals("complexvalue")) {
				NodeList subValueNodes = subnode.getChildNodes();

				String valueLabel="";
				for (int j = 0 ; j < subValueNodes.getLength() ; j++) {
					if (subValueNodes.item(j).getNodeName().equals("label")) {
						valueLabel = subValueNodes.item(j).getTextContent();
						values.put(valueLabel, prob);
					}
				}
			}
		}
		return values;
	}

	
	
	/**
	 * Returns the probability of the value defined in the XML node
	 * (default to 1.0f is none is declared)
	 * 
	 * @param node the XML node
	 * @return the value probability
	 * @throws DialException if probability is ill-formatted
	 */
	private float getValueProbability (Node node) throws DialException {
		
		float prob = 1.0f;
		
		if (node.hasAttributes() && 
				node.getAttributes().getNamedItem("prob") != null) {
			String probStr = node.getAttributes().getNamedItem("prob").getNodeValue();
			
			try { prob = Float.parseFloat(probStr);	}
			catch (NumberFormatException e) {
				throw new DialException("probability " + probStr +  " not valid");
			}
		}
		return prob;
	}
	
	
	// ===================================
	//  INITIAL STATE FEATURES
	// ===================================

	
	/**
	 * Extracts all (partial or full) features of the initial state from the XML node
	 * 
	 * @param node the XML node
	 * @param fluent the fluent where the feature is defined (necessary for partial features)
	 * @return the list of feature fluents
	 * @throws DialException 
	 */
	private List<ConditionalFluent> extractFeatures(Node node, Fluent fluent) throws DialException {
		List<ConditionalFluent> features = new LinkedList<ConditionalFluent>();
		features.addAll(extractFullFeatures(node, fluent));
		features.addAll(extractPartialFeatures(node, fluent));
		return features;
	}


	// ===================================
	//  FULL FEATURES
	// ===================================


	/**
	 * Extracts (full feature) conditional fluents from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features fluents
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<ConditionalFluent> extractFullFeatures(Node node, 
			Fluent fluent) throws DialException {
		
		NodeList contentList = node.getChildNodes();

		List<ConditionalFluent> cfluents = new LinkedList<ConditionalFluent>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("feature")) {

				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("type") != null) {
					String featName = subnode.getAttributes().getNamedItem("type").getNodeValue();

					if (fluent.getType().hasFeature(featName)) {
						
						// creating a new conditional fluent
						FeatureType type = fluent.getType().getFeature(featName);
						ConditionalFluent feat = new ConditionalFluent(type, fluent);

						// populating it with values
						Map<String,Float> values = extractFluentValues (subnode);
						feat.addValues(values);

						// extracting subfeatures
						List<ConditionalFluent> subfeatures = extractFeatures(subnode, feat);
						feat.addFeatures(subfeatures);

						cfluents.add(feat);
					}
					else {
						throw new DialException("feature " + featName + 
								" was not found in domain declarations");
					}
				}
				else {
					throw new DialException("\"feature\" tag must have a type");
				}
			}
		}
		return cfluents;
	}


	
	// ===================================
	//  PARTIAL FEATURES
	// ===================================

	

	/**
	 * Extracts the list of partial feature fluents from the XML node
	 * TODO: make this fully recursive (?)
	 * 
	 * @param node the XML node
	 * @param fluent the base fluent
	 * @return the list of partial feature fluents
	 * @throws DialException if the XML node is ill-formatted
	 */
	private List<ConditionalFluent> extractPartialFeatures(Node node, 
			Fluent topFluent) throws DialException {
		
		List<ConditionalFluent> partialFeatures = new LinkedList<ConditionalFluent>();

		NodeList contentList = node.getChildNodes();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node valueNode = contentList.item(i);
			if (valueNode.getNodeName().equals("complexvalue")) {
				NodeList subValueNodes = valueNode.getChildNodes();

				String baseValue= extractBaseValue (valueNode);

				for (int j = 0 ; j < subValueNodes.getLength() ; j++) {

					Node insideNode = subValueNodes.item(j);

					if (insideNode.getNodeName().equals("feature") && 
							insideNode.hasAttributes() && 
							insideNode.getAttributes().getNamedItem("type")!=null) {
						
						ConditionalFluent partialFeat = 
							extractPartialFeature (insideNode, topFluent, baseValue);
						
						partialFeatures.add(partialFeat);
					}
				}
			}
		}
		return partialFeatures;
	}
	
	
	/**
	 * Extract the partial feature fluent declared inside a XML complex value node
	 * 
	 * @param node the XML node
	 * @param topFluent the top fluent for the feature
	 * @param baseValue the base value for which the feature is declared
	 * @return the corresponding conditional fluent
	 * @throws DialException if the node is ill-formatted
	 */
	private ConditionalFluent extractPartialFeature(Node node, Fluent topFluent, 
			String baseValue) throws DialException {

		String featTypeStr = node.getAttributes().getNamedItem("type").getNodeValue();

		if (topFluent.getType().hasFeature(featTypeStr)) {
			FeatureType featType = topFluent.getType().getFeature(featTypeStr);

			if (featType.isDefinedForBaseValue(baseValue)) {
				
				// creating new conditional fluent
				ConditionalFluent condFluent = new ConditionalFluent(featType, topFluent);

				// populating it with values
				Map<String,Float> values = extractFluentValues(node);
				condFluent.addValues(values);

				return condFluent;
			}
			else {
				throw new DialException("feature type " + featTypeStr + 
						" not declared for value " + baseValue);
			}
		}		
		else {
			throw new DialException ("feature type " + featTypeStr + 
					" not declared for type " + topFluent.getType());
		}
	}


	/**
	 * Returns the base value specified in a complex value node. 
	 * 
	 * @param node the XML node
	 * @return the specified base value.
	 * @throws DialException if no base value can be found
	 */
	private String extractBaseValue(Node node) throws DialException {
		
		String baseValue = "";
		
		NodeList subValueNodes = node.getChildNodes();
		for (int j = 0 ; j < subValueNodes.getLength() ; j++) {
			Node insideNode = subValueNodes.item(j);
			if (insideNode.getNodeName().equals("label")) {
				baseValue = subValueNodes.item(j).getTextContent();
			}
		}
		
		if (baseValue.equals("")) {
			throw new DialException("base value not specified in node");
		}
		return baseValue;
	}

}
