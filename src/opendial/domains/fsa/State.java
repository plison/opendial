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

package opendial.domains.fsa;


import java.util.HashSet;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Template;

public class State {

	// logger
	public static Logger log = new Logger("FiniteState", Logger.Level.NORMAL);

	String id;
	
	Template action;
	
	public State(String id) {
		this.id = id;
	}
	
	public State (String id, Template action) {
		this.id = id;
		this.action = action;
	}
	
	public boolean isEmpty() {
		return action == null;
	}
	
	public String getAction(Assignment fillers) {
		try {
			return action.fillSlots(fillers);
		}
		catch (DialException e) {
			log.warning("could not fill all slots: " + action + " with " + fillers);
			return action.toString();
		}
	}
	
	public String getId() {
		return id;
	}
	
	public int hashCode() {
		return id.hashCode() - action.hashCode();
	}
	
	public String toString() {
		String s = id;
		if (action !=null) {
			s += ":" + action.toString();
		}
		return s;
	}

	public Set<String> getActionSlots() {
		Set<String> slots = new HashSet<String>();
		if (action != null) {
			slots.addAll(action.getSlots());
		}
		return slots;
	}

	public void setAction(String actionStr) {
		action = new Template(actionStr);
	}
}

