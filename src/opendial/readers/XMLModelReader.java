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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;


import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.CaseBasedRule;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLModelReader {

	static Logger log = new Logger("XMLModelReader", Logger.Level.DEBUG);


	public static Model<? extends CaseBasedRule> getModel(Node topNode, Domain domain) throws DialException {

		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("type")== null) {
			throw new DialException("Model must declare its type as attribute: [update, prediction, decision]");
		}
		
		String typeStr = topNode.getAttributes().getNamedItem("type").getNodeValue();
		if (typeStr.equalsIgnoreCase("update")) {
			return createModel(topNode, domain, UpdateRule.class);
		}
		else if (typeStr.equalsIgnoreCase("prediction")) {
			return createModel(topNode, domain, PredictionRule.class);
		}
		else if (typeStr.equalsIgnoreCase("decision")) {
			return createModel(topNode, domain, DecisionRule.class);
		}
		throw new DialException("Model type is not supported, must be [update, prediction, decision]");
		
	}
	
	
	private static <T extends CaseBasedRule> Model<T> createModel(Node topNode, Domain domain, Class<T> cls) throws DialException {
		Model<T> model = new Model<T>();
		for (int i = 0 ; i < topNode.getChildNodes().getLength() ; i++) {
			Node node = topNode.getChildNodes().item(i);
			if (node.getNodeName().equals("rule")) {
				T rule = XMLRuleReader.getRule(node, cls);
				model.addRule(rule);
			}
		}
		
		model.addTriggers(getTriggers(topNode, domain));

		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("id")!= null) {
			String id = topNode.getAttributes().getNamedItem("id").getNodeValue();
			model.setId(id);
		}
		
		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("view")!= null) {
			String viewing = topNode.getAttributes().getNamedItem("view").getNodeValue();
			boolean viewingBool = Boolean.parseBoolean(viewing);
		}

		return model;
	}


	private static List<String> getTriggers (Node topNode, Domain domain) throws DialException {

		List<String> triggers = new LinkedList<String>();

		if (topNode.hasAttributes() && topNode.getAttributes().getNamedItem("trigger")!= null) {
			String[] triggerArray = topNode.getAttributes().getNamedItem("trigger").getNodeValue().split(",");
			triggers.addAll(Arrays.asList(triggerArray));			
		}

		return triggers;
	}


}
