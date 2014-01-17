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

package opendial.modules;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections15.ListUtils;

import opendial.DialogueSystem;
import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

public class Simulator implements Module {

	// logger
	public static Logger log = new Logger("Simulator", Logger.Level.DEBUG);

	DialogueState curState;

	Domain domain;

	// the main system (to which the simulator is attached)
	DialogueSystem system;

	// the internal state variables for the simulator
	List<String> stateVars;

	// observation variables (e.g. external context)
	List<String> obsVars;



	/**
	 * Creates a new simulator 
	 * @param simDomain
	 * @throws DialException
	 */
	public Simulator(DialogueSystem system) throws DialException  {
		this(system, extractDomain(system.getSettings()));
	}


	/**
	 * Creates a new simulator 
	 * @param simDomain
	 * @throws DialException
	 */
	public Simulator(DialogueSystem system, Domain domain) throws DialException {

		this.system = system;
		this.domain = domain;
		curState = domain.getInitialState().copy();
		curState.setParameters(domain.getParameters());
		this.system.changeSettings(domain.getSettings());

		List<String> requiredParams = new ArrayList<String>(Arrays.asList("state", "observations"));
		requiredParams.removeAll(system.getSettings().params.keySet());
		if (!requiredParams.isEmpty()) {
			throw new MissingParameterException(requiredParams);
		}
		stateVars = Arrays.asList(system.getSettings().params.get("state").split(","));
		obsVars = Arrays.asList(system.getSettings().params.get("observations").split(","));
	}



	private static Domain extractDomain(Settings settings) throws DialException {
		if (!settings.params.containsKey("simulator-domain")) {
			throw new MissingParameterException("simulator-domain");
		}
		return XMLDomainReader.extractDomain(settings.params.get("simulator-domain"));
	}


	@Override
	public void start() throws DialException {
		if (system.isPaused()) {
			system.getState().addToState(new Assignment(system.getSettings().systemOutput, ValueFactory.none()));
		}
		else {
			system.addContent(new Assignment(system.getSettings().systemOutput, ValueFactory.none()));
		}
	}
	
	public boolean isRunning() {
		return !system.isPaused();
	}


	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		String actionVar = system.getSettings().systemOutput;
		if (updatedVars.contains(actionVar)) {
			TurnTakingProcess process = new TurnTakingProcess(state);
			process.start();
		}
	}

	@Override
	public void pause(boolean toPause) {	}


	public class TurnTakingProcess extends Thread {

		DialogueState systemState;

		public TurnTakingProcess(DialogueState systemState) {
			this.systemState = systemState;
		}


		public void run() {
			try {
				synchronized (curState) {

					Assignment systemAction = new Assignment(system.getSettings().systemOutput, ValueFactory.none());
					if (systemState.hasChanceNode(system.getSettings().systemOutput)) {
						systemAction = systemState.queryProb(system.getSettings().systemOutput).toDiscrete().getBest().removePrimes();
					}

					boolean turnPerformed = false;
					while (!turnPerformed) {
					curState.setParameters(domain.getParameters());									
					curState.addToState(systemAction);
					turnPerformed = performTurn();
					}
				}
			}
			catch (DialException e) {
				log.debug("cannot update simulator: " + e);
			}
		}


		private boolean performTurn() throws DialException {
			while (!curState.getNewVariables().isEmpty()) {

				Set<String> toProcess = curState.getNewVariables();
				curState.reduce();	

				for (Model model : domain.getModels()) {
					model.trigger(curState, toProcess);
				}

				if (!curState.getUtilityNodeIds().isEmpty()) {
					double reward = curState.queryUtil();
					system.recordComment("Reward: " + reward);
					curState.removeNodes(curState.getUtilityNodeIds());
				}

				Assignment fullSample = curState.getSample();
				fullSample.trim(ListUtils.union(stateVars, StringUtils.addPrimes(stateVars)));
				curState.addEvidence(fullSample);

				// step 2: generate new observations (if any)
				List<String> newObsVars = ListUtils.intersection
						(obsVars, new ArrayList<String>(toProcess));
				if (!newObsVars.isEmpty() && curState.hasChanceNodes(newObsVars)) {
					CategoricalTable newObs = curState.queryProb(newObsVars).toDiscrete();
					system.addContent(newObs.copy());
				}

				if (toProcess.contains(system.getSettings().userInput) 
						&& curState.hasChanceNode(system.getSettings().userInput)) {

					// step 3: generate the next user input
					CategoricalTable newInput = curState.queryProb(system.getSettings().userInput).toDiscrete();
					log.debug("Generated user input: " + newInput);

					// wait for the main system to be ready
					while (system.isPaused()) {
						try { Thread.sleep(50); } catch (InterruptedException e) { }
					}
					system.addContent(newInput.copy());	
					return true;
				}	
			}
			return false;
		}
	}

}

