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

package opendial.bn.distribs.discrete;


import java.util.Arrays;
import java.util.Collection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.discrete.functions.DeterministicFunction;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;

public class DeterministicDistribution implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("valueDistributionWrapper",
			Logger.Level.DEBUG);
	
	String variable;
	DeterministicFunction function;
	
	
	public DeterministicDistribution (String variable, DeterministicFunction function) {
		this.variable = variable;
		this.function = function;
	}


	@Override
	public boolean isWellFormed() {
		return true;
	}


	@Override
	public ProbDistribution copy() {
		return new DeterministicDistribution(variable, function.copy());
	}


	@Override
	public String prettyPrint() {
		String str = function.prettyPrint();
		return "deterministic distribution for " + variable + " with function " + function.prettyPrint();
	}


	@Override
	public void modifyVarId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
		function.modifyVarId(oldId, newId);
	}


	@Override
	public Assignment sample(Assignment condition) throws DialException {
		return new Assignment(variable, function.getFunctionValue(condition));
	}


	@Override
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}


	@Override
	public ContinuousProbDistribution toContinuous() throws DialException {
		throw new DialException ("cannot convert to a continuous distribution");
	}


	@Override
	public Collection<String> getHeadVariables() {
		return Arrays.asList(variable);
	}


	@Override
	public double getProb(Assignment condition, Assignment head) {
		Value value = function.getFunctionValue(condition);
		if (head.containsVar(variable) && head.getValue(variable).equals(value)) {
			return 1.0;
		}
		return 0.0;
	}


	@Override
	public boolean hasProb(Assignment condition, Assignment head) {
		Value value = function.getFunctionValue(condition);
		if (head.containsVar(variable) && head.getValue(variable).equals(value)) {
			return true;
		}
		return false;
	}


	@Override
	public SimpleTable getProbTable(Assignment condition) throws DialException {
		Value value = function.getFunctionValue(condition);
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment(variable, value), 1.0);
		return table;
	}
	
	
	public int hashCode() {
		return variable.hashCode() - function.hashCode();
	}
	
	public String toString() {
		return prettyPrint();
	}

}

