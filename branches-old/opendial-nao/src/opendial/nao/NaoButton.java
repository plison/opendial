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
public class NaoButton implements Module, Runnable {

	static Logger log = new Logger("NaoButton", Logger.Level.DEBUG);

	NaoSession session;
	DialogueSystem system;


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

		(new Thread(this)).start();
	}


	public boolean isRunning() {
		return true;
	}


	public void run() {
		NaoASR asr = system.getModule(NaoASR.class);
		if (asr == null) {
			return;
		}

		while (true) {
			try {	
				Thread.sleep(200);  
				Object sensorVal = session.call("ALMemory", "getData", "MiddleTactilTouched");
				if (sensorVal instanceof Float && ((Float)sensorVal) > 0.99f) {
					if (asr.isRunning()) {
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
			catch (Exception e) { log.debug("exception : " + e); }

		}
	}

	public void trigger(DialogueState state, Collection<String> updatedVars) {		 }


	public void pause(boolean toPause) { }




}
