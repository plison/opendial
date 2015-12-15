// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import opendial.DialogueState;
import opendial.domains.rules.Rule;
import opendial.templates.Template;

/**
 * Representation of a rule model -- that is, a collection of rules of identical
 * types (prediction, decision or update), associated with a trigger on specific
 * variables.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Model {

	final static Logger log = Logger.getLogger("OpenDial");

	// identifier for the model
	String id;

	// whether triggering the model should block other models
	boolean blocking = false;

	// counter for the model identifier, if not explicitly given
	public static int idCounter = 0;

	// triggers associated with the model
	List<Template> triggers;

	// collection of rules for the model
	Collection<Rule> rules;

	// ===================================
	// MODEL CONSTRUCTION
	// ===================================

	/**
	 * Creates a new model, with initially no trigger and an empty list of rules
	 */
	public Model() {
		triggers = new LinkedList<Template>();
		rules = new LinkedList<Rule>();
		id = "model" + idCounter;
		idCounter++;
	}

	public void start() {
	}

	public void pause(boolean shouldBePaused) {
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
		triggers.add(Template.create(trigger));
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

	/**
	 * Sets the model as "blocking" (forbids other models to be triggered in parallel
	 * when this model is triggered). Default is false.
	 * 
	 * @param blocking whether to set the model in blocking mode or not
	 */
	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	// ===================================
	// GETTERS
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
	 * @return the list of rules
	 */
	public List<Rule> getRules() {
		return new ArrayList<Rule>(rules);
	}

	/**
	 * Triggers the model with the given state and list of recently updated
	 * variables.
	 * 
	 * @param state the current dialogue state
	 * @return true if the state has been changed, false otherwise
	 */
	public boolean trigger(DialogueState state) {
		for (Rule r : rules) {
			try {
				state.applyRule(r);
			}
			catch (RuntimeException e) {
				log.warning("rule " + r.getRuleId() + " could not be applied: "
						+ e.toString());
				e.printStackTrace();
			}
		}
		return !state.getNewVariables().isEmpty();
	}

	/**
	 * Returns true if the model is triggered by the updated variables.
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the updated variables
	 * @return true if triggered, false otherwise
	 */
	public boolean isTriggered(DialogueState state, Collection<String> updatedVars) {

		if (rules.isEmpty()) {
			return false;
		}
		for (Template trigger : triggers) {
			for (String updatedVar : updatedVars) {
				if (trigger.match(updatedVar).isMatching()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if the model is triggered by the updated variables.
	 * 
	 * @param updatedVars the updated variables
	 * @return true if triggered, false otherwise
	 */
	public boolean isTriggered(Collection<String> updatedVars) {

		if (rules.isEmpty()) {
			return false;
		}
		for (Template trigger : triggers) {
			for (String updatedVar : updatedVars) {
				if (trigger.match(updatedVar).isMatching()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the model triggers
	 * 
	 * @return the model triggers
	 */
	public Collection<Template> getTriggers() {
		return triggers;
	}

	/**
	 * Returns true if the model is set in "blocking" mode and false otherwise.
	 * 
	 * @return whether blocking mode is activated or not (default is false).
	 */
	public boolean isBlocking() {
		return blocking;
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns the string representation of the model
	 */
	@Override
	public String toString() {
		String str = id;
		str += " [triggers=";
		for (Template trigger : triggers) {
			str += "(" + trigger + ")" + " v ";
		}
		str = str.substring(0, str.length() - 3) + "] with " + rules.size()
				+ " rules: ";

		for (Rule rule : rules) {
			str += rule.getRuleId() + ",";
		}
		return str.substring(0, str.length() - 1);
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
