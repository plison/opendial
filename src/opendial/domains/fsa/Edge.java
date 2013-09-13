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

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;

public class Edge {

	// logger
	public static Logger log = new Logger("Edge", Logger.Level.NORMAL);

	String source;

	String target;

	String conditionPtr = "";
	Condition condition = new VoidCondition();

	double threshold = 1.0;

	int priority = 1;


	public Edge (String source, String target) {
		this.source = source;
		this.target = target;
	}
	public void setConditionPointer(String conditionPtr) {
		this.conditionPtr = conditionPtr;
	}
	
	public String getConditionPtr() {
		return conditionPtr;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public void setPriority (int priority) {
		this.priority = priority;
	}

	public boolean matches (Assignment input) {
		return (condition.isSatisfiedBy(input));
	}
	
	public double getThreshold() {
		return threshold;
	}

	public int getPriority() {
		return priority;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public int hashCode() {
		return source.hashCode() - target.hashCode() + condition.hashCode();
	}

	public String toString() {
		String s = source +  " -> " + target;
		if (!(condition instanceof VoidCondition)) {
			s += " if " + condition;
			if (threshold < 1.0) {
				s += " [threshold=" + threshold+"]";
			}
		}
		if (priority != 1) {
			s += " [priority=" + priority+"]";
		}
		return s;
	}

	public Set<String> getConditionVariables() {
		Set<String> conditionVars = new HashSet<String>();
		for (Template t: condition.getInputVariables()) {
			if (!t.containsTemplate()) {
				conditionVars.add(t.getRawString());
			}
			else {
				log.warning("edge does not support templated variables: "  + t);
			}
		}
		return conditionVars;
	}
	public boolean isVoid() {
		return (condition instanceof VoidCondition);
	}
	
	
	public Assignment getLocalOutput(Assignment input) {
		return condition.getLocalOutput(input);
	}
}

