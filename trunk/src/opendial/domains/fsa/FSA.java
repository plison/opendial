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

package opendial.domains.fsa;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.domains.Model;
import opendial.domains.rules.DecisionRule;
import opendial.domains.rules.conditions.Condition;
import opendial.modules.SynchronousModule;
import opendial.state.DialogueState;
import opendial.state.StatePruner;

public class FSA extends Model<DecisionRule> {

	// logger
	public static Logger log = new Logger("FiniteStateAutomaton",  Logger.Level.DEBUG);

	Map<String, State> states;

	List<Edge> edges;

	State curState;

	String actionVar;

	String triggerVar;

	String initState;


	public FSA(String triggerVar, String actionVar, List<State> states, 
			List<Edge> edges, String initState) {

		this.states = new HashMap<String,State>();
		for (State state : states) {
			this.states.put(state.getId(), state);
		}
		this.edges = new ArrayList<Edge>();
		for (Edge edge : edges) {
			if (!this.states.containsKey(edge.getSource())) {
				log.warning("FSA does not contain source state " + edge.getSource());
			}
			if (!this.states.containsKey(edge.getTarget())) {
				log.warning("FSA does not contain target state " + edge.getTarget());
			}
			this.edges.add(edge);
		}
		this.triggerVar = triggerVar;
		this.actionVar = actionVar;
		this.initState = initState;
		if (this.states.containsKey(initState)) {
			curState = this.states.get(initState);
		}
	}

	private boolean canBeTraversed(DialogueState dstate, Edge edge) throws DialException {

		if (edge.getSource().equals(curState.getId()) && !edge.isVoid()) {

			Set<String> conditionVars = edge.getConditionVariables();
			conditionVars.retainAll(dstate.getNetwork().getChanceNodeIds());
			SimpleTable distrib = dstate.getContent(conditionVars, true)
					.toDiscrete().getProbTable(new Assignment());

			State nextState = states.get(edge.getTarget());
			Set<String> intersect = nextState.getActionSlots();
			intersect.retainAll(conditionVars);
			if (intersect.isEmpty()) {
				double totalMatchProb = 0.0;
				for (Assignment input : distrib.getRows()) {
					if (edge.matches(input)) {
						totalMatchProb += distrib.getProb(input);
					}
				}
				if (totalMatchProb > edge.getThreshold()) {
					return true;
				}
			}
			else {
				for (Assignment input : distrib.getRows()) {
					if (edge.matches(input) && 
							distrib.getProb(input) > edge.getThreshold()) {
						return true;
					}
				}
			}	
		}
		return false;
	}

	private Edge selectEdge (DialogueState dstate) throws DialException {
		Collections.shuffle(edges);
		for (int priority = 1 ; priority < 5 ; priority++) {
			for (Edge edge :edges) {
				if (edge.getPriority() == priority && canBeTraversed(dstate,edge)) {
					return edge;
				}

			}
		}
		return null;
	}


	private void traverseEmptyEdges() {
		for (Edge edge :edges) {
			if (edge.getSource().equals(curState.getId()) && edge.isVoid()) {
				curState = states.get(edge.getTarget());
				traverseEmptyEdges();
				return;
			}
		}
	}

	private Assignment extractFillers(DialogueState dstate, Edge edge) throws DialException {
		Set<String> fillerVars = curState.getActionSlots();
		if (!fillerVars.isEmpty()) {
			fillerVars.addAll(edge.getConditionVariables());
			fillerVars.retainAll(dstate.getNetwork().getChanceNodeIds());
			SimpleTable distrib = dstate.getContent(fillerVars, true)
					.toDiscrete().getProbTable(new Assignment());
			Assignment bestFiller = new Assignment();
			double highestProb = 0.0;
			for (Assignment row : distrib.getRows()) {
				if (edge.matches(row) && distrib.getProb(row) > highestProb) {
					bestFiller = new Assignment(row, edge.getLocalOutput(row));
					highestProb = distrib.getProb(row);
				}
			}
			return bestFiller;
		}
		return new Assignment();
	}

	@Override
	public void trigger(DialogueState dstate) {
		
		StatePruner pruner = new StatePruner(dstate);
		pruner.run();
		
		try {
			Assignment action = Assignment.createDefault(Arrays.asList(actionVar));
			Edge edge = selectEdge(dstate);
			if (edge != null) {
				curState = states.get(edge.getTarget());
				if (!curState.isEmpty()) {
					Assignment filler = extractFillers(dstate, edge);
					action = new Assignment(actionVar, curState.getAction(filler));
					traverseEmptyEdges();
				}
				dstate.addContent(action, "fsa");
			}
		}
		catch (DialException e) {
			log.warning("could not select next action: " + e);
		}
	}


	@Override
	public boolean isTriggered(DialogueState state) {
		return (state.isVariableToProcess(triggerVar+"'"));
	}

	@Override
	public void shutdown() {
		return;
	}


	public List<Edge> getEdges() {
		return edges;
	}

	public List<State> getStates() {
		List<State> l = new ArrayList<State>(states.values());
		Collections.sort(l, new Comparator<State>() { 
			public int compare(State s1, State s2) { return s1.getId().compareTo(s2.getId()); } });
		return l;
	}


	public State getCurrentState() {
		return curState;
	}

	public State getInitState() {
		return states.get(initState);
	}


	public void reset() {
		curState = states.get(initState);
	}


	public int hashCode() {
		return states.values().hashCode() - edges.hashCode();
	}
	
	public String toString() {
		return "FSA with " + states.size() + " states and " + edges.size() + " edges";
	}

}

