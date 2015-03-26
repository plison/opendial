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
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;



/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoButton implements Module, NaoEventListener {

	static Logger log = new Logger("NaoButton", Logger.Level.DEBUG);

	NaoSession session;
	DialogueSystem system;
	boolean paused = true;
	long lastCallback = System.currentTimeMillis();

	public NaoButton(DialogueSystem system) throws DialException {
		this.system = system;
		session = NaoSession.grabSession(system.getSettings());
	}

	/**
	 * @throws DialException 
	 *
	 */
	@Override
	public void start() throws DialException {
		log.info("starting up Nao Buttons....");

		session.call("ALSensors", "subscribe", "NaoButton");

		session.listenToEvent("MiddleTactilTouched", this);
		paused = false;
	}


	@Override
	public boolean isRunning() {
		return !paused;
	}



	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 }


	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}


	@Override
	public void callback (Object event) {
		final NaoASR asr = system.getModule(NaoASR.class);
		if (asr == null || (System.currentTimeMillis() - lastCallback) < 1000 ) {
			return;
		}
		lastCallback = System.currentTimeMillis();

		final boolean isASRRunning = asr.isRunning();

		final Assignment feedback = (isASRRunning)? 
				new Assignment(system.getSettings().systemOutput, "OK, I'll go to sleep then") :
					new Assignment(system.getSettings().systemOutput, "OK, I am now listening");

		asr.lockASR("NaoButton");
		Runnable runnable = () -> {
				try {
					system.addContent(feedback);
				}
				catch (DialException e) {
					log.warning("could not add content: " + e);
				}
		};
		(new Thread(runnable)).start();	
		if (!isASRRunning) {
			asr.unlockASR("NaoButton");
		}			

	}


}
