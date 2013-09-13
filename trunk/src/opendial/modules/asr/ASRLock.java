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


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.NaoASR;
import opendial.state.DialogueState;

public class ASRLock {

	// logger
	public static Logger log = new Logger("ASRLock", Logger.Level.DEBUG);

	static List<String> locks = new ArrayList<String>();
	
	static NaoASR asr;
	
	public static void connectASR(NaoASR asr2) {
		asr = asr2;
	}
	
	public synchronized static void addLock (String lockName) {
		locks.add(lockName);
		if (asr != null) asr.lockASR();
		log.debug("adding lock: " + lockName);
	}
	
	public static void removeLock(String lockName) {
		locks.remove(lockName);
		if (locks.isEmpty() && asr != null) {
			asr.unlockASR();
		}
		log.debug("releasing lock: " + lockName);
	}
	
	public static List<String> getLocks() {
		return new LinkedList<String>(locks);
	}
}

