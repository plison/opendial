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


package opendial.arch.statechange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import opendial.arch.ConfigurationSettings;
import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.gui.GUIFrame;
import opendial.modules.SynchronousModule;

public class StateController {

	// logger
	public static Logger log = new Logger("StateController", Logger.Level.DEBUG);

	DialogueState state;

	Stack<String> newVariables;

	List<SynchronousModule> modules;
	
	ForwardPlanner planner;
	
	StatePruner pruner;

	// number of processes currently working on the dialogue state
	List<Object> busyProcesses;

	// number of processes recently completed (but not yet pruned)
	List<Object> completedProcesses;



	public StateController(DialogueState state) {
		this.state = state;
		modules = new ArrayList<SynchronousModule>();
		newVariables = new Stack<String>();

		busyProcesses = new ArrayList<Object>();
		completedProcesses = new ArrayList<Object>();
	}


	public void attachModule(SynchronousModule module) {
		modules.add(module);
	}


	public synchronized void applyRule(Rule rule) throws DialException {
		RuleInstantiator instantiator = new RuleInstantiator(state, rule);
		setAsBusy(instantiator);
		instantiator.start();
	}


	/**
	 * 
	 * @param string
	 * @return
	 */
	public synchronized boolean hasNewVariable(String nodeId) {
		return newVariables.contains(nodeId);
	}

	/**
	 * 
	 * @param id
	 */
	public synchronized void setVariableAsNew(String nodeId) {
		newVariables.add(nodeId);
	}

	protected synchronized void setAsBusy(Object process) {
		busyProcesses.add(process);
	}

	protected synchronized void setAsCompleted(Object process) {
		busyProcesses.remove(process);
		completedProcesses.add(process);
		if (busyProcesses.isEmpty()) {
			if (!newVariables.isEmpty()) {
				triggerUpdates();
			}
			else {
				stabilise();
			}
		}

	}


	public synchronized void triggerUpdates() {
		setAsBusy(this);
		while (!newVariables.isEmpty()) {
			String newVariable = newVariables.pop();
			for (SynchronousModule module : modules) {

				if (!(state.isFictive() && module.isExternal()) && 
						!state.getNetwork().hasActionNode(newVariable)) {
					
					// trying to avoid infinite loops of triggers
					if (Collections.frequency(completedProcesses, module) < 2) {
						module.trigger(state, newVariable);
					}
					else {
						log.warning("looping processes: " + completedProcesses);
					}
				}
			}
		}
		setAsCompleted(this);
	}


	public synchronized boolean isStable() {
		return (busyProcesses.isEmpty());
	}


	private synchronized void stabilise () {
		completedProcesses.clear();	
		if (ConfigurationSettings.getInstance().isGUIShown()) {
			GUIFrame.getSingletonInstance().updateCurrentState(state);
		}
		//	synchronized (state) { state.notify(); }
	}


	public String toString() {
		String s = "";
		s += "Busy processes: [";
		for (Object p : busyProcesses) {
			s += p.getClass().getSimpleName() + ",";
		}
		s = (s + "]").replace(",]", "]")+ "\n";
		s += "Completed processes: [";
		for (Object p : completedProcesses) {
			s += p.getClass().getSimpleName() + ",";
		}
		s = (s + "]").replace(",]", "]") + "\n";
		s += "New variables: " + newVariables;
		return s;
	}


}
