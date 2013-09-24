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

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aldebaran.qimessaging.CallError;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Case;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.modules.asr.ASRLock;
import opendial.modules.asr.NaoASR_lockupdate;
import opendial.modules.asr.NaoASR_loop;
import opendial.modules.asr.RecognitionGrammar;
import opendial.readers.XMLASRGrammarReader;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoASR extends AsynchronousModule {

	static Logger log = new Logger("NaoASR", Logger.Level.DEBUG);

	com.aldebaran.qimessaging.Object asrProxy;
	com.aldebaran.qimessaging.Object sensorProxy;
	com.aldebaran.qimessaging.Object memoryProxy;

	List<String> keywords;


	public NaoASR(DialogueSystem system) {
		super(system);
	}


	/**
	 *
	 */
	@Override
	public void shutdown() {
		try {
			memoryProxy.call("unsubscribeToEvent", "WordRecognized", "naoASR"); 
			asrProxy.call("removeAllContext");
			asrProxy.call("unsubscribe", "naoASR");
			asrProxy.call("pause", true); 
		}	catch (Exception e) {}
	}

	int incr = 0;

	/**
	 *
	 */
	@Override
	public void run() {

		log.info("starting up NAO ASR....");
		if (initialise()) {
			ASRLock.connectASR(this);
			while (true) {
				try {	
					Thread.currentThread().sleep(80);  
					NaoASR_loop thread = new NaoASR_loop(memoryProxy, state, !ASRLock.getLocks().isEmpty());
					thread.start();
					int counter = 0;
					while (thread.isAlive() && counter < 300) {
						Thread.currentThread().sleep(20);
						counter++;
					}
					if (counter == 100) {
						thread.stop();
						try { asrProxy.call("unsubscribe", "naoASR"); } catch (RuntimeException e) { }
						try { asrProxy.call("subscribe", "naoASR"); } catch (RuntimeException e) { }
						log.debug("LOOP THREAD DESTROYED");
					}
				}
				catch (Exception e) { log.debug("exception : " + e); }

			}
		}
	}



	/**
	 * 
	 */
	private boolean initialise() {

		if (asrProxy == null || memoryProxy == null) {
			try {
				log.debug("connecting to Nao with address " + Settings.getInstance().nao.ip);
				asrProxy = NaoSession.grabSession().getService("ALSpeechRecognition");
				asrProxy.call("pause", true);
				asrProxy.call("pause", false);
				asrProxy.call("setAudioExpression", false);
				memoryProxy = NaoSession.grabSession().getService("ALMemory");

				sensorProxy = NaoSession.grabSession().getService("ALSensors");
				sensorProxy.call("subscribe", "naoASR");
			}
			catch (Exception e) {
				log.warning("problem connecting to the Nao");
				return false;
			}
		}

		if (Settings.getInstance().nao.asr != null) {
			String filename =Settings.getInstance().nao.asr;
			try {
					if (asrProxy.<ArrayList>call("getSubscribersInfo").get().size() > 0) {
						asrProxy.call("unsubscribe", "naoASR");
					}
					log.info("Using ASR grammar: " + Settings.getInstance().nao.asr);
					asrProxy.call("compile", Settings.getInstance().nao.asr, 
							Settings.getInstance().nao.asr.replace("bnf", "lcf"), "English");
					log.debug("using context: " + Settings.getInstance().nao.asr.replace("bnf", "lcf"));
					asrProxy.call("removeAllContext");
					asrProxy.call("addContext", Settings.getInstance().nao.asr.replace("bnf", "lcf"), "UserEval");
					log.debug("ASR sensitivity: " + asrProxy.call("getParameter", "Sensitivity").get());
					asrProxy.call("setParameter", "Sensitivity", 100);
					asrProxy.call("subscribe", "naoASR");
					memoryProxy.call("insertData", "WordRecognized", "");
					asrProxy.call("pause", false); 
					log.debug("ASR successfully started!");
				} 
				catch (Exception e) {
					log.warning("ASR setup error: " + e);
				}

		}
		else {
			log.warning("recognition grammar must be provided");
			return false;
		}
		return true;
	}


	public void lockASR() {
		try {
			asrProxy.call("pause", true);
		} catch (CallError e) {
			e.printStackTrace();
		}
	}


	public void unlockASR() {
		try {
			asrProxy.call("pause", false);
			memoryProxy.call("insertData", "WordRecognized", "");
		} catch (CallError e) {
			e.printStackTrace();
		}
	}



}
