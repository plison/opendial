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

import opendial.arch.DialConstants.Relation;
import opendial.arch.DialException;
import opendial.domains.rules.variables.Variable;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicCondition extends Condition {

	static Logger log = new Logger("BasicCondition", Logger.Level.NORMAL);
		
	Variable var;
	
	String value;
	
	Relation rel = Relation.EQUAL;
	
	public BasicCondition (Variable var, String value) throws DialException {
		this.var = var;
		this.value = value;
		
		if (!var.getType().acceptsValue(value)) {
			throw new DialException("variable " + var.getType().getName() + " does not accept value " + value);
		}
	}
	
	public BasicCondition (Variable var, String value, Relation rel) throws DialException {
		this(var,value);
		this.rel = rel;
	}
	
	public Variable getVariable() {
		return var;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setRelation(Relation rel) {
		this.rel = rel;
	}
	
	public Relation getRelation() {
		return rel;
	}
	
}
