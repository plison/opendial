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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import opendial.DialogueSystem;
import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;

 
/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoASR  implements Module, Runnable {

	static Logger log = new Logger("NaoASR", Logger.Level.DEBUG);

	NaoSession session;

	DialogueSystem system;
	boolean paused = true; 

	public NaoASR(DialogueSystem system) throws DialException {
		this.system = system;

		List<String> params = new ArrayList<String>(Arrays.asList("grammar", "nao_ip"));
		params.removeAll(system.getSettings().params.keySet());
		if (!params.isEmpty()) {
			throw new MissingParameterException(params);
		}
		session = NaoSession.grabSession(system.getSettings());
		log.debug("connecting to Nao with address " + system.getSettings().params.get("nao_ip"));

	}
	
	/**
	 * @throws DialException 
	 *
	 */ 
	@Override
	public void start() throws DialException {
		log.info("starting up NAO ASR....");
		paused = false;

		session.call("ALSpeechRecognition", "pause", false);
		session.call("ALSpeechRecognition", "setAudioExpression", false);
	/**	session.call("ALSpeechRecognition", "setParameter", "NBFirstPassHypotheses", 3);
		session.call("ALSpeechRecognition", "setParameter", "NBSecondPassHypotheses", 5); */


		if (session.<ArrayList>call("ALSpeechRecognition", "getSubscribersInfo").size() > 0) {
			session.call("ALSpeechRecognition", "unsubscribe", "naoASR");
		}
		String asrGrammar = system.getSettings().params.getProperty("grammar");
		log.info("Using ASR grammar: " + asrGrammar);
		session.call("ALSpeechRecognition", "compile", asrGrammar, 
				asrGrammar.replace("bnf", "lcf"), "English");
		session.call("ALSpeechRecognition", "removeAllContext");
		session.call("ALSpeechRecognition", "addContext", asrGrammar.replace("bnf", "lcf"), "UserEval");
		session.call("ALSpeechRecognition", "setParameter", "Sensitivity", 100);
		log.debug("ASR sensitivity: " + session.call("ALSpeechRecognition", "getParameter", "Sensitivity"));
		session.call("ALSpeechRecognition", "subscribe", "naoASR");
		session.call("ALMemory", "insertData", "WordRecognized", "");
		session.call("ALSpeechRecognition", "pause", false); 
		log.debug("ASR successfully started!");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {  
				System.out.println("Shutting down Nao ASR");
				try {  
					session.call("ALMemory", "unsubscribeToEvent", "WordRecognized", "naoASR");
					session.call("ALSpeechRecognition", "removeAllContext");
					session.call("ALSpeechRecognition", "unsubscribe", "naoASR");
					session.call("ALSpeechRecognition", "pause", true);
				}	catch (Exception e) {}
			}
		});

		(new Thread(this)).start();
	}


	@Override
	public void run() {
		while (true) {
			try {	
				Thread.sleep(80);  
				
					if (!paused) {
						Map<String,Float> hypotheses = new HashMap<String,Float>();
						Object o = session.call("ALMemory", "getData", "WordRecognized");
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
						session.call("ALMemory", "insertData", "WordRecognized", "");

						if (!hypotheses.isEmpty()) {
							log.debug("initial hypotheses: " + hypotheses.toString());
							CategoricalTable chypotheses = correctHypotheses(hypotheses);
							log.debug("corrected hypotheses: " + chypotheses.toString().replace("\n", ", "));
							system.addContent(chypotheses);
						}

					} 

			}
			catch (Exception e) { log.debug("exception : " + e); }

		}
	}



	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {	}


	public void pause(boolean toPause) {
		try {
			if (toPause) {
				session.call("ALSpeechRecognition", "pause", true);
			}
			else {
				session.call("ALSpeechRecognition", "pause", false);
				session.call("ALMemory", "insertData", "WordRecognized", "");	
			}
		} 
		catch (DialException e) {
			e.printStackTrace();
		}
		paused = toPause;
	}
	
	public boolean isRunning() {
		return !paused;
	}




	private CategoricalTable correctHypotheses(Map<String,Float> initHypotheses) {

		float total = (float)Math.exp(3.0f);	
		for (String initHyp : initHypotheses.keySet()) {
			total += (float)Math.exp(initHypotheses.get(initHyp)*8);;
		}

		CategoricalTable newHypotheses = new CategoricalTable();
		for (String initHyp : initHypotheses.keySet()) {
			float newProb = (float)Math.exp(initHypotheses.get(initHyp)*8);
			newHypotheses.addRow(new Assignment(system.getSettings().userInput, initHyp), newProb/total);
		}
		return newHypotheses;
	}




}
