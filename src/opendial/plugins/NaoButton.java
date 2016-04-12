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
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSensors;

import java.util.Collection;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.datastructs.Assignment;
import opendial.modules.Module;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoButton implements Module {

	final static Logger log = Logger.getLogger("OpenDial");

	Session session;
	DialogueSystem system;
	boolean paused = true;
	long lastCallback = System.currentTimeMillis();

	public NaoButton(DialogueSystem system) {
		this.system = system;
		session = NaoUtils.grabSession(system.getSettings());
	}

	@Override
	public void start() {
		log.info("starting up Nao Buttons....");
		try {
		ALSensors sensors = new ALSensors(session);
		ALMemory memory = new ALMemory(session);
		sensors.subscribe("NaoButton");
		memory.subscribeToEvent("MiddleTactilTouched", f -> processEvent(f));
		}
		catch (Exception e) {
			log.warning("Cannot start NaoButton: " + e.toString());
		}
		paused = false;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
	}

	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	public void processEvent(Object event) {
		final NaoASR asr = system.getModule(NaoASR.class);
		if (asr == null || (System.currentTimeMillis() - lastCallback) < 1000) {
			return;
		}
		lastCallback = System.currentTimeMillis();

		final boolean isASRRunning = asr.isRunning();

		final Assignment feedback = (isASRRunning)
				? new Assignment(system.getSettings().systemOutput,
						"OK, I'll go to sleep then")
				: new Assignment(system.getSettings().systemOutput,
						"OK, I am now listening");

		asr.lockASR("NaoButton");
		(new Thread(() -> system.addContent(feedback))).start();
		if (!isASRRunning) {
			asr.unlockASR("NaoButton");
		}
		
	}
	


}
