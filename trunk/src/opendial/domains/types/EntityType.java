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

package opendial.domains.types;

import java.util.List;

import opendial.domains.types.values.Value;

/**
 * Representation of an entity type
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-10-07 16:21:33 #$
 *
 */
public class EntityType extends AbstractType {

	// static Logger log = new Logger("EntityType", Logger.Level.NORMAL);

	/**
	 * @param name
	 */
	public EntityType(String name) {
		super(name);
	}
	
	public void addValue(Value val) {
		internalAddValue(val);
	}
	
	public void addValues(List<Value> val) {
		internalAddValues(val);
	}



}
