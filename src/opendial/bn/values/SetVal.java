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

import java.util.logging.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Value that is defined as a set of values.
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class SetVal implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the set of values
	final Set<Value> set;
	final int hashcode;

	/**
	 * Creates the list of values (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected SetVal(Collection<Value> values) {
		this.set = new LinkedHashSet<Value>();
		for (Value v : values) {
			if (v instanceof SetVal) {
				this.set.addAll(((SetVal) v).getSubValues());
			}
			else {
				this.set.add(v);
			}
		}
		hashcode = set.hashCode();
	};

	/**
	 * Creates the set of values (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected SetVal(Value... values) {
		this(Arrays.asList(values));
	};

	/**
	 * Returns the hashcode for the list
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Returns true if the lists are equals (contain the same elements), false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		return ((o instanceof SetVal
				&& ((SetVal) o).getSubValues().equals(getSubValues())));
	}

	/**
	 * Returns the set length
	 * 
	 * @return the length
	 */
	@Override
	public int length() {
		return set.size();
	}

	/**
	 * Returns the set of values
	 * 
	 * @return the set
	 */
	@Override
	public Set<Value> getSubValues() {
		return set;
	}

	/**
	 * Returns a copy of the list
	 *
	 * @return the copy
	 */
	@Override
	public SetVal copy() {
		return new SetVal(set);
	}

	/**
	 * Returns a string representation of the set
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "" + set.toString();
	}

	/**
	 * Concatenates the two sets.
	 */
	@Override
	public Value concatenate(Value v) {
		if (v instanceof SetVal) {
			Set<Value> newSet = new LinkedHashSet<Value>(set);
			newSet.addAll(((SetVal) v).getSubValues());
			return new SetVal(newSet);
		}
		else if (v instanceof NoneVal) {
			return this;
		}
		else {
			Set<Value> newSet = new LinkedHashSet<Value>(set);
			newSet.add(v);
			return new SetVal(newSet);
		}
	}

	/**
	 * Compares the list value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	/**
	 * Returns true if subvalue is contained, and false otherwise
	 * 
	 * @return true if contained, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		return set.contains(subvalue);
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

}