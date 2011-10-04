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

package opendial.domains.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.domains.actions.Action;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ActionType extends StandardType {

	static Logger log = new Logger("ActionType", Logger.Level.NORMAL);

	
	// list of values for the entity
	Map<String,Action> values;

		
	/**
	 * @param name
	 */
	public ActionType(String name) {
		super(name);
		values = new HashMap<String,Action>();
	}
	
	public void addActionValue(Action action) {
		values.put(action.getLabel(), action);
	}
	
	
	public Action getActionValue(String label) {
		return values.get(label);
	}
	
	
	public List<Action> getActionValues() {
		return new ArrayList<Action>(values.values());
	}

	/**
	 * 
	 * @param values
	 */
	public void addActionValues(List<Action> values) {
		for (Action value: values) {
			addActionValue(value);
		}
	}

}
