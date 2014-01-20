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

import java.util.Collection;


import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
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

	public NaoTTS(DialogueSystem system) throws DialException {
		this.system = system;
		session = NaoSession.grabSession(system.getSettings());
	}
	
	public void start() {
		try {
			paused = false;
			say("OK, ready to start!");
		}
		catch (Exception e) { log.warning("unable to connect to Nao"); } 	
	}
	
	
	public void pause(boolean toPause) {
		paused = toPause;
	}
	
	public boolean isRunning() {
		return !paused;
	}

	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 

		if  (!paused && updatedVars.contains(system.getSettings().systemOutput) 
				&& state.hasChanceNode(system.getSettings().systemOutput)) {

			CategoricalTable actionTable = state.queryProb(system.getSettings().systemOutput).toDiscrete();
			Value value = actionTable.getBest().getValue(system.getSettings().systemOutput);
			
		if (!value.equals(ValueFactory.none())) {
			say(value.toString());
		}
		}
	}
	
	

	private void say(String utterance) {

		NaoASR asr = system.getModule(NaoASR.class);
		if (asr != null) asr.pause(true);

		try {
			log.debug("saying utterance: " + utterance);
			session.call("ALTextToSpeech", "say", utterance);
		}
		catch (Exception e) {
			log.warning("cannot use TTS: " + e.toString());
		}
		if (asr != null) asr.pause(false);
	}

	

}
