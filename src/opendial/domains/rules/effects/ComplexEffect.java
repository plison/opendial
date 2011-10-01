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

import java.util.LinkedList;
import java.util.List;

import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexEffect extends Effect {

	static Logger log = new Logger("ComplexEffect", Logger.Level.NORMAL);
	
	public static enum Operator {AND, OR}
	
	List<Effect> subeffects;

	Operator binaryOp;
	
	
	public ComplexEffect(float prob) {
		super(prob);
		subeffects = new LinkedList<Effect>();
	}
	
	public void setOperator(Operator binaryOp) {
		this.binaryOp = binaryOp;
	}
	
	public void addSubEffect(Effect effect) {
		subeffects.add(effect);
	}

	/**
	 * 
	 * @param subeffects2
	 */
	public void addSubeffects(List<Effect> subeffects2) {
		subeffects.addAll(subeffects2);
	}
}
