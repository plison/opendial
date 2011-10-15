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

package opendial.inference.converters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import opendial.arch.DialException;
import opendial.domains.Type;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.inference.bn.distribs.Distribution;
import opendial.inference.bn.distribs.ProbabilityTable;
import opendial.state.DialogueState;
import opendial.state.Fluent;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StateConverter {
	
	static Logger log = new Logger("StateConverter", Logger.Level.DEBUG);
	
	Map<Fluent,BNode> mapping;
	
	
	
	
	public BNetwork buildBayesianNetwork (DialogueState state) {
	
		mapping = new HashMap<Fluent,BNode>();
		
		BNetwork bn = new BNetwork();
		
		for (Fluent fluent : state.getFluents()) {
			try {
					bn.addNodes(processRootFluent(fluent));
			} 
			catch (DialException e) {
				log.warning("Error building node from fluent " + fluent);
			}
		}
		
		return bn;
	}
	

	public List<BNode> processRootFluent (Fluent fluent) throws DialException {
		
		List<BNode> nodes = new LinkedList<BNode>();
		
		if (fluent.getExistenceProb() < 1.0f) {
		BNode enode = createExistenceNode(fluent);
		nodes.add(enode);
		BNode mainNode = createEntityNode(fluent, enode);
		mapping.put(fluent, mainNode);
		nodes.add(mainNode);
		
		}
		else {
			BNode mainNode = createBasicNode(fluent);
			mapping.put(fluent, mainNode);
			nodes.add(mainNode);
		}
		
		List<Fluent> depStack = new LinkedList<Fluent>();
		depStack.add(fluent);

		for (Fluent cfluent : fluent.getFeatures()) {
			if (!cfluent.getLabel().equals("Exists")) {
			nodes.addAll(processFeatureFluent(cfluent, depStack));
		}
		} 
			
		return nodes;
	}
	
	
	
	public List<BNode> processFeatureFluent(Fluent fluent, List<Fluent> baseFluents) throws DialException {
		
		List<BNode> nodes = new LinkedList<BNode>();
	
		log.debug("fluent to analyse: " + fluent);
		log.debug("values: " + fluent.getValues().keySet());
		boolean found = false;
		Fluent previousFluent = fluent;
		for (int i = baseFluents.size() -1 ; i >= 0 && !found ; i--) {
			Fluent baseFluent = baseFluents.get(i);
			if (baseFluent.getType().hasPartialFeature(previousFluent.getType().getName())) {
				BNode featNode = createFeatureNode(fluent, baseFluent);
				mapping.put(fluent, featNode);
				nodes.add(featNode);
				found = true;
			}
			if (i==0) {
				if (baseFluent.getExistenceProb() < 1.0f) {
					BNode entityNode = mapping.get(baseFluent);
					BNode existenceNode = entityNode.getInputNodes().get(0);
					BNode mainNode = createEntityNode(fluent, existenceNode);
					mapping.put(fluent, mainNode);
					nodes.add(mainNode);
					found = true;
				}
			}
			previousFluent = baseFluent;
		}
		
		baseFluents.add(fluent);

		for (Fluent cfluent : fluent.getFeatures()) {
			if (!cfluent.getLabel().equals("Exists")) {
			nodes.addAll(processFeatureFluent(cfluent, baseFluents));
		}
		} 
		
		return nodes;
	}
	

	
	/**
	 * 
	 * @param fluent
	 * @param enode
	 * @return
	 * @throws DialException 
	 */
	private static BNode createEntityNode(Fluent fluent, BNode enode) throws DialException {
		
		String label = fluent.getLabel();
		
		BNode node = new BNode(label, fluent.getValues().keySet());
		
		node.addInputNode(enode);
		
		ProbabilityTable distrib = new ProbabilityTable(node);
		
		float total = 0.0f;
		Assignment trueAssign = new Assignment(enode.getId(), Boolean.TRUE);
		Assignment falseAssign = new Assignment(enode.getId(), Boolean.FALSE);
		
		for (Object val : fluent.getValues().keySet()) {
			distrib.addRow(new Assignment(trueAssign, fluent.getLabel(), val), fluent.getValues().get(val));
			distrib.addRow(new Assignment(falseAssign, fluent.getLabel(), val), 0.0f);
			total += fluent.getValues().get(val);
		}
		
		node.addValue("None");
		distrib.addHeadVariable(node.getId(), Arrays.asList((Object)"None"));

		distrib.addRow(new Assignment(falseAssign, fluent.getLabel(), "None"), 1.0f);
		
		if (total < 1.0f) {
			distrib.addRow(new Assignment(node.getId(), "None"), 1.0f - total);
		}
		
		node.setDistribution(distrib);
		
		return node;
	}


	public static BNode createExistenceNode(Fluent fluent) throws DialException {
		
		BNode node = new BNode("Exists(" + fluent.getLabel() + ")");
		
		ProbabilityTable distrib = new ProbabilityTable(node);
		distrib.addRow(new Assignment(node.getId(), Boolean.TRUE), fluent.getExistenceProb());
		distrib.addRow(new Assignment(node.getId(), Boolean.FALSE), 1.0f - fluent.getExistenceProb());		
		
		node.setDistribution(distrib);
		
		return node;
	}
	
	
	/**
	 * Creates an independent node from the definition of a state fluent,
	 * complete with values and probability distribution
	 * 
	 * @param fluent the fluent to convert
	 * @throws DialException if problem arises in the conversion
	 */
	public static BNode createBasicNode(Fluent fluent) throws DialException {
		
		String label = fluent.getLabel();

		BNode node = new BNode(label, fluent.getValues().keySet());

		ProbabilityTable distrib = new ProbabilityTable(node);
		
		float total = 0.0f;
		for (Object val : fluent.getValues().keySet()) {
			distrib.addRow(new Assignment(node.getId(), val), fluent.getValues().get(val));
			total += fluent.getValues().get(val);
		}
		if (total < 1.0f) {
			node.addValue("None");
			distrib.addHeadVariable(node.getId(), Arrays.asList((Object)"None"));
			distrib.addRow(new Assignment(node.getId(), "None"), 1.0f - total);
		}
		
		node.setDistribution(distrib);
		
		return node;
	}
	

	
	
	public BNode createFeatureNode (Fluent cfluent, Fluent baseFluent) throws DialException {
		
		BNode baseNode = mapping.get(baseFluent);

		log.debug("creating feature: " + cfluent.getLabel());
		
			BNode node = new BNode(cfluent.getLabel(), cfluent.getValues().keySet());
			
			node.addInputNode(baseNode);
			Distribution distrib = createDependentProbabilityTable(cfluent, node, baseNode, baseFluent);	
			node.setDistribution(distrib);
			return node;
	} 
	
	
	
	public static Distribution createDependentProbabilityTable(Fluent cfluent, BNode node, BNode baseNode, Fluent baseFluent) {
		
		ProbabilityTable distrib= new ProbabilityTable(node); 
					
		for (Object baseVal : baseNode.getValues()) {
	
			if (baseFluent.getType().hasPartialFeature(cfluent.getType().getName(), baseVal)) {
			Assignment base = new Assignment(baseNode.getId(), baseVal);

			float total = 0.0f;

			for (Object val : cfluent.getValues().keySet()) {
				Assignment full = new Assignment(base, node.getId(), val);
				distrib.addRow(full, cfluent.getValues().get(val));
				total += cfluent.getValues().get(val);
			}
			
			if (total < 1.0f) {
				node.addValue("None");
				distrib.addHeadVariable(node.getId(), Arrays.asList((Object)"None"));
				distrib.addRow(new Assignment(node.getId(), "None"), 1.0f - total);
			}
			}
			else {
				Assignment base = new Assignment(baseNode.getId(), baseVal);
				for (Object val : cfluent.getValues().keySet()) {
					Assignment full = new Assignment(base, node.getId(), val);
					distrib.addRow(full, 0.0f);
				}
				node.addValue("None");
				distrib.addHeadVariable(node.getId(), Arrays.asList((Object)"None"));
				distrib.addRow(new Assignment(base, node.getId(), "None"), 1.0f);
			}
		}	
		log.debug("distrib: " + distrib);
 
		return distrib;
	} 
	
}
