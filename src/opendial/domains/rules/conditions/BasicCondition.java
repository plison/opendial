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

package opendial.domains.rules.conditions;

import java.util.Map;

import opendial.arch.DialConstants.Relation;
import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.domains.rules.variables.TypedVariable;
import opendial.domains.rules.variables.Variable;
import opendial.inference.bn.Assignment;
import opendial.utils.Logger;

/**
 * Representation of a basic condition specified in the rule.  A basic condition
 * is made of three elements: a variable reference, a value, and a binary relation
 * between these two (e.g. equality, inequality, etc.).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicCondition implements Condition {

	// logger
	static Logger log = new Logger("BasicCondition", Logger.Level.DEBUG);
		
	// variable reference
	Variable var;
	
	// the value to check
	Object value;
	
	// the relation which needs to hold between the variable and the value
	Relation rel = Relation.EQUAL;
	
	
	/**
	 * Creates a new basic condition between the variable and the value.  The
	 * default relation is Relation.EQUAL
	 * 
	 * @param var the variable reference
	 * @param value the value
	 * @throws DialException if the type of the variable does not accept the given value
	 */
	public BasicCondition (Variable var, Object value) throws DialException {
		this.var = var;
		this.value = value;
		
		if (var instanceof TypedVariable && !((TypedVariable)var).getType().containsValue(value)) {
			throw new DialException("variable " + var.getIdentifier() + " does not accept value " + value);
		}
	}
	
	
	/**
	 * Creates a new basic condition between an (untyped) variable and a value.  The
	 * default relation is Relation.EQUAL
	 * 
	 * @param var
	 * @param value
	 */
	public BasicCondition (String var, Object value) {
		this.var = new Variable(var);
		this.value = value;
	}
	
	
	/**
	 * Creates a new basic condition with a variable, a value and a binary relation 
	 * between the two 
	 * 
	 * @param var the variable reference
	 * @param value the value
	 * @param rel the binary relation
	 * @throws DialException if the type of the variable does not accept the given value
	 */
	public BasicCondition (TypedVariable var, Object value, Relation rel) throws DialException {
		this(var,value);
		setRelation(rel);
	}
	
	
	/**
	 * Returns the variable reference
	 * 
	 * @return the variable reference
	 */
	public Variable getVariable() {
		return var;
	}
	
	/**
	 * Returns the value specified in the condition
	 * 
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Sets the binary relation holding in the condition
	 * 
	 * @param rel the binary relation
	 */
	public void setRelation(Relation rel) {
		this.rel = rel;
	}
	
	
	/**
	 * Returns the binary relation in the condition
	 * 
	 * @return the binary relation
	 */
	public Relation getRelation() {
		return rel;
	}


	/**
	 * Returns true is the condition is satisfied by the following
	 * assignment, and false otherwise
	 * 
	 * @param input the assignment to check
	 * @return true if the condition is satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input, Map<Variable,String> anchors) {
		if (input.getVariables().contains(anchors.get(var)) && 
				rel.equals(Relation.EQUAL)) {
			return input.getValue(anchors.get(var)).equals(value);
		}
		else if (input.getVariables().contains(anchors.get(var)) && 
				rel.equals(Relation.UNEQUAL)) {
			return (!input.getValue(anchors.get(var)).equals(value));
		}
		return false;
	}
	
	
	/**
	 * Returns true is the condition is satisfied by the following
	 * assignment, and false otherwise
	 * 
	 * @param input the assignment to check
	 * @return true if the condition is satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		if (input.getVariables().contains(var.getIdentifier()) && 
				rel.equals(Relation.EQUAL)) {
			return input.getValue(var.getIdentifier()).equals(value);
		}
		else if (input.getVariables().contains(var.getIdentifier()) && 
				rel.equals(Relation.UNEQUAL)) {
			return (!input.getValue(var.getIdentifier()).equals(value));
		}
		return false;
	}
	
	
	/**
	 * Returns a string representation of the basic condition
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return var.getIdentifier() + DialConstants.toString(rel) + value;
	}
	
}
