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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import opendial.arch.Logger;


/**
 * Value that is defined as an ordered list of values.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class ListVal implements Value {
	
	 // logger
	 public static Logger log = new Logger("ListVal", Logger.Level.DEBUG);

	 
	// the list of values
	final List<Value> list;
	
	/**
	 * Creates the list of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected ListVal(Collection<Value> values) {
		this.list = new ArrayList<Value>(); 
		for (Value v : values) {
			if (v instanceof ListVal) {
				this.list.addAll(((ListVal)v).getList());
			}
			else {
				this.list.add(v);
			}
		}
		};

	/**
	 * Creates the list of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected ListVal(Value...values) { this(Arrays.asList(values)) ;};
	
	
	/**
	 * Returns the hashcode for the list
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return list.hashCode(); }
	
	/**
	 * Returns true if the lists are equals (contain the same elements), false
	 * otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return ((o instanceof ListVal && ((ListVal)o).getList().equals(getList())));
	}
	
	
	/**
	 * Returns the list of values
	 *  
	 * @return the list
	 */
	public List<Value> getList() {return list; }
	
	/**
	 * Returns a copy of the list
	 *
	 * @return the copy
	 */
	@Override
	public ListVal copy() { return new ListVal(list); }
	
	/**
	 * Returns a string representation of the list
	 *
	 * @return the string
	 */
	@Override
	public String toString() { 
		return ""+list.toString();
	}

	/**
	 * Concatenates the two lists. 
	 */
	@Override
	public Value concatenate (Value v) {
		if (v instanceof ListVal) {
			List<Value> newList = new ArrayList<Value>(list);
			newList.addAll(((ListVal)v).getList());
			return new ListVal(newList);
		}
		else if (v instanceof NoneVal) {
			return this;
		}
		else {
			log.warning("cannot concatenate " + this + " and " +  v);
			return ValueFactory.noneValue;
		}
	}
	
	/**
	 * Adds all the values in the given ListVal to this value
	 * 
	 * @param values the ListVal with the values to add
	 */
	public void addAll(ListVal values) {
		list.addAll(values.getList());
	}
	
	public void removeAll(Collection<Value> discardValues) {
		list.removeAll(discardValues);
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

	public void remove(Value object) {
		list.remove(object);
	}

	public void add(Value object) {
		list.add(object);
	}

	/**
	 * Returns true if subvalue is contained, and false otherwise
	 * 
	 * @return true if contained, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		return list.contains(subvalue);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}


	
}