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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.values.Value;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.ValueFactory;
import opendial.utils.StringUtils;

public class EqualityDistribution implements DiscreteProbDistribution {

	// logger
	public static Logger log = new Logger("EqualityDistribution", Logger.Level.DEBUG);

	String equalityId;

	String variable;

	Random sampler;

	public static final double PROB_WITH_SINGLE_NONE = 0.03;
	public static final double PROB_WITH_DOUBLE_NONE = 0.03;
 

	public EqualityDistribution(String equalityId, String variable) {
		this.variable = variable;
		this.equalityId = equalityId;
		sampler = new Random();
	}


	@Override
	public boolean isWellFormed() {
		return true;
	}

	@Override
	public EqualityDistribution copy() {
		return new EqualityDistribution(equalityId, variable);
	}

	@Override
	public String prettyPrint() {
		return "==";
	}

	@Override
	public void modifyVarId(String oldId, String newId) {
		if (variable.equals(oldId)) {
			variable = newId;
		}
		if (equalityId.equals(oldId)) {
			equalityId = newId;
		}
	}

	@Override
	public Assignment sample(Assignment condition) throws DialException {
		double prob = getProb(condition);
		if (prob == 0) {
			return new Assignment(equalityId, false);
		}
		else if (prob ==1) {
			return new Assignment(equalityId, true);
		}
		else {
			if (sampler.nextDouble() < prob) {
				return new Assignment(equalityId, true);
			}
			return new Assignment(equalityId, false);
		}
	}
	
	
	/**
	 * Returns the identifier for the equality
	 * 
	 * @return a singleton set with the equality identifier
	 */
	@Override
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>(Arrays.asList(equalityId));
		return headVars;
	}
	




	@Override
	public DiscreteProbDistribution toDiscrete() {
		return this;
	}

	@Override
	public ContinuousProbDistribution toContinuous() throws DialException {
		throw new DialException("cannot convert equality distribution");
	}

	@Override
	public double getProb(Assignment condition, Assignment head) {
		try {
			double prob = getProb(condition);
			if (head.containsVar(equalityId) && 
					head.getValue(equalityId) instanceof BooleanVal) {
				boolean val = ((BooleanVal) head.getValue(equalityId)).getBoolean();
				if (val) { 
					return prob;
				}
				else {
					return 1 - prob;
				}
			}
			log.warning("cannot extract prob for P(" + head + "|" + condition +")");
		} 
		catch (DialException e) { log.warning(e.toString()); } 
		return 0.0;
	}

	
	@Override
	public boolean hasProb(Assignment condition, Assignment head) {
		try {
			double prob = getProb(condition);
			return (head.containsVar(equalityId) && 
					head.getValue(equalityId) instanceof BooleanVal);
		}
		catch (DialException e) {
			return false;
		}
	}

	
	@Override
	public SimpleTable getProbTable(Assignment condition) throws DialException {
		double prob = getProb(condition);
		SimpleTable table = new SimpleTable();
		if (prob > 0) {
			table.addRow(new Assignment(equalityId, true), prob);
		}
		if (prob < 1) {
			table.addRow(new Assignment(equalityId, false), 1 - prob);
		}
		return table;
	}



	private double getProb(Assignment condition) throws DialException {
		
		String actualVar = null;
		for (int i = 3 ; i >= 0 && actualVar==null; i--) {
			if (condition.containsVar(variable+StringUtils.createNbPrimes(i))) {
				actualVar = variable+StringUtils.createNbPrimes(i);
			}
		}
		
		Assignment trimmed = condition.getTrimmed(variable+"^p",actualVar);
		if (trimmed.size() == 2) {	
			List<Value> valList = new ArrayList<Value>(trimmed.getValues());

			// if the none value is present, the equality is random
			if (valList.get(0).equals(ValueFactory.none()) && 
					valList.get(1).equals(ValueFactory.none())) {	
				return PROB_WITH_DOUBLE_NONE;
			}
			else if (valList.get(0).equals(ValueFactory.none()) || 
					valList.get(1).equals(ValueFactory.none())) {	
				return PROB_WITH_SINGLE_NONE;
			}
			else {
				if (valList.get(0).equals(valList.get(1))) {
					return 1.0;
				}
				else {
					return 0.0;
				}
			}			
		}
		else {
			throw new DialException("equality distribution (with variable " + variable + 
					") cannot process condition " + trimmed);
		}
	}
	
	
}
