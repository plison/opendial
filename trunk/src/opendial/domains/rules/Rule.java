// =================================================================                                                                   
// Copyright (C) 2009-2011 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.utils.Logger;

/**
 * Representation of a rule.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Rule {

	static Logger log = new Logger("Rule", Logger.Level.NORMAL);
	
	Map<String,Variable> inputs;
	
	Map<String,Variable> outputs;
	
	List<Case> cases;
	
	public Rule() {
		inputs = new HashMap<String,Variable>();
		outputs = new HashMap<String,Variable>();
		cases = new LinkedList<Case>();
	}
	
	public void addInput(Variable var) {
		inputs.put(var.getDenotation(), var);
	}
	
	public void addOutput(Variable var) {
		outputs.put(var.getDenotation(), var);
	}

	public void addCase(Case case1) {
		cases.add(case1);
	}
	
}


