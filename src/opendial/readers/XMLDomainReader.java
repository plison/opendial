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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.utils.XMLUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainReader {

	public static Logger log = new Logger("XMLDomainReader", Logger.Level.DEBUG);

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
	public static Domain extractDomain(String topDomainFile) throws DialException {
		
		// create a new, empty domain
		Domain domain = new Domain();
		
		if (priorValidation) {
			XMLUtils.validateXML(topDomainFile, domainSchema);
		}

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(topDomainFile);

		Node mainNode = XMLUtils.getMainNode(doc);

		// determine the root path and filename
		File f = new File(topDomainFile);
		String rootpath = f.getParent();		
		String filename = f.getName();

		if (mainNode.hasAttributes() && 
				mainNode.getAttributes().getNamedItem("name") != null) {
			domain.setName(mainNode.getAttributes().getNamedItem("name").getNodeValue());
		}
		else {
			domain.setName(filename);
		}

		NodeList firstElements = mainNode.getChildNodes();
		for (int j = 0 ; j < firstElements.getLength() ; j++) {
			
			Node node = firstElements.item(j);	
			domain = extractPartialDomain(node, domain, rootpath);
		}
		
		return domain;
	}
	
	
	public static Domain extractPartialDomain (Node mainNode, Domain domain, String rootpath) throws DialException {

			// extracting initial state
			if (mainNode.getNodeName().equals("initialstate")) {
				BNetwork state = XMLInitialStateReader.getInitialState(mainNode, domain);
				domain.setInitialState(new DialogueState(state));
		//		log.debug(state);
			}
			
			// extracting rule-based probabilistic model
			else if (mainNode.getNodeName().equals("model")) {
				Model model = XMLModelReader.getModel(mainNode, domain);
		//		log.debug(model);
				domain.addModel(model);
			}
			
			else if (mainNode.getNodeName().equals("parameters")) {
				BNetwork parameters = XMLInitialStateReader.getInitialState(mainNode, domain);
				domain.addParameters(parameters);
			}
			
			
			// extracting imported references
			else if (mainNode.getNodeName().equals("import")) {
				String fileName = XMLUtils.getReference(mainNode);
				// TODO: system independent reading of directory!
				Document subdoc = XMLUtils.getXMLDocument(rootpath+"/" + fileName);
				domain = extractPartialDomain(XMLUtils.getMainNode(subdoc), domain, rootpath);		
			}
			
		
		return domain;
	}


}
