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
 * @version $Date::                      $
 *
 */
public final class SetVal implements Value {
	
	// the set of values
	Set<Value> set;
	
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
	public String toString() { return ""+set; }

	
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


	
}