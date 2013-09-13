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

package opendial.state.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.quantification.UnboundPredicate;

public class DistributionRule implements Rule {

	// logger
	public static Logger log = new Logger("DistributionRule", Logger.Level.DEBUG);

	ProbDistribution distrib;

	String ruleId;

	boolean clearPrevious = true;

	public DistributionRule(ProbDistribution distrib, String ruleId, boolean clearPrevious) {
		this.distrib = distrib;
		this.ruleId = ruleId;
		this.clearPrevious = clearPrevious;
	}

	@Override
	public Set<Template> getInputVariables() {
		return new HashSet<Template>();
	}


	public Set<UnboundPredicate> getUnboundPredicates() {
		return new HashSet<UnboundPredicate>();
	}


	@Override
	public OutputTable getEffectOutputs(Assignment input) {
		OutputTable outputs = new OutputTable();
		try {
			SimpleTable tableOutput = distrib.toDiscrete().getProbTable(input);
			for (Assignment a : tableOutput.getRows()) {
				Output o = new Output();
				for (String var : a.getVariables()) {
					if (!a.getValue(var).equals(ValueFactory.none())) {
						o.setValueForVariable(var, a.getValue(var));
					}
					else if (clearPrevious) {
						o.clearVariable(var);
					}
				}
				double param = tableOutput.getProb(a);
				outputs.addOutput(o, new FixedParameter(param));
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

	public String toString() {
		return ruleId + ": " + distrib.toString();
	}

	public boolean toClear() {
		return clearPrevious;
	}

}
