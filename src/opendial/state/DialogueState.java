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

package opendial.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.MultivariateTable;
import opendial.bn.distribs.SingleValueDistribution;
import opendial.bn.distribs.UtilityTable;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.Rule;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.approximate.SamplingAlgorithm;
import opendial.state.distribs.EquivalenceDistribution;
import opendial.state.distribs.OutputDistribution;
import opendial.state.distribs.RuleDistribution;
import opendial.state.distribs.RuleUtilDistribution;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Representation of a dialogue state. A dialogue state is essentially a directed
 * graphical model (i.e. a Bayesian or decision network) over a set of specific state
 * variables. Probabilistic rules can be applied on this dialogue state in order to
 * update its content. After applying the rules, the dialogue state is usually pruned
 * to only retain relevant state variables.
 * 
 * 
 * The dialogue state may also include an assignment of evidence values. A subset of
 * state variables can be marked as denoting parameter variables.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class DialogueState extends BNetwork {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// evidence values for state variables
	Assignment evidence;

	/** Subset of variables that denote parameters */
	Set<String> parameterVars;

	/** Subset of variables that are currently incrementally constructed */
	Set<String> incrementalVars;

	// ===================================
	// DIALOGUE STATE CONSTRUCTION
	// ===================================

	/**
	 * Creates a new, empty dialogue state.
	 */
	public DialogueState() {
		this(new BNetwork());
	}

	/**
	 * Creates a new dialogue state that contains the Bayesian network provided as
	 * argument.
	 * 
	 * @param network the Bayesian network to include
	 */
	public DialogueState(BNetwork network) {
		super();
		super.reset(network);
		evidence = new Assignment();
		parameterVars = new HashSet<String>();
		incrementalVars = new HashSet<String>();
	}

	/**
	 * Creates a new dialogue state that contains the set of nodes provided as
	 * argument.
	 * 
	 * @param nodes the nodes to include
	 * @param evidence the evidence
	 */
	public DialogueState(Collection<BNode> nodes, Assignment evidence) {
		super(nodes);
		this.evidence = new Assignment(evidence);
		parameterVars = new HashSet<String>();
		incrementalVars = new HashSet<String>();
	}

	/**
	 * Creates a new dialogue state that contains the Bayesian network provided as
	 * argument.
	 * 
	 * @param network the Bayesian network to include
	 * @param evidence the additional evidence
	 */
	public DialogueState(BNetwork network, Assignment evidence) {
		super();
		super.reset(network);
		this.evidence = new Assignment(evidence);
		parameterVars = new HashSet<String>();
		incrementalVars = new HashSet<String>();
	}

	/**
	 * Resets the content of the dialogue state to the network contained as argument
	 * (and deletes the rest).
	 * 
	 */
	@Override
	public void reset(BNetwork network) {
		if (this == network) {
			return;
		}
		evidence.removePairs(getChanceNodeIds());
		super.reset(network);
		if (network instanceof DialogueState) {
			evidence.addAssignment(((DialogueState) network).getEvidence());
		}
	}

	/**
	 * Clear the assignment of values for the variables provided as argument
	 * 
	 * @param variables the variables for which to clear the assignment
	 */
	public void clearEvidence(Collection<String> variables) {
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
	 * @throws RuntimeException if the inclusion of parameters failed
	 */
	public void setParameters(BNetwork parameters) throws RuntimeException {
		addNetwork(parameters);
		this.parameterVars.clear();
		this.parameterVars.addAll(parameters.getChanceNodeIds());
	}

	// ===================================
	// STATE UPDATE
	// ===================================

	/**
	 * Adds the content provided as argument to the dialogue state. If the state
	 * variables in the assignment already exist, they are erased.
	 * 
	 * 
	 * @param assign the value assignment to add
	 * @throws RuntimeException if the content could not be added.
	 */
	public synchronized void addToState(Assignment assign) throws RuntimeException {
		for (String var : assign.getVariables()) {
			addToState(new SingleValueDistribution(var, assign.getValue(var)));
		}
	}

	/**
	 * Adds the content provided as argument to the dialogue state. If the state
	 * variables in the assignment already exist, they are erased.
	 * 
	 * 
	 * @param distrib the multivariate distribution to add
	 * @throws RuntimeException if the content could not be added.
	 */
	public synchronized void addToState(MultivariateDistribution distrib)
			throws RuntimeException {
		for (String var : distrib.getVariables()) {
			addToState(distrib.getMarginal(var));
		}
	}

	/**
	 * Adds the content provided as argument to the dialogue state. If the state
	 * variables in the categorical table already exist, they are erased.
	 * 
	 * <p>
	 * It should be noted that the method only adds the content to the dialogue state
	 * but does not trigger models or modules following this change. In order to
	 * trigger such chain of updates, the method addContent(...) in DialogueSystem
	 * should be used instead.
	 * 
	 * @param distrib a distribution over values for particular state variables
	 * @throws RuntimeException if the content could not be added.
	 */
	public synchronized void addToState(IndependentProbDistribution distrib)
			throws RuntimeException {

		String variable = distrib.getVariable() + "'";
		setAsCommitted(variable);
		distrib.modifyVariableId(distrib.getVariable(), variable);
		ChanceNode newNode = new ChanceNode(variable, distrib);

		if (hasNode(variable)) {
			BNode toRemove = getNode(variable);
			removeNodes(toRemove.getDescendantIds());
			removeNode(toRemove.getId());
		}

		addNode(newNode);
		connectToPredictions(newNode);
		incrementalVars.remove(variable);
	}

	/**
	 * Concatenates the current value for the new content onto the current content,
	 * if followPrevious is true. Else, simply overwrites the current content of the
	 * variable.
	 * 
	 * @param distrib the distribution to add as incremental unit of content
	 * @param followPrevious whether the results should be concatenated to the
	 *            previous values, or reset the content (e.g. when starting a new
	 *            utterance)
	 * @throws RuntimeException if the incremental operation could not be performed
	 */
	public synchronized void addToState_incremental(CategoricalTable distrib,
			boolean followPrevious) throws RuntimeException {

		if (!followPrevious) {
			setAsCommitted(distrib.getVariable());
		}

		String var = distrib.getVariable();
		if (hasChanceNode(var) && isIncremental(var) & followPrevious) {
			CategoricalTable newtable =
					((CategoricalTable) queryProb(var)).concatenate(distrib);
			getChanceNode(var).setDistrib(newtable);
			getChanceNode(var).setId(var + "'");
		}
		else {
			addToState(distrib);
		}
		incrementalVars.add(var);
	}

	/**
	 * Merges the dialogue state included as argument into the current one.
	 * 
	 * @param newState the state to merge into the current state
	 * @throws RuntimeException if the new dialogue state could not be merged
	 */
	public synchronized void addToState(DialogueState newState)
			throws RuntimeException {
		addToState((BNetwork) newState);
		evidence.addAssignment(newState.getEvidence().addPrimes());
	}

	/**
	 * Merges the dialogue state included as argument into the current one.
	 * 
	 * @param newState the state to merge into the current state
	 * @throws RuntimeException if the new dialogue state could not be merged
	 */
	public synchronized void addToState(BNetwork newState) throws RuntimeException {
		for (ChanceNode cn : new ArrayList<ChanceNode>(newState.getChanceNodes())) {
			cn.setId(cn.getId() + "'");
			addNode(cn);
			connectToPredictions(cn);
		}
	}

	/**
	 * Removes the variable from the dialogue state
	 * 
	 * @param variableId the node to remove
	 */
	public synchronized void removeFromState(String variableId) {
		addToState(new Assignment(variableId, ValueFactory.none()));
	}

	/**
	 * Applies a (probability or utility) rule to the dialogue state:
	 * <ul>
	 * <li>For a probability rule, the method creates a chance node containing the
	 * rule effects depending on the input variables of the rule, and connected via
	 * outgoing edges to the output variables.
	 * <li>For a utility rule, the method creates a utility node specifying the
	 * utility of particular actions specified by the rule depending on the input
	 * variables.
	 * </ul>
	 * 
	 * <p>
	 * The method creates both the rule node, its corresponding output or action
	 * nodes, and the directed edges resulting from the rule application. See Pierre
	 * Lison's PhD thesis, Section 4.3 for details.
	 * 
	 * @param r the rule to apply.
	 * @throws RuntimeException if the rule could not be applied.
	 */
	public void applyRule(Rule r) throws RuntimeException {

		AnchoredRule arule = new AnchoredRule(r, this);
		if (arule.isRelevant()) {
			switch (r.getRuleType()) {
			case PROB:
				addProbabilityRule(arule);
				break;
			case UTIL:
				addUtilityRule(arule);
				break;
			}
		}
	}

	/**
	 * Sets the dialogue state to consist of all new variables (to trigger right
	 * after the system initialisation.
	 */
	public void setAsNew() {
		for (ChanceNode var : new ArrayList<ChanceNode>(getChanceNodes())) {
			var.setId(var.getId() + "'");
		}
	}

	// ===================================
	// GETTERS
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
	 * Returns the probability distribution corresponding to the values of the state
	 * variable provided as argument.
	 * 
	 * @param variable the variable label to query
	 * @return the corresponding probability distribution
	 */
	public IndependentProbDistribution queryProb(String variable) {
		return queryProb(variable, true);
	}

	/**
	 * Returns the probability distribution corresponding to the values of the state
	 * variable provided as argument.
	 * 
	 * @param variable the variable label to query
	 * @param includeEvidence whether to include or ignore the evidence in the
	 *            dialogue state
	 * @return the corresponding probability distribution
	 */
	public IndependentProbDistribution queryProb(String variable,
			boolean includeEvidence) {

		if (hasChanceNode(variable)) {
			ChanceNode cn = getChanceNode(variable);

			// if the distribution can be retrieved without inference, we simply
			// return it
			if (cn.getDistrib() instanceof IndependentProbDistribution
					&& Collections.disjoint(cn.getClique(), evidence.getVariables())) {
				return (IndependentProbDistribution) cn.getDistrib();
			}

			else {
				try {
					Assignment queryEvidence =
							(includeEvidence) ? evidence : new Assignment();
					return new SwitchingAlgorithm().queryProb(this, variable,
							queryEvidence);
				}
				catch (RuntimeException e) {
					log.warning("Error querying variable " + variable + " : " + e);
					return new CategoricalTable(variable);
				}
			}
		}
		else {
			log.warning("Variable " + variable
					+ " not included in the dialogue state");
			return new CategoricalTable(variable);
		}
	}

	/**
	 * Returns the probability distribution corresponding to the values of the state
	 * variables provided as argument.
	 * 
	 * @param variables the variable labels to query
	 * @return the corresponding probability distribution
	 */
	public MultivariateDistribution queryProb(Collection<String> variables) {

		if (!getNodeIds().containsAll(variables)) {
			log.warning(variables + " not contained in " + getNodeIds());
		}
		// else, perform the inference operation
		try {
			return new SwitchingAlgorithm().queryProb(this, variables, evidence);
		}

		// if everything fails, returns an empty table
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			e.printStackTrace();
			return new MultivariateTable();
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
		try {
			return new SwitchingAlgorithm().queryUtil(this, variables, evidence);
		}
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			return new UtilityTable();
		}
	}

	/**
	 * Returns the total utility of the dialogue state (marginalising over all
	 * possible state variables).
	 * 
	 * @return the total utility
	 */
	public double queryUtil() {
		try {
			return (new SamplingAlgorithm()).queryUtil(this);
		}
		catch (Exception e) {
			log.warning("cannot perform inference: " + e);
			return 0.0;
		}

	}

	/**
	 * Returns the chance nodes whose variable labels match the provided templates.
	 * 
	 * @param templates the templates to match
	 * @return the corresponding chance nodes
	 */
	public List<ChanceNode> getMatchingNodes(Collection<Template> templates) {
		List<ChanceNode> inputVars = new ArrayList<ChanceNode>();
		for (Template t : templates) {
			for (String currentVar : getChanceNodeIds()) {
				if (!currentVar.endsWith("'") && t.match(currentVar).isMatching()) {
					inputVars.add(getChanceNode(currentVar));
				}
			}
		}
		return inputVars;
	}

	/**
	 * Returns the set of parameter variables in the dialogue state
	 * 
	 * @return the parameter variables
	 */
	public Set<String> getParameterIds() {
		return parameterVars;
	}

	/**
	 * Returns a sample of all the variables in the dialogue state
	 * 
	 * @return a sample assignment
	 * @throws RuntimeException if no sample could be extracted
	 */
	public Assignment getSample() throws RuntimeException {
		return SamplingAlgorithm.extractSample(this, getChanceNodeIds());
	}

	/**
	 * Returns the set of updated variables in the dialogue state (that is, the one
	 * that have a prime ' in their label.
	 * 
	 * @return the list of updated variables
	 */
	public synchronized Set<String> getNewVariables() {
		Set<String> newVars = new HashSet<String>();
		for (String var : getChanceNodeIds()) {
			if (var.endsWith("'")) {
				newVars.add(var.substring(0, var.length() - 1));
			}
		}
		return newVars;
	}

	public boolean isIncremental(String var) {
		return incrementalVars.contains(var.replace("'", ""));
	}

	/**
	 * Returns all rule nodes in the dialogue state (both probability and utility
	 * rules)
	 * 
	 * @return the set of identifiers for the rule nodes
	 */
	public Set<String> getRuleNodeIds() {
		return getNodeIds().stream().filter(i -> isRuleNode(i))
				.collect(Collectors.toSet());
	}

	/**
	 * Returns true if the node identifier refers to a rule node, and false otherwise
	 * 
	 * @param id the node identifier
	 * @return true if the node with identifier id is a (probability or utility) rule
	 *         node
	 */
	public boolean isRuleNode(String id) {
		if (hasNode(id)) {
			BNode n = getNode(id);
			if ((n instanceof ChanceNode)
					&& ((ChanceNode) n).getDistrib() instanceof RuleDistribution) {
				return true;
			}
			else if (n instanceof UtilityNode
					&& ((UtilityNode) n).getFunction() instanceof RuleUtilDistribution) {
				return true;
			}
		}
		return false;
	}

	// ===================================
	// UTILITY FUNCTIONS
	// ===================================

	public void setAsCommitted(String var) {
		if (incrementalVars.contains(var)) {
			incrementalVars.remove(var);
			StatePruner.prune(this);
		}
	}

	/**
	 * Prunes the dialogue state (see Section 4.4 of Pierre Lison's PhD thesis).
	 * 
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
	@Override
	public DialogueState copy() throws RuntimeException {
		DialogueState sn = new DialogueState(super.copy());
		sn.addEvidence(evidence.copy());
		sn.parameterVars = new HashSet<String>(parameterVars);
		sn.incrementalVars = new HashSet<String>(incrementalVars);
		return sn;
	}

	/**
	 * Returns a string representation of the dialogue state
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = super.toString();
		if (!evidence.isEmpty()) {
			str += "[evidence=" + evidence.toString() + "]";
		}
		return str;
	}

	/**
	 * Returns the hashcode for the dialogue state
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return super.hashCode() - 2 * evidence.hashCode();
	}

	/**
	 * Generates an XML element that encodes the dialogue state content.
	 * 
	 * @param doc the document to which the element must comply
	 * @param varsToRecord the set of variables to record
	 * @return the resulting XML element
	 * @throws RuntimeException if the XML generation failed
	 */
	public Element generateXML(Document doc, Collection<String> varsToRecord)
			throws RuntimeException {

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
	// PRIVATE METHODS
	// ===================================

	/**
	 * Adds the probability rule to the dialogue state
	 * 
	 * @param arule the anchored rule (must be of type PROB)
	 * @throws RuntimeException if the creation of new nodes fails
	 */
	private void addProbabilityRule(AnchoredRule arule) throws RuntimeException {

		String ruleId = arule.getRule().getRuleId();
		if (hasChanceNode(ruleId)) {
			removeNode(ruleId);
		}

		ChanceNode ruleNode = new ChanceNode(ruleId);
		ruleNode.setDistrib(new RuleDistribution(arule));
		ruleNode.getValues();

		arule.getInputs().forEach(i -> ruleNode.addInputNode(getChanceNode(i)));
		arule.getParameters().forEach(i -> ruleNode.addInputNode(getChanceNode(i)));
		addNode(ruleNode);

		// looping on each output variable
		for (String updatedVar : arule.getOutputs()) {

			ChanceNode outputNode;
			OutputDistribution outputDistrib;

			// if the output node does not yet exist, create it
			if (!hasNode(updatedVar)) {
				outputDistrib = new OutputDistribution(updatedVar);
				outputNode = new ChanceNode(updatedVar, outputDistrib);
				addNode(outputNode);

				// connecting to prior predictions
				connectToPredictions(outputNode);
			}
			// else, simply add an additional edge
			else {
				outputNode = getChanceNode(updatedVar);
				outputDistrib = (OutputDistribution) outputNode.getDistrib();
			}
			outputNode.addInputNode(ruleNode);
			outputDistrib.addEffects(arule.getEffects());

		}
	}

	/**
	 * Adds the utility rule to the dialogue state.
	 * 
	 * @param arule the anchored rule (must be of type UTIL)
	 * @throws RuntimeException if the creation of new nodes fails
	 */
	private void addUtilityRule(AnchoredRule arule) throws RuntimeException {

		String ruleId = arule.getRule().getRuleId();

		if (hasUtilityNode(ruleId)) {
			removeNode(ruleId);
		}
		UtilityNode ruleNode = new UtilityNode(ruleId);
		ruleNode.setDistrib(new RuleUtilDistribution(arule));
		arule.getInputs().forEach(i -> ruleNode.addInputNode(getChanceNode(i)));
		arule.getParameters().forEach(i -> ruleNode.addInputNode(getChanceNode(i)));
		addNode(ruleNode);

		// retrieving the set of actions and their values
		ValueRange actions = arule.getOutputRange();

		// looping on every action variable
		for (String actionVar : actions.getVariables()) {

			ActionNode actionNode;
			// if the action variable does not yet exist, create it
			if (!hasActionNode(actionVar)) {
				actionNode = new ActionNode(actionVar);
				addNode(actionNode);
			}
			else {
				actionNode = getActionNode(actionVar);
			}

			ruleNode.addInputNode(actionNode);
			actionNode.addValues(actions.getValues(actionVar));
		}
	}

	/**
	 * Connects the chance node to its prior predictions (if any).
	 * 
	 * @param outputNode the output node to connect
	 * @throws RuntimeException if the connection fails
	 */
	private void connectToPredictions(ChanceNode outputNode) throws RuntimeException {

		String outputVar = outputNode.getId();

		// adding the connection between the predicted and observed values
		String baseVar = outputVar.substring(0, outputVar.length() - 1);
		String predictEquiv = baseVar + "^p";
		if (hasChanceNode(predictEquiv) && !outputVar.equals(predictEquiv)
				&& !outputVar.contains("^p")) {
			ChanceNode equalityNode = new ChanceNode("=_" + baseVar);
			equalityNode.addInputNode(outputNode);
			equalityNode.addInputNode(getNode(predictEquiv));
			equalityNode.setDistrib(new EquivalenceDistribution(baseVar));
			addEvidence(new Assignment(equalityNode.getId(), true));
			addNode(equalityNode);
		}

	}

}
