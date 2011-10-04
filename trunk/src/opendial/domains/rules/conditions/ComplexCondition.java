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

import java.util.LinkedList;
import java.util.List;

import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-10-01 19:25:25 #$
 *
 */
public class ComplexCondition extends Condition {

	static Logger log = new Logger("ComplexCondition", Logger.Level.NORMAL);

	public static enum Operator {AND, OR}
	
	List<Condition> subconditions;

	Operator binaryOp;
	
	
	public ComplexCondition() {
		subconditions = new LinkedList<Condition>();
	}
	
	public void setOperator(Operator binaryOp) {
		this.binaryOp = binaryOp;
	}
	
	public void addSubcondition(Condition subcondition) {
		subconditions.add(subcondition);
	}

	/**
	 * 
	 * @param subconditions2
	 */
	public void addSubconditions(List<Condition> subconditions2) {
		subconditions.addAll(subconditions2);
	}

	/**
	 * 
	 * @return
	 */
	public List<Condition> getSubconditions() {
		return subconditions;
	}
}
