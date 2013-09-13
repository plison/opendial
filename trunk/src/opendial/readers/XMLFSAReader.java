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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.domains.fsa.Edge;
import opendial.domains.fsa.FSA;
import opendial.domains.fsa.State;
import opendial.domains.rules.conditions.Condition;
import opendial.utils.XMLUtils;

public class XMLFSAReader {

	// logger
	public static Logger log = new Logger("FSAReader", Logger.Level.DEBUG);
	
	
	public static FSA extractFSA(String FSAFile) throws DialException {

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(FSAFile);

		Node mainNode = XMLUtils.getMainNode(doc);
		
		return extractFSA(mainNode);
	}

		
		
	public static FSA extractFSA(Node mainNode) throws DialException {
		String triggerVar = "";
		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("trigger")!= null) {
			triggerVar = mainNode.getAttributes().getNamedItem("trigger").getNodeValue();
		}
		else {
			throw new DialException("must specify trigger variable!");
		}
		
		String actionVar = "";
		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("actionvar")!= null) {
			actionVar = mainNode.getAttributes().getNamedItem("actionvar").getNodeValue();
		}
		else {
			throw new DialException("must specify action variable!");
		}
		String initState = "";
		if (mainNode.hasAttributes() && mainNode.getAttributes().getNamedItem("initstate")!= null) {
			initState = mainNode.getAttributes().getNamedItem("initstate").getNodeValue();
		}
		else {
			throw new DialException("must specify initial state!");
		}
		
		List<State> states = null;
		List<Edge> edges = null;
		
		NodeList firstElements = mainNode.getChildNodes();
		for (int i = 0 ; i < firstElements.getLength() ; i++) {
			Node node = firstElements.item(i);
			if (node.getNodeName().equals("states")) {
				states = extractStates(node);
			}
			else if (node.getNodeName().equals("edges")) {
				edges = extractEdges(node);
			}
			else if (node.getNodeName().equals("conditions")) {
				Map<String,Condition> conditions = extractConditions(node);
				for (Edge edge : edges) {
					if (!edge.getConditionPtr().equals("")) {
						if (conditions.containsKey(edge.getConditionPtr())) {
							Condition condition = conditions.get(edge.getConditionPtr());
							edge.setCondition(condition);
						}
						else {
							throw new DialException("could not link condition in edge : " 
									+ edge.getConditionPtr());
						}
					}				
				}
			}
		}
		
		FSA fsa = new FSA(triggerVar, actionVar, states, edges, initState);
		return fsa;		
	}


	private static List<Edge> extractEdges(Node topNode) throws DialException {
		List<Edge> edges = new ArrayList<Edge>();
		for (int i = 0 ; i < topNode.getChildNodes().getLength() ; i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("edge")) {
				if (node.hasAttributes() 
					&& node.getAttributes().getNamedItem("source")!= null 
					&& node.getAttributes().getNamedItem("target")!= null) {
					String source = node.getAttributes().getNamedItem("source").getTextContent();
					String target = node.getAttributes().getNamedItem("target").getTextContent();
					Edge edge = new Edge(source, target);
					edges.add(edge);
					
					if (node.getAttributes().getNamedItem("condition") != null) {
						String conditionPtr = node.getAttributes().
								getNamedItem("condition").getTextContent();
						edge.setConditionPointer(conditionPtr);
					}
					
					if (node.getAttributes().getNamedItem("threshold") != null) {
						double threshold = Double.parseDouble(node.getAttributes().
								getNamedItem("threshold").getTextContent());
						edge.setThreshold(threshold);
					}
					if (node.getAttributes().getNamedItem("priority") != null) {
						int priority = Integer.parseInt(node.getAttributes().
								getNamedItem("priority").getTextContent());
						edge.setPriority(priority);
					}
					
			}
			else {
				throw new DialException ("edge must specify source and target");
			}
				
			}
		}
		return edges;
	}


	private static List<State> extractStates(Node topNode) throws DialException {
		List<State> states = new ArrayList<State>();
		for (int i = 0 ; i < topNode.getChildNodes().getLength() ; i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("state")) {
				if (node.hasAttributes() && node.getAttributes().getNamedItem("id")!= null) {
					State state = new State(node.getAttributes().getNamedItem("id").getTextContent());
					states.add(state);
					if (node.getAttributes().getNamedItem("action") != null) {
						state.setAction(node.getAttributes().getNamedItem("action").getTextContent());
					}
				}
				else {
					throw new DialException ("state must have an identifier");
				}
			}
		}
		return states;
	}


	private static Map<String, Condition> extractConditions(Node topNode) throws DialException {
		Map<String,Condition> conditions = new HashMap<String,Condition>();
		for (int i = 0 ; i < topNode.getChildNodes().getLength() ; i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("condition")) {
				if (node.hasAttributes() && node.getAttributes().getNamedItem("id")!= null) {
					String condId = node.getAttributes().getNamedItem("id").getTextContent();
					Condition condition = XMLRuleReader.getFullCondition(node);
					conditions.put(condId, condition);
				}
				else {
					throw new DialException ("condition must have an identifier");
				}
			}
		}
		return conditions;
	}

	
}

