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
import opendial.bn.distribs.discrete.EqualityDistribution;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.domains.Domain;
import opendial.gui.GUIFrame;
import opendial.inference.sampling.WoZQuerySampling;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLSettingsReader;
import opendial.readers.XMLStateReader;
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.DialogueState;
import opendial.state.rules.AnchoredRuleCache;
import opendial.utils.DistanceUtils;
import opendial.utils.XMLUtils;

public class WozLearnerSimulator implements Simulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);

	public static final int NB_PASSES = 2;
	public static final int TEST_FREQ = 100;
	int currentPass = 0;

	DialogueState initState;

	DialogueState systemState;

	List<WoZDataPoint> data;

	List<WoZDataPoint> testData;

	WozTestSimulator testSimulator;

	int curIndex = 0;

	boolean paused = false;

	String inputDomain;
	String suffix;


	public WozLearnerSimulator (DialogueState systemState, List<WoZDataPoint> data)
			throws DialException {
		initState = systemState;
		this.systemState = systemState.copy();
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

	public void setTestData(List<WoZDataPoint> testData) {
		this.testData = testData;
	}


	public void startSimulator() {
		log.info("starting WOZ simulator");
		log.info("PARAMETERS: Learn factor: " + WoZQuerySampling.RATE + 
				", min/max: " + WoZQuerySampling.MIN + "/" + WoZQuerySampling.MAX +
				", Equality factor: "+EqualityDistribution.PROB_WITH_SINGLE_NONE + 
				", Likelihood threshold: " + AnchoredRuleCache.PROB_THRESHOLD + 
				", using KDE: " + SimpleEmpiricalDistribution.USE_KDE);
	//		writeResults();
	//		performTests();
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

		try {		

			if (curIndex < data.size() && currentPass < NB_PASSES) {
				log.debug("-- new WOZ turn, current index " + curIndex);

				DialogueState newState = new DialogueState(data.get(curIndex).getState().copy());

				String goldActionValue= data.get(curIndex).getOutput().getValue("a_m").toString();
				newState.addContent(new Assignment("a_m-gold", goldActionValue), "woz");

				addNewDialogueState(newState);

				showInformation(systemState, newState);

				if (newState.getNetwork().hasChanceNode("a_u")) {
					systemState.addContent(newState.getNetwork().getChanceNode("a_u").getDistrib(), "woz1");		
				}

				systemState.getNetwork().removeNode("motion");

				if ((curIndex % 10) == 9) {
					List<String> paramIds = systemState.getParameterIds();
					Collections.sort(paramIds);
					for (String var: paramIds) {
						log.debug("==> parameter " + var + ": " + systemState.getContent(var, true));
					}
				}

				if ((curIndex % TEST_FREQ) == (TEST_FREQ-1)) {
					log.debug("-------------   Start testing ------------");
					writeResults();
					performTests();
				} 

				curIndex++;

			}
			else if (currentPass < NB_PASSES) {
				curIndex = 0;
				currentPass++;
				log.debug("----> moving to pass " + currentPass);
				systemState = initState.copy();		
			}
			else {
				log.info("END. reached the end of the training data");
				if (inputDomain != null && suffix != null) {
					log.debug("writing the results");
					writeResults();
				}
				System.exit(0);
			}
		}
		catch (DialException e) {
			log.warning("cannot perform the turn: " +e);
		}
	}


	private void performTests() {

		try {
			String testDomain = (new File(inputDomain)).getParent().toString() 
					+ "/" + suffix + "/" + (new File(inputDomain)).getName();
			log.debug("Path of test domain: " + testDomain);
			Domain domain = XMLDomainReader.extractDomain(testDomain);
			new WozTestSimulator(domain, testData);
			log.debug("back in the training phase!");
		}
		catch (Exception e) {
			log.warning("cannot perform tests: " + e);
		}
	}

	protected void addNewDialogueState(DialogueState newState) throws DialException {
		addNewDialogueState(systemState, newState);
	}

	protected static void addNewDialogueState(DialogueState systemState, DialogueState newState) throws DialException {

		for (ChanceNode cn : newState.getNetwork().getChanceNodes()) {
			if (Arrays.asList("carried", "perceived", "motion", "a_m-gold").contains(cn.getId())) {
				if (!systemState.getNetwork().hasChanceNode(cn.getId())) {
					systemState.getNetwork().addNode(cn);
				}
				else {
					systemState.getNetwork().getChanceNode(cn.getId()).setDistrib(cn.getDistrib());
				}
			}
		}

		systemState.activateUpdates(true);				
		systemState.setVariableToProcess("a_m");
		systemState.triggerUpdates();

		systemState.activateUpdates(false);


	}


	protected static void showInformation(DialogueState systemState, DialogueState newState) {
		try {

			String lastAction = "None";
			String lastRealAction = "None";
			String lastMove = "None";

			if (systemState.getNetwork().hasChanceNode("a_m")) {
				lastAction = systemState.getContent("a_m", true).toDiscrete().getProbTable(
						new Assignment()).getRows().iterator().next().getValue("a_m").toString();
			}
			if (systemState.getNetwork().hasChanceNode("last(a_m)")) {		
				lastRealAction = systemState.getContent("last(a_m)", true).toDiscrete().getProbTable(
						new Assignment()).getRows().iterator().next().getValue("last(a_m)").toString();
			}
			if (systemState.getNetwork().hasChanceNode("lastMove")) {
				lastMove = systemState.getContent("lastMove", true).toDiscrete().getProbTable(
						new Assignment()).getRows().iterator().next().getValue("lastMove").toString();
			}
			log.debug("Last a_m: " + lastAction + " (last real action: " + lastRealAction + " and last move: " + lastMove+ ")");


			if (newState.getNetwork().hasChanceNode("perceived") && 
					newState.getNetwork().hasChanceNode("carried")) {
				log.debug("Perceived : " + newState.getContent("perceived", true).prettyPrint() 
						+ " and carried " + newState.getContent("carried", true).prettyPrint());
			}

			if (newState.getNetwork().hasChanceNode("a_u")) {
				log.debug("Initial a_u: " + newState.getContent("a_u", true).prettyPrint());
			}
		}
		catch (DialException e) {
			log.warning("could not show information about the current dialogue state: " + e);
		}
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

