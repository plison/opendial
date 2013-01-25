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
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.BasicCondition.Relation;

public class CheckFactory {

	// logger
	public static Logger log = new Logger("CheckFactory", Logger.Level.NORMAL);

	public static AbstractCheck createCheck(Template templateVariable, 
			Template expectedVal, Relation rel) {	
		
	
		if (!templateVariable.getSlots().isEmpty()) {
			return new TemplateVarCheck(templateVariable, expectedVal, rel);
		}
		
		return createCheck(templateVariable.getRawString(), expectedVal, rel);
		
	}
	
	public static AbstractCheck createCheck(String variable, Template expectedVal, Relation rel) {
		
		if (expectedVal.containsTemplate()) {
			return new TemplateCheck(variable, expectedVal, rel);
		}
		
		Value val = ValueFactory.create(expectedVal.getRawString());

		return createCheck(variable, val, rel);
	}

	
	public static AbstractCheck createCheck(String variable, Value expectedVal, Relation rel) {

		if (rel == Relation.CONTAINS) {
			return new ContainsCheck(variable, expectedVal);
		}
		else if (rel == Relation.NOT_CONTAINS) {
			ContainsCheck check = new ContainsCheck(variable, expectedVal);
			check.setNegation(true);
			return check;
		}
		else if (rel == Relation.GREATER_THAN) {
			if (!(expectedVal instanceof DoubleVal)) {
				log.warning("val should be a number: " + expectedVal);
			}
			return new GreaterThanCheck(variable, (DoubleVal)expectedVal);
		}
		else if (rel == Relation.LOWER_THAN) {
			if (!(expectedVal instanceof DoubleVal)) {
				log.warning("val should be a number: " + expectedVal);
			}
			return new LowerThanCheck(variable, (DoubleVal)expectedVal);
		}
		else if (rel == Relation.EQUAL) {
			return new EqualityCheck(variable, expectedVal);
		}
		else if (rel == Relation.UNEQUAL) {
			EqualityCheck check = new EqualityCheck(variable, expectedVal);
			check.setNegation(true);
			return check;
		}

		
		log.warning("check could not be created with " + variable + rel + expectedVal);
		return null;
	}
}

