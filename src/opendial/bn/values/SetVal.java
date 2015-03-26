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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Value that is defined as a set of values (with no duplicate elements).
 * Note that the set if not sorted.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 *
 */
public final class SetVal implements Value {
	
	// the set of values
	final Set<Value> set;
	
	/**
	 * Creates the set of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected SetVal(Collection<Value> values) { this.set = new HashSet<Value>(values); };

	/**
	 * Creates the set of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected SetVal(Value...values) { this(Arrays.asList(values)) ;};
	
	
	/**
	 * Returns the hashcode for the set
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return set.hashCode(); }
	
	/**
	 * Returns true if the sets are equals (contain the same elements), false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return ((o instanceof SetVal && ((SetVal)o).getSet().equals(getSet())));
	}
	
	
	/**
	 * Returns the set of values
	 *  
	 * @return the set
	 */
	public Set<Value> getSet() {return set; }
	
	/**
	 * Returns a copy of the set
	 *
	 * @return the copy
	 */
	@Override
	public SetVal copy() { return new SetVal(set); }
	
	/**
	 * Returns a string representation of the set
	 *
	 * @return the string
	 */
	@Override
	public String toString() { 
		return ""+set.toString();
	}

	
	/**
	 * Adds all the values in the given SetVal to this value
	 * 
	 * @param values the setVal with the values to add
	 */
	public void addAll(SetVal values) {
		set.addAll(values.getSet());
	}
	
	public void removeAll(Set<Value> discardValues) {
		set.removeAll(discardValues);
	}
	
	/**
	 * Compares the set value to another value
	 * 
	 * @return hashcode difference
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	public void remove(Value object) {
		set.remove(object);
	}

	public void add(Value object) {
		set.add(object);
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


	
}