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

package opendial.domains.datastructs;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.domains.rules.parameters.DirichletParameter;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;

public class OutputTable {

	// logger
	public static Logger log = new Logger("OutputTable", Logger.Level.DEBUG);
	
	Map<Output, Parameter> outputs;
	
	
	public OutputTable() {
		outputs = new HashMap<Output,Parameter>();
	}
	
	public void addOutput(Output output, Parameter param) {
		outputs.put(output, param);
	}
	
	
	public Set<Output> getOutputs() {
		return outputs.keySet();
	}
	

	public Map<Output,Double> getProbTable(Assignment input) throws DialException {
		Map<Output,Double> table = new HashMap<Output,Double>();
		double totalMass = getTotalMass(input);
		if (totalMass > 1.02) {
			for (Output o : outputs.keySet()) {
				double paramValue = outputs.get(o).getParameterValue(input);
				if (paramValue > 0) {
					table.put(o, paramValue / totalMass);
				}
			}
		}
		else {
			for (Output o : outputs.keySet()) {
				double paramValue = outputs.get(o).getParameterValue(input);
				if (paramValue > 0) {
					table.put(o, paramValue);
				}
			}
			if (totalMass < 0.98) {
				table.put(new Output(), 1 - totalMass);
			}
		} 
		return table;
	}
	
	

	public Map<Output,Double> getRawTable(Assignment input) throws DialException {
		Map<Output,Double> table = new HashMap<Output,Double>();

		for (Output o : outputs.keySet()) {
			table.put(o, outputs.get(o).getParameterValue(input));
		}
				
		return table;
	}
	
	
	
	private double getTotalMass(Assignment input) throws DialException {
		double mass = 0;
		for (Parameter param : outputs.values()) {
			double paramValue = param.getParameterValue(input);
			if (paramValue > 0) {
				mass += param.getParameterValue(input);
			}
		}
		return mass;
	}

	
	public void setAsPrediction() {
		Map<Output,Parameter> newOutputs = new HashMap<Output,Parameter>();
		for (Output o : outputs.keySet()) {
			Output o2 = o.copy();
			o2.addEndingToVariables("^p");
			newOutputs.put(o2, outputs.get(o));
		}
		outputs = newOutputs;
	}


	public Parameter getParameter(Output o) {
		return outputs.get(o);
	}

	public Map<Output, Parameter> getParameterTable() {
		return outputs;
	}

	public int size() {
		return outputs.size();
	}
	
	public boolean isEmpty() {
		return outputs.isEmpty();
	}

	public double getFixedMass() {
		double totalFixedMass = 0.0;
		for (Parameter param : outputs.values()) {
			if (param instanceof FixedParameter) {
				totalFixedMass += ((FixedParameter)param).getParameterValue();
			}
		}
		return totalFixedMass;
	}
	
	
	public String toString() {
		String s = "";
		for (Output o : outputs.keySet()) {
			s += "T(" + o +"):=" + outputs.get(o) + "\n";
		}
		if (s.length() > 0) {
		return s.substring(0, s.length() - 1);
		}
		return s;
	}

}

