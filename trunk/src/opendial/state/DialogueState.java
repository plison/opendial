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

package opendial.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.ComplexEmpiricalDistribution;
import opendial.bn.distribs.empirical.EmpiricalDistribution;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.domains.Model;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.PredictionRule;
import opendial.domains.rules.UpdateRule;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.learning.SARSALearner;
import opendial.learning.WoZLearner;
import opendial.modules.SynchronousModule;
import opendial.planning.ForwardPlanner;
import opendial.state.rules.DistributionRule;
import opendial.state.rules.Rule;
import opendial.state.rules.Rule.RuleType;
import opendial.utils.DistanceUtils;
import opendial.utils.StringUtils;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueState {

	// logger
	public static Logger log = new Logger("DialogueState", Logger.Level.DEBUG);

	String name = "";

	protected BNetwork network;

	Assignment evidence;

	Set<String> parameterIds;

	Stack<String> variablesToProcess;

	Map<String,Long> updateStamps;

	List<SynchronousModule> modules;

	StatePruner pruner;

	ForwardPlanner planner;

	boolean isFictive = false;
	
	public static double LIKELIHOOD_THRESHOLD = 0.2;


	boolean activateDecisions = true;
	boolean activatePredictions = true;
	boolean activateUpdates = true;

	List<StateListener> listeners;

	public DialogueState() {
		this(new BNetwork());
	}

	public DialogueState(BNetwork network) {
		this.network = network;
		evidence = new Assignment();

		modules = new ArrayList<SynchronousModule>();
		variablesToProcess = new Stack<String>();
		updateStamps = new HashMap<String,Long>();

		pruner = new StatePruner(this);

		if (Settings.getInstance().planning.isSarsa()) {
			planner = new SARSALearner(this);
		}
		else if (Settings.getInstance().planning.isWoZ()) {
			planner = new WoZLearner(this);
		}
		else {
			planner = new ForwardPlanner(this);
		}
		listeners = new LinkedList<StateListener>();

		parameterIds = new HashSet<String>();
	}


	public void setName(String name) {
		this.name = name;
	}

	public void startState() {
		for (String nodeId : network.getNodeIds()) {
			setVariableToProcess(nodeId);
		}
		triggerUpdates();
	}

	public void addListener(StateListener listener) {
		listeners.add(listener);
	}


	public void addEvidence(Assignment newEvidence) {
		evidence.addAssignment(newEvidence);
	}

	public Assignment getEvidence() {
		return evidence;
	}

	public BNetwork getNetwork() {
		return network;
	}


	public void attachModule(SynchronousModule module) {
		modules.add(module);
	}

	public void removeAllModules() {
		modules = new ArrayList<SynchronousModule>();
	}


	public void applyRule(Rule rule) throws DialException {
		if (!activateDecisions && rule.getRuleType() == RuleType.UTIL) {
			return;
		}
		else if (!activateUpdates && rule instanceof UpdateRule) {
			return;
		}
		else if (!activatePredictions && rule instanceof PredictionRule) {
			return;
		}
		RuleInstantiator instantiator = new RuleInstantiator(this, rule);
		instantiator.run();
	}


	/**
	 * 
	 * @param string
	 * @return
	 */
	public boolean isVariableToProcess(String nodeId) {
		return variablesToProcess.contains(nodeId);
	}

	/**
	 * 
	 * @param id
	 */
	public void setVariableToProcess(String nodeId) {
		variablesToProcess.add(nodeId);
	}

	public Stack<String> getVariablesToProcess() {
		return variablesToProcess;
	}


	public void refresh() {
		for (StateListener listener : listeners) {
			listener.update(this);
		}
	}




	public synchronized void triggerUpdates() {

		refresh();

		while (true) {
			if (!variablesToProcess.isEmpty()) {
				List<SynchronousModule> modulesToTrigger = 
						new ArrayList<SynchronousModule>();

				for (SynchronousModule module : modules) {
					if (module.isTriggered(this)) {
						modulesToTrigger.add(module);
					}
				}
				for (String var : variablesToProcess) {
					if (!network.hasActionNode(var)) {
						updateStamps.put(var.replace("'", ""), System.currentTimeMillis());
					}
				}
				variablesToProcess.clear();

				for (SynchronousModule module : modulesToTrigger) {
					if (module instanceof Model && ((Model)module).getModelType().equals(DecisionRule.class)
							& Settings.getInstance().activatePruning) {
						filterLowProbabilityValues();
					}
					module.trigger(this);
				}
			}
			else if (pruner.isPruningNeeded()) {
				pruner.run();
			}	
			else if (planner.isPlanningNeeded()) {	
				showInformation();
				planner.run();
			}	
			else {
				refresh();
				break;
			}
		}
	}

	private void showInformation() {
		if (network.hasActionNode("a_m'")) { try { 
			if (network.hasChanceNode("a_u") && network.hasChanceNode("i_u") && !isFictive()) { 
				if (network.hasChanceNode("motion")) {
					log.debug("motion : " + getContent("motion", true).prettyPrint());
				}
				log.debug("Interpreted a_u : " + getContent("a_u", true).prettyPrint());
				log.debug("Interpreted i_u (before action) : " +  getContent("i_u", true).prettyPrint());
			} 
		}
		catch (DialException e) { e.printStackTrace(); } 	
		}
	}
	
	private void filterLowProbabilityValues() {
			pruner.run();
			for (ChanceNode cn : network.getChanceNodes()) {
				if (cn.getDistrib() instanceof EmpiricalDistribution) {
					((EmpiricalDistribution)cn.getDistrib()).filterValuesBelowThreshold(cn.getId(), LIKELIHOOD_THRESHOLD);
				}
				if (cn.getDistrib() instanceof DiscreteProbDistribution) {
					((DiscreteProbDistribution)cn.getDistrib()).filterValuesBelowThreshold(cn.getId(), LIKELIHOOD_THRESHOLD);
				}
			}
	}


	public boolean isStable() {
		return (variablesToProcess.isEmpty() && !pruner.isPruningNeeded() && !planner.isPlanningNeeded());
	}

	public void addContent(Assignment assign, String origin) 
			throws DialException {
		SimpleTable table = new SimpleTable();
		table.addRow(assign, 1.0);
		addContent(table, origin);
	}


	public void addContent(ProbDistribution distrib, String origin) 
			throws DialException {
		addContent(distrib, origin, true);
	}

	public void addContent(ProbDistribution distrib, String origin, 
			boolean clearPrevious) throws DialException {
		origin = network.getUniqueNodeId(origin);
		applyRule(new DistributionRule(distrib, origin, clearPrevious));
		triggerUpdates();
	}


	public ProbDistribution getContent(String variable, boolean withEvidence) throws DialException {
		ProbDistribution distrib = getContent(Arrays.asList(getLatestNodeId(variable)), withEvidence);
		distrib.modifyVarId(getLatestNodeId(variable), variable);
		return distrib;
	}


	private String getLatestNodeId (String baseId) {
		for (int i = 4 ; i >= 0 ; i--) {
			String modifiedVar = baseId + StringUtils.createNbPrimes(i);
			if (network.hasChanceNode(modifiedVar)) {
				return modifiedVar;
			}
		}
		return baseId;
	}


	public ProbDistribution getContent(Collection<String> variables, 
			boolean withEvidence) throws DialException {
		if (!network.hasChanceNodes(variables)) {
			throw new DialException("variables " + variables + " are not in the dialogue state");
		}
		try {
			ProbQuery query = (withEvidence)? new ProbQuery(this, variables): new ProbQuery(network, variables);
			if (isFictive) {
				query.setAsLightweight(true);
			}
			InferenceAlgorithm algo = Settings.getInstance().inferenceAlgorithm.newInstance();
			return algo.queryProb(query);
		}
		catch (Exception e) {
			throw new DialException ("inference could not be performed: " + variables + " -reason " + e);
		}
	}


	public DialogueState copy() throws DialException {
		BNetwork netCopy = network.copy();
		DialogueState stateCopy = new DialogueState(netCopy);
		stateCopy.addEvidence(evidence.copy());
		for (SynchronousModule module : modules) {
			stateCopy.attachModule(module);
		}
		stateCopy.parameterIds = parameterIds;
		return stateCopy;
	}


	public void activateDecisions(boolean activateDecisions) {
		this.activateDecisions = activateDecisions;
	}

	public void activatePredictions(boolean activatePredictions) {
		this.activatePredictions = activatePredictions;
	}

	public void activateUpdates(boolean activateUpdates) {
		this.activateUpdates = activateUpdates;
	}

	public void setAsFictive(boolean fictive) {
		this.isFictive = fictive;
	}

	public boolean isFictive() {
		return isFictive;
	}

	@Override
	public String toString() {
		String s = "Current network: ";
		s += network.toString();
		if (!evidence.isEmpty()) {
			s += " (with evidence " + evidence + ")";
		}
		if (!variablesToProcess.isEmpty()) {
			s += "\nNew variables: " + variablesToProcess;
		}
		s += "\n";
		return s;
	}



	public synchronized void reset(BNetwork network, Assignment evidence) {
		this.network = network;
		this.evidence = evidence;
	}


	public boolean isUpdated(String varId, long referenceStamp) {
		if (updateStamps.containsKey(varId)) {
			long updateStamp = updateStamps.get(varId).longValue();
			return (updateStamp - referenceStamp) > 0.0;
		}
		return false;
	}


	public void addParameters(BNetwork parameterNetwork) {
		try {
			network.addNetwork(parameterNetwork);
			parameterIds.addAll(parameterNetwork.getNodeIds());
		}
		catch (DialException e) {
			log.warning ("cannot add parameters to the dialogue state: " + e);
		}
	}

	public boolean isParameter(String nodeId) {
		return parameterIds.contains(nodeId);
	}

	public String getName() {
		return name;
	}



	public void shutdown() {
		for (SynchronousModule module: modules) {
			module.shutdown();
		}
	}



	public Element generateXML(Document doc, boolean includePredictions) throws DialException {

		Element root = doc.createElement("state");
		for (String nodeId : network.getChanceNodeIds()) {
			if (!isParameter(nodeId) && (includePredictions || !(nodeId.contains("^p")))) {
				Element var = doc.createElement("variable");

				Attr id = doc.createAttribute("id");
				id.setValue(nodeId);
				var.setAttributeNode(id);

				SimpleTable table = getContent(nodeId,true).toDiscrete().getProbTable(new Assignment());
				for (Assignment a : table.getRows()) {
					Element valueNode = doc.createElement("value");
					Attr prob = doc.createAttribute("prob");
					prob.setValue(""+DistanceUtils.shorten(table.getProb(a)));
					valueNode.setAttributeNode(prob);
					valueNode.setTextContent(""+a.getValue(nodeId));
					var.appendChild(valueNode);	
				}
				root.appendChild(var);
			}
		}
		return root;
	}


	public List<String> getParameterIds() {
		return new ArrayList<String>(parameterIds);
	}


}

