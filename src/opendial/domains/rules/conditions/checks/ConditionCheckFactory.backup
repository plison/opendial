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
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.conditions.BasicCondition.Relation;

/**
 * Factory for creating ConditionCheck objects in use for the rule conditions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ConditionCheckFactory {

	// logger
	public static Logger log = new Logger("ConditionCheckFactory", Logger.Level.DEBUG);

	/**
	 * Creates a new condition check on the basis of an expected value, a relation
	 * and a value assignment containing the actual variable values.
	 * 
	 * @param expectedVal the expected value
	 * @param rel the relation to satisfy
	 * @param input the actual assignment of values
	 * @return the condition check
	 */
	public static ConditionCheck createCheck(TemplateString expectedVal, 
			Relation rel, Assignment input) {			
		
		// we first fill the possible slots in the expected value
		TemplateString filledExpectedVal = expectedVal;
		if (!expectedVal.getSlots().isEmpty()) {
			filledExpectedVal = expectedVal.fillSlotsPartial(input);
		}
		
		// creating a string match check
		if (rel == Relation.EXACT_MATCH) {
		 return new StringMatch(filledExpectedVal, false);
		}
		else if (rel == Relation.PARTIAL_MATCH) {
		 return new StringMatch(filledExpectedVal, true);
		}
		
		// for these values, the expected values must be void of unfilled slots
		else if (filledExpectedVal.getSlots().isEmpty()) {
			
			Value value = ValueFactory.create(filledExpectedVal.getRawString());
			switch (rel) {
			case EQUAL: return new Equal(value);
			case UNEQUAL: return new Neg(new Equal(value));
			case GREATER_THAN: return (value instanceof DoubleVal)? 
					new GreaterThan((DoubleVal)value) : new False();
			case LOWER_THAN: return (value instanceof DoubleVal)? 
					new LowerThan((DoubleVal)value) : new False();
			case CONTAINS: return new Contains(value) ;
			case NOT_CONTAINS: return new Contains(value, false) ;
		}
		}
		else if (rel == Relation.EQUAL){
			return new StringMatch(filledExpectedVal, false);
		}

		else if (rel == Relation.UNEQUAL) {
			return new Neg(new StringMatch(filledExpectedVal, false));
		}
		else if (rel == Relation.NOT_CONTAINS) {
			return new True();
		}

		return new False();
	}
	
}
