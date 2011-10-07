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

package opendial.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.utils.Logger;

/**
 * Implement the dialogue state
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueState {

	static Logger log = new Logger("DialogueState", Logger.Level.DEBUG);
	
		
	Map<String,Fluent> fluents;
	
	
	public DialogueState () {
		fluents = new HashMap<String, Fluent>();
	}
	/**
	 * 
	 * @return
	 */
	public List<Fluent> getFluents() {
		return new ArrayList<Fluent>(fluents.values());
	}
	/**
	 * 
	 * @param entity
	 */
	public void addFluent(Fluent fluent) {
		fluents.put(fluent.getLabel(), fluent);
	}
	
	public synchronized void dummyChange() {
		notifyAll();
	}
	
	
	
	
	public DialogueState copy() {
		DialogueState copy = new DialogueState();
		
		for (Fluent fl : fluents.values()) {
			copy.addFluent(fl.copy());
		}
		return copy;
	}
	
	
	@Override
	public String toString() {
		String str = "";
		for (Fluent fl: fluents.values()) {
			str += fl.toString() + "\n";
		}
		return str;
	}
	
}
