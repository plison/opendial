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

import opendial.DialogueSystem;
import opendial.arch.AnytimeProcess;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.utils.TimingUtils;



/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoButton extends Thread implements Module {

	static Logger log = new Logger("NaoASR", Logger.Level.DEBUG);

	NaoSession session;
	DialogueSystem system;


	/**
	 * @throws DialException 
	 *
	 */
	@Override
	public void start(DialogueSystem system) throws DialException {

		this.system = system;
		log.info("starting up Nao Buttons....");

		session = NaoSession.grabSession(system.getSettings());
		session.call("ALSensors", "subscribe", "NaoButton");

		start();
	}

	
	@Override
	public void run() {
		while (true) {
			try {	
				Thread.sleep(80);  
				ButtonLoop thread = new ButtonLoop();
				thread.start();
				TimingUtils.setTimeout(thread, NaoSession.MAX_RESPONSE_DELAY);

				while (thread.isAlive()) {
					thread.wait();
				}
			}
			catch (Exception e) { log.debug("exception : " + e); }

		}
	}


	public void pause(boolean toPause) { }

	public void trigger() {		 }


	public class ButtonLoop extends Thread implements AnytimeProcess {

		public void run() {
			NaoASR asr = system.getModule(NaoASR.class);
			if (asr == null) {
				return;
			}
			try {
				Object sensorVal = session.call("ALMemory", "getData", "MiddleTactilTouched");
				if (sensorVal instanceof Float && ((Float)sensorVal) > 0.99f) {
					if (!asr.paused ) {
						asr.pause(true);
						Assignment feedback = new Assignment(system.getSettings().systemOutput, "OK, I'll go to sleep then");
						system.addContent(feedback);
					}
					else {
						asr.pause(false);
						Assignment feedback = new Assignment(system.getSettings().systemOutput, "OK, I am now listening");
						system.addContent(feedback);
					}
					session.call("ALMemory", "insertData", "MiddleTactilTouched", 0.0f); 
				}
			}
			catch (Exception e) {
				log.warning("cannot run recognition: " + e);
			}
			notifyAll();
		}



		@Override
		public void terminate() {
			log.debug("terminating button loop");
			interrupt();
			notifyAll();
		}



		@Override
		public boolean isTerminated() {
			return !isAlive();
		}


	}




}
