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


import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.aldebaran.qimessaging.Application;
import com.aldebaran.qimessaging.CallError;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.MapVal;
import opendial.bn.values.SetVal;
import opendial.bn.values.ValueFactory;
import opendial.state.DialogueState;
import opendial.utils.InferenceUtils;
import org.apache.commons.net.ftp.FTPClient;

public class NaoPerception extends AsynchronousModule {

	public static Logger log = new Logger("NaoPerception", Logger.Level.DEBUG);

	public static final int MEMORISATION_TIME = 5000;
	public static final double INIT_PROB = 0.7;
	
	com.aldebaran.qimessaging.Object memory;
	com.aldebaran.qimessaging.Object detection;
	
	Map<String,Long> currentPerception;
	
	
	public NaoPerception(DialogueSystem system) {
		super(system);
		try {
		memory = NaoSession.grabSession().getService("ALMemory");
		detection = NaoSession.grabSession().getService("ALLandMarkDetection");
		try { detection.call("unsubscribe", "naoPerception"); } catch (RuntimeException e) { }
		detection.call("subscribe", "naoPerception", 200, 0);
		currentPerception = new HashMap<String,Long>();
		}
		catch (Exception e) {
			log.warning("could not initiate Nao perception: " + e);
		}
	}
	

	@Override
	public void run() {
			
		while (memory != null) {
							
				Set<String> detected = new HashSet<String>();
				try {
				Object v = memory.call("getData", "LandmarkDetected").get();
				
			 if (v instanceof ArrayList && ((ArrayList)v).size()>0) {
				 ArrayList<ArrayList> landmarkList = (ArrayList) ((ArrayList)v).get(1);
					for (ArrayList<ArrayList<Integer>> landmark: landmarkList) {
						int id = landmark.get(1).get(0);
						if (id == 84) {
							detected.add("RedObj");
							memory.call("insertData", "Position(RedObj)", landmark);
						}
						else if (id == 107 || id == 127) {
							detected.add("BlueObj");
							memory.call("insertData", "Position(BlueObj)", landmark);
						}
					}
				} 	 
				updateState(state, detected);
				Thread.sleep(500);
				
				} 
				catch (Exception e) { e.printStackTrace(); }
			
		} 
	}

	
	public static void updateState(DialogueState state, Set<String> newObs) throws DialException {
		SimpleTable curTable = state.getContent("perceived", false).toDiscrete().getProbTable(new Assignment());
		
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
		
		//	log.debug("PERCEPTION: " + newTable);
		if (!state.getNetwork().hasChanceNode("perceived")) {
			ChanceNode newNode = new ChanceNode("perceived");
			state.getNetwork().addNode(newNode);
		}
		state.getNetwork().getChanceNode("perceived").setDistrib(new SimpleTable(newTable));

		if (curTable.getRows().size() != newTable.size()) {
			while (!state.isStable()) {
				try { Thread.sleep(100); }
				catch (InterruptedException e) { }
			}
			log.debug("reupdating the user intention");
			state.setVariableToProcess("perceived");
			state.triggerUpdates();
		}
	}

	
	
	@Override
	public void shutdown() {
		try {
			detection.call("unsubscribe", "naoPerception");
		} catch (CallError e) {
			e.printStackTrace();
		}
	}



}

