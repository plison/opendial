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

import java.util.Set;

import opendial.bn.Assignment;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.quantification.UnboundPredicate;

/**
 * Generic interface for a condition used in a probability or utility rule. 
 * 
 * <p>A condition operates on a number of (possibly underspecified) input variables,
 * and can be applied to any input assignment to determine if it satisfies the condition
 * or not.  In addition, the condition can also produce some local output, for instance
 * based on slots filled via string matching.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface Condition {
	
	public Set<TemplateString> getInputVariables();
		
	public boolean isSatisfiedBy(Assignment input);
	
	public Assignment getLocalOutput(Assignment input);

	public Set<UnboundPredicate> getUnboundPredicates();

	
}

