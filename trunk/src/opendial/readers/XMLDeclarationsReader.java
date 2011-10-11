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

import opendial.arch.DialConstants.PrimitiveType;
import opendial.arch.DialException;
import opendial.domains.actions.SurfaceRealisationTemplate;
import opendial.domains.observations.SurfaceTrigger;
import opendial.domains.types.GenericType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.values.ActionValue;
import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.ObservationValue;
import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.utils.Logger;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML reader for declarations types.  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDeclarationsReader {

	static Logger log = new Logger("XMLDeclarationsReader", Logger.Level.DEBUG);
	
	
	// ===================================
	//  GENERIC TYPES
	// ===================================


	/**
	 * Extracts the type declarations from the XML document
	 * 
	 * @param doc the XML document
	 * @return the list of types which have been declared
	 * @throws DialException if the document is ill-formatted
	 */
	public List<GenericType> getTypes(Document doc) throws DialException {

		List<GenericType> allTypes = new LinkedList<GenericType>();

		// getting the main node
		Node mainNode = XMLUtils.getMainNode(doc,"declarations");
		NodeList midList = mainNode.getChildNodes();

		for (int i = 0 ; i < midList.getLength() ; i++) {
			Node node = midList.item(i);

			if (node.hasAttributes() && node.getAttributes().getNamedItem("name") != null) {

				// entities
				if (node.getNodeName().equals("entity")) {
					GenericType type = getGenericType(node);
					allTypes.add(type);
				}
				
				// fixed variables
				else if (node.getNodeName().equals("variable")) {
					GenericType type = getGenericType(node);
					type.setAsFixed(true);
					allTypes.add(type);
				}
				
				// observations
				else if (node.getNodeName().equals("trigger")) {
					GenericType type = getObservation(node);
					type.setAsFixed(true);
					allTypes.add(type);
				}
				
				// actions
				else if (node.getNodeName().equals("actiontemplate")) {
					GenericType type = getAction(node);
					allTypes.add(type);
				}
				else {
					throw new DialException("declaration type not recognised");
				}
			}
			
			// if the type does not specify a name
			else if (!node.getNodeName().equals("#text") && (!node.getNodeName().equals("#comment"))){
				log.debug("node name: " + node.getNodeName());
				throw new DialException("name attribute not provided");
			}
		}

		return allTypes;
	}

	
	
	/**
	 * Returns the generic type declared in the XML node
	 * 
	 * @param node the XML node
	 * @return the corresponding generic type
	 * @throws DialException if the node is ill-formatted
	 */
	private GenericType getGenericType(Node node) throws DialException {
		String name = node.getAttributes().getNamedItem("name").getNodeValue();

		// create the type
		GenericType type = new GenericType(name);
		
		// adding the type values
		type.addValues(extractTypeValues (node));
		
		// adding the type features
		type.addFeatures(extractFeatures(node));
		return type;
	}
	

	// ===================================
	// TYPES VALUES
	// ===================================

	
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
			
			// basic value
			if (valueNode.getNodeName().equals("value")) {
				values.add(new BasicValue<String>(valueNode.getTextContent()));
			}
			
			// range value
			else if (valueNode.getNodeName().equals("range")) {
				if (valueNode.getTextContent().equals("string")) {
					values.add(new RangeValue(PrimitiveType.STRING));
				}
				else if (valueNode.getTextContent().equals("integer")) {
					values.add(new RangeValue(PrimitiveType.INTEGER));					
				}
				else if (valueNode.getTextContent().equals("float")) {
					values.add(new RangeValue(PrimitiveType.FLOAT));
				}
				else if (valueNode.getTextContent().equals("boolean")) {
					values.add(new RangeValue(PrimitiveType.BOOLEAN));
				}
			}
			
			// complex value (with partial features)
			else if (valueNode.getNodeName().equals("complexvalue")) {
				NodeList subValueNodes = valueNode.getChildNodes();
				
				String valueLabel="";

				for (int j = 0 ; j < subValueNodes.getLength() ; j++) {
					if (subValueNodes.item(j).getNodeName().equals("label")) {
						valueLabel = subValueNodes.item(j).getTextContent();
						values.add(new BasicValue<String>(valueLabel));
					}
				}
			}
 		}
		return values;
	}


	// ===================================
	//  FEATURE TYPES
	// ===================================

	
	
	/**
	 * Extract (full and partial) features from the XML node
	 * 
	 * @param node the XML node
	 * @return the list of extracted features
	 * @throws DialException if the node is ill-formatted
	 */
	private List<FeatureType> extractFeatures(Node node) throws DialException {
		List<FeatureType> allFeatures = new LinkedList<FeatureType>();
		allFeatures.addAll(extractFullFeatures(node));
		allFeatures.addAll(extractPartialFeatures(node));
		return allFeatures;
	}
	
	
	/**
	 * Extract partial features from the XML node
	 * 
	 * @param node the XML node
	 * @return the list of extracted features
	 * @throws DialException if the node is ill-formatted
	 */
	private List<FeatureType> extractPartialFeatures(Node node) throws DialException {
		
		List<FeatureType> partialFeatures = new LinkedList<FeatureType>();
		
		NodeList contentList = node.getChildNodes();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node valueNode = contentList.item(i);
			
			// if a complex value is declared
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
						
						// creating a partial feature
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
	 * Extracts full features from a XML node
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
	//  OBSERVATION TYPES
	// ===================================


	/**
	 * Returns the corresponding observation type from the XML node
	 * 
	 * @param obsNode the XML node
	 * @return the extracted type
	 * @throws DialException if node is ill-formatted
	 */
	private GenericType getObservation(Node obsNode) throws DialException {

		GenericType obs;

		if (obsNode.hasAttributes() && obsNode.getAttributes().getNamedItem("name") != null && 
				obsNode.getAttributes().getNamedItem("type")!= null &&
				obsNode.getAttributes().getNamedItem("content")!= null) {

			String name = obsNode.getAttributes().getNamedItem("name").getNodeValue();
			String type = obsNode.getAttributes().getNamedItem("type").getNodeValue();
			String content = obsNode.getAttributes().getNamedItem("content").getNodeValue();


			ObservationValue<String> obsValue;
			if (type.equals("surface") ) {
				obsValue = new ObservationValue<String>(new SurfaceTrigger(content));
				obs = new GenericType(name);
				obs.addValue(obsValue);
				
				List<FeatureType> feats = getObservationFeatures(obsValue);
				obs.addFeatures(feats);
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
	 * Returns the (implicit) features associated with the observation value
	 * 
	 * @param value the observation value
	 * @return the list of implicit features
	 */
	private List<FeatureType> getObservationFeatures(ObservationValue<?> value) {
		
		List<FeatureType> feats = new LinkedList<FeatureType>();
		
			if (!value.getTrigger().getSlots().isEmpty()) {
				for (String slot : value.getTrigger().getSlots()) {
					FeatureType feat = new FeatureType(slot);
					feats.add(feat);
				}
			}
		return feats;
	}
	
	
	// ===================================
	//  ACTION TYPES
	// ===================================

	

	/**
	 * Returns the corresponding action type from the XML node
	 * 
	 * @param actionNode the XML node
	 * @return the extracted type
	 * @throws DialException if the node is ill-formatted
	 */
	private GenericType getAction(Node actionNode) throws DialException {

		GenericType action; 
		if (actionNode.hasAttributes() && actionNode.getAttributes().getNamedItem("name") != null) {

			String actionName = actionNode.getAttributes().getNamedItem("name").getNodeValue();
			action = new GenericType(actionName);

			List<ActionValue<?>> values = getActionValues(actionNode);
			action.addValues(values);
			
			List<FeatureType> feats = getActionFeatures(values);
			action.addFeatures(feats);
		}
		else {
			throw new DialException("action must have a \"name\" attribute");
		}


		return action;
	}
	
	
	/**
	 * Returns the values for the XML node declaring an action
	 * 
	 * @param topNode the XML node
	 * @return the extracted values
	 */
	private List<ActionValue<?>> getActionValues(Node topNode) {

		List<ActionValue<?>> values = new LinkedList<ActionValue<?>>();

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

				ActionValue<String> option;
				if (type.equals("surface")) {
					option = new ActionValue<String>(label, new SurfaceRealisationTemplate(content));
					values.add(option);
				}
			}
		}
		return values;
	}



	/**
	 * Returns the (implicit) features defined in the action values
	 * 
	 * @param values the action values
	 * @return the list of implicit features
	 */
	private List<FeatureType> getActionFeatures(List<ActionValue<?>> values) {
		
		List<FeatureType> feats = new LinkedList<FeatureType>();
		
		for (ActionValue<?> value: values) {
		if (value instanceof ActionValue &&  !((ActionValue<?>)value).getTemplate().getSlots().isEmpty()) {
			for (String slot : ((ActionValue<?>)value).getTemplate().getSlots()) {
				FeatureType feat = new FeatureType(slot);
				feat.addBaseValue(value.getValue());
				feats.add(feat);
			}
		}
		}
		
		return feats;
	}


}
