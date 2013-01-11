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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Value that is defined as a list of values (with a specific ordering).
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public final class ListVal implements Value {
	
	// the set of values
	List<Value> list;
	
	/**
	 * Creates the list of values
	 * (protected, should be created via ValueFactory)
	 * 
	 * @param values the values
	 */
	protected ListVal(Collection<Value> values) { this.list = new ArrayList<Value>(values); };

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
	 * Returns true if the list are equals (contain the same elements in the same order), 
	 * and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return (o instanceof ListVal && ((ListVal)o).getList().equals(getList()));
	}
	
	
	/**
	 * Returns the list of values
	 *  
	 * @return the set
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
	public String toString() { return ""+list; }

	
	/**
	 * Adds all the values in the given List to this value
	 * 
	 * @param values the setVal with the values to add
	 */
	public void addAll(ListVal values) {
		list.addAll(values.getList());
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
	
}