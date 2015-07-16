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

import static org.junit.Assert.assertFalse;

import java.util.logging.Logger;

import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class AssignmentTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void testAssignInterchance() {
		Assignment a1 = new Assignment(new Assignment("Burglary", true),
				"Earthquake", ValueFactory.create(false));
		Assignment a1bis = new Assignment(new Assignment("Earthquake", false),
				"Burglary", ValueFactory.create(true));
		Assignment a2 = new Assignment(new Assignment("Burglary", false),
				"Earthquake", ValueFactory.create(true));
		Assignment a2bis = new Assignment(new Assignment("Earthquake", true),
				"Burglary", ValueFactory.create(false));
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
