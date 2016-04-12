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

import java.util.logging.*;

import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;

import java.util.Collection;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.Module;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoTTS implements Module {

	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;
	ALTextToSpeech tts;
	boolean paused = true;

	public NaoTTS(DialogueSystem system) throws Exception {
		this.system = system;
		Session session = NaoUtils.grabSession(system.getSettings());
		tts = new ALTextToSpeech(session);
	}

	@Override
	public void start() {
		try {
			paused = false;
			say("OK, ready to start!");
		}
		catch (Exception e) {
			log.warning("unable to connect to Nao");
		}
	}

	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		String output = system.getSettings().systemOutput;
		if (!paused && updatedVars.contains(output) && state.hasChanceNode(output)) {
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
			log.fine("saying utterance: " + utterance);
			tts.say(utterance);
		}
		catch (Exception e) {
			log.warning("cannot use TTS: " + e.toString());
		}
		if (asr != null) asr.unlockASR("NaoTTS");
	}

}
