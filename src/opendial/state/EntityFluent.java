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
import opendial.domains.types.AbstractType;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class EntityFluent extends Fluent {

	static Logger log = new Logger("EntityFluent", Logger.Level.NORMAL);
	
	float existenceProb = 1.0f;

	/**
	 * @param type
	 */
	public EntityFluent(AbstractType type) {
		super(type);
	}

	public void setExistenceProb(float existenceProb) {
		this.existenceProb = existenceProb;
	}
	
	
	public EntityFluent copy() {
		EntityFluent copy = new EntityFluent(type);
		copy.setLabel(label);
		
		for (String v : values.keySet()) {
			try {
			copy.addValue(v, values.get(v));
			}
			catch (DialException e) {
				log.warning("Strange problem copying a fluent, aborting copy operation");
			}
		}
		for (Fluent f : features.values()) {
			copy.addFeature(f.copy());
		}
		copy.setExistenceProb(existenceProb);
		return copy;
	}
	
}
