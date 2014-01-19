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

package opendial.state;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.approximate.LikelihoodWeighting;
import opendial.inference.exact.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.modules.Module;
import opendial.state.anchoring.AnchoredRule;
import opendial.state.distribs.EquivalenceDistribution;
import opendial.state.distribs.OutputDistribution;
import opendial.state.nodes.ProbabilityRuleNode;
import opendial.state.nodes.UtilityRuleNode;
import opendial.utils.StringUtils;


/**
 * Representation of a dialogue state.  A dialogue state is essentially a directed 
 * graphical model (i.e. a Bayesian or decision network) over a set of specific 
 * state variables.  Probabilistic rules can be applied on this dialogue state in 
 * order to update its content.  After applying the rules, the dialogue state
 * is usually pruned to only retain relevant state variables. 
 * 
 * 
 * The dialogue state may also include an assignment of evidence values. A subset of 
 * state variables can be marked as denoting parameter variables.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class DialogueState extends BNetwork {

	// logger
	public static Logger log = new Logger("DialogueState", Logger.Level.DEBUG);

	// evidence values for state variables
	Assignment evidence;
	
	/** Subset of variables that denote parameters */
	Set<String> parameterVars;

	// ===================================
	//  DIALOGUE STATE CONSTRUCTION
	// ===================================


	/**
	 * Creates a new, empty dialogue state.
	 */
	public DialogueState() {
		this(new BNetwork());	
	}

	/**
	 * Creates a new dialogue state that contains the Bayesian network provided
	 * as argument.
	 * 
	 * @param network the Bayesian network to include
	 */
	public DialogueState(BNetwork network) {
		super();
		super.reset(network);
		evidence = new Assignment();
		parameterVars = new HashSet<String>();
	}


	/**
	 * Resets the content of the dialogue state to the network
	 * contained as argument (and deletes the rest).
	 * 
	 */
	public void reset(BNetwork network) {
		evidence.removePairs(getChanceNodeIds());
		super.reset(network);
	}



	/**
	 * Clear the assignment of values for the variables provided as
	 * argument
	 *  
	 * @param variables the variables for which to clear the assignment
	 */
	public void clearEvidence(Set<String> variables) {
		evidence.removePairs(variables);
	}


	/**
	 * Adds a new assignment of values to the evidence
	 * 
	 * @param assignment the assignment of values to add
	 */
	public void addEvidence(Assignment assignment) {
		evidence.addAssignment(assignment);
	}



	/**
	 * Adds a set of parameter variables to the dialogue state.
	 * 
	 * @param parameters the parameters
	 * @throws DialException if the inclusion of parameters failed
	 */
	public void setParameters(BNetwork parameters) throws DialException {
		addNetwork(parameters);
		this.parameterVars.clear();
		this.parameterVars.addAll(parameters.getChanceNodeIds());
	}




	// ===================================
	//  STATE UPDATE
	// ===================================


	/**
	 * Adds the assignment provided as argument to the dialogue state.  If the
	 * state variables in the categorical table already exist, they are erased.
	 * 
	 * <p>It should be noted that the method only adds the content to the dialogue
	 * state but does not trigger models or modules following this change.  In order
	 * to trigger such chain of updates, the method addContent(...) in DialogueSystem
	 * should be used instead.
	 * 
	 * @param distrib a distribution over values for particular state variables
	 * @throws DialException if the content could not be added.
	 */
	public void addToState(Assignment assign) throws DialException {
		addToState(new CategoricalTable(assign));
	}

	/**
	 * Adds the content provided as argument to the dialogue state.  If the
	 * state variables in the categorical table already exist, they are erased.
	 * 
	 * <p>It should be noted that the method only adds the content to the dialogue
	 * state but does not trigger models or modules following this change.  In order
	 * to trigger such chain of updates, the method addContent(...) in DialogueSystem
	 * should be used instead.
	 * 
	 * @param distrib a distribution over values for particular state variables
	 * @throws DialException if the content could not be added.
	 */
	public void addToState(CategoricalTable distrib) throws DialException {

		for (String headVar : distrib.getHeadVariables()) {
			distrib.modifyVariableId(headVar, headVar + "'");
			headVar = headVar + "'";
			CategoricalTable marginal = distrib.getMarginalTable(headVar);
			ChanceNode newNode = new ChanceNode(headVar, marginal);

			if (hasNode(headVar)) {
				BNode toRemove = getNode(headVar);
				removeNodes(toRemove.getDescendantIds());
				removeNode(toRemove.getId());
			}

			addNode(newNode);
			connectToPredictions(newNode);
		}
	}



	/**
	 * Merges the dialogue state included as argument into the current one.
	 * 
	 * @param newState the state to merge into the current state
	 * @throws DialException if the new dialogue state could not be merged
	 */
	public void addToState(DialogueState newState) throws DialException {
		addToState((BNetwork)newState);
		evidence.addAssignment(newState.getEvidence().addPrimes());
	}
	


	/**
	 * Merges the dialogue state included as argument into the current one.
	 * 
	 * @param newState the state to merge into the current state
	 * @throws DialException if the new dialogue state could not be merged
	 */
	public void addToState(BNetwork newState) throws DialException {
		for (ChanceNode cn : new ArrayList<ChanceNode>(newState.getChanceNodes())) {
			cn.setId(cn.getId()+ "'");		
			addNode(cn);
			connectToPredictions(cn);
		}
	}



	/**
	 * Applies a (probability or utility) rule to the dialogue state: <ul>
	 * <li> For a probability rule, the method creates a chance node containing 
	 * the rule effects depending on the input variables of the rule, and connected 
	 * via outgoing edges to the output variables. 
	 * <li> For a utility rule, the method creates a utility node specifying the
	 * utility of particular actions specified by the rule depending on the input
	 * variables.
	 * </ul>
	 * 
	 * <p>The method creates both the rule node, its corresponding output or action 
	 * nodes, and the directed edges resulting from the rule application.  See Pierre 
	 * Lison's PhD thesis, Section 4.3 for details.
	 * 
	 * @param r the rule to apply.
	 * @throws DialException if the rule could not be applied.
	 */
	public void applyRule(Rule r) throws DialException {

		AnchoredRule arule = new AnchoredRule(r, this);
		
		// first case: probability rule
		if (r.getRuleType() == RuleType.PROB && arule.isRelevant()) {
			ProbabilityRuleNode ruleNode = new ProbabilityRuleNode(arule);
			for (ChanceNode inputNode : arule.getInputNodes()) {
				ruleNode.addInputNode(inputNode);
			}
			for (ChanceNode parameter : arule.getParameters()) {
				ruleNode.addInputNode(parameter);
			}
			addNode(ruleNode);
			addOutputNodes(ruleNode);

		}

		// second case: utility rule
		else if (r.getRuleType() == RuleType.UTIL && arule.isRelevant()){
			UtilityRuleNode ruleNode = new UtilityRuleNode(arule);
			for (ChanceNode inputNode : arule.getInputNodes()) {
				ruleNode.addInputNode(inputNode);
			}
			for (ChanceNode parameter : arule.getParameters()) {
				ruleNode.addInputNode(parameter);
			}
			addNode(ruleNode);
			addActionNodes(ruleNode);
		}
	}

	

	/**
	 * Sets the dialogue state to consist of all new variables (to trigger right after
	 * the system initialisation.
	 */
	public void setAsNew() {
		for (ChanceNode var : new ArrayList<ChanceNode>(getChanceNodes())) {
			var.setId(var.getId()+"'");
		}
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the evidence associated with the dialogue state.
	 * 
	 * @return the assignment of values for the evidence
	 */
	public Assignment getEvidence() {
		return new Assignment(evidence);
	}



	/**
	 * Returns the probability distribution corresponding to the values
	 * of the state variables provided as argument.
	 * 
	 * @param variables the variable labels to query
	 * @return the corresponding probability distribution
	 */
	public IndependentProbDistribution queryProb(String... variables) {
		return queryProb(Arrays.asList(variables));
	}


	/**
	 * Returns the probability distribution corresponding to the values
	 * of the state variables provided as argument.
	 * 
	 * @param variables the variable labels to query
	 * @return the corresponding probability distribution
	 */
	public IndependentProbDistribution queryProb(Collection<String> variables)  {

		// we first ensure that the variable labels are correctly referenced
		TreeSet<String> matchVariables = getMatchingVariables(variables);

		// if the distribution can be retrieved without inference, we simply return it
		if (matchVariables.size() == 1 && hasChanceNode(matchVariables.first()) 
				&& evidence.isEmpty() && getChanceNode(matchVariables.first()).getDistrib() 
				instanceof IndependentProbDistribution) {
			IndependentProbDistribution result =  (IndependentProbDistribution)
					getChanceNode(matchVariables.first()).getDistrib();
			return result;
		}

		if (!getNodeIds().containsAll(matchVariables)) {
			log.warning(matchVariables + " not contained in " + getNodeIds());
		}

		// else, perform the inference operation
		try {
			ProbQuery query = new ProbQuery(this, matchVariables);
			IndependentProbDistribution result = new SwitchingAlgorithm().queryProb(query);
			return result;
		}

		// if everything fails, returns an empty table
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			e.printStackTrace();
			return new CategoricalTable();
		}
	}


	/**
	 * Returns the utility table associated with a particular set of (state or 
	 * action) variables.
	 * 
	 * @param variables the state or action variables to consider
	 * @return the corresponding utility table
	 */
	public UtilityTable queryUtil(Collection<String> variables) {
		UtilQuery query = new UtilQuery(this, variables);
		try {
			return new SwitchingAlgorithm().queryUtil(query);
		} 
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			return new UtilityTable();
		}
	}


	/**
	 * Returns the total utility of the dialogue state (marginalising over all possible
	 * state variables).
	 * 
	 * @return the total utility
	 */
	public double queryUtil() {
		try {
		return (new LikelihoodWeighting()).queryUtil(this);
		} 
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			return 0.0;
		}

	}




	/**
	 * Returns the chance nodes whose variable labels match the provided
	 * templates.
	 * 
	 * @param templates the templates to match
	 * @return the corresponding chance nodes
	 */
	public List<ChanceNode> getMatchingNodes(Collection<Template> templates) {
		List<ChanceNode> inputVars = new ArrayList<ChanceNode>();
		for (Template t : templates) {

			for (String currentVar : getChanceNodeIds()) {
				if (!currentVar.contains("'") && t.match(currentVar, true).isMatching()) {
					inputVars.add(getChanceNode(currentVar));
				}
			}
		}
		return inputVars;
	}


	/**
	 * Returns the set of parameter variables in the dialogue state
	 * @return the parameter variables
	 */
	public Set<String> getParameterIds() {
		return parameterVars;
	}


	/**
	 * Returns a sample of all the variables in the dialogue state
	 * 
	 * @return a sample assignment
	 * @throws DialException if no sample could be extracted
	 */
	public Assignment getSample() throws DialException {
		return LikelihoodWeighting.extractSample(new ProbQuery(this, getChanceNodeIds()));
	}


	/**
	 * Returns the set of updated variables in the dialogue state (that is, the
	 * one that have a prime ' in their label.
	 * 
	 * @return the list of updated variables
	 */
	public Set<String> getNewVariables() {
		Set<String> newVars = new HashSet<String>();
		for (String var : getChanceNodeIds()) {
			if (var.contains("'")) {
				newVars.add(var.replace("'", ""));
			}
		}
		return newVars;
	}
	
	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================



	/**
	 * Prunes the dialogue state (see Section 4.4 of Pierre Lison's PhD thesis).
	 * 
	 * @param settings the system settings (used to determine how to perform the pruning)
	 */
	public void reduce() {
		if (!getNewVariables().isEmpty() || !evidence.isEmpty()) {
			StatePruner.prune(this);
		}
	}


	/**
	 * Returns a copy of the dialogue state
	 * 
	 * @return the copy
	 */
	public DialogueState copy() throws DialException {
		DialogueState sn= new DialogueState(super.copy());
		sn.addEvidence(evidence.copy());
		sn.parameterVars = new HashSet<String>(parameterVars);
		return sn;
	}


	/**
	 * Returns a string representation of the dialogue state
	 * 
	 * @return the string representation
	 */
	public String toString() {
		String str = super.toString();
		if (!evidence.isEmpty()) {
			str +="[evidence=" + evidence.toString() + "]";
		}
		return str;
	}


	/**
	 * Returns the hashcode for the dialogue state
	 * 
	 * @return the hashcode
	 */
	public int hashCode() {
		return super.hashCode() - 2*evidence.hashCode();
	}


	/**
	 * Generates an XML element that encodes the dialogue state content.
	 * 
	 * @param doc the document to which the element must comply
	 * @param varsToRecord the set of variables to record
	 * @return the resulting XML element
	 * @throws DialException if the XML generation failed
	 */
	public Element generateXML(Document doc, Collection<String> varsToRecord) throws DialException {

		Element root = doc.createElement("state");
		for (String nodeId : varsToRecord) {
			if (getChanceNodeIds().contains(nodeId)) {
				IndependentProbDistribution distrib = queryProb(nodeId);
				Node var = distrib.generateXML(doc);
				root.appendChild(var);
			}
		}
		return root;
	}



	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Adds the output nodes specified in the rule node and connects them to the
	 * rule node.
	 * 
	 * @param ruleNode the rule node from which the outputs "spawn"
	 * @throws DialException if the creation of new nodes fails
	 */
	private void addOutputNodes(ProbabilityRuleNode ruleNode) throws DialException {

		// looping on each output variable
		for (String updatedVar : ruleNode.getAnchor().getOutputVariables()) {

			// if the output node does not yet exist, create it
			if (!hasNode(updatedVar)) {
				ChanceNode outputNode = new ChanceNode(updatedVar, 
						new OutputDistribution(updatedVar));
				addNode(outputNode);

				// adding the connection to the previous version of the variable (if any)
				if (hasChanceNode(updatedVar.replaceFirst("'", "")) && !(updatedVar.contains("^p"))) {
					outputNode.addInputNode(getChanceNode(updatedVar.replaceFirst("'", "")));
				}

				// connecting to prior predictions
				connectToPredictions(outputNode);

				// adding dependency edge
				outputNode.addInputNode(ruleNode);

				// safety check
				if (ruleNode.getId().contains("^2") && outputNode.getInputNodeIds().
						contains(ruleNode.getId().replace("^2", ""))) {
					log.warning("node " + outputNode.getId() + " has duplicates: " + ruleNode.getId());
				}
			}

			// else, simply add an additional edge
			else {
				getNode(updatedVar).addInputNode(ruleNode);
			}
		}
	}



	/**
	 * Adds the action nodes specified in the rule node and connects them to the
	 * rule node.
	 * 
	 * @param ruleNode the utility rule node to which the action node points
	 * @throws DialException if the creation of new nodes fails
	 */
	private  void addActionNodes(UtilityRuleNode ruleNode) throws DialException {

		// retrieving the set of actions and their values
		ValueRange actions = ruleNode.getAnchor().getOutputs();

		// looping on every action variable
		for (String actionVar : actions.getVariables()) {

			// if the action variable does not yet exist, create it
			if (!hasNode(actionVar)) {
				ActionNode actionNode = new ActionNode(actionVar);
				addNode(actionNode);
				ruleNode.addInputNode(actionNode);
			}

			// else, simply connect it to the rule node
			else {
				ruleNode.addInputNode(getNode(actionVar));
			}

			// and add the values specified in the utility rule to the variable
			getActionNode(actionVar).addValues(actions.getValues(actionVar));
		}
	}



	/**
	 * Connects the chance node to its prior predictions (if any).
	 * 
	 * @param outputNode the output node to connect
	 * @throws DialException if the connection fails
	 */
	private void connectToPredictions(ChanceNode outputNode) throws DialException {

		String outputVar = outputNode.getId();

		// adding the connection between the predicted and observed values
		String predictEquiv = outputVar.replace("'", "") + "^p";
		if (hasChanceNode(predictEquiv) && 
				!outputVar.equals(predictEquiv) && !outputVar.contains("^p")) {
			ChanceNode equalityNode = new ChanceNode("=_" + outputVar.replace("'", ""));
			equalityNode.addInputNode(outputNode);
			equalityNode.addInputNode(getNode(predictEquiv));
			equalityNode.setDistrib(new EquivalenceDistribution(outputVar.replace("'", "")));
			addEvidence(new Assignment(equalityNode.getId(), true));
			addNode(equalityNode);
		}
	}


	/**
	 * Returns the variable labels that are matching the ones provided as argument,
	 * modulo the removal of prime characters.
	 * 
	 * @param variables the initial variable labels
	 * @return the edited labels
	 */
	private TreeSet<String> getMatchingVariables(Collection<String> variables) {
		TreeSet<String> variables2 = new TreeSet<String>();
		for (String variable : variables) {
			if (!hasChanceNode(variable) && hasChanceNode(variable.replace("'", ""))) {
				variables2.add(variable.replace("'", ""));
			}
			else {
				variables2.add(variable);
			}
		}
		return variables2;
	}





}

