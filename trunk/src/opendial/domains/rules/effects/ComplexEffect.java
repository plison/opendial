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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.Template;


/**
 * A complex effect, represented as a combination of elementary sub-effects connected
 * via an implicit AND relation.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexEffect implements Effect {

	// logger
	static Logger log = new Logger("ComplexEffect", Logger.Level.DEBUG);

	// the sub-effects included in the effect
	Collection<Effect> subeffects;

	/**
	 * Creates a new, empty complex effect
	 */
	public ComplexEffect() {
		subeffects = new LinkedList<Effect>();
	}

	/**
	 * Creates a new complex effect with a collection of existing effects
	 * 
	 * @param effects the effects to include
	 */
	public ComplexEffect(List<Effect> effects) {
		subeffects = effects;
	}

	/**
	 * Adds a new sub-effect in the complex effect
	 * 
	 * @param effect the effect to add
	 */
	public void addSubEffect(Effect effect) {
		subeffects.add(effect);
	}

	
	/**
	 * Returns all the sub-effect included in the complex effect
	 * 
	 * @return the collection of sub-effects
	 */
	public Collection<Effect> getSubEffects() {
		return subeffects;
	}


	/**
	 * Returns the additional input variables for the complex effect
	 * 
	 * @return the set of labels for the additional input variables
	 */
	@Override
	public Set<String> getAdditionalInputVariables() {
		Set<String> variables = new HashSet<String>();
		for (Effect e : subeffects) {
			variables.addAll(e.getAdditionalInputVariables());
		}
		return variables;
	}

	
	/**
	 * Returns the output variables for the complex effect
	 * (including all the output variables for the sub-effects)
	 * 
	 * @return the set of all output variables
	 */
	@Override
	public Set<Template> getOutputVariables() {
		Set<Template> variables = new HashSet<Template>();
		for (Effect e : subeffects) {
			variables.addAll(e.getOutputVariables());
		}
		return variables;
	}


	
	/**
	 * Creates a new output combining the outputs of all the sub-effects
	 * 
	 * @param additionalInput additional input for the effect parameterisations
	 * @return the resulting output
	 */
	@Override
	public Output createOutput(Assignment additionalInput) {

			Output globalOutput = new Output();
			
			for (Effect e : subeffects) {
				Output subOutput = e.createOutput(additionalInput);
				globalOutput.includeOutput(subOutput);
			}
			return globalOutput;
	}


	/**
	 * Returns the hashcode for the complex effect
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return subeffects.hashCode();
	}

	/**
	 * Returns true if the object is a complex effect with an identical content
	 *
	 * @param o the object to compare
	 * @return true if the objects are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return o.hashCode() == hashCode();
	}

	
	/**
	 * Returns a string representation for the effect
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Effect e: subeffects) {
			str += e.toString() + " ^ ";
		}
		return str.substring(0, str.length()-3);
	}




}
