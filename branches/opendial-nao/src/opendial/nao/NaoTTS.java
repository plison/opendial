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

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aldebaran.qimessaging.Application;
import com.aldebaran.qimessaging.Future;
import com.aldebaran.qimessaging.Object;
import com.aldebaran.qimessaging.Session;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoTTS implements Module {

	static Logger log = new Logger("NaoTTS", Logger.Level.DEBUG);
	
	DialogueSystem system;
	NaoSession session;
	boolean paused = true;

	
	public void start(DialogueSystem system) {
		this.system = system;
		try {
			session = NaoSession.grabSession(system.getSettings());
			paused = false;
			say("OK, ready to start!");
		}
		catch (Exception e) { log.warning("unable to connect to Nao"); } 	
	}
	
	
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 

		String output = system.getSettings().systemOutput + "'";
		if  (!paused && system.getState().getChanceNodeIds().contains(output)) {

			CategoricalTable actionTable = system.getContent(output).toDiscrete();
			Value value = actionTable.getBest().getValue(output);
			
		if (!value.equals(ValueFactory.none())) {
			NaoASR asr = system.getModule(NaoASR.class);
			if (asr != null) asr.pause(true);
			say(value.toString());
			if (asr != null) asr.pause(false);

		}
		}
	}
	
	

	private void say(String utterance) {

		try {
			log.debug("saying utterance: " + utterance);

			session.call("ALTextToSpeech", "say", utterance);

			boolean synthesisStarted = false;
			int nbLoopsStart = 0;
			while (!synthesisStarted && nbLoopsStart <20) {
				int textDone = session.<Integer>call("ALTextToSpeech",  "getData", "ALTextToSpeech/TextDone");
				if (textDone != 1) {
					synthesisStarted = true;
				}
				else {
					try { 
						Thread.sleep(50); 
						nbLoopsStart++ ; 
						} 
					catch (InterruptedException e) { }
				} 
			}
			if (nbLoopsStart == 20) {
				log.warning("Problem starting up the TTS engine!");
			}
			
			boolean synthesisEnded = false;
			while (!synthesisEnded) {
				int textDone = session.<Integer>call("ALTextToSpeech", "getData", "ALTextToSpeech/TextDone");
				if (textDone == 1) {
					synthesisEnded = true;
				}
				else {
					try { 
						Thread.currentThread().sleep(50); 
						nbLoopsStart++ ; 
						} 
					catch (InterruptedException e) { }
				} 
			}
		}
		catch (Exception e) {
			log.warning("cannot use TTS: " + e.toString());
		}
	}

	

}
