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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import opendial.arch.Logger;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.SetVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

public class ValueTest {

	// logger
	public static Logger log = new Logger("ValueTest", Logger.Level.DEBUG);

 /** 	@Test
	public void mapTest() {
		String stringVersion = "<feature2:333.0;feature1:blablabla>";
		Value val = ValueFactory.create(stringVersion);
		assertTrue(val instanceof MapVal);
		assertEquals(((MapVal)val).getMap().size(), 2);
		assertEquals(((MapVal)val).getMap().get("feature1").toString(), "blablabla");
		assertEquals(val.toString(), stringVersion);
	} */
	
	@Test
	public void testAssign() {
		Assignment a = Assignment.createFromString("blabla=3 ^ !bloblo^TTT=32.4 ^v=[0.4,0.6] ^ final");
		assertEquals(5, a.getVariables().size());
		assertEquals(new HashSet<String>(Arrays.asList("blabla", "bloblo", "TTT", "v", "final")), a.getVariables());
		assertEquals(ValueFactory.create("3"), a.getValue("blabla"));
		assertEquals(ValueFactory.create(false), a.getValue("bloblo"));
		assertEquals(ValueFactory.create("32.4"), a.getValue("TTT"));
		assertEquals(ValueFactory.create(new Double[]{0.4, 0.6}), a.getValue("v"));
		assertEquals(ValueFactory.create(true), a.getValue("final"));
	}
	
	@Test
	public void testClassical() {
		assertTrue(ValueFactory.create(" blabla ") instanceof StringVal);
		assertEquals("blabla", ((StringVal)ValueFactory.create(" blabla ")).getString());
		assertTrue(ValueFactory.create("3") instanceof DoubleVal);
		assertTrue(ValueFactory.create("3.6") instanceof DoubleVal);
		assertEquals(3.0, ((DoubleVal)ValueFactory.create("3")).getDouble(), 0.0001);
		assertTrue(ValueFactory.create("[firstItem, secondItem, 3.6]") instanceof SetVal);
		assertEquals(3, ((SetVal)ValueFactory.create("[firstItem, secondItem, 3.6]")).getSet().size());
		assertEquals(new HashSet<Value>(Arrays.asList(ValueFactory.create("firstItem"), ValueFactory.create("secondItem"), 
				ValueFactory.create(3.6))), ((SetVal)ValueFactory.create("[firstItem, secondItem,3.6]")).getSet());
		assertTrue(ValueFactory.create("[0.6, 0.4, 32]") instanceof ArrayVal);
		assertEquals(3, ((ArrayVal)ValueFactory.create("[0.6, 0.4, 32]")).getArray().length);
		assertEquals(32, ((ArrayVal)ValueFactory.create("[0.6, 0.4, 32]")).getArray()[2], 0.0001);
		assertTrue(ValueFactory.create("true") instanceof BooleanVal);
		assertFalse(((BooleanVal)ValueFactory.create("false")).getBoolean());
		assertEquals(ValueFactory.none(), ValueFactory.create(" None"));
		assertEquals(0, ValueFactory.create("firsttest ").compareTo(ValueFactory.create(" firsttest")));
		assertEquals(-13, ValueFactory.create("firsttest ").compareTo(ValueFactory.create(" secondTest")));
		assertEquals(-1, ValueFactory.create(3).compareTo(ValueFactory.create(5)));
		assertEquals(1, ValueFactory.create(5).compareTo(ValueFactory.create(3)));
		assertEquals(ValueFactory.create("test").compareTo(ValueFactory.create(5)), 
				-ValueFactory.create(5).compareTo(ValueFactory.create("test")));		
	}
}

