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

package opendial.domains.rules.variables;

import opendial.domains.types.GenericType;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Variable {

	// static Logger log = new Logger("Variable", Logger.Level.NORMAL);
			
		String denotation;
		
		GenericType type;
		
		public Variable (GenericType type) {
			this.denotation = type.getName();
			this.type = type;
		}
		public Variable(String denotation, GenericType type) {
			this.denotation = denotation;
			this.type = type;
		}


		public String getDenotation() {
			return denotation;
		}
	 
		/**
		 * 
		 * @param denotation2
		 */
		public void setDenotation(String denotation) {
			this.denotation = denotation;
		}
		
		public void setType(GenericType type) {
			this.type = type;
		}
		
		public GenericType getType() {
			return type;
		}


}
