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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import opendial.arch.DialException;
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
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.DialogueState;
import opendial.utils.DistanceUtils;

public class WoZSimulator implements Simulator {

	// logger
	public static Logger log = new Logger("WoZSimulator", Logger.Level.DEBUG);

	public static final int NB_PASSES = 2;
	int currentPass = 0;
	
	DialogueState systemState;

	List<WoZDataPoint> data;

	int curIndex = 4;

	boolean paused = false;
	

	String inputDomain;
	String suffix;
	

	public WoZSimulator (DialogueState systemState, List<WoZDataPoint> data)
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
		
		if (curIndex < data.size() && currentPass < NB_PASSES) {
			log.debug("-- new WOZ turn, current index " + curIndex);
			DialogueState newState = new DialogueState(data.get(curIndex).getState());
			String goldActionValue= data.get(curIndex).getOutput().getValue("a_m").toString();
			
			// problem: we include an a_m', which means the trigger of the system decisions is screwed up
			try {		

				addNewDialogueState(newState, goldActionValue);
				
		//		log.debug("system state: " + systemState.getNetwork().getNodeIds());

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
			}
			catch (DialException e) {
				log.warning("cannot perform the turn: " +e);
			}
		}
		
		else {
			log.info("reached the end of the training data");
			curIndex = 0;
			currentPass++;
			if (inputDomain != null && suffix != null) {
				writeResults();
			}
			System.exit(0);
		}
		
		curIndex++;
	}


	private void writeResults() {
		
		Map<String,String> domainFiles = getDomainFiles();
		
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
		
		String rootpath = new File(domainFiles.keySet().iterator().next()).getParent();
		if ((new File(rootpath)).exists()) {
			final File[] files = (new File(rootpath)).listFiles();
			for (File f : files) {
				log.debug("deleting file " + f.getAbsolutePath());
				f.delete();
			}
			log.debug("deleting file " + rootpath);
			(new File(rootpath)).delete();
		}
		if ((new File(rootpath).mkdir())) {
			log.debug("created path " + rootpath);
		}
		
		for (String domainFile : domainFiles.keySet()) {
			try {
			 FileWriter fileWriter = new FileWriter(domainFile);
		            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		            bufferedWriter.write(domainFiles.get(domainFile));
		            // Always close files.
		            bufferedWriter.close();
			}
			catch (IOException e) {
				log.warning("could not write output to files. " + e);
			}
		}
	}
	
	private Map<String,String> getDomainFiles() {
		return getDomainFiles(inputDomain);
	}
	

	private Map<String,String> getDomainFiles(String topFile) {
        Map<String,String> outputFiles = new HashMap<String,String>();

        try {
		FileReader fileReader =  new FileReader(topFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
     	String rootpath = (new File(topFile)).getParent();   
        String text = "";
        String line = null;
        while((line = bufferedReader.readLine()) != null) {
            if (line.contains("import href")) {
            	String fileStr = line.substring(line.indexOf("href=\"")+6, line.indexOf("/>")-1);
            	outputFiles.putAll(getDomainFiles(rootpath +"/" + fileStr));
            }
            text += line + "\n";
        }	
        bufferedReader.close();
        outputFiles.put(topFile.replace(rootpath, rootpath+"/" + suffix).replace(rootpath.replace("/", "//"), 
        		rootpath+"/" + suffix), text);
		}
		catch (Exception e) {
			log.warning("could not extract domain files: " + e);
		}
        return outputFiles;
	}
	

	private void addNewDialogueState(DialogueState newState, String goldActionValue) throws DialException {
	
		log.debug("Initial user action: " + newState.getContent("a_u", true).prettyPrint());
		if (newState.getNetwork().hasChanceNode("perceived") && newState.getNetwork().hasChanceNode("carried")) {
		log.debug("Perceived objects: " + newState.getContent("perceived", true).prettyPrint() 
				+ " and carried objects " + newState.getContent("carried", true).prettyPrint());
		}
		
		if (systemState.getNetwork().hasChanceNode("i_u")) {
			ProbDistribution iudistrib = systemState.getContent("i_u", true);
			systemState.getNetwork().getChanceNode("i_u").removeAllRelations();
			systemState.getNetwork().getChanceNode("i_u").setDistrib(iudistrib);
		}
		systemState.getNetwork().removeNodes(Arrays.asList(new String[]{"u_u", "a_m-gold", 
				"carried", "perceived", "motion", "a_u", "a_m", "u_m"}));
		ChanceNode goldNode = new ChanceNode("a_m-gold");
		goldNode.addProb(ValueFactory.create(goldActionValue), 1.0);
		systemState.getNetwork().addNode(goldNode);
		
		systemState.getNetwork().addNetwork(newState.getNetwork());	
		
		systemState.activateUpdates(true);				
		systemState.setVariableToProcess("a_m");
		systemState.triggerUpdates();
		
		if (newState.getNetwork().hasChanceNode("a_u^p")) {
		log.debug("Predicted user action: " + systemState.getContent("a_u^p", true).prettyPrint());
		}
		// TODO Auto-generated method stub
		
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

