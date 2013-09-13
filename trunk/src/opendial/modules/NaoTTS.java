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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.domains.rules.UpdateRule;
import opendial.modules.asr.ASRLock;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoTTS implements SynchronousModule {

	static Logger log = new Logger("NaoTTS", Logger.Level.DEBUG);
	
	public NaoTTS() {
		try {
			say("OK, ready to start!");
		}
		catch (Exception e) { log.warning("unable to connect to Nao"); } 	
	}
	
	/**
	 *
	 * @param varnode
	 * @param change
	 * @return
	 */
	@Override
	public boolean isTriggered(DialogueState state) {
		if  (state.isVariableToProcess("u_m'") && 
				state.getNetwork().hasChanceNode("u_m'")) {
			return true;
		}
		return false;
	}


	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state) {

		String systemUtterance = getUtteranceValue(state);
		if (!systemUtterance.equals("")) {
			ASRLock.addLock("tts");
			say(systemUtterance);
			ASRLock.removeLock("tts");
		}
	}
	
	

	private String getUtteranceValue(DialogueState state) {
		try {
	//		 {
		SimpleTable actionTable = state.getContent("u_m'", true).toDiscrete().getProbTable(new Assignment());
		String value = actionTable.getRows().iterator().next().getValue("u_m'").toString();
		return (value.equals("None"))? "" : value;
		}
		catch (DialException e) {
			log.warning("problem extracting the action value: " + e);
			return "";
		}
	}


	private void say(String utterance) {

		try {
			log.debug("saying utterance: " + utterance);

			NaoSession sess = NaoSession.grabSession();
			com.aldebaran.qimessaging.Object tts = sess.getService("ALTextToSpeech");
			com.aldebaran.qimessaging.Object memory = sess.getService("ALMemory");
			tts.call("say", utterance);

			boolean synthesisStarted = false;
			int nbLoopsStart = 0;
			while (!synthesisStarted && nbLoopsStart <20) {
				int textDone = memory.<Integer>call("getData", "ALTextToSpeech/TextDone").get();
				if (textDone != 1) {
					synthesisStarted = true;
				}
				else {
					try { Thread.currentThread().sleep(50); nbLoopsStart++ ; } catch (InterruptedException e) { }
				} 
			}
			if (nbLoopsStart == 20) {
				log.warning("Problem starting up the TTS engine!");
			}
			
			long initTime = System.currentTimeMillis();
			boolean synthesisEnded = false;
			int nbLoops = 0;
			while (!synthesisEnded) {
				int textDone = memory.<Integer>call("getData", "ALTextToSpeech/TextDone").get();
				if (textDone == 1) {
					synthesisEnded = true;
				}
				else {
					try { Thread.currentThread().sleep(100); } catch (InterruptedException e) { }
					nbLoops++;
		//			log.debug("not yet done with the text!");
				} 
			}
			log.debug("synthesis time: " + (System.currentTimeMillis()-initTime) + " ms (" + nbLoops + " loops)");
		}
		catch (Exception e) {
			log.warning("cannot use TTS: " + e.toString());
		}
	}

	
	
	@Override
	public void shutdown() {
			
	}



}
