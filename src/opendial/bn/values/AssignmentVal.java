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

import opendial.bn.Assignment;


/**
 * Representation of a assignment value.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
 public final class AssignmentVal implements Value {
	
	 // the boolean
	Assignment a;
	
	/**
	 * Creates the assignment value
	 * (protected, use the ValueFactory to create it)
	 * 
	 * @param a the assignment
	 */
	public AssignmentVal(Assignment a) { this.a = a; };
	
	/**
	 * Returns the hashcode of the assignment
	 *
	 * @return hashcode
	 */
	@Override
	public int hashCode() { return a.hashCode(); }
	
	/**
	 * Returns true if the assignment value is similar, false otherwise
	 *
	 * @param o the value to compare
	 * @return true if similar, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return (o instanceof AssignmentVal && ((Assignment)o) == getAssignment());
	}
	
	/**
	 * Returns the assignment value 
	 * 
	 * @return
	 */
	public Assignment getAssignment() {return a; }
	
	/**
	 * Copies the boolean value
	 *
	 * @return
	 */
	@Override
	public AssignmentVal copy() { return new AssignmentVal(a); }
	
	/**
	 * Returns a string representation of the boolean value
	 *
	 * @return
	 */
	@Override
	public String toString() { return"Assign("+a.toString()+")";}

	 
	/**
	 * Compares the boolean to another value, based on the assignment string
	 * 
	 * @return usual ordering, or hashcode difference if the value is not an assignment
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof AssignmentVal) {
			return (new Assignment(a)).toString().compareTo(((AssignmentVal)o).getAssignment().toString());
		}
		else {
			return hashCode() - o.hashCode();
		}
	}
	
}