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

public class NaoPerception implements Module, Runnable {

	public static Logger log = new Logger("NaoPerception", Logger.Level.DEBUG);

	public static final int MEMORISATION_TIME = 5000;
	public static final double INIT_PROB = 0.7;
	
	DialogueSystem system;
	NaoSession session;
	boolean paused = true;
	
	public NaoPerception(DialogueSystem system) throws DialException {
		this.system = system;
		session = NaoSession.grabSession(system.getSettings());
	}
	
	public void start() {
		try {
			paused = false;
			session.call("ALVideoDevice", "setActiveCamera", 1);
			session.call("ALLandMarkDetection", "subscribe", "naoPerception", 200, 0);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				   @Override
				   public void run() {  
					  System.out.println("Shutting down Nao perception");
					  try {  
						session.call("ALLandMarkDetection", "unsubscribe", "naoPerception");
					}	catch (Exception e) {}
				   }
				 });
		}			
			catch (Exception e) {
				log.warning("could not initiate Nao perception: " + e);
			}
		(new Thread(this)).start();
	}
	
	public void pause(boolean toPause) {
		paused = toPause;
	}
	
	public boolean isRunning() {
		return !paused;
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

	
	public void updateState(Set<String> perceivedObjects) throws DialException {
		
		CategoricalTable perception = system.getState().hasChanceNode("perceived")? 
				system.getContent("perceived").toDiscrete().copy()
				: new CategoricalTable(new Assignment("perceived", ValueFactory.none()));

		CategoricalTable newperception = perception.copy();
		for (Assignment a : perception.getRows()) {
			newperception.addRow(a, (1-INIT_PROB)*perception.getProb(a));
		}
		SetVal newVal = (SetVal)ValueFactory.create(perceivedObjects.toString());
		newperception.incrementRow(new Assignment("perceived", newVal), INIT_PROB);
	
		if (newperception.getRows().size() != perception.getRows().size()) {
			system.addContent(newperception);
		}
		else {
			system.getState().getChanceNode("perceived").setDistrib(newperception);
			
		}
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 }



}

