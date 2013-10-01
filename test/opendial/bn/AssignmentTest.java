// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.bn;

import static org.junit.Assert.*;

import org.junit.Test;

import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AssignmentTest {

	// logger
	public static Logger log = new Logger("AssignmentTest", Logger.Level.NORMAL);
	
	@Test
	public void assignInterchanceTest() {
		Assignment a1 = new Assignment(new Assignment("Burglary", true), "Earthquake", ValueFactory.create(false));
		Assignment a1bis = new Assignment(new Assignment("Earthquake", false), "Burglary", ValueFactory.create(true));
		Assignment a2 = new Assignment(new Assignment("Burglary", false), "Earthquake", ValueFactory.create(true));
		Assignment a2bis = new Assignment(new Assignment("Earthquake", true), "Burglary", ValueFactory.create(false));
		assertFalse(a1.equals(a2));
		assertFalse(a1.hashCode() == a2.hashCode());
		assertFalse(a1bis.equals(a2bis));
		assertFalse(a1bis.hashCode() == a2bis.hashCode());
		assertFalse(a1.equals(a2bis));
		assertFalse(a1.hashCode() == a2bis.hashCode());
		assertFalse(a1bis.equals(a2));
		assertFalse(a1bis.hashCode() == a2.hashCode());
	}
}
