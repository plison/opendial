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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.domains.Domain;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLSettingsReader;
import opendial.readers.XMLStateReader;
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.DialogueState;
import opendial.utils.DistanceUtils;

public class WozTestSimulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);


	public WozTestSimulator (Domain domain, List<WoZDataPoint> data)
			throws DialException {
		String origWozFile = Settings.getInstance().planning.wozFile;
		Settings.getInstance().planning.wozFile = "";

		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem();
		
		int curIndex = 0;
		double nbCorrectActions = 0.0;

		for (WoZDataPoint testPoint : data) {
			log.info("-- new WOZ testing turn, current index " + curIndex);
			nbCorrectActions = performTurn(system.getState(), testPoint)? nbCorrectActions+1 : nbCorrectActions;
			curIndex++;
		}
		log.info("reached the end of the testing data");
		log.info("number of correct actions = " + nbCorrectActions + "/"  + data.size() + " (" + (nbCorrectActions*100/data.size()) + "%)");
		Settings.getInstance().planning.wozFile = origWozFile;
	}


	protected boolean performTurn(DialogueState systemState, WoZDataPoint point) {

			DialogueState newState = new DialogueState(point.getState());
			String goldActionValue= point.getOutput().getValue("a_m").toString();

			try {		

				WozLearnerSimulator.addNewDialogueState(systemState, newState);

				if (newState.getNetwork().hasChanceNode("a_u")) {
					systemState.addContent(newState.getNetwork().getChanceNode("a_u").getDistrib(), "woztest");		
				}
				
				String actualAction = "None";
				if (systemState.getNetwork().hasChanceNode("a_m")) {
					actualAction = systemState.getContent("a_m",true).toDiscrete().getProbTable(
						new Assignment()).getRows().iterator().next().getValue("a_m").toString();
				}
				systemState.addContent(new Assignment("a_m", goldActionValue), "woztest");	
				
				log.info("Actual action = " + actualAction + " vs. gold standard: " + goldActionValue);
					if (actualAction.equals(goldActionValue)) {
						return true;
					}
			}
			catch (DialException e) {
				log.warning("cannot perform the turn: " +e);
			}
			return false;
	}




}

