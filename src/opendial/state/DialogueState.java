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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.domains.Type;
import opendial.domains.values.Value;
import opendial.inference.bn.BNetwork;
import opendial.utils.Logger;

/**
 * Representation of a dialogue state, which comprises a list of 
 * <i>fluents</i>.  
 * 
 * <p>Process threads from the dialogue system can wait for a state change, 
 * and will be duly notified.
 * 
 * @see Fluent
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueState {

	// logger
	static Logger log = new Logger("DialogueState", Logger.Level.DEBUG);
	
	// the list of fluents, indexed by label
	SortedMap<String,Fluent> fluents;
	
	Prediction prediction;

	/**
	 * Creates a new dialogue state, with an empty list of fluents
	 */
	public DialogueState () {
		fluents = new TreeMap<String, Fluent>();
		prediction = new Prediction();
	}
	
	public Prediction getPrediction() {
		return prediction;
	}
	
	
	/**
	 * Returns the list of fluents associated with the current state
	 * 
	 * @return the list of fluents
	 */
	public List<Fluent> getFluents() {
		return new ArrayList<Fluent>(fluents.values());
	}
	
	
	/**
	 * Adds a new fluent to the dialogue state
	 * 
	 * @param fluent the new fluent
	 */
	public void addFluent(Fluent fluent) {
		if (fluent.getExistenceProb() > 0.0001f) {
		fluents.put(fluent.getLabel(), fluent);
		}
		else {
			fluents.remove(fluent.getLabel());
		}
	}
	

	
	
	/**
	 * Copy the dialogue state
	 * 
	 * @return a dialogue state copy
	 */
	public DialogueState copy() {
		DialogueState copy = new DialogueState();
		
		for (Fluent fl : fluents.values()) {
			copy.addFluent(fl.copy());
		}
		return copy;
	}
	
	
	/**
	 * Returns a string representation of the current state
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Fluent fl: fluents.values()) {
			str += fl.toString() + "\n";
		}
		return str;
	}


	/**
	 * 
	 * @param string
	 * @return
	 */
	public Fluent getFluent(String fluentId) {
		return fluents.get(fluentId);
	}

	/**
	 * 
	 * @param label
	 */
	public void removeFluent(String label) {
		fluents.remove(label);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public List<Fluent> getFluents(Type type) {
		List<Fluent> fluentsOfType = new LinkedList<Fluent>(); 
		for (String s: fluents.keySet()) {
			Fluent f = fluents.get(s);
			if (f.getType().equals(type)) {
				fluentsOfType.add(f);
			}
		}
		return fluentsOfType;
	}
	
}
