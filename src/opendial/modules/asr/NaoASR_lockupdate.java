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

package opendial.modules.asr;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aldebaran.qimessaging.CallError;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.modules.asr.ASRLock;
import opendial.state.DialogueState;

public class NaoASR_lockupdate extends Thread {

	// logger
	public static Logger log = new Logger("NaoASR_lockupdate", Logger.Level.DEBUG);

	com.aldebaran.qimessaging.Object memoryProxy;
	com.aldebaran.qimessaging.Object asrProxy;
	DialogueState state;
	List<String> initLocks;
	List<String> newLocks;
	
	public NaoASR_lockupdate (com.aldebaran.qimessaging.Object memoryProxy, com.aldebaran.qimessaging.Object asrProxy, 
			DialogueState state, List<String> initLocks) {
		this.memoryProxy = memoryProxy;
		this.state = state;
		this.asrProxy = asrProxy;
		this.initLocks = initLocks;
		newLocks = initLocks;
	}


	public void run() {
		try {
		newLocks = ASRLock.getLocks();
		if (memoryProxy != null && !newLocks.equals(initLocks)) {	
			if (!newLocks.isEmpty()) {
				memoryProxy.call("insertData", "WordRecognized", "");
			//	if (asrProxy.getSubscribersInfo().getSize() > 0) {
					try { asrProxy.call("pause", true); } catch (RuntimeException e) { }
			//	} 
				log.debug("putting a lock - " + newLocks);
			}
			else {
				memoryProxy.call("insertData", "WordRecognized", "");
			//	if (asrProxy.getSubscribersInfo().getSize() == 0) {
				try { asrProxy.call("pause", false);  } catch (RuntimeException e) { }
			//	} 
				log.debug("ASR is unlocked");
			}	
		}
		}
		catch (CallError e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getLocks() {
		return newLocks;
	}


}

