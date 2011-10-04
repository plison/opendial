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

import java.util.LinkedList;
import java.util.List;

import opendial.arch.DialException;
import opendial.domains.realisations.Realisation;
import opendial.domains.realisations.SurfaceRealisation;
import opendial.domains.triggers.SurfaceTrigger;
import opendial.domains.triggers.Trigger;
import opendial.domains.types.AbstractType;
import opendial.domains.types.ActionType;
import opendial.domains.types.EntityType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.FixedVariableType;
import opendial.domains.types.ObservationType;
import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.utils.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDeclarationsReader {

	static Logger log = new Logger("XMLDeclarationsReader", Logger.Level.DEBUG);
	

	/**
	 * Extracts the entity type declarations from the XML document
	 * 
	 * @param doc the XML document
	 * @return the list of entity types which have been declared
	 * @throws DialException if the document is ill-formatted
	 */
	public List<AbstractType> getTypes(Document doc) throws DialException {

		List<AbstractType> allTypes = new LinkedList<AbstractType>();

		Node mainNode = XMLDomainReader.getMainNode(doc,"declarations");
		NodeList midList = mainNode.getChildNodes();

		for (int i = 0 ; i < midList.getLength() ; i++) {
			Node node = midList.item(i);


			if (node.hasAttributes() && node.getAttributes().getNamedItem("name") != null) {

				if (node.getNodeName().equals("entity")) {
					EntityType type = getEntity(node);
					allTypes.add(type);
				}
				else if (node.getNodeName().equals("variable")) {
					FixedVariableType type = getFixedVariableType(node);
					allTypes.add(type);
				}
				else if (node.getNodeName().equals("feature")) {
		//			FeatureType type = getFeatureType(node);
		//			allTypes.add(type);
				}
				else if (node.getNodeName().equals("trigger")) {
					ObservationType type = getObservation(node);
					allTypes.add(type);
				}
				else if (node.getNodeName().equals("realisation")) {
					ActionType type = getAction(node);
					allTypes.add(type);
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

		return allTypes;
	}

	/**
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private FixedVariableType getFixedVariableType(Node node) throws DialException {
		String name = node.getAttributes().getNamedItem("name").getNodeValue();

		FixedVariableType type = new FixedVariableType(name);
		List<Value> values = extractTypeValues (node);
		type.addValues(values);
		List<FeatureType> features = extractFeatures(node);
		type.addFeatures(features);
		return type;
	}

	/**
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private EntityType getEntity(Node node) throws DialException {
		String name = node.getAttributes().getNamedItem("name").getNodeValue();

		EntityType type = new EntityType(name);
		List<Value> values = extractTypeValues (node);
		type.addValues(values);
		List<FeatureType> features = extractFeatures(node);
		type.addFeatures(features);
		return type;
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
						values.add(new BasicValue(valueLabel));
					}
				}
			}
 		}
		return values;
	}


	public List<FeatureType> extractFeatures(Node node) throws DialException {
		List<FeatureType> allFeatures = new LinkedList<FeatureType>();
		allFeatures.addAll(extractFullFeatures(node));
		allFeatures.addAll(extractPartialFeatures(node));
		return allFeatures;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 * @throws DialException 
	 */
	private List<FeatureType> extractPartialFeatures(Node node) throws DialException {
		
		List<FeatureType> partialFeatures = new LinkedList<FeatureType>();
		
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
							insideNode.getAttributes().getNamedItem("name")!=null) {
						
						String featLabel = insideNode.getAttributes().getNamedItem("name").getNodeValue();
						FeatureType featType = new FeatureType(featLabel);
						List<Value> basicValues = extractTypeValues(insideNode);
						featType.addValues(basicValues);
						
						featType.addBaseValue(baseValue);
						
						partialFeatures.add(featType);
					}
				}
			}
 		}
		
		return partialFeatures;
	}

	/**
	 * Extracts features from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted features
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private List<FeatureType> extractFullFeatures(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<FeatureType> features = new LinkedList<FeatureType>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node featNode = contentList.item(j);
			if (featNode.getNodeName().equals("feature")) {

				if (featNode.hasAttributes() && featNode.getAttributes().getNamedItem("name") != null) {
					String featName = featNode.getAttributes().getNamedItem("name").getNodeValue();
					FeatureType newFeat = new FeatureType(featName);
					
					List<Value> basicValues = extractTypeValues(featNode);
					newFeat.addValues(basicValues);
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


			Trigger trigger ;
			if (type.equals("surface") ) {
				trigger = new SurfaceTrigger(content);
				obs = new ObservationType(name, trigger);
			}
			else {
				throw new DialException("type " + type + " currently not supported");
			}			
		}
		else {
			throw new DialException("trigger type not correctly specified (missing attributes)");
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

			List<Realisation> values = getActionValues(actionNode);
			action.addActionValues(values);
		}
		else {
			throw new DialException("action must have a \"name\" attribute");
		}


		return action;
	}


	private List<Realisation> getActionValues(Node topNode) {

		List<Realisation> values = new LinkedList<Realisation>();

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

				Realisation option;
				if (type.equals("surface")) {
					option = new SurfaceRealisation(label, content);
					values.add(option);
				}
			}
		}
		return values;
	}


}
