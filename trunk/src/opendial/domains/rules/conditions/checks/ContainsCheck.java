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

package opendial.domains.rules.conditions.checks;


import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.SetVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.domains.datastructs.Template;

public class ContainsCheck extends AbstractCheck {

	// logger
	public static Logger log = new Logger("ContainsCheck", Logger.Level.DEBUG);

	String variable;
	Value expectedVal;
	
	boolean negation = false;
	
	public ContainsCheck(String variable, Value expectedVal) {
		this.variable = variable;
		this.expectedVal = expectedVal;
	}
	
	public void setNegation (boolean negation) {
		this.negation = negation;
	}
	
	@Override
	public boolean isSatisfied(Assignment input) {
		if (input.containsVar(variable)) {
			Value actualVal = input.getValue(variable);
			if (actualVal instanceof SetVal) {
				boolean contained = ((SetVal)actualVal).getSet().contains(expectedVal);
				return negation? !contained : contained;
			}
			else if (actualVal instanceof StringVal && expectedVal instanceof StringVal) {
				String actualValStr = actualVal.toString();
				Template expectedValStr = new Template(expectedVal.toString());
				boolean contained = expectedValStr.isMatching(actualValStr, true);
				return negation? !contained : contained;
			}
		}
		return negation;
	}
	
	
	@Override
	public Assignment getLocalOutput(Assignment input) {
		Value actualVal = input.getValue(variable);
		if (actualVal instanceof StringVal && expectedVal instanceof StringVal) {
			String actualValStr = actualVal.toString();
			String expectedValStr = expectedVal.toString();
			return (new Template(expectedValStr)).getMatchBoundaries(actualValStr, true);
		}
		return new Assignment();
	}

}

