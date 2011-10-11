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

package opendial.state;


import opendial.arch.DialException;
import opendial.domains.types.GenericType;
import opendial.utils.Logger;

/**
 * Representation of a conditional fluent (used to describe content of features)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ConditionalFluent extends Fluent {

	// logger
	static Logger log = new Logger("ConditionalFluent", Logger.Level.NORMAL);
	
	// input variable (i.e. variable to which the feature is attached)
	Fluent inputVariable;
			
	
	/**
	 * Creates a new condition fluent, with a given type and input variable
	 * 
	 * @param type the type
	 * @param inputVariable the input variable
	 */
	public ConditionalFluent(GenericType type, Fluent inputVariable) {
		super(type);
		this.inputVariable = inputVariable;
	}
	
	
	/**
	 * Returns the input variable
	 * 
	 * @return input variable
	 */
	public Fluent getInputVariable() {
		return inputVariable;
	}
	
	
	/**
	 * Sets the input variable for the fluent
	 *   
	 * @param inputVariable2 the input variable
	 */
	public void setInputVariable(Fluent inputVariable) {
		this.inputVariable = inputVariable;
	}
	
	
	/**
	 * Returns a copy of the conditional fluent
	 *
	 * @return the copy
	 */
	public ConditionalFluent copy() {
		ConditionalFluent copy = new ConditionalFluent(type, inputVariable);
		copy.setLabel(label);
		
		for (String v : values.keySet()) {
			try {
			copy.addValue(v, values.get(v));
			}
			catch (DialException e) {
				log.warning("Strange problem copying a fluent, aborting copy operation");
			}
		}
		for (ConditionalFluent f : features.values()) {
			copy.addFeature(f.copy());
		}
		return copy;
	}
	



}
