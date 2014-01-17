// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.nao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.SetVal;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.utils.InferenceUtils;

public class NaoPerception implements Module {

	public static Logger log = new Logger("NaoPerception", Logger.Level.DEBUG);

	public static final int MEMORISATION_TIME = 5000;
	public static final double INIT_PROB = 0.7;
	
	DialogueSystem system;
	NaoSession session;
	boolean paused = true;
	
	Map<String,Long> currentPerception;
	
	
	public void start(DialogueSystem system) {
		this.system = system;
		try {
			paused = false;
			session = NaoSession.grabSession(system.getSettings());
			session.call("ALLandmarkDetection", "unsubscribe", "naoPerception");
			session.call("AlLandmarkDetection", "subscribe", "naoPerception", 200, 0);
			currentPerception = new HashMap<String,Long>();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				   @Override
				   public void run() {  
					  System.out.println("Shutting down Nao perceptioin");
					  try {  
						session.call("ALLandmarkDetection", "unsubscribe", "naoPerception");
					}	catch (Exception e) {}
				   }
				 });
		}			
			catch (Exception e) {
				log.warning("could not initiate Nao perception: " + e);
			}
		run();
	}
	
	public void pause(boolean toPause) {
		paused = toPause;
	}
	

	public void run() {
			
		while (session != null) {
							
				Set<String> detected = new HashSet<String>();
				try {
				Object v = session.call("ALMemory", "getData", "LandmarkDetected");
				
			 if (v instanceof ArrayList && ((ArrayList)v).size()>0) {
				 ArrayList<ArrayList> landmarkList = (ArrayList) ((ArrayList)v).get(1);
					for (ArrayList<ArrayList<Integer>> landmark: landmarkList) {
						int id = landmark.get(1).get(0);
						if (id == 84) {
							detected.add("RedObj");
							session.call("ALMemory", "insertData", "Position(RedObj)", landmark);
						}
						else if (id == 107 || id == 127) {
							detected.add("BlueObj");
							session.call("ALMemory", "insertData", "Position(BlueObj)", landmark);
						}
					}
				} 	 
				updateState(detected);
				Thread.sleep(500);
				
				} 
				catch (Exception e) { e.printStackTrace(); }
			
		} 
	}

	
	public void updateState(Set<String> newObs) throws DialException {
		CategoricalTable curTable = system.getContent("perceived").toDiscrete();
		
		Map<Assignment,Double> newTable = new HashMap<Assignment,Double>();

		for (Assignment row : curTable.getRows()) {
			double newProb =  (1- INIT_PROB)*curTable.getProb(row);
			if (newProb > 0.001) {
				newTable.put(row, newProb);
			}
		} 
		
		SetVal newVal = (SetVal)ValueFactory.create("[]");
		for (String label : newObs) {
			//	newVal.add(ValueFactory.create("<label:"+label+">"));
			newVal.add(ValueFactory.create(label));
		}
		Assignment assign = new Assignment("perceived", newVal);
		if (!newTable.containsKey(assign)) {
			newTable.put(assign, 0.0);
		}
		newTable.put(assign, newTable.get(assign) + INIT_PROB);
		
		newTable = InferenceUtils.normalise(newTable);
		
		if (!system.getState().hasChanceNode("perceived")) {
			ChanceNode newNode = new ChanceNode("perceived");
			system.getState().addNode(newNode);
		}

		if (curTable.getRows().size() != newTable.size()) {
			system.addContent(new CategoricalTable(newTable));
		}
		else {
			system.getState().getChanceNode("perceived").setDistrib(new CategoricalTable(newTable));
			
		}
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 }



}

