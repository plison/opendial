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
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

public class Simulator extends DialogueSystem implements Module {

	// logger
	public static Logger log = new Logger("Simulator", Logger.Level.DEBUG);

	// the main system (to which the simulator is attached)
	DialogueSystem mainSystem;
	
	// the internal state variables for the simulator
	List<String> stateVars;
	
	// observation variables (e.g. external context)
	List<String> observationVars;

	
	/**
	 * Creates a new simulator 
	 * @param simDomain
	 * @throws DialException
	 */
	public Simulator(Domain simDomain) throws DialException {
		super(simDomain);
		if (!settings.params.containsKey("state") || !settings.params.containsKey("observations")) {
			throw new DialException("domain settings must specify state and observations variables");
		}
		stateVars = Arrays.asList(settings.params.get("state").split(","));
		observationVars = Arrays.asList(settings.params.get("observations").split(","));
		modules.clear();
	}


	@Override
	public void start(DialogueSystem system) throws DialException {
		paused = false;
		this.mainSystem = system;
		system.changeSettings(settings);
		TurnTakingProcess process = new TurnTakingProcess(new Assignment(settings.systemOutput, ValueFactory.none()));
		process.start();
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		String actionVar = domain.getSettings().systemOutput;
		if (updatedVars.contains(actionVar) && !paused) {
			Assignment systemAction = (state.hasChanceNode(actionVar))? 
					state.queryProb(actionVar).toDiscrete().getBest().removePrimes()
					: new Assignment(settings.systemOutput, ValueFactory.none());
			TurnTakingProcess process = new TurnTakingProcess(systemAction);
			process.start();
		}
	}

	@Override
	public void pause(boolean toPause) {
		super.pause(toPause);
	}


	@Override
	public void update() {
		
		while (!curState.getNewVariables().isEmpty()) {
			
			Set<String> toProcess = curState.getNewVariables();
			curState.reduce();	
			
			for (Model model : domain.getModels()) {
					model.trigger(curState, toProcess);
			}
			
			if (!curState.getUtilityNodeIds().isEmpty()) {
				double reward = curState.queryUtil();
				mainSystem.recordComment("Reward: " + reward);
				curState.removeNodes(curState.getUtilityNodeIds());
			}
			
			try {
				Assignment fullSample = curState.getSample();
				fullSample.trim(ListUtils.union(stateVars, StringUtils.addPrimes(stateVars)));
				curState.addEvidence(fullSample);
			}
			catch (DialException e) {
				log.warning("could not sample : " + e);
			}
		}
	}

	
	public class TurnTakingProcess extends Thread {

		Assignment systemAction;

		public TurnTakingProcess(Assignment systemAction) {
			this.systemAction = systemAction;
		}


		public void run() {
			try {
				synchronized (curState) {
					// step 1 : integrate the system action in the simulator state
					curState.setParameters(domain.getParameters());
					addContent(systemAction);

					// if the state does not generate any user input, rerun the inference engine
					if (!curState.hasChanceNode(settings.userInput)) {
						run();
					}

					// wait for the main system to be ready
					while (mainSystem.isPaused() 
							|| !mainSystem.getState().getNewVariables().isEmpty() 
							|| !mainSystem.getState().getActionNodeIds().isEmpty()) {
						try { Thread.sleep(50); } catch (InterruptedException e) { }
					}

					// step 2: generate new observations (if any)
					List<String> obsVars = ListUtils.intersection(observationVars, 
							new ArrayList<String>(curState.getChanceNodeIds()));
					if (!obsVars.isEmpty()) {
						CategoricalTable newObs = getContent(obsVars).toDiscrete();
						mainSystem.addContent(newObs.copy());
					}

					// step 3: generate the next user input
					CategoricalTable newInput = getContent(settings.userInput).toDiscrete();
					log.debug("Generated user input: " + newInput);
					mainSystem.addContent(newInput.copy());	
					}
			}
			catch (DialException e) {
				log.debug("cannot update simulator: " + e);
			}
		}
		

	}

}

