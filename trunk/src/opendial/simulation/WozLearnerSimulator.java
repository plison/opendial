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
import opendial.utils.XMLUtils;

public class WozLearnerSimulator implements Simulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);

	public static final int NB_PASSES = 2;
	public static final int TEST_FREQ = 2;
	int currentPass = 0;

	DialogueState systemState;

	List<WoZDataPoint> data;
	
	WozTestSimulator testSimulator;

	int curIndex = 0;

	boolean paused = false;

	String inputDomain;
	String suffix;


	public WozLearnerSimulator (DialogueState systemState, List<WoZDataPoint> data)
			throws DialException {
		this.systemState = systemState;
		this.data = data;
	}

	public void specifyOutput(String inputDomain, String suffix) {
		this.inputDomain = inputDomain;
		if (suffix.length() > 0) {
			this.suffix = suffix;
		}
		else {
			log.warning("suffix cannot be empty");
			this.suffix = "default";
		}
	}

	public void setTestSimulator(WozTestSimulator testSimulator) {
		this.testSimulator = testSimulator;
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



	protected void performTurn() {

		if (curIndex < data.size() && currentPass < NB_PASSES) {
			log.debug("-- new WOZ turn, current index " + curIndex);
		
			DialogueState newState = new DialogueState(data.get(curIndex).getState());
			
			String goldActionValue= data.get(curIndex).getOutput().getValue("a_m").toString();
			ChanceNode goldNode = new ChanceNode("a_m-gold");
			goldNode.addProb(ValueFactory.create(goldActionValue), 1.0);
			newState.getNetwork().addNode(goldNode);

			try {		
				addNewDialogueState(newState);

				systemState.activateUpdates(false);

				if (newState.getNetwork().hasChanceNode("a_u")) {
					systemState.addContent(newState.getNetwork().getChanceNode("a_u").getDistrib(), "woz1");		
				}

				if (goldActionValue.contains("Do(")) {
					String lastMove = goldActionValue.replace("Do(", "").substring(0, goldActionValue.length()-4);
					systemState.addContent(new Assignment("lastMove", lastMove), "woz2");
				}

				if ((curIndex % 10) == 9) {
					List<String> paramIds = systemState.getParameterIds();
					Collections.sort(paramIds);
					for (String var: paramIds) {
						log.debug("==> parameter " + var + ": " + systemState.getContent(var, true));
					}
				}

			/**	if ((curIndex % TEST_FREQ) == (TEST_FREQ-1)) {
					log.debug("-------------   Start testing ------------");
							writeResults();
							performTests();
				} */
			}
			catch (DialException e) {
				log.warning("cannot perform the turn: " +e);
			}
		}
		else if (currentPass < NB_PASSES) {
			curIndex = 0;
			currentPass++;
			log.debug("----> moving to pass " + currentPass);
		}
		else {
			log.info("END. reached the end of the training data");
			if (inputDomain != null && suffix != null) {
				log.debug("writing the results");
				writeResults();
			}
			System.exit(0);
		}

		curIndex++;
	}


	private void performTests() {

		try {
			String testDomain = (new File(inputDomain)).getParent().toString() 
					+ "/" + suffix + "/" + (new File(inputDomain)).getName();
			log.debug("Path of test domain: " + testDomain);
			Domain domain = XMLDomainReader.extractDomain(testDomain);
			DialogueSystem system = new DialogueSystem(domain);
			
		}
		catch (DialException e) {
			log.warning("cannot perform tests: " + e);
		}
	}

	protected void addNewDialogueState(DialogueState newState) throws DialException {

		systemState.getNetwork().removeNodes(Arrays.asList(new String[]{"u_u", "a_m-gold", 
				"carried", "perceived", "motion", "a_u", "a_m", "u_m"}));

		if (newState.getNetwork().hasChanceNode("perceived") && 
				newState.getNetwork().hasChanceNode("carried")) {
			log.debug("Perceived : " + newState.getContent("perceived", true).prettyPrint() 
					+ " and carried " + newState.getContent("carried", true).prettyPrint());
		}

		/**	if (systemState.getNetwork().hasChanceNode("i_u")) {
			ProbDistribution iudistrib = systemState.getContent("i_u", true);
			log.debug("input and outputs for i_u" + 
				systemState.getNetwork().getChanceNode("i_u").getInputNodeIds() + " and " + 
				systemState.getNetwork().getChanceNode("i_u").getOutputNodesIds());
			systemState.getNetwork().getChanceNode("i_u").removeAllRelations();
			systemState.getNetwork().getChanceNode("i_u").setDistrib(iudistrib);
		} */

		systemState.getNetwork().addNetwork(newState.getNetwork());	

		systemState.activateUpdates(true);				
		systemState.setVariableToProcess("a_m");
		systemState.triggerUpdates();

		if (newState.getNetwork().hasChanceNode("a_u")) {
			log.debug("Initial a_u: " + newState.getContent("a_u", true).prettyPrint());
		}

		/** if (newState.getNetwork().hasChanceNode("a_u^p")) {
		log.debug("Predicted user action: " + systemState.getContent("a_u^p", true).prettyPrint());
		} */

	}

	private void writeResults() {

		Map<String,String> domainFiles = XMLUtils.getDomainFiles(inputDomain, suffix);
		updateDomainWithParameters(domainFiles);
		XMLUtils.writeDomain(domainFiles);
	}
	
	
	private void updateDomainWithParameters(Map<String,String> domainFiles) {
		for (String id : systemState.getParameterIds()) {
			try {
				ContinuousProbDistribution distrib = systemState.getNetwork().getChanceNode(id).getDistrib().toContinuous();
				if (distrib instanceof UnivariateDistribution) {
					double mean = ((UnivariateDistribution)distrib).getMean();
					for (String domainFile : new HashSet<String>(domainFiles.keySet())) {
						String text = domainFiles.get(domainFile).replace("\""+id+"\"", "\""+DistanceUtils.shorten(mean) + "\"");
						domainFiles.put(domainFile, text);
					}
				}
				else if (distrib instanceof MultivariateDistribution) {
					Double[] mean = ((MultivariateDistribution)distrib).getMean();
					for (String domainFile : new HashSet<String>(domainFiles.keySet())) {
						String text = domainFiles.get(domainFile);
						for (int i = 0 ; i < mean.length ;i++) {
							text = text.replace(id+"["+i+"]", ""+DistanceUtils.shorten(mean[i]));
						}
						domainFiles.put(domainFile, text);
					}
				}
			}
			catch (DialException e) {
				log.warning("could not convert parameter into continuous probability distribution: " + e);
			}
		}
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

