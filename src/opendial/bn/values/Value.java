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

import java.util.Collection;

/**
 * Generic class for a variable value. The value can be:
 * <ol>
 * <li>compared to other values
 * <li>copied in a new value
 * <li>check if it contains a sub-value
 * <li>concatenated with another value.
 * </ol>
 * .
 * 
 * <p>
 * <b>IMPORTANT</b>: all implementations of Value <i>must</i> implement the three
 * core methods equals(Object o), toString() and hashCode().
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public interface Value extends Comparable<Value> {

	/**
	 * Copies the value
	 * 
	 * @return the value
	 */
	public Value copy();

	/**
	 * Return true if the value contains the value given as argument
	 * 
	 * @param subvalue the value to check for inclusion
	 * @return true if the value is contained, false otherwise
	 */
	public boolean contains(Value subvalue);

	/**
	 * If the value is a container for other values, returns the collection of
	 * contained values. Else, returns an empty set.
	 * 
	 * @return the collection of values inside the present value
	 */
	public Collection<Value> getSubValues();

	/**
	 * Returns a value that is the concatenation of the two values
	 * 
	 * @param value the value to concatenate with the current one
	 * @return the concatenated result
	 */
	public Value concatenate(Value value);

	/**
	 * Returns the length of the value
	 * 
	 * @return the value length
	 */
	public int length();

	/**
	 * Returns the hash code for the value
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode();

	/**
	 * Returns the string representation of the value
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString();

	/**
	 * Returns true if o and the current object are equal, and false otherwise
	 * 
	 * @param o the other to compare
	 * @return true if this==o, false otherwise
	 */
	@Override
	public boolean equals(Object o);

}
