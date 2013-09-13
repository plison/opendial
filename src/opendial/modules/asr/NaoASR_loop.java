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

package opendial.modules.asr;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.gui.GUIFrame;
import opendial.modules.asr.ASRLock;
import opendial.state.DialogueState;

public class NaoASR_loop extends Thread {

	// logger
	public static Logger log = new Logger("NaoASR_loop", Logger.Level.DEBUG);

	com.aldebaran.qimessaging.Object memoryProxy;
	DialogueState state;
	boolean locked;

	public NaoASR_loop (com.aldebaran.qimessaging.Object memoryProxy, DialogueState state, boolean locked) {
		this.memoryProxy = memoryProxy;
		this.state = state;
		this.locked = locked;
	}


	public void run() {

		try {
			if (!locked) {
				Map<String,Float> hypotheses = new HashMap<String,Float>();
				Object o = memoryProxy.call("getData", "WordRecognized").get();
				ArrayList val = new ArrayList();
				if (o instanceof ArrayList) {
					val.addAll(((ArrayList)o));
				}
				if (val.size() > 0) {
					for (int i = 0 ; i < val.size()/2 ; i++) {
						String str = val.get(i*2).toString();
						float f = (Float) val.get(i*2+1);
						if (f > 0.1) {
							hypotheses.put(str, f);
						}
					}
				} 
				memoryProxy.call("insertData", "WordRecognized", "");

				if (!hypotheses.isEmpty()) {
					log.debug("initial hypotheses: " + hypotheses.toString());
					SimpleTable chypotheses = correctHypotheses(hypotheses);
					log.debug("corrected hypotheses: " + chypotheses.toString().replace("\n", ", "));
					while (!state.isStable()) {
						Thread.sleep(50);
					}
					state.addContent(chypotheses, "asr");
				}

				else {

					Object sensorVal = memoryProxy.call("getData", "MiddleTactilTouched").get();
					if (sensorVal instanceof Float && ((Float)sensorVal) > 0.99f) {
						ASRLock.addLock("button");
						Assignment feedback = new Assignment(
								Settings.getInstance().gui.systemUtteranceVar, "OK, I'll go to sleep then");
						state.addContent(feedback, "asrfeedback");
						memoryProxy.call("insertData", "MiddleTactilTouched", 0.0f); 
					} 
				} 
			}
		else {	
			Object sensorVal = memoryProxy.call("getData", "MiddleTactilTouched").get();
			if (sensorVal instanceof Float && ((Float)sensorVal) > 0.99f) {
					ASRLock.removeLock("button");
					Assignment feedback = new Assignment(
							Settings.getInstance().gui.systemUtteranceVar, "OK, I am now listening");
					state.addContent(feedback, "asrfeedback");
					memoryProxy.call("insertData", "MiddleTactilTouched", 0.0f); 
				} 
			} 
		}
		catch (Exception e) {
			log.warning("cannot run recognition: " + e);
		}
	}



	private SimpleTable correctHypotheses(Map<String,Float> initHypotheses) {

		float total = (float)Math.exp(3.0f);	
		for (String initHyp : initHypotheses.keySet()) {
			total += (float)Math.exp(initHypotheses.get(initHyp)*8);;
		}

		SimpleTable newHypotheses = new SimpleTable();
		for (String initHyp : initHypotheses.keySet()) {
			float newProb = (float)Math.exp(initHypotheses.get(initHyp)*8);
			newHypotheses.addRow(new Assignment(Settings.getInstance().gui.userUtteranceVar, initHyp), newProb/total);
		}
		return newHypotheses;
	}


	private boolean isContainedHypothesis(String hyp, Set<String> allHyps) {

		for (String otherHyp : allHyps) {
			if (!otherHyp.equals(hyp) && otherHyp.contains(hyp)) {
				log.debug("hypothesis " + hyp + " contained in "  + otherHyp);
				return true;
			}
		}
		return false;
	}
}

