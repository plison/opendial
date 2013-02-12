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

import java.util.HashSet;

import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.Template;

/**
 * A void effect, having no effect on any variable.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VoidEffect implements Effect {

	// logger
	static Logger log = new Logger("VoidEffect", Logger.Level.NORMAL);

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
	 * Returns an empty set
	 * 
	 * @return an empty set
	 */
	@Override
	public Set<Template> getOutputVariables() {
		return new HashSet<Template>();
	}

	/**
	 * Returns an empty output
	 * 
	 * @param input the additional input (ignored)
	 * @return an empty output
	 */
	@Override
	public Output createOutput(Assignment input) {
		return new Output();
	}
	
	
	/**
	 * Returns "Void"
	 *
	 * @return "Void"
	 */
	@Override
	public String toString() {
		return "Void";
	}
	
	/**
	 * Returns a constant as hashcode
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return 79;
	}
	
	
	/**
	 * Returns true if o is also a void effect
	 *
	 * @param o the object to compare
	 * @return true if o is also a VoidEffect
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof VoidEffect) {
			return true;
		}
		return false;
	}

	
}
