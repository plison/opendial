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

package opendial.arch.statechange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;

public class DistributionRule implements Rule {

	// logger
	public static Logger log = new Logger("DistributionRule", Logger.Level.DEBUG);

	ProbDistribution distrib;
	
	String ruleId;
	
	public DistributionRule(ProbDistribution distrib, String ruleId) {
		this.distrib = distrib;
		this.ruleId = ruleId;
	}
	
	@Override
	public Set<TemplateString> getInputVariables() {
		return new HashSet<TemplateString>();
	}

	@Override
	public Map<Output, Parameter> getEffectOutputs(Assignment input) {
		Map<Output,Parameter> outputs = new HashMap<Output,Parameter>();
		try {
			SimpleTable tableOutput = distrib.toDiscrete().getProbTable(input);
			for (Assignment a : tableOutput.getRows()) {
				Output o = new Output();
				o.setValuesForVariables(a);
				outputs.put(o, new FixedParameter(tableOutput.getProb(a)));
			}
			
		}
		catch (DialException e) {
			log.warning("cannot construct outputs for distribution rule: " + e);
		}
		
		return outputs;
	}

	@Override
	public String getRuleId() {
		return ruleId;
	}

	@Override
	public RuleType getRuleType() {
		return RuleType.PROB;
	}

}
