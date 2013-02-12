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
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.BasicCondition.Relation;

public class TemplateCheck extends AbstractCheck {

	// logger
	public static Logger log = new Logger("TemplateCheck", Logger.Level.DEBUG);

	String variable;
	Template expectedVal;

	Relation relation;

	public TemplateCheck(String variable, Template expectedVal, Relation relation) {
		this.variable = variable;
		this.expectedVal = expectedVal;
		this.relation = relation;
	}



	@Override
	public boolean isSatisfied(Assignment input) {
		if (input.containsVar(variable)) {
			Value actualVal = input.getValue(variable);

			Template partialFill = expectedVal.fillSlotsPartial(input);
			if (!partialFill.containsTemplate()) {
				AbstractCheck check = CheckFactory.createCheck(variable, 
						ValueFactory.create(partialFill.toString()), relation);
				return check.isSatisfied(input);
			}
			else if (actualVal instanceof StringVal) {
				String actualValStr = ((StringVal)actualVal).getString();

				if (relation == Relation.EQUAL && !expectedVal.isSingleSlot()) {
					return partialFill.isMatching(actualValStr, false);
				}
				else if (relation == Relation.CONTAINS) {
					return partialFill.isMatching(actualValStr, true);
				}
				else if (relation == Relation.UNEQUAL) {
					return !partialFill.isMatching(actualValStr, false);
				}
				else if (relation == Relation.NOT_CONTAINS) {
					return !partialFill.isMatching(actualValStr, true);
				}
				else {
					return false;
				}
			}
		}

		if (relation == Relation.UNEQUAL) {
			return true;
		}
		return false;
	}



	@Override
	public Assignment getLocalOutput(Assignment input) {

		if (input.containsVar(variable)) {
			Value actualVal = input.getValue(variable);

			Template partialFill = expectedVal.fillSlotsPartial(input);
			if (!partialFill.getSlots().isEmpty() && actualVal instanceof StringVal) {
				String actualValStr = ((StringVal)actualVal).getString();

				if (relation == Relation.EQUAL) {
					return partialFill.extractParameters(actualValStr, false);
				}
				else if (relation == Relation.CONTAINS) {
					Assignment params = partialFill.extractParameters(actualValStr, true);
					Assignment boundaries = partialFill.getMatchBoundaries(actualValStr, true);
					return new Assignment(params, boundaries);
				}
			}
		}
		return new Assignment();
	}
	
	
	public String toString() {
			switch (relation) {
			case EQUAL: return variable + "=" + expectedVal ; 
			case UNEQUAL: return variable + "!=" + expectedVal ; 
			case GREATER_THAN: return variable + ">" + expectedVal; 
			case LOWER_THAN : return variable + "<" + expectedVal; 
			case CONTAINS: return expectedVal + " in " + variable; 
			case NOT_CONTAINS: return expectedVal + " !in " + variable;
			default: return ""; 
			}
	}

}

