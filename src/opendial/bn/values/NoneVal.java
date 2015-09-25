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

package opendial.bn.values;

import java.util.ArrayList;
import java.util.Collection;

/**
 * "None" value (describing the lack of value, or an empty assignment)
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class NoneVal implements Value {

	/**
	 * Creates the none value (protected, use the value factory)
	 * 
	 */
	protected NoneVal() {
	}

	/**
	 * Returns a hashcode for the value
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return 346;
	}

	/**
	 * Returns true if both values are none
	 *
	 * @param o the object
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof NoneVal);
	}

	/**
	 * Returns its own instance
	 *
	 * @return its own instance
	 */
	@Override
	public NoneVal copy() {
		return this;
	}

	/**
	 * Returns the value v provided as argument.
	 */
	@Override
	public Value concatenate(Value v) {
		return v;
	}

	/**
	 * Returns the string "None
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "None";
	}

	/**
	 * Compares the none value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	/**
	 * Returns 0
	 * 
	 * @return 0
	 */
	@Override
	public int length() {
		return 0;
	}

	/**
	 * True if subvalue is contained in the current instance, and false otherwise
	 * 
	 * @param subvalue the possibly contained value
	 * @return true if contained, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}

	/**
	 * Returns an empty list
	 */
	@Override
	public Collection<Value> getSubValues() {
		return new ArrayList<Value>();
	}

}