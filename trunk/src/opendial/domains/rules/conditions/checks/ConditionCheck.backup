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
import opendial.bn.values.StringVal;
import opendial.bn.values.SetVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.conditions.BasicCondition.Relation;

/**
 * Abstract class for all possible condition checks in use inside
 * rule conditions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class ConditionCheck {

	// logger
	public static Logger log = new Logger("ConditionCheck", Logger.Level.DEBUG);

	/**
	 * Returns whether the check is satisfied with the given value
	 * 
	 * @param value
	 * @return
	 */
	public abstract boolean isSatisfied (Value value);

	/**
	 * Returns the (optional) local output produced by the check 
	 * given the value.
	 * 
	 * <p>Example: if the condition is "u_m matches "take the ${X}"
	 * and u_m = "take the box", the condition will produce a local
	 * output "X=box".
	 * 
	 * @param value the value
	 * @return the local output for the condition
	 */
	public Assignment getLocalOutput(Value value) {
		return new Assignment();
	}

}


/**
 * Equality check
 *
 */
final class Equal extends ConditionCheck {

	Value expected ;

	public Equal (Value value) { this.expected = value; }
	public boolean isSatisfied (Value value) { 	
		return this.expected.equals(value);
	}
}


/**
 * Negation of another check
 *
 */
final class Neg extends ConditionCheck {

	ConditionCheck check;

	public Neg(ConditionCheck check) { this.check = check; }
	public boolean isSatisfied(Value value) { return !check.isSatisfied(value); }
}


/**
 * Inclusion (or non-inclusion) of a specific value
 *
 */
final class Contains extends ConditionCheck {

	Value expected;

	boolean positive = true;

	public Contains(Value value) { this.expected = value; };
	public Contains(Value value, boolean positive) { this(value); this.positive = positive; };

	public boolean isSatisfied(Value set) {
		if (set instanceof SetVal) {
			boolean contained = ((SetVal)set).getSet().contains(this.expected);
			return (positive)? contained : !contained;
		}
		return false;
	}
}


/**
 * Value greater than a threshold
 *
 */
final class GreaterThan extends ConditionCheck {
	DoubleVal value;

	public GreaterThan(DoubleVal value) { this.value = value;}

	public boolean isSatisfied(Value value) { 
		if (value instanceof DoubleVal) {
			return ((DoubleVal)value).getDouble() > (this.value.getDouble()) ;
		}
		return false;
	}
}

/**
 * Value lower than a threshold
 *
 */
final class LowerThan extends ConditionCheck {
	DoubleVal value;

	public LowerThan(DoubleVal value) { this.value = value;}


	public boolean isSatisfied(Value value) { 
		if (value instanceof DoubleVal) {
			return ((DoubleVal)value).getDouble() < (this.value.getDouble());
		}
		return false;
	}
}

/**
 * String matching (partial or exact)
 *
 */
final class StringMatch extends ConditionCheck {

	boolean partial = false;

	TemplateString template;

	public StringMatch(TemplateString template, boolean partial) {
		this.template = template;
		this.partial =  partial;
	}

	public boolean isSatisfied(Value value) {
		if (value instanceof StringVal) {
			return template.isMatching(((StringVal)value).getString(), partial);
		}
		else {
			return false;
		}
	}

	@Override
	public Assignment getLocalOutput(Value value) {
		Assignment params =  template.extractParameters(value.toString(), partial);
		Assignment boundaries = template.getMatchBoundaries(value.toString(), partial);
		return new Assignment(params, boundaries);
	}
}


/**
 * Trivially false check
 *
 */
final class False extends ConditionCheck {

	public boolean isSatisfied(Value value) { return false; }
}



/**
 * Trivially true check
 *
 */
final class True extends ConditionCheck {

	public boolean isSatisfied(Value value) { return true; }
}



/**
 * Trivially true check, with a match
 *
 */
final class TrueWithMatch extends ConditionCheck {

	boolean partial = false;

	TemplateString template;

	public TrueWithMatch(TemplateString template, boolean partial) {
		this.template = template;
		this.partial =  partial;
	}
	
	public boolean isSatisfied(Value value) { return true; }
	
	
	@Override
	public Assignment getLocalOutput(Value value) {
		Assignment params =  template.extractParameters(value.toString(), partial);
		if (partial) {
			Assignment boundaries = template.getMatchBoundaries(value.toString(), partial);
			return new Assignment(params, boundaries);
		}
		else {
			return params;
		}
	}
}
