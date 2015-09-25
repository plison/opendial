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
import java.util.logging.*;

import opendial.utils.StringUtils;

/**
 * Representation of a double value
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */

public final class DoubleVal implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the double
	final double d;
	final int hashcode;

	/**
	 * Creates the double value (protected, use the ValueFactory instead)
	 * 
	 * @param d the double
	 */
	protected DoubleVal(double d) {
		this.d = d;
		hashcode = new Double(d).hashCode();
	};

	/**
	 * Returns the hashcode for the double
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Returns true if the objects are similar, false otherwise
	 *
	 * @param o the object to compare
	 * @return true if similar, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		boolean result = (o instanceof DoubleVal
				&& Math.abs(((DoubleVal) o).getDouble() - getDouble()) < 0.000001);
		return result;
	}

	/**
	 * Returns the double value
	 * 
	 * @return the double value
	 */
	public Double getDouble() {
		return d;
	}

	/**
	 * Returns a copy of the double value
	 *
	 * @return the copy
	 */
	@Override
	public DoubleVal copy() {
		return new DoubleVal(d);
	}

	/**
	 * Returns a string representation of the double
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "" + StringUtils.getShortForm(d);
	}

	/**
	 * Returns 1
	 * 
	 * @return 1
	 */
	@Override
	public int length() {
		return 1;
	}

	/**
	 * If v is a DoubleVal, returns the addition of the two values. If v is a
	 * StringVal, returns the string concatenation of the two. Else, returns none.
	 */
	@Override
	public Value concatenate(Value v) {
		if (v instanceof DoubleVal) {
			return new DoubleVal(d + ((DoubleVal) v).getDouble());
		}
		else if (v instanceof StringVal) {
			return ValueFactory.create(toString() + " " + v.toString());
		}
		else if (v instanceof NoneVal) {
			return this;
		}
		else {
			log.warning("cannot concatenate " + this + " and " + v);
			return ValueFactory.noneValue;
		}
	}

	/**
	 * Compares the double value to another value
	 * 
	 * @return usual ordering, or hashcode difference if the value is not a double
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof DoubleVal) {
			return Double.compare(d, ((DoubleVal) o).getDouble());
		}
		else {
			return 0;
		}
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean contains(Value subvalue) {
		return equals(subvalue);
	}

	/**
	 * Returns an empty list
	 */
	@Override
	public Collection<Value> getSubValues() {
		return new ArrayList<Value>();
	}

}