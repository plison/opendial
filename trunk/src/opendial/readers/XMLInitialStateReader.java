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
import opendial.domains.types.StandardType;
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
public class XMLInitialStateReader {

	static Logger log = new Logger("XMLInitialStateReader", Logger.Level.NORMAL);
	
	Domain domain;
	

	/**
	 * TODO: implement this method
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
			if (node.getNodeName().equals("entity") && node.hasAttributes() && 
					node.getAttributes().getNamedItem("type") != null) {
				String name = node.getAttributes().getNamedItem("type").getNodeValue();
				if (domain.hasType(name)) {
					StandardType type = domain.getType(name);
					StateEntity entity = new StateEntity(type);

					Map<String,Float> values = extractStateEntityValues(node);
					entity.addValues(values);

					List<StateEntity> features = extractStateEntityFeatures(node, type);
					entity.addFeatures(features);

					state.addEntity(entity);
				}
				else {
					log.debug("type not found: " + name);
					throw new DialException("type " + name +  " was not found in domain declarations");
				}

			}
		}

		return state;
	}



	/**
	 * Extract values from a XML node
	 * 
	 * @param node the node
	 * @return the list of extracted values
	 * @throws DialException if the XML fragment is ill-formatted
	 */
	private Map<String,Float> extractStateEntityValues(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		Map<String,Float> values = new HashMap<String,Float>();

		for (int i = 0 ; i < contentList.getLength() ; i++) {

			Node subnode = contentList.item(i);
			if (subnode.getNodeName().equals("value") && subnode.hasAttributes() && subnode.getAttributes().getNamedItem("prob") != null) {

				String value = subnode.getTextContent();
				float prob = 0.0f;
				try {
					prob = Float.parseFloat(subnode.getAttributes().getNamedItem("prob").getNodeValue());
				}
				catch (NumberFormatException e) {
					throw new DialException("probability " + subnode.getAttributes().getNamedItem("prob").getNodeValue() +  " not valid");
				}
				values.put(value,prob);
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
	private List<StateEntity> extractStateEntityFeatures(Node node, StandardType topEntityType) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<StateEntity> types = new LinkedList<StateEntity>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("feature")) {

				if (subnode.hasAttributes() && subnode.getAttributes().getNamedItem("type") != null) {
					String featName = subnode.getAttributes().getNamedItem("type").getNodeValue();

					// TODO: add feature type here
					if (topEntityType instanceof StandardType && ((StandardType)topEntityType).hasFeature(featName)) {
						FeatureType type = ((StandardType)topEntityType).getFeature(featName);
						StateEntity feat = new StateEntity(type);

						Map<String,Float> values = extractStateEntityValues (subnode);
						feat.addValues(values);

						List<StateEntity> features = extractStateEntityFeatures(subnode, type);
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

}
