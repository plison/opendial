// =================================================================                                                                   
// Copyright (C) 2009-2011 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.imports;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.EntityType;
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

	public static final String domainSchema = "resources//schemata//domain.xsd";


	public static Domain extractDomain(String topDomainFile) throws IOException, DialException {

		Document doc = getXMLDocument(topDomainFile);

		String rootpath = topDomainFile.substring(0, topDomainFile.lastIndexOf("//")+1);

		Domain domain = new Domain("");

		NodeList top = doc.getChildNodes();
		for (int i = 0 ; i < top.getLength(); i++) {
			Node topNode = top.item(i);
			NodeList firstElements = topNode.getChildNodes();
			for (int j = 0 ; j < firstElements.getLength() ; j++) {
				Node node = firstElements.item(j);

				if (node.getNodeName().equals("declarations")) {
					String fileReference = getReference(node);
					Document refDoc = getXMLDocument(rootpath+fileReference);
					List<EntityType> entityTypes = getEntityTypes(refDoc);
					domain.addEntityTypes(entityTypes);
				}
			}
		}


		return domain;
	}

	

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



	private static List<String> extractValues(Node node) throws DialException {
		NodeList contentList = node.getChildNodes();

		List<String> values = new LinkedList<String>();

		for (int j = 0 ; j < contentList.getLength() ; j++) {

			Node subnode = contentList.item(j);
			if (subnode.getNodeName().equals("value")) {
				values.add(subnode.getNodeValue());
			}
		}
		return values;
	}



	private static List<EntityType> extractFeatures(Node node) throws DialException {
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

	private static List<EntityType> getEntityTypes(Document doc) throws DialException {

		List<EntityType> entityTypes = new LinkedList<EntityType>();

		NodeList topList =doc.getChildNodes();

		if (topList.getLength() == 1 && topList.item(0).getNodeName().equals("declarations")) {

			NodeList midList = topList.item(0).getChildNodes();

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
		}
		else if (topList.getLength() == 0) {
			throw new DialException("Document is empty");
		}
		else  {
			throw new DialException("Document contains other tags than \"declarations\": " + topList.item(0).getNodeName());
		}

		return entityTypes;

	}
}
