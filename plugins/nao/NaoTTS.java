// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.plugins;

import java.util.Collection;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
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

	@Override
	public void start() {
		try {
			paused = false;
			say("OK, ready to start!");
		}
		catch (Exception e) { log.warning("unable to connect to Nao"); } 	
	}


	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}

	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 

		String output = system.getSettings().systemOutput;
		if  (!paused && updatedVars.contains(output) && state.hasChanceNode(output)) {
			Value value = state.queryProb(output).toDiscrete().getBest();		
			if (!value.equals(ValueFactory.none())) {
				say(value.toString());
			}
		}
	}



	private void say(String utterance) {

		NaoASR asr = system.getModule(NaoASR.class);
		if (asr != null) asr.lockASR("NaoTTS");

		try {
			log.debug("saying utterance: " + utterance);
			session.call("ALTextToSpeech", "say", utterance);
		}
		catch (Exception e) {
			log.warning("cannot use TTS: " + e.toString());
		}
		if (asr != null) asr.unlockASR("NaoTTS");
	}



}
