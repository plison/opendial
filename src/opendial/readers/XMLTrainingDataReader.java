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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLTrainingDataReader {

	static Logger log = new Logger("XMLTrainingDataReader", Logger.Level.DEBUG);
	
	
	public static List<WoZDataPoint> extractTrainingSample (String dataFile) throws DialException {
		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(dataFile);
		Node mainNode = XMLUtils.getMainNode(doc);

		File f = new File(dataFile);
		String rootpath = f.getParent();	
		
		List<WoZDataPoint> sample = new LinkedList<WoZDataPoint>();
		for (int j = 0 ; j < mainNode.getChildNodes().getLength() ; j++) {

			Node node = mainNode.getChildNodes().item(j);	
			if (node.getNodeName().equals("data")) {
				BNetwork state = null;
				Assignment output = new Assignment();
				for (int k = 0 ; k < node.getChildNodes().getLength(); k++) {
					
					Node insideNode = node.getChildNodes().item(k);
					
					if (insideNode.getNodeName().equals("state")) {
						state = XMLStateReader.getBayesianNetwork(insideNode);
					}
					else if (insideNode.getNodeName().equals("output")) {
						BNetwork outputState = XMLStateReader.getBayesianNetwork(insideNode);
						for (String var : outputState.getNodeIds()) {
							output.addAssignment(new Assignment(var, outputState.getChanceNode(var).sample()));
						}
					}
				}
				
				if (state != null && !output.isEmpty()) {
					WoZDataPoint data = new WoZDataPoint(state, output);
					sample.add(data);
				}
			}
			else if (node.getNodeName().equals("import")) {
				String fileName = XMLUtils.getReference(node);
				// TODO: system independent reading of directory!
				List<WoZDataPoint> points = extractTrainingSample(rootpath+"/" + fileName);	
				sample.addAll(points);
			}

		}

		return sample;
	}


}
