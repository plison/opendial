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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.domains.Domain;
import opendial.modules.asr.CFGRule;
import opendial.modules.asr.RecognitionGrammar;
import opendial.utils.XMLUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLASRGrammarReader {

	static Logger log = new Logger("XMLASRGrammarReader", Logger.Level.DEBUG);

	public static RecognitionGrammar extractGrammar (String grammarFile) throws DialException {

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(grammarFile);

		Node mainNode = XMLUtils.getMainNode(doc);

		NodeList firstElements = mainNode.getChildNodes();

		String topElement = "";
		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("topElement")!= null) {
			topElement = mainNode.getAttributes().getNamedItem("topElement").getNodeValue();
		}
		else {
			throw new DialException("must specify top element!");
		}
		RecognitionGrammar grammar = new RecognitionGrammar(topElement);

		for (int j = 0 ; j < firstElements.getLength() ; j++) {

			Node node = firstElements.item(j);	
			if (node.getNodeName().equals("rule")) {
				CFGRule rule = extractRule(node);
				grammar.addRule(rule);
			}

		}

		return grammar;
	}


	public static CFGRule extractRule(Node node) throws DialException {

		String lhs;
		if (node.hasAttributes() && node.getAttributes().getNamedItem("lhs")!=null) {
			lhs = node.getAttributes().getNamedItem("lhs").getNodeValue();
		}
		else {
			throw new DialException("must specify left hand side");
		}
		CFGRule rule = new CFGRule(lhs);
		
		for (int i = 0 ; i < node.getChildNodes().getLength(); i++) {
			Node subnode = node.getChildNodes().item(i);
			if (subnode.getNodeName().equals("rhs")) {
				rule.addRightHandSide(subnode.getTextContent());
			}
		}
		
		return rule;
	}
}
