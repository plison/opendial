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

package opendial.domains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.datastructs.Template;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.state.StatePruner;
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
public class Model {

	static Logger log = new Logger("Model", Logger.Level.DEBUG);

	// identifier for the model
	String id;

	// counter for the model identifier, if not explicitly given
	public static int idCounter = 0;

	// triggers associated with the model
	List<Template> triggers;

	// collection of rules for the model
	Collection<Rule> rules;
	

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
		rules = new LinkedList<Rule>();
		id = "model" + idCounter;
		idCounter++;
	}


	public void start() {	}
	public void pause(boolean shouldBePaused) {	}
	
	
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
	public void addRule(Rule rule) {
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
	 * Returns the list of rules contained in the model
	 * 
	 * @return
	 */
	public Collection<Rule> getRules() {
		return new ArrayList<Rule>(rules);
	}


	public void trigger(DialogueState state, Set<String> updatedVars) {
		if (isTriggered(updatedVars)) {
			for (Rule r : rules) {
				try {
					state.applyRule(r); 
				}
				catch (DialException e) {
					log.warning("rule " + r.getRuleId() + " could not be applied: " + e.toString()); 
				}				
			}
		}
	}

	
	
	public boolean isTriggered(Collection<String> updatedVars) {
		
		if (rules.isEmpty()) {
			return false;
		}
			for (Template trigger : triggers) {
				for (String updatedVar : updatedVars) {
					if (trigger.match(updatedVar, true).isMatching()) {
						return true;
					}
				}
			}
		return false;
	}
	

	public Collection<Template> getTriggers() {
		return triggers;
	}

	

	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns the string representation of the model
	 */
	@Override
	public String toString() {
		String str =id;
		str += " [triggers=";
		for (Template trigger : triggers) {
			str+= "("+trigger+")" + " v ";
		}
		str = str.substring(0, str.length()-3) + "] with " + rules.size() + " rules: ";

		for (Rule rule : rules) {
			str += rule.getRuleId() + ",";
		}
		return str.substring(0, str.length()-1);
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

}
