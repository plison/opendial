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

public class WozTestSimulator extends WozLearnerSimulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);

	public static final int NB_PASSES = 2;
	public static final int TEST_FREQ = 2;
	int currentPass = 0;

	DialogueState systemState;

	List<WoZDataPoint> data;

	int curIndex = 0;

	boolean paused = false;

	List<WoZDataPoint> testPoints;

	String inputDomain;
	String suffix;


	public WozTestSimulator (DialogueState systemState, List<WoZDataPoint> data)
			throws DialException {
		super(systemState, data);
	}


	@Override
	protected void performTurn() {

		if (curIndex < data.size() && currentPass < NB_PASSES) {
			log.debug("-- new WOZ turn, current index " + curIndex);
			DialogueState newState = new DialogueState(data.get(curIndex).getState());
			String goldActionValue= data.get(curIndex).getOutput().getValue("a_m").toString();

			try {		

				addNewDialogueState(newState);

				systemState.activateUpdates(false);

				if (newState.getNetwork().hasChanceNode("a_u")) {
					systemState.addContent(newState.getNetwork().getChanceNode("a_u").getDistrib(), "woztest");		
				}

				if (goldActionValue.contains("Do(")) {
					String lastMove = goldActionValue.replace("Do(", "").substring(0, goldActionValue.length()-4);
					systemState.addContent(new Assignment("lastMove", lastMove), "woz2");
				}

			}
			catch (DialException e) {
				log.warning("cannot perform the turn: " +e);
			}
		}
		else if (currentPass < NB_PASSES) {
			curIndex = 0;
			currentPass++;
			log.debug("---- moving to pass " + currentPass);
		}
		else {
			log.info("reached the end of the testing data");
			if (inputDomain != null && suffix != null) {
				log.debug("writing the results");
			}
			try { Thread.sleep(1000); } catch (Exception e) { }
			System.exit(0);
		}

		curIndex++;
	}




}

