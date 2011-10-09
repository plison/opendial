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

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLInitialStateReader {

	static Logger log = new Logger("XMLInitialStateReader", Logger.Level.DEBUG);

	Domain domain;


	/**
	 * @param refDoc
	 * @return
	 * @throws DialException 
	 */
	public DialogueState getInitialState(Document doc, Domain domain) throws DialException {

		this.domain = domain;

		DialogueState state = new DialogueState();

		Node mainNode = XMLDomainReader.getMainNode(doc, "initialstate");
		NodeList mainNodeList = mainNode.getChildNodes();

		for (int i = 0; i < mainNodeList.getLength(); i++) {
			Node node = mainNodeList.item(i);
			if (node.hasAttributes() && node.getAttributes().getNamedItem("type") != null) {
				String typeStr = node.getAttributes().getNamedItem("type").getNodeValue();
				if (domain.hasType(typeStr)) {
					GenericType type = domain.getType(typeStr);

					if (node.getNodeName().equals("entity")) {
						EntityFluent entity = new EntityFluent(type);

						Map<String,Float> values = extractFluentValues(node);
						entity.addValues(values);

						List<Fluent> features = extractFeatures(node, entity);
						entity.addFeatures(features);

						if (node.getAttributes().getNamedItem("existsProb") != null) {
							float existsProb = Float.parseFloat(node.getAttributes().getNamedItem("existsProb").getNodeValue());
							entity.setExistenceProb(existsProb);
						}

						state.addFluent(entity);
					}
					else if (node.getNodeName().equals("variable")) {
						Fluent variable = new Fluent(type);

						Map<String,Float> values = extractFluentValues(node);
						variable.addValues(values);

						List<Fluent> features = extractFeatures(node, variable);
						variable.addFeatures(features);

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



	/**
	 * 
	 * @param node
	 * @param type
	 * @return
	 * @throws DialException 
	 */
	private List<Fluent> extractFeatures(Node node, Fluent fluent) throws DialException {
		List<Fluent> features = new LinkedList<Fluent>();
		features.addAll(extractFullFeatures(node, fluent.getType()));
		features.addAll(extractPartialFeatures(node, fluent));
		return features;
	}






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

			float prob = 1.0f;
			if (subnode.hasAttributes() && 
					subnode.getAttributes().getNamedItem("prob") != null) {

				try {
					prob = Float.parseFloat(subnode.getAttributes().getNamedItem("prob").getNodeValue());
				}
				catch (NumberFormatException e) {
					throw new DialException("probability " + 
							subnode.getAttributes().getNamedItem("prob").getNodeValue() +  " not valid");
				}
			}
			
			if (subnode.getNodeName().equals("value")) {

				String value = subnode.getTextContent();
				values.put(value,prob);
			}

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
	 * Extracts features from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<Fluent> extractFullFeatures(Node node, GenericType topEntityType) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<Fluent> types = new LinkedList<Fluent>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("feature")) {

				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("type") != null) {
					String featName = subnode.getAttributes().getNamedItem("type").getNodeValue();

					if (topEntityType instanceof GenericType && ((GenericType)topEntityType).hasFeature(featName)) {
						FeatureType type = ((GenericType)topEntityType).getFeature(featName);
						Fluent feat = new Fluent(type);

						Map<String,Float> values = extractFluentValues (subnode);
						feat.addValues(values);

						List<Fluent> features = extractFullFeatures(subnode, type);
						feat.addFeatures(features);

						types.add(feat);
					}
					else {
						log.debug("feature " + featName + " was not found in domain declarations");
						throw new DialException("feature " + featName + " was not found in domain declarations");
					}
				}
				else {
					throw new DialException("\"feature\" tag must have a type");
				}
			}
		}
		return types;
	}



	/**
	 * 
	 * @param node
	 * @param fluent
	 * @return
	 * @throws DialException 
	 */
	private List<ConditionalFluent> extractPartialFeatures(Node node, Fluent topFluent) throws DialException {
		List<ConditionalFluent> partialFeatures = new LinkedList<ConditionalFluent>();


		NodeList contentList = node.getChildNodes();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node valueNode = contentList.item(i);
			if (valueNode.getNodeName().equals("complexvalue")) {
				NodeList subValueNodes = valueNode.getChildNodes();

				String baseValue="";

				for (int j = 0 ; j < subValueNodes.getLength() ; j++) {

					Node insideNode = subValueNodes.item(j);
					if (insideNode.getNodeName().equals("label")) {
						baseValue = subValueNodes.item(j).getTextContent();
					}

					else if (insideNode.getNodeName().equals("feature") && 
							insideNode.hasAttributes() && 
							insideNode.getAttributes().getNamedItem("type")!=null) {

						String featTypeStr = insideNode.getAttributes().getNamedItem("type").getNodeValue();

						if (topFluent.getType().hasFeature(featTypeStr)) {
							FeatureType featType = topFluent.getType().getFeature(featTypeStr);

							if (featType.isDefinedForBaseValue(baseValue)) {
								ConditionalFluent condFluent = new ConditionalFluent(featType, topFluent);

								Map<String,Float> values = extractFluentValues(insideNode);
								condFluent.addValues(values);

								partialFeatures.add(condFluent);
							}
							else {
								throw new DialException("feature type " + featTypeStr + " not declared for value " + baseValue);
							}
						}		
						else {
							throw new DialException ("feature type " + featTypeStr + " not declared for type " + topFluent.getType());
						}

					}
				}
			}
		}

		return partialFeatures;
	}

}
