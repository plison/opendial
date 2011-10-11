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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import opendial.arch.DialConstants.BinaryOperator;
import opendial.utils.Logger;

/**
 * Representation of a complex effect, made of several sub-effects connected
 * with a binary logical operator
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexEffect implements Effect {

	// the logger
	static Logger log = new Logger("ComplexEffect", Logger.Level.NORMAL);
		
	// the sub-effects
	List<Effect> subeffects;

	// the binary logical operator
	BinaryOperator binaryOp;
	
	/**
	 * Creates a new complex effect, with an empty list of sub-effects
	 */
	public ComplexEffect() {
		subeffects = new LinkedList<Effect>();
	}
	
	/**
	 * Creates a new complex effect, with the given list of sub-effects
	 * 
	 * @param subeffects the sub-effects
	 */
	public ComplexEffect(List<Effect> subeffects) {
		this.subeffects = subeffects;
	}
	
	
	/**
	 * Sets the binary operator which combines the sub-effects
	 * 
	 * @param binaryOp the binary operator
	 */
	public void setOperator(BinaryOperator binaryOp) {
		this.binaryOp = binaryOp;
	}
	
	
	/**
	 * Gets the binary operator between the sub-effects
	 * 
	 * @return the binary operator
	 */
	public BinaryOperator getOperator() {
		return binaryOp;
	}
	
	
	/**
	 * Adds a sub-effect to the current list
	 * 
	 * @param effect the new sub-effect
	 */
	public void addSubEffect(Effect effect) {
		subeffects.add(effect);
	}

	
	/**
	 * Adds a list of sub-effects to the current list
	 *  
	 * @param subeffects the sub-effects to add
	 */
	public void addSubeffects(List<Effect> subeffects) {
		this.subeffects.addAll(subeffects);
	}

	
	/**
	 * Returns the list of sub-effects comprised in the complex effect
	 * 
	 * @return the list of sub-effects
	 */
	public List<Effect> getSubeffects() {
		return subeffects;
	}
	
	
	/**
	 * Returns a string representation of the effect
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "Complex: (";
		Iterator<Effect> it = subeffects.iterator();
		while (it.hasNext()) {
			str += it.next();
			if (it.hasNext()) {
				if (binaryOp.equals(BinaryOperator.AND)) {
					str += " ^ ";
				}
				else if (binaryOp.equals(BinaryOperator.OR)) {
					str += " v ";
				}
			}
		}
		return str + ")";
	}
	

	
	/**
	 * Compare the effect to the given one
	 * 
	 * @param e the effect to compare with the current object
	 * @return 0 if equals, -1 otherwise
	 */
	@Override
	public int compareTo(Effect e) {
		if (e instanceof ComplexEffect) {
			for (Effect sub1: subeffects) {
				boolean foundVal = false;
				for (Effect sub2 : ((ComplexEffect)e).getSubeffects()) {
					if (sub1.compareTo(sub2) == 0) {
						foundVal = true;
					}
				}
				if (!foundVal) {
					return -1;
				}
			}
			return 0;
		}
		return -1;
	}
	
}
