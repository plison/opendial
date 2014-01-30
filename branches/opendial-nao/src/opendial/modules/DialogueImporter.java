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

package opendial.modules;


import java.util.List;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.state.DialogueState;
 
public class DialogueImporter extends Thread {

	// logger
	public static Logger log = new Logger("DialogueImporter", Logger.Level.DEBUG);

	DialogueSystem system;
	List<DialogueState> dialogue;

	public DialogueImporter(DialogueSystem system, List<DialogueState> dialogue) {
		this.system = system;
		this.dialogue = dialogue;
	}
	
	@Override
	public void run() {
		system.attachModule(WizardLearner.class);
		for (final DialogueState state : dialogue) {
			try {
				while (system.isPaused() || !system.getModule(DialogueRecorder.class).isRunning()) {
					try { Thread.sleep(100); } catch (Exception e) { }
				}
				system.addContent(state.copy()); 
			} 
			catch (DialException e) {	
				log.warning("could not add content: " + e);	
			}
		}
		system.detachModule(WizardLearner.class);
	}


}

