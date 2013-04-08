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

package opendial.bn.distribs.continuous;


import java.util.Arrays;
import java.util.Collection;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.discrete.DeterministicDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.functions.DeterministicFunction;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;

public class FuzzyDistribution implements ContinuousProbDistribution {

	String variable;
	DeterministicFunction function;
	double variance;

	public FuzzyDistribution(String variable, DeterministicFunction function, double variance) {
		this.variable = variable;
		this.function = function;
		this.variance = variance;
	}
	
	// logger
	public static Logger log = new Logger("FuzzyDistribution",
			Logger.Level.NORMAL);

	@Override
	public boolean isWellFormed() {
		return true;
	}

	@Override
	public ProbDistribution copy() {
		return new FuzzyDistribution(variable, function, variance);
	}

	@Override
	public String prettyPrint() {
		return "fuzzy distribution on " + variable + " with function " + function + " and variance " + variance;
	}
	
	public int hashCode() {
		return function.hashCode() - variable.hashCode() + (new Double(variance)).hashCode();
	}

	@Override
	public void modifyVarId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
	}

	@Override
	public Assignment sample(Assignment condition) throws DialException {
		Value value = function.getFunctionValue(condition);
		if (value instanceof DoubleVal) {
			double dval = ((DoubleVal)value).getDouble();
			double fuzzyval = new GaussianDensityFunction(dval, variance).sample();
			return new Assignment(variable, fuzzyval);
		}
		log.warning("function value is not a double: " + value);
		return new Assignment(variable, value);
	}
	
	
	public Assignment getRealValue(Assignment condition) {
		return new Assignment(variable, function.getFunctionValue(condition));
	}

	@Override
	public DiscreteProbDistribution toDiscrete() {
		return new DeterministicDistribution(variable, function);
	}

	@Override
	public ContinuousProbDistribution toContinuous() throws DialException {
		return this;
	}

	@Override
	public Collection<String> getHeadVariables() {
		return Arrays.asList(variable);
	}

	@Override
	public double getProbDensity(Assignment condition, Assignment head) {
		Value value = function.getFunctionValue(condition);
		if (head.containsVar(variable) && head.getValue(variable) instanceof DoubleVal && value instanceof DoubleVal) {
			double dval = ((DoubleVal)value).getDouble();
			double headval = ((DoubleVal)head.getValue(variable)).getDouble();
			return new GaussianDensityFunction(dval, variance).getDensity(headval);
		}
		log.warning("function value is not a double: " + value + " or head assignment inconsistent: " + head);
		return 0.0;
	}

	@Override
	public double getCumulativeProb(Assignment condition, Assignment head) {
		Value value = function.getFunctionValue(condition);
		if (head.containsVar(variable) && head.getValue(variable) instanceof DoubleVal && value instanceof DoubleVal) {
			double dval = ((DoubleVal)value).getDouble();
			double headval = ((DoubleVal)head.getValue(variable)).getDouble();
			return new GaussianDensityFunction(dval, variance).getCDF(headval);
		}
		log.warning("function value is not a double: " + value + " or head assignment inconsistent: " + head);
		return 0.0;
	}

	
	@Override
	public int getDimensionality() {
		return 1;
	}

}

