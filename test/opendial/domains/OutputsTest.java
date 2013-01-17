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

package opendial.domains;


import static org.junit.Assert.*;

import org.junit.Test;

import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Output;

public class OutputsTest {

	// logger
	public static Logger log = new Logger("OutputsTest", Logger.Level.NORMAL);

	
	@Test
	public void OutputParsing() {
		
		Output o = new Output();
		assertEquals(o, Output.parseOutput("Void"));
		o.setValueForVariable("v1", ValueFactory.create("val1"));
		assertEquals(o, Output.parseOutput("v1:=val1"));

		o.addValueForVariable("v2", ValueFactory.create("val2"));
		assertEquals(o, Output.parseOutput("v1:=val1 ^ v2+=val2"));
		
		o.discardValueForVariable("v2", ValueFactory.create("val3"));
		assertEquals(o, Output.parseOutput("v1:=val1 ^ v2+=val2 ^ v2!=val3"));
		
		o.clearVariable("v3");
		assertEquals(o, Output.parseOutput("v1:=val1 ^ v2+=val2 ^ v2!=val3 ^ v3:={}"));
		
	}
}

