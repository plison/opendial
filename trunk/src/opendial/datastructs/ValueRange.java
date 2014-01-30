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

package opendial.datastructs;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.utils.CombinatoricsUtils;

public class ValueRange {

	// logger
	public static Logger log = new Logger("ValueRange", Logger.Level.NORMAL);
	
	Map<String,Set<Value>> range;
	
	public ValueRange() {
		range = new HashMap<String,Set<Value>>();
	}
	
	public ValueRange(ValueRange range1, ValueRange range2) {
		this();
		addRange(range1);
		addRange(range2);
	}
	
	public ValueRange(Map<String,Set<Value>> range) {
		this.range = new HashMap<String,Set<Value>>(range);
	}
	
	public void addValue(String variable, Value val) {
		if (!range.containsKey(variable)) {
			range.put(variable, new HashSet<Value>());
		}
		range.get(variable).add(val);
	}

	
	public Set<Assignment> linearise() {
		return CombinatoricsUtils.getAllCombinations(range);
	}

	public void addValues(String id, Set<Value> values) {
		for (Value val : values) {
			addValue(id, val);
		}
	}

	public Set<String> getVariables() {
		return range.keySet();
	}
	
	public Set<Value> getValues(String variable) {
		return range.get(variable);
	}

	public void addAssign(Assignment assignment) {
		for (String var : assignment.getVariables()) {
			addValue(var, assignment.getValue(var));
		}
	}

	public void addRange(ValueRange newRange) {
		for (String var : newRange.getVariables()) {
			addValues(var, newRange.getValues(var));
		}
	}
	
	@Override
	public String toString() {
		return range.toString();
	}
	
	@Override
	public int hashCode() {
		return range.hashCode() - 1;
	}

	public boolean isEmpty() {
		return range.isEmpty();
	}

	public void intersectRange(ValueRange groundings) {
		for (String id : groundings.getVariables()) {
			if (range.containsKey(id)) {
				range.get(id).retainAll(groundings.getValues(id));
			}
			else {
				addValues(id, groundings.getValues(id));
			}
		}
	}
}

