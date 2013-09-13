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

package opendial.simulation;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.domains.Domain;
import opendial.gui.GUIFrame;
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.DialogueState;

public class WoZSimulator implements Simulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);
	
	DialogueState systemState;
	
	List<WoZDataPoint> data;
	
	int curIndex = 0;
	
	boolean paused = false;
	
	public WoZSimulator (DialogueState systemState, List<WoZDataPoint> data)
			throws DialException {
		this.systemState = systemState;
		this.data = data;
	}

	
	public void startSimulator() {
		log.info("starting WOZ simulator");
		(new Thread(this)).start();
	}

	
	
	@Override
	public void run() {
		while (true) {
			try {
			while (!systemState.isStable() || paused) {
				Thread.sleep(50);
			}

			performTurn();
			}
			catch (Exception e) {
				e.printStackTrace();
				log.warning("simulator error: " + e);
			}
		}
	}


	private void performTurn() {
		if (curIndex < data.size()) {
			log.debug("-- new WOZ turn, current index " + curIndex);
			DialogueState newState = new DialogueState(data.get(curIndex).getState());
			
			// problem: we include an a_m', which means the trigger of the system decisions is screwed up
			try {
				Assignment goldAction = new Assignment("a_m-gold", data.get(curIndex).getOutput().getValue("a_m"));
				systemState.addContent(goldAction, "wozsim");

				for (ChanceNode dataNode : newState.getNetwork().getChanceNodes()) {
				if (dataNode.getId().equals("a_m")) {
					systemState.activateUpdates(false);
					systemState.addContent(dataNode.getDistrib(), "woz");
					systemState.activateUpdates(true);
				}
				else {
					systemState.addContent(dataNode.getDistrib(), "woz");					
				}
			}
		//	log.debug("system state: " + systemState.getNetwork().getNodeIds());
			
			if ((curIndex % 10) == 9)
			for (String var: systemState.getParameterIds()) {
				log.debug("==> parameter " + var + ": " + systemState.getContent(var, true));
			}
			}
			catch (DialException e) {
				log.warning("cannot add the new content: " +e);
			}
		}
		else {
			log.info("reached the end of the training data");
			System.exit(0);
		}
		curIndex++;
	}


	@Override
	public void pause(boolean shouldBePaused) {
		paused = true;
	}


	@Override
	public void addListener(StateListener listener) {
		return;
	}

}

