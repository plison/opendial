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

package opendial.domains.rules.effects;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.Template;

/**
 * Effect that is clearing all current values for a variable.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ClearEffect implements Effect {

	// logger
	public static Logger log = new Logger("ClearEffect", Logger.Level.NORMAL);

	// the variable to clear
	Template variable;
	
	/**
	 * Creates a new clear effect with the given variable
	 * (that can include slots)
	 * 
	 * @param variable the raw string for the variable label
	 */
	public ClearEffect(String variable) {
		this.variable = new Template(variable);
	}
	
	
	/**
	 * Returns an empty set
	 * 
	 * @return an empty set
	 */
	@Override
	public Set<String> getAdditionalInputVariables() {
		return new HashSet<String>();
	}

	
	/**
	 * Returns a singleton set with the variable to clear
	 * 
	 * @return the output variable
	 */
	@Override
	public Set<Template> getOutputVariables() {
		return new HashSet<Template>(Arrays.asList(variable));
	}

	/**
	 * Get the output corresponding to the effect -- namely, an output
	 * that clears the variable.
	 * 
	 * @param input the assignment for additional input variables, that can
	 *              be used to fill slots in the variable label
	 * @return the output
	 */
	@Override
	public Output createOutput(Assignment input) {
		Output o = new Output();
		Template filledVariable = variable.fillSlotsPartial(input);
		if (filledVariable.getSlots().isEmpty()) {
			o.clearVariable(filledVariable.getRawString());			
		}
		return o;
	}
}
