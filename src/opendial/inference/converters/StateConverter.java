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

import opendial.arch.DialException;
import opendial.domains.values.Value;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.bn.BNode;
import opendial.inference.distribs.ProbabilityTable;
import opendial.state.DialogueState;
import opendial.state.Fluent;
import opendial.utils.Logger;

/**
 * A conversion utility for generating a Bayesian network out of the dialogue
 * state (defined as a list of fluents).  
 * 
 * <p>The Bayesian network incorporates various types of dependencies between the 
 * values of the fluents, such as existence dependencies (for entities with an 
 * existence  probability < 1.0f) and partial features (only defined for specific 
 * values of the root fluent).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StateConverter {

	// logger
	static Logger log = new Logger("StateConverter", Logger.Level.DEBUG);

	// mapping between fluents and their corresponding nodes
	Map<Fluent,BNode> fluentsToBNodes;

	
	
	
	// ===================================
	//  GENERIC METHODS
	// ===================================

	
	
	/**
	 * Creates a Bayesian network corresponding to the fluents included in
	 * the dialogue state.  
	 * 
	 * @param state the dialogue state
	 * @return the corresponding Bayesian network
	 * @throws DialException if a conversion error occurs
	 */
	public BNetwork buildBayesianNetwork (DialogueState state) throws DialException {

		// initialise the mapping between fluents and nodes
		fluentsToBNodes = new HashMap<Fluent,BNode>();

		// creates an empty Bayesian network
		BNetwork bn = new BNetwork();

		// creates the nodes for each root fluent in the dialogue state
		for (Fluent fluent : state.getFluents()) {
			bn.addNodes(processTopFluent(fluent));
		}

		return bn;
	}


	/**
	 * Processes a top fluent (i.e. an entity or a fixed variable, but not a feature),
	 * and creates the BN nodes corresponding to it.  
	 * 
	 * <p>In addition to the "main" node which is directly linked to the node content, 
	 * other nodes can also be created, such as existence nodes (if the existence 
	 * probability of the entity is < 1.0f) and features nodes if features are defined 
	 * for the fluent.
	 * 
	 * @param fluent the fluent to convert
	 * @return the corresponding list of Bayesian network nodes
	 * @throws DialException if a conversion error occurs
	 */
	private List<BNode> processTopFluent (Fluent fluent) throws DialException {

		List<BNode> nodes = new LinkedList<BNode>();

		// if the fluent has an existence probability < 1.0f, create an existence node
		if (fluent.getExistenceProb() < 1.0f) {
			BNode enode = createExistenceNode(fluent);
			BNode mainNode = createNodeWithExistenceDependency(fluent, enode);
			fluentsToBNodes.put(fluent, mainNode);
			nodes.add(enode);
			nodes.add(mainNode);
		}
		
		// else, create an independent node
		else {
			BNode mainNode = createIndependentNode(fluent);
			fluentsToBNodes.put(fluent, mainNode);
			nodes.add(mainNode);
		}

		// create additional nodes for the features attached to the fluent
		Map<Fluent,Fluent> fluentHierarchy = new HashMap<Fluent,Fluent>();	
		for (Fluent cfluent : fluent.getFeatures()) {
			fluentHierarchy.put(cfluent, fluent);
			nodes.addAll(processFeatureFluent(cfluent, fluentHierarchy));
		} 

		return nodes;
	}


	/**
	 * Creates the nodes corresponding to the information contained in a "feature"
	 * fluent, with the appropriate conditional dependencies if the feature is 
	 * partially defined or attached to a fluent with an existence probability.
	 * 
	 * @param featFluent the feature fluent to convert
	 * @param fluentHierarchy the hierarchy of feature fluents
	 * @return the corresponding list of Bayesian Network nodes
	 * @throws DialException if a conversion error occurs
	 */
	private List<BNode> processFeatureFluent(Fluent featFluent,  
			Map<Fluent,Fluent> fluentHierarchy) throws DialException {

		List<BNode> nodes = new LinkedList<BNode>();

		boolean foundAttachment = false;
		Fluent curFluent = featFluent;

		while (!foundAttachment) {
			
			// get the parent fluent of the current one
			Fluent  parentFluent = fluentHierarchy.get(curFluent);
			
			// if the parent fluent defines curFluent as a partial feature (only valid
			// for specific values), create a dependent node with value dependency
			if (parentFluent.getType().hasPartialFeature(curFluent.getType().getName())) {
				BNode conditionalNode = fluentsToBNodes.get(parentFluent);
				Value baseValue = parentFluent.getType().getBaseValueForPartialFeature(curFluent.getType().getName());
				BNode featNode = createNodeWithValueDependency(featFluent, conditionalNode, baseValue);
				fluentsToBNodes.put(featFluent, featNode);
				nodes.add(featNode);
				foundAttachment = true;
			}
			
			// else, if the feature hierarchy only contains full features...
			else if (!fluentHierarchy.containsKey(parentFluent)) {
				
				// case 1, the parent is an entity with existence probability:  In this
				// context, we create a node with an existence dependency
				if (parentFluent.getExistenceProb() < 1.0f) {
					BNode entityNode = fluentsToBNodes.get(parentFluent);
					BNode existenceNode = entityNode.getInputNodes().get(0);
					BNode mainNode = createNodeWithExistenceDependency(featFluent, existenceNode);
					fluentsToBNodes.put(featFluent, mainNode);
					nodes.add(mainNode);
					foundAttachment = true;
				}
				
				// case 2, no existence dependency: the feature is then an independent node
				else {
					BNode mainNode = createIndependentNode(featFluent);
					fluentsToBNodes.put(featFluent, mainNode);
					nodes.add(mainNode);
				}
			}
			
			// if no attachment has been found, we go one step higher in the hierarchy
			curFluent = parentFluent;
		}

		// also creates nodes for the sub-features of this feature
		for (Fluent cfluent : featFluent.getFeatures()) {
			fluentHierarchy.put(cfluent, featFluent);
			nodes.addAll(processFeatureFluent(cfluent, fluentHierarchy));
		} 

		return nodes;
	}


	// ========================================
	//  CREATION OF BAYESIAN NETWORK NODES
	// ========================================


	
	
	/**
	 * Creates an independent Bayesian Network node over the values 
	 * defined in the fluent
	 * 
	 * @param fluent the fluent to convert
	 * @return the corresponding BN node
	 * @throws DialException if a conversion error occurs 
	 * 			(ill-formed distribution for instance)
	 */
	public BNode createIndependentNode(Fluent fluent) throws DialException {

		String label = fluent.getLabel();

		BNode node = new BNode(label, fluent.getValues().keySet());

		ProbabilityTable distrib = new ProbabilityTable(node);

		float total = 0.0f;
		for (Object val : fluent.getValues().keySet()) {
			distrib.addRow(new Assignment(node.getId(), val), fluent.getValues().get(val));
			total += fluent.getValues().get(val);
		}

		node.addValue("None");
		distrib.addHeadVariable(node.getId(), Arrays.asList((Object)"None"));
		distrib.addRow(new Assignment(node.getId(), "None"), 1.0f - total);

		node.setDistribution(distrib);

		return node;
	}
	


	/**
	 * Creates an existence node for the fluent, with two values: true/false, with
	 * the probability defined in the fluent. 
	 * 
	 * @param fluent the fluent for which the existence node is creates
	 * @return the existence node
	 * @throws DialException if distribution is ill-formed
	 */
	public BNode createExistenceNode(Fluent fluent) throws DialException {

		// create a new node
		BNode node = new BNode("Exists(" + fluent.getLabel() + ")");

		// set the distribution
		ProbabilityTable distrib = new ProbabilityTable(node);
		distrib.addRow(new Assignment(node.getId(), Boolean.TRUE), fluent.getExistenceProb());
		distrib.addRow(new Assignment(node.getId(), Boolean.FALSE), 1.0f - fluent.getExistenceProb());		
		node.setDistribution(distrib);

		return node;
	}



	/**
	 * Creates a node with an existence dependency for the fluent, with the existence
	 * node already given as parameter.  
	 * 
	 * @param fluent the fluent to convert
	 * @param existenceNode the already created existence node
	 * @return the 
	 * @throws DialException
	 */
	private BNode createNodeWithExistenceDependency(Fluent fluent, BNode existenceNode) throws DialException {

		// create a new node
		String label = fluent.getLabel();
		BNode node = new BNode(label, fluent.getValues().keySet());
		node.addValue("None");

		// add the existence node to the input
		node.addInputNode(existenceNode);

		// create a new distribution
		ProbabilityTable distrib = new ProbabilityTable(node);

		// define the basic assignments for the existence node
		Assignment trueAssign = new Assignment(existenceNode.getId(), Boolean.TRUE);
		Assignment falseAssign = new Assignment(existenceNode.getId(), Boolean.FALSE);

		// loop on the node values and define the probability table for them
		float total = 0.0f;
		for (Object val : fluent.getValues().keySet()) {
			distrib.addRow(new Assignment(trueAssign, fluent.getLabel(), val), fluent.getValues().get(val));
			distrib.addRow(new Assignment(falseAssign, fluent.getLabel(), val), 0.0f);
			total += fluent.getValues().get(val);
		}

		// add the probabilities for the "None" value
		distrib.addRow(new Assignment(trueAssign, node.getId(), "None"), 1.0f - total);
		distrib.addRow(new Assignment(falseAssign, fluent.getLabel(), "None"), 1.0f);

		node.setDistribution(distrib);

		return node;
	}



	/**
	 * Creates a Bayesian Network node for a partial feature (defined only for a
	 * specific value of the base fluent).
	 * 
	 * 
	 * @param featFluent the feature fluent
	 * @param baseFluent the base fluent (to which the partial feature is attached)
	 * @return the corresponding Bayesian Network node
	 * @throws DialException if resulting probability table is ill-formed
	 */
	public BNode createNodeWithValueDependency (Fluent featFluent, BNode conditionalNode, Value baseValue) throws DialException {

		// create a new node
		BNode node = new BNode(featFluent.getLabel(), featFluent.getValues().keySet());
		node.addValue("None");

		// attach the conditional node
		node.addInputNode(conditionalNode);

		// set the probability table
		ProbabilityTable distrib= new ProbabilityTable(node); 

		// loop on the conditional node values
		for (Object baseVal : conditionalNode.getValues()) {

			// check if the partial feature is defined for the conditional node value
			if (baseValue.containsValue(baseVal)) {
				
				Assignment base = new Assignment(conditionalNode.getId(), baseVal);

				float total = 0.0f;

				// loop on the feature values, and populate the CPT
				for (Object val : featFluent.getValues().keySet()) {
					Assignment full = new Assignment(base, node.getId(), val);
					distrib.addRow(full, featFluent.getValues().get(val));
					total += featFluent.getValues().get(val);
				}

				distrib.addRow(new Assignment(base, node.getId(), "None"), 1.0f - total);

			}
			
			// else, set all probability mass onto the "None" value
			else {
				Assignment base = new Assignment(conditionalNode.getId(), baseVal);
				for (Object val : featFluent.getValues().keySet()) {
					distrib.addRow(new Assignment(base, node.getId(), val), 0.0f);
				}
				distrib.addRow(new Assignment(base, node.getId(), "None"), 1.0f);
			}
		}	

		node.setDistribution(distrib);

		return node;
	} 

}
