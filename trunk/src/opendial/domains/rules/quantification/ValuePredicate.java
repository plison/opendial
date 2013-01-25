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

package opendial.domains.rules.quantification;


import java.util.Set;

import opendial.domains.datastructs.TemplateString;

public class ValuePredicate implements UnboundPredicate {

	String variable;
	TemplateString predicate;
	
	public ValuePredicate (String variable, TemplateString predicate) {
		this.predicate = predicate;
		this.variable = variable;
	}
	
	
	public String getVariable() {
		return variable;
	}
	
	@Override
	public TemplateString getPredicate() {
		return predicate;
	}
	
	public int hashCode() {
		return predicate.hashCode();
	}
	
	public String toString() {
		return variable + "?=" + predicate;
	}

	
	public Set<String> getVariables() {
		return predicate.getSlots();
	}
}

