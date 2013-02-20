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

package opendial.domains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.CaseBasedRule;
import opendial.domains.rules.PredictionRule;
import opendial.modules.SynchronousModule;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

/**
 * Representation of a rule model -- that is, a collection of rules of 
 * identical types (prediction, decision or update), associated with a
 * trigger on specific variables.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Model<T extends CaseBasedRule> implements SynchronousModule {

	static Logger log = new Logger("Model", Logger.Level.DEBUG);

	// identifier for the model
	String id;

	// counter for the model identifier, if not explicitly given
	public static int idCounter = 0;

	// triggers associated with the model
	List<Template> triggers;

	// collection of rules for the model
	Collection<T> rules;


	// ===================================
	//  MODEL CONSTRUCTION
	// ===================================


	/**
	 * Creates a new model, with initially no trigger and an empty list of rules
	 * 
	 * @param cls the rule class
	 */
	public Model() {
		triggers = new LinkedList<Template>();
		rules = new LinkedList<T>();
		id = "model" + idCounter;
		idCounter++;
	}

	/**
	 * Changes the identifier for the model
	 * 
	 * @param id the model identifier
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * Adds a new trigger to the model, defined by the variable label
	 * 
	 * @param trigger the variable
	 */
	public void addTrigger(String trigger) {
		triggers.add(new Template(trigger));
	}



	/**
	 * Adds a list of triggers to the model, defined by the variable label
	 * 
	 * @param triggers the list of triggers
	 */
	public void addTriggers(List<String> triggers) {
		for (String s : triggers) {
			addTrigger(s);
		}
	}


	/**
	 * Adds a new rule to the model
	 * 
	 * @param rule the rule to add
	 */
	public void addRule(T rule) {
		rules.add(rule);
	}



	// ===================================
	//  GETTERS
	// ===================================



	/**
	 * Returns the model identifier
	 * 
	 * @return the model identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the list of triggers for the model
	 * 
	 * @return the triggers
	 */
	public List<Template> getTriggers() {
		return new ArrayList<Template>(triggers);
	}


	/**
	 * Returns the list of rules contained in the model
	 * 
	 * @return
	 */
	public Collection<T> getRules() {
		return new ArrayList<T>(rules);
	}


	@Override
	public void trigger(DialogueState state) {
			for (CaseBasedRule r : rules) {
				try {	
					//		log.debug("applying rule " + r.getRuleId());
					state.applyRule(r); 
				}
				catch (DialException e) {
					log.warning("rule " + r.getRuleId() + " could not be applied: " + e.toString()); 
				}				
			}
	}

	/**
	 * Checks whether the model is triggered by the occurrence of a new node identifier
	 * (which must have the ' mark at the end of its identifier).  The trigger can be
	 * direct or indirect, in case the node is connected to an older prediction
	 *
	 * @param network the Bayesian network
	 * @param newNodeId variable label for the new node in the network
	 * @return true if the model is triggered, false otherwise
	 */
	public boolean isTriggered(DialogueState state) {

		for (String newNodeId : state.getVariablesToProcess()) {
			String trimmedNodeId = newNodeId.replaceAll("'", "");

			// direct triggers
			for (Template trigger : triggers) {
				if (trigger.isMatching(trimmedNodeId, false) 
						&& !state.getNetwork().hasActionNode(newNodeId)) {
						return true;
				}
			}

			// indirect triggers
			/** if (!network.getNode(newNodeId).getOutputNodes().isEmpty()) {
			List<String> influencedNodes = getInfluencedNodeIds(network, newNodeId);
			for (String trigger : triggers) {
				if (influencedNodes.contains(trigger)) {
					log.debug("model " + id + "(trigger=" + trigger + ") indirectly triggered via " + newNodeId);
					return true;
				}
			}
		} */
		}
		return false;
	}



	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns the string representation of the model
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str =id;
		str += " [triggers=";
		for (Template trigger : triggers) {
			str+= "("+trigger+")" + " v ";
		}
		str = str.substring(0, str.length()-3) + "] with " + rules.size() + " rules:\n";

		for (CaseBasedRule rule : rules) {
			str += rule.toString() + "\n";
		}
		return str;
	}


	/**
	 * Returns the hashcode for the model
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return id.hashCode() + triggers.hashCode() - rules.hashCode();
	}


	/**
	 * Extract the identifiers of all nodes that may be indirectly influenced by the
	 * appearance of the new node
	 * 
	 * @param network the Bayesian network
	 * @param newNodeId the new node id
	 * @return the list of node identifiers that may be influenced
	 */
	/** private static List<String> getInfluencedNodeIds(BNetwork network, String newNodeId) {

		List<String> influencedNodes = new ArrayList<String>();

		// indirect trigger, in case the new node is related to an older prediction
		String predictionEquiv = StringUtils.removeSpecifiers(newNodeId)+ "^p";
		if (network.hasNode(predictionEquiv) && 
				network.getNode(predictionEquiv).getOutputNodes().equals(network.getNode(newNodeId).getOutputNodes())) {

			List<String> predictAncestors = network.getNode(predictionEquiv).getAncestorIds();

			// add all the nodes that are ancestors of the prediction nodes,
			// plus the ones that are the children of these ancestors
			for (String anc : predictAncestors) {
				influencedNodes.add(anc);
				influencedNodes.addAll(network.getNode(anc).getDescendantIds());
			}
		}
		return influencedNodes;
	}
*/

	public Class<? extends CaseBasedRule> getModelType() {
		if (rules.isEmpty()) {
			log.warning("cannot determine model type");
			return CaseBasedRule.class;
		}
		else {
			return rules.iterator().next().getClass();
		}
	}
}
