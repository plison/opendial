// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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
import java.util.LinkedList;
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLInteractionReader {

	static Logger log = new Logger("XMLInteractionReader", Logger.Level.DEBUG);
	
	
	public static List<DialogueState> extractInteraction (String dataFile) throws DialException {
		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(dataFile);
		Node mainNode = XMLUtils.getMainNode(doc);

		File f = new File(dataFile);
		String rootpath = f.getParent();	
		
		List<DialogueState> sample = new LinkedList<DialogueState>();
		for (int j = 0 ; j < mainNode.getChildNodes().getLength() ; j++) {

			Node node = mainNode.getChildNodes().item(j);	
			if (node.getNodeName().contains("Turn")) {
				BNetwork state = XMLStateReader.getBayesianNetwork(node);
				sample.add(new DialogueState(state));
			}
			else if (node.getNodeName().equals("wizard")) {
				Assignment assign = Assignment.createFromString(node.getFirstChild().getNodeValue().trim());
				sample.get(sample.size()-1).addEvidence(assign);
			}
			else if (node.getNodeName().equals("import")) {
				String fileName = mainNode.getAttributes().getNamedItem("href").getNodeValue();
				List<DialogueState> points = extractInteraction(rootpath+"/" + fileName);	
				sample.addAll(points);
			}

		}

		return sample;
	}


}
