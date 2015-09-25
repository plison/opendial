// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.bn;

import java.util.logging.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.CategoricalTable.Builder;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.SetVal;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

import org.junit.Test;

public class ValueTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void testAssign() {
		Assignment a = Assignment.createFromString(
				"blabla=3 ^ !bloblo^TTT=32.4 ^v=[0.4,0.6] ^ final");
		assertEquals(5, a.getVariables().size());
		assertEquals(
				new HashSet<String>(
						Arrays.asList("blabla", "bloblo", "TTT", "v", "final")),
				a.getVariables());
		assertEquals(ValueFactory.create("3"), a.getValue("blabla"));
		assertEquals(ValueFactory.create(false), a.getValue("bloblo"));
		assertEquals(ValueFactory.create("32.4"), a.getValue("TTT"));
		assertEquals(ValueFactory.create(new double[] { 0.4, 0.6 }),
				a.getValue("v"));
		assertEquals(ValueFactory.create(true), a.getValue("final"));
	}

	@Test
	public void testClassical() {
		assertTrue(ValueFactory.create(" blabla ") instanceof StringVal);
		// assertEquals("blabla",
		// ((StringVal)ValueFactory.create(" blabla ")).getString());
		assertTrue(ValueFactory.create("3") instanceof DoubleVal);
		assertTrue(ValueFactory.create("3.6") instanceof DoubleVal);
		assertEquals(3.0, ((DoubleVal) ValueFactory.create("3")).getDouble(),
				0.0001);
		assertTrue(ValueFactory
				.create("[firstItem, secondItem, 3.6]") instanceof SetVal);
		assertEquals(3, (ValueFactory.create("[firstItem, secondItem, 3.6]"))
				.getSubValues().size());
		assertEquals(
				new HashSet<Value>(Arrays.asList(ValueFactory.create("firstItem"),
						ValueFactory.create("secondItem"),
						ValueFactory.create(3.6))),
				(ValueFactory.create("[firstItem, secondItem,3.6]")).getSubValues());
		assertTrue(ValueFactory.create("[0.6, 0.4, 32]") instanceof ArrayVal);
		assertEquals(3, ((ArrayVal) ValueFactory.create("[0.6, 0.4, 32]"))
				.getArray().length);
		assertEquals(32,
				((ArrayVal) ValueFactory.create("[0.6, 0.4, 32]")).getArray()[2],
				0.0001);
		assertTrue(ValueFactory.create("true") instanceof BooleanVal);
		assertFalse(((BooleanVal) ValueFactory.create("false")).getBoolean());
		assertEquals(ValueFactory.none(), ValueFactory.create("None"));
		assertEquals(0, ValueFactory.create("firsttest")
				.compareTo(ValueFactory.create("firsttest")));
		assertEquals(-13, ValueFactory.create("firsttest")
				.compareTo(ValueFactory.create("secondTest")));
		assertEquals(-1, ValueFactory.create(3).compareTo(ValueFactory.create(5)));
		assertEquals(1, ValueFactory.create(5).compareTo(ValueFactory.create(3)));
		assertEquals(ValueFactory.create("test").compareTo(ValueFactory.create(5)),
				-ValueFactory.create(5).compareTo(ValueFactory.create("test")));
		assertEquals(3, ((SetVal) ValueFactory.create("[test,[1,2],true]"))
				.getSubValues().size());
		assertTrue(((SetVal) ValueFactory.create("[test,[1,2],true]")).getSubValues()
				.contains(ValueFactory.create("test")));
		assertTrue(((SetVal) ValueFactory.create("[test,[1,2],true]")).getSubValues()
				.contains(ValueFactory.create("[1,2]")));
		assertTrue(((SetVal) ValueFactory.create("[test,[1,2],true]")).getSubValues()
				.contains(ValueFactory.create("true")));
		assertEquals(3, ((SetVal) ValueFactory.create("[a1=test,a2=[1,2],a3=true]"))
				.getSubValues().size());

	}

	public void testClosest() {
		Builder builder = new CategoricalTable.Builder("v");
		builder.addRow(new double[] { 0.2, 0.2 }, 0.3);
		builder.addRow(new double[] { 0.6, 0.6 }, 0.4);
		IndependentDistribution table = builder.build();
		assertEquals(table.getProb(new double[] { 0.25, 0.3 }), 0.3, 0.01);
		assertEquals(table.getProb(new double[] { 0.5, 0.4 }), 0.4, 0.01);

	}
}
