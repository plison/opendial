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

package opendial.outputs;


/**
 * Representation of a "verbal action", which is a string to synthesise 
 * via the Text-to-Speech module.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VerbalAction implements Action {

	// the string to synthesise
	String stringToSynthesize;
	
	
	/**
	 * Creates a new verbal action with the string to synthesise
	 * @param stringToSynthesize
	 */
	public VerbalAction(String stringToSynthesize) {
		this.stringToSynthesize = stringToSynthesize;
	}
	
	public String getString() {
		return stringToSynthesize;
	}
	
	
	/**
	 * Returns a string representation of the action
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "VerbalAction: \"" + stringToSynthesize + "\"";
	}
}
